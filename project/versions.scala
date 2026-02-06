import org.jetbrains.sbtidea.IntelliJPlatform

object versions {
  val platform: Option[String] = sys.props.get("intellij.platform")
  lazy val intellijPlatform: IntelliJPlatform =
    platform match {
      case Some("clion") => IntelliJPlatform.CLion
      case _             => IntelliJPlatform.IdeaUltimate
    }
  val intellijBuild233 = "233.11799.241"
  val intellijBuild241 = "241.14494.240"
  lazy val intellijBuild252: String = {
    if (intellijPlatform == IntelliJPlatform.CLion)
      "2025.2.6"
    else "252.23892.409"
  }

  def getBuildPart(build: String): (String, String, String) = {
    if (build == intellijBuild233) {
      (intellijBuild233, "233.0", "233.*")
    } else if (build == intellijBuild241) {
      (intellijBuild241, "241.0", "251.*")
    } else if (build == intellijBuild252) {
      (intellijBuild252, "252.0", null)
    } else {
      throw new IllegalArgumentException(s"Unsupported IntelliJ build: $build")
    }
  }
}
