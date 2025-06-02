import sbtjooq.codegen.CodegenMode.Unmanaged
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import sbt.librarymanagement.VersionNumber.SemVer
import scala.io.Source
import scala.util.Using

ThisBuild / scalaVersion     := "3.7.0"
ThisBuild / intellijPlatform := versions.intellijPlatform
ThisBuild / intellijBuild    := versions.intellijBuild

def markdownToHtml(file: File): String = {
  val options  = new MutableDataSet()
  val parser   = Parser.builder(options).build()
  val renderer = HtmlRenderer.builder(options).build()

  Using(Source.fromFile(file)) { markdownSource =>
    val document = parser.parse(markdownSource.mkString)
    renderer.render(document)
  }.get
}

lazy val codeEpiphany = (project in file("."))
  .settings(
    name         := "CodeEpiphany",
    version      := s"${versions.pluginVersion}-${VersionNumber(versions.intellijBuild)._1.get}",
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
        "--add-opens=java.desktop/javax.swing.text=ALL-UNNAMED" // apple m4 need this parameter
      )
    ),
    patchPluginXml := pluginXmlOptions { xml =>
      xml.version = version.value
      xml.sinceBuild = versions.sinceBuild
      xml.untilBuild = versions.untilBuild
      xml.changeNotes = s"<![CDATA[${markdownToHtml(baseDirectory.value / "CHANGELOG.md")}]]>"
      xml.pluginDescription = s"<![CDATA[${markdownToHtml(baseDirectory.value / "DESCRIPTION.md")}]]>"
    },
    libraryDependencies ++= Seq(
      // add scala reflect
      "org.typelevel"           %% "cats-effect"              % "3.6.1",
      "org.typelevel"           %% "cats-core"                % "2.13.0",
      "org.typelevel"           %% "cats-mtl"                 % "1.5.0",
      "io.circe"                %% "circe-core"               % "0.14.13",
      "io.circe"                %% "circe-generic"            % "0.14.13",
      "io.circe"                %% "circe-parser"             % "0.14.13",
      "io.circe"                %% "circe-optics"             % "0.15.0",
      "co.fs2"                  %% "fs2-core"                 % "3.12.0",
      "dev.optics"              %% "monocle-core"             % "3.3.0",
      "dev.optics"              %% "monocle-macro"            % "3.3.0",
      "org.typelevel"           %% "log4cats-core"            % "2.7.1",
      "org.typelevel"           %% "case-insensitive"         % "1.5.0",
      "org.http4s"              %% "http4s-client"            % "0.23.30",
      "org.http4s"              %% "http4s-dsl"               % "0.23.30",
      "org.http4s"              %% "http4s-circe"             % "0.23.30",
      "com.squareup.okhttp3"     % "okhttp"                   % "4.12.0",
      "org.jsoup"                % "jsoup"                    % "1.20.1",
      "com.vladsch.flexmark"     % "flexmark"                 % "0.64.8",
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
      // add jooq and sqlite,
      "org.jooq"            % "jooq"                          % "3.19.18",
      "org.reactivestreams" % "reactive-streams"              % "1.0.4",
      "io.r2dbc"            % "r2dbc-spi"                     % "1.0.0.RELEASE",
      "org.xerial"          % "sqlite-jdbc"                   % "3.49.0.0",
      "org.jooq"            % "jooq-meta"                     % "3.19.18"  % JooqCodegen,
      "org.jooq"            % "jooq-codegen"                  % "3.19.18"  % JooqCodegen,
      "org.xerial"          % "sqlite-jdbc"                   % "3.49.0.0" % JooqCodegen,
      "org.flywaydb"        % "flyway-core"                   % "11.3.2",
      "com.zaxxer"          % "HikariCP"                      % "6.3.0",
      "org.scalatest"      %% "scalatest"                     % "3.2.19"   % Test,
      "junit"               % "junit"                         % "4.13.2"   % Test,
      "org.hamcrest"        % "hamcrest"                      % "3.0"      % Test,
      "com.novocode"        % "junit-interface"               % "0.11"     % Test,
      "org.opentest4j"      % "opentest4j"                    % "1.3.0"    % Test,
      "org.typelevel"      %% "cats-effect-testing-scalatest" % "1.6.0"    % Test
    ).map(
      _.exclude("org.slf4j", "*")
        .exclude("org.typelevel", "log4cats-slf4j_3")
        .exclude("org.jetbrains.kotlin", "*")
    ),
    // copy all graphql files in src/main/scala to target when compile
    Compile / unmanagedSourceDirectories += baseDirectory.value / "gen",
    Compile / unmanagedSourceDirectories += baseDirectory.value / "src" / "main" / "jooq-generated",
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
    Compile / unmanagedResourceDirectories += target.value / "webviewResources",
    Test / managedResourceDirectories += baseDirectory.value / "testResources",
    // jooq
    jooqVersion       := "3.19.18",
    jooqCodegenConfig := file("jooq-codegen.xml"),
    jooqCodegenMode   := Unmanaged
  )
  .enablePlugins(SbtIdeaPlugin, JooqCodegenPlugin)
