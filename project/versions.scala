import org.jetbrains.sbtidea.IntelliJPlatform

object versions {
  val intellijPlatform: IntelliJPlatform = IntelliJPlatform.IdeaCommunity
  val intellijBuild233                   = "233.11799.241"
  val intellijBuild241                   = "241.14494.240"
  var intellijBuild252                   = "252.13776.59"


  def getBuildPart(build: String): (String, String, String) = {
    if (build == intellijBuild233) {
      (intellijBuild233, intellijBuild233, intellijBuild241)
    } else if (build == intellijBuild241) {
      (intellijBuild241, intellijBuild241, "251.*")
    } else if (build == intellijBuild252) {
      (intellijBuild252, intellijBuild252, null)
    } else {
      throw new IllegalArgumentException(s"Unsupported IntelliJ build: $build")
    }
  }
}
