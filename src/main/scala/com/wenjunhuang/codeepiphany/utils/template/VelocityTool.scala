package com.wenjunhuang.codeepiphany.utils.template

object VelocityTool {
  // split str with any space, hyphen , capitalize each word, and join them with no space
  def camelCaseName(str: String): String = str match
    case null => ""
    case _    => str.split("[\\s-_]").map(_.capitalize).mkString("")

  // split str with any space, hyphen , capitalize each word, and join them with no space
  def smallCamelCaseName(str: String): String = str match
    case null => ""
    case _ =>
      str.split("[\\s-_]").map(_.capitalize).mkString("") match
        case "" => ""
        case s  => s.head.toLower + s.tail

  // change from camel case to snake case
  def snakeCaseName(str: String): String = str match
    case null => ""
    case _    => str.split("(?=[A-Z])").map(_.toLowerCase).mkString("_")
}
