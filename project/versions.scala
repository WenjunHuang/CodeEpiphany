import org.jetbrains.sbtidea.IntelliJPlatform

object versions {
  val intellijPlatform: IntelliJPlatform = IntelliJPlatform.IdeaCommunity
  val intellijBuild233                   = "233.11799.241"
  val intellijBuild241                   = "241.14494.240"
  var intellijBuild252                   = "252.23892.409"

  def getBuildPart(build: String): (String, String, String) = {
    if (build == intellijBuild233) {
      (intellijBuild233, "233.0", "233.*")
    } else if (build == intellijBuild241) {
      (intellijBuild241, "241.0", "251.*")
    } else if (build == intellijBuild252) {
      (intellijBuild252, "252.0", "252.*")
    } else {
      throw new IllegalArgumentException(s"Unsupported IntelliJ build: $build")
    }
  }
}
