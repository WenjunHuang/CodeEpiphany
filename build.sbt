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
    intellijVMOptions := intellijVMOptions.value.copy(xmx = 2048, xms = 256)
  )
  .enablePlugins(SbtIdeaPlugin)
