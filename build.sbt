import sbt.Keys.libraryDependencies

ThisBuild / scalaVersion     := "3.5.2"
ThisBuild / intellijPlatform := IntelliJPlatform.IdeaCommunity
ThisBuild / intellijBuild    := "242.20224.300"

lazy val codeEpiphany = (project in file("."))
  .settings(
    name    := "CodeEpiphany",
    version := "0.1.0",
    scalacOptions ++= Seq(
      "-Wunused:imports",
      "-source:future",              // enabling better-monadic-for syntax
      "-Xkind-projector:underscores" // enabling underscore type lambdas will disable usage of _ as a wildcard
    ),
    intellijAttachSources := true,
    javacOptions ++= "--release" :: "17" :: Nil,
    instrumentThreadingAnnotations := true,
    bundleScalaLibrary             := true,
    intellijPlugins += "com.intellij.java".toPlugin,
    intellijPlugins += "com.intellij.properties".toPlugin,
    intellijVMOptions := intellijVMOptions.value.copy(xmx = 2048, xms = 256),
    patchPluginXml := pluginXmlOptions { xml =>
      xml.version = version.value
    },
    libraryDependencies ++= Seq(
      "org.typelevel"       %% "cats-effect"      % "3.5.7",
      "org.typelevel"       %% "cats-core"        % "2.12.0",
      "org.typelevel"       %% "cats-mtl"         % "1.5.0",
      "io.circe"            %% "circe-core"       % "0.14.10",
      "io.circe"            %% "circe-generic"    % "0.14.10",
      "io.circe"            %% "circe-parser"     % "0.14.10",
      "io.circe"            %% "circe-optics"     % "0.15.0",
      "co.fs2"              %% "fs2-core"         % "3.11.0",
      "dev.optics"          %% "monocle-core"     % "3.3.0",
      "dev.optics"          %% "monocle-macro"     % "3.3.0",
      "com.github.cb372"    %% "cats-retry"       % "3.1.3",
      "org.typelevel"       %% "log4cats-core"    % "2.7.0",
      "org.typelevel"       %% "case-insensitive" % "1.4.2",
      "org.http4s"          %% "http4s-client"    % "0.23.30",
      "org.http4s"          %% "http4s-dsl"       % "0.23.30",
      "org.http4s"          %% "http4s-circe"     % "0.23.30",
      "com.squareup.okhttp3" % "okhttp"           % "4.12.0",
      "org.jsoup"            % "jsoup"            % "1.18.3",
      "org.scalatest"       %% "scalatest"        % "3.2.19" % Test,
      "com.novocode"         % "junit-interface"  % "0.11"   % Test,
      "org.opentest4j"       % "opentest4j"       % "1.3.0"  % Test
    ),
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Test / unmanagedResourceDirectories += baseDirectory.value / "testResources"
  )
  .enablePlugins(SbtIdeaPlugin)
