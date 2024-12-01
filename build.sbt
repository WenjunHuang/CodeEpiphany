import sbt.Keys.libraryDependencies

import scala.collection.Seq

ThisBuild / scalaVersion     := "3.5.2"
ThisBuild / intellijPlatform := IntelliJPlatform.IdeaCommunity
ThisBuild / intellijBuild    := "242.20224.300"

lazy val codeEpiphany = (project in file("."))
  .settings(
    name                  := "CodeEpiphany",
    version               := "0.1.0",
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
      "org.scalatest" %% "scalatest"   % "3.2.19" % Test,
      "org.typelevel" %% "cats-effect" % "3.5.7",
      "org.typelevel" %% "cats-core"   % "2.12.0",
      // add cats mtl
      "org.typelevel" %% "cats-mtl" % "1.5.0",

      // add jsoup
      "org.jsoup" % "jsoup" % "1.18.2",
      // add circe
      "io.circe" %% "circe-core"           % "0.14.10",
      "io.circe" %% "circe-generic"        % "0.14.10",
      "io.circe" %% "circe-parser"         % "0.14.10",
      "io.circe" %% "circe-optics"         % "0.15.0",

      // add fs2
      "co.fs2" %% "fs2-core" % "3.11.0",
      //        "io.circe" %% "circe-generic-extras" % "0.14.10",
      // add redis4cats
      "dev.profunktor" %% "redis4cats-effects" % "1.7.1",
      // ad monocle
      "dev.optics" %% "monocle-core" % "3.3.0",
      // add cats retry
      "com.github.cb372" %% "cats-retry" % "3.1.3",
      // add log4cats
      "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
      // add http4s client
      "org.http4s" %% "http4s-client" % "0.23.29",
      "org.http4s" %% "http4s-dsl"    % "0.23.29",
      "org.http4s" %% "http4s-circe"  % "0.23.29",

      // add ok http
      "com.squareup.okhttp3" % "okhttp" % "4.12.0"
    ),
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Test / unmanagedResourceDirectories += baseDirectory.value / "testResources"
  )
  .enablePlugins(SbtIdeaPlugin)
