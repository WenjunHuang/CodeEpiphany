val requestedVersion = sys.props.get("intellij.version")
if (requestedVersion.contains("252")) {
  addSbtPlugin("org.jetbrains" % "sbt-idea-plugin" % "4.1.17")
} else {
  addSbtPlugin("org.jetbrains" % "sbt-idea-plugin" % "3.26.2")
}

addSbtPlugin("com.github.kxbmap" % "sbt-jooq-codegen" % "0.7.1")
