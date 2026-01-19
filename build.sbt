import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import sbtjooq.codegen.CodegenMode.Unmanaged

import java.io.File
import scala.io.Source
import scala.sys.process.*
import scala.util.Using

val pluginVersion: String = "1.11.0"

ThisBuild / scalaVersion     := "3.7.0"
ThisBuild / intellijPlatform := versions.intellijPlatform

// 动态设置IntelliJ版本
ThisBuild / intellijBuild := {
  val requestedVersion = sys.props.get("intellij.version").orElse(sys.env.get("INTELLIJ_VERSION"))

  val selectedBuild = requestedVersion match {
    case Some("233") =>
      println(s"Using IntelliJ version 233 (${versions.intellijBuild233})")
      versions.intellijBuild233
    case Some("241") =>
      println(s"Using IntelliJ version 241 (${versions.intellijBuild241})")
      versions.intellijBuild241
    case Some("252") =>
      println(s"Using IntelliJ version 252 (${versions.intellijBuild252})")
      versions.intellijBuild252
    case Some(other) =>
      println(s"Warning: Unknown IntelliJ version '$other', falling back to default (252)")
      versions.intellijBuild252
    case None =>
      println(s"No IntelliJ version specified, using default (252: ${versions.intellijBuild252})")
      versions.intellijBuild252
  }

  selectedBuild
}

def markdownToHtml(file: File): String = {
  val options  = new MutableDataSet()
  val parser   = Parser.builder(options).build()
  val renderer = HtmlRenderer.builder(options).build()

  Using(Source.fromFile(file)) { markdownSource =>
    val document = parser.parse(markdownSource.mkString)
    renderer.render(document)
  }.get
}

// Custom task to build webview with npm
lazy val buildWebview = taskKey[Unit]("Build webview using npm")

buildWebview := {
  val log        = streams.value.log
  val webviewDir = baseDirectory.value / "webview"
  val targetDir  = target.value / "webviewResources" / "webview"

  log.info("Building webview with npm...")

  // Check if package.json exists
  if (!(webviewDir / "package.json").exists()) {
    log.warn(s"package.json not found in ${webviewDir.getAbsolutePath}")
  } else {
    // Determine npm command based on OS
    val npmCmd = if (System.getProperty("os.name").toLowerCase.contains("windows")) "npm.cmd" else "npm"

    // Check if node_modules exists, if not run npm install
    if (!(webviewDir / "node_modules").exists()) {
      log.info("node_modules not found, running npm install...")
      val installResult = Process(s"$npmCmd install", webviewDir).!
      if (installResult != 0) {
        throw new MessageOnlyException("npm install failed")
      }
    }

    // Run npm run build
    val buildResult = Process(s"$npmCmd run build", webviewDir).!
    if (buildResult != 0) {
      throw new MessageOnlyException("npm run build failed")
    }

    // Verify that build output exists
    if (targetDir.exists()) {
      log.info(s"Webview build output found at ${targetDir.getAbsolutePath}")
    } else {
      log.warn(s"Build output directory ${targetDir.getAbsolutePath} not found")
    }

    log.info("Webview build completed successfully")
  }
}

lazy val codeEpiphany = (project in file("."))
  .settings(
    name         := "CodeEpiphany",
    version      := s"$pluginVersion-${VersionNumber((ThisBuild / intellijBuild).value)._1.get}",
    compileOrder := CompileOrder.Mixed,
    fork         := true,
    scalacOptions ++= Seq(
      "-Wunused:imports",
      "-language:implicitConversions",
      "-Xkind-projector:underscores",
      "-source:future", // enabling better-monadic-for syntax
      "-feature",
      "-deprecation",
      "-Xmax-inlines:100",
      "-explain-cyclic"
    ),
    intellijAttachSources          := true,
    instrumentThreadingAnnotations := true,
    bundleScalaLibrary             := true,
    intellijVMOptions := intellijVMOptions.value.copy(
      xmx = 2048,
      xms = 256,
      defaultOptions = intellijVMOptions.value.defaultOptions ++ Seq(
        "--add-opens=java.management/sun.management=ALL-UNNAMED",
        "--add-opens=java.desktop/javax.swing.text=ALL-UNNAMED",
        "--add-opens=java.desktop/javax.swing.text.html.parser=ALL-UNNAMED"
      )
    ),
    patchPluginXml := pluginXmlOptions { xml =>
      val currentBuild                = (ThisBuild / intellijBuild).value
      val (_, sinceBuild, untilBuild) = versions.getBuildPart(currentBuild)

      xml.version = version.value
      xml.sinceBuild = sinceBuild
      xml.untilBuild = untilBuild
      xml.changeNotes = s"<![CDATA[${markdownToHtml(baseDirectory.value / "CHANGELOG.md")}]]>"
      xml.pluginDescription = s"<![CDATA[${markdownToHtml(baseDirectory.value / "DESCRIPTION.md")}]]>"
    },
    // Make buildWebview and generateBuildConfig run before compile
    Compile / unmanagedResources := (Compile / unmanagedResources).dependsOn(buildWebview).value,
    Compile / unmanagedSourceDirectories += baseDirectory.value / "gen",
    // Compile / unmanagedSourceDirectories += target.value / "gen",
    Compile / unmanagedSourceDirectories += baseDirectory.value / "src" / "main" / "jooq-generated",
    Compile / unmanagedResourceDirectories += target.value / "webviewResources",
    Compile / unmanagedSourceDirectories ++= {
      if (
        VersionNumber((ThisBuild / intellijBuild).value)
          .matchesSemVer(SemanticSelector(s">=${versions.intellijBuild252}"))
      ) {
        Seq(baseDirectory.value / "src" / "main" / "252")
      } else if (
        VersionNumber((ThisBuild / intellijBuild).value)
          .matchesSemVer(SemanticSelector(s">=${versions.intellijBuild241}"))
      ) {
        Seq(baseDirectory.value / "src" / "main" / "241")
      } else {
        Seq(baseDirectory.value / "src" / "main" / "233")
      }
    },
    // 常规测试配置 - 排除集成测试
    Test / managedResourceDirectories += baseDirectory.value / "testResources",
    Test / testOptions += Tests.Filter(name => !name.startsWith("integration")),

    // jooq
    jooqVersion       := "3.19.18",
    jooqCodegenConfig := file("jooq-codegen.xml"),
    jooqCodegenMode   := Unmanaged,
    libraryDependencies ++= Seq(
      "org.typelevel"           %% "cats-effect"              % "3.6.3",
      "org.typelevel"           %% "cats-core"                % "2.13.0",
      "org.typelevel"           %% "cats-mtl"                 % "1.6.0",
      "io.circe"                %% "circe-core"               % "0.14.15",
      "io.circe"                %% "circe-generic"            % "0.14.15",
      "io.circe"                %% "circe-parser"             % "0.14.15",
      "io.circe"                %% "circe-optics"             % "0.15.1",
      "co.fs2"                  %% "fs2-core"                 % "3.12.2",
      "dev.optics"              %% "monocle-core"             % "3.3.0",
      "dev.optics"              %% "monocle-macro"            % "3.3.0",
      "org.typelevel"           %% "log4cats-core"            % "2.7.1",
      "org.typelevel"           %% "case-insensitive"         % "1.5.0",
      "org.http4s"              %% "http4s-client"            % "0.23.33",
      "org.http4s"              %% "http4s-dsl"               % "0.23.33",
      "org.http4s"              %% "http4s-circe"             % "0.23.33",
      "com.squareup.okhttp3"     % "okhttp-jvm"               % "5.3.2",
      "org.jsoup"                % "jsoup"                    % "1.22.1",
      "com.vladsch.flexmark"     % "flexmark"                 % "0.64.8",
      "com.vladsch.flexmark"     % "flexmark-ext-attributes"  % "0.64.8",
      "com.vladsch.flexmark"     % "flexmark-util-data"       % "0.64.8",
      "com.vladsch.flexmark"     % "flexmark-util-ast"        % "0.64.8",
      "com.vladsch.flexmark"     % "flexmark-util-misc"       % "0.64.8",
      "com.vladsch.flexmark"     % "flexmark-util-builder"    % "0.64.8",
      "com.vladsch.flexmark"     % "flexmark-util-sequence"   % "0.64.8",
      "com.vladsch.flexmark"     % "flexmark-util-collection" % "0.64.8",
      "com.vladsch.flexmark"     % "flexmark-util-dependency" % "0.64.8",
      "com.vladsch.flexmark"     % "flexmark-util-format"     % "0.64.8",
      "com.vladsch.flexmark"     % "flexmark-util-html"       % "0.64.8",
      "com.vladsch.flexmark"     % "flexmark-util-options"    % "0.64.8",
      "com.vladsch.flexmark"     % "flexmark-util-visitor"    % "0.64.8",
      "com.softwaremill.common" %% "id-generator"             % "1.4.0",
      "io.monix"                %% "newtypes-core"            % "0.3.0",
      "io.monix"                %% "newtypes-circe-v0-14"     % "0.3.0",
      "com.github.cb372"        %% "cats-retry"               % "4.0.0",
      "com.github.weisj"         % "jsvg"                     % "2.0.0",
      "org.apache.commons"       % "commons-text"             % "1.15.0",
      // add jooq and sqlite,
      "org.jooq"            % "jooq"                          % "3.19.18",
      "org.reactivestreams" % "reactive-streams"              % "1.0.4",
      "io.r2dbc"            % "r2dbc-spi"                     % "1.0.0.RELEASE",
      "org.xerial"          % "sqlite-jdbc"                   % "3.51.1.0",
      "org.jooq"            % "jooq-meta"                     % "3.19.18"  % JooqCodegen,
      "org.jooq"            % "jooq-codegen"                  % "3.19.18"  % JooqCodegen,
      "org.xerial"          % "sqlite-jdbc"                   % "3.50.3.0" % JooqCodegen,
      "org.flywaydb"        % "flyway-core"                   % "11.9.1",
      "com.zaxxer"          % "HikariCP"                      % "7.0.2",
      "org.scalatest"      %% "scalatest"                     % "3.2.19"   % Test,
      "junit"               % "junit"                         % "4.13.2"   % Test,
      "org.hamcrest"        % "hamcrest"                      % "3.0"      % Test,
      "com.novocode"        % "junit-interface"               % "0.11"     % Test,
      "org.opentest4j"      % "opentest4j"                    % "1.3.0"    % Test,
      "org.typelevel"      %% "cats-effect-testing-scalatest" % "1.7.0"    % Test
    ).map(
      _.exclude("org.slf4j", "*")
        .exclude("org.typelevel", "log4cats-slf4j_3")
        .exclude("org.jetbrains.kotlin", "*")
    )
  )
  .enablePlugins(SbtIdeaPlugin, JooqCodegenPlugin)
