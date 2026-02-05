val requestedVersion = sys.props.get("intellij.platform")
if (requestedVersion.contains("clion")) {
  println("Using sbt-idea-plugin 4.1.17 for CLion")
  addSbtPlugin("org.jetbrains" % "sbt-idea-plugin" % "4.1.17")
} else {
  println("Using sbt-idea-plugin 3.26.2 for non-CLion IDEs")
  addSbtPlugin("org.jetbrains" % "sbt-idea-plugin" % "3.26.2")
}

addSbtPlugin("com.github.kxbmap" % "sbt-jooq-codegen" % "0.7.1")
