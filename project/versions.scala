import org.jetbrains.sbtidea.IntelliJPlatform

object versions {
  val intellijPlatform: IntelliJPlatform = IntelliJPlatform.IdeaCommunity
  val intellijBuild241                   = "241.14494.240"
  val intellijBuild242                   = "242.20224.300"
  val intellijBuild233                   = "233.11799.241"
  val intellijBuild243                   = "243.24978.46"

  val intellijBuild: String = intellijBuild233
  val sinceBuild: String    = intellijBuild233
  val untilBuild: String    = null
  val pluginVersion:String = "0.9.1"

  //  val intellijPlatform: IntelliJPlatform = CLion
//  val intellijPlatform: IntelliJPlatform = IntelliJPlatform.IdeaUltimate
}
