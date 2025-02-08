import sbtjooq.codegen.CodegenMode.Unmanaged
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import scala.io.Source
import scala.util.Using

ThisBuild / scalaVersion     := "3.3.4"
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
    version      := "0.7.0",
    compileOrder := CompileOrder.Mixed,
    scalacOptions ++= Seq(
      "-Wunused:imports",
      "-language:implicitConversions",
      "-Ykind-projector:underscores",
      "-source:future", // enabling better-monadic-for syntax
      "-feature",
      "-deprecation",
      "-Xmax-inlines:100"
    ),
    intellijAttachSources          := true,
    instrumentThreadingAnnotations := true,
    bundleScalaLibrary             := true,
    intellijVMOptions := intellijVMOptions.value.copy(
      xmx = 2048,
      xms = 256,
      defaultOptions =
        intellijVMOptions.value.defaultOptions ++ Seq("--add-opens=java.management/sun.management=ALL-UNNAMED")
    ),
    patchPluginXml := pluginXmlOptions { xml =>
      xml.version = version.value
      xml.sinceBuild = intellijBuild.value
      xml.changeNotes = s"<![CDATA[${markdownToHtml(baseDirectory.value / "CHANGELOG.md")}]]>"
      xml.pluginDescription = s"<![CDATA[${markdownToHtml(baseDirectory.value / "README_en.md")}]]>"
    },
    libraryDependencies ++= Seq(
      "org.typelevel"           %% "cats-effect"          % "3.5.7",
      "org.typelevel"           %% "cats-core"            % "2.13.0",
      "org.typelevel"           %% "cats-mtl"             % "1.5.0",
      "io.circe"                %% "circe-core"           % "0.14.10",
      "io.circe"                %% "circe-generic"        % "0.14.10",
      "io.circe"                %% "circe-parser"         % "0.14.10",
      "io.circe"                %% "circe-optics"         % "0.15.0",
      "co.fs2"                  %% "fs2-core"             % "3.11.0",
      "dev.optics"              %% "monocle-core"         % "3.3.0",
      "dev.optics"              %% "monocle-macro"        % "3.3.0",
      "org.typelevel"           %% "log4cats-core"        % "2.7.0",
      "org.typelevel"           %% "case-insensitive"     % "1.4.2",
      "org.http4s"              %% "http4s-client"        % "0.23.30",
      "org.http4s"              %% "http4s-dsl"           % "0.23.30",
      "org.http4s"              %% "http4s-circe"         % "0.23.30",
      "com.squareup.okhttp3"     % "okhttp"               % "4.12.0",
      "org.jsoup"                % "jsoup"                % "1.18.3",
      "com.softwaremill.common" %% "id-generator"         % "1.4.0",
      "io.monix"                %% "newtypes-core"        % "0.3.0",
      "io.monix"                %% "newtypes-circe-v0-14" % "0.3.0",
      // add jooq and sqlite,
      "org.jooq"            % "jooq"             % "3.19.18",
      "org.reactivestreams" % "reactive-streams" % "1.0.4",
      "io.r2dbc"            % "r2dbc-spi"        % "1.0.0.RELEASE",
      "org.xerial"          % "sqlite-jdbc"      % "3.48.0.0",
      "org.jooq"            % "jooq-meta"        % "3.19.18"  % JooqCodegen,
      "org.jooq"            % "jooq-codegen"     % "3.19.18"  % JooqCodegen,
      "org.xerial"          % "sqlite-jdbc"      % "3.48.0.0" % JooqCodegen,
      "org.flywaydb"        % "flyway-core"      % "11.3.0",
      "com.zaxxer"          % "HikariCP"         % "6.2.1",
      "org.scalatest"      %% "scalatest"        % "3.2.19"   % Test,
      "junit"               % "junit"            % "4.13.2"   % Test,
      "org.hamcrest"        % "hamcrest"         % "3.0"      % Test,
      "com.novocode"        % "junit-interface"  % "0.11"     % Test,
      "org.opentest4j"      % "opentest4j"       % "1.3.0"    % Test
    ).map(
      _.exclude("org.slf4j", "*")
        .exclude("org.typelevel", "log4cats-slf4j_3")
        .exclude("org.jetbrains.kotlin", "*")
    ),
    // copy all graphql files in src/main/scala to target when compile

    Compile / unmanagedSourceDirectories += baseDirectory.value / "gen",
    Compile / unmanagedSourceDirectories += baseDirectory.value / "src" / "main" / "jooq-generated",
    Compile / unmanagedSourceDirectories += baseDirectory.value / "src" / "main" / "refactor",
    Test / managedResourceDirectories += baseDirectory.value / "testResources",

    // jooq
    jooqVersion       := "3.19.18",
    jooqCodegenConfig := file("jooq-codegen.xml"),
    jooqCodegenMode   := Unmanaged
  )
  .enablePlugins(SbtIdeaPlugin, JooqCodegenPlugin)
