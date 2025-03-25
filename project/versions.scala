import org.jetbrains.sbtidea.IntelliJPlatform

object versions {
  val intellijPlatform: IntelliJPlatform = IntelliJPlatform.IdeaCommunity
  val intellijBuild233                   = "233.11799.241"
  val intellijBuild241                   = "241.14494.240"
  val intellijBuild242                   = "242.20224.300"
  val intellijBuild243                   = "243.24978.46"

  val intellijBuild: String = intellijBuild233
  val sinceBuild: String    = intellijBuild233
  val untilBuild: String    = intellijBuild241
  val pluginVersion: String = "1.0.0"

  //  val intellijPlatform: IntelliJPlatform = CLion
//  val intellijPlatform: IntelliJPlatform = IntelliJPlatform.IdeaUltimate
}
