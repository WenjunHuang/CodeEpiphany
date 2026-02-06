package com.wenjunhuang.codeepiphany.utils

import java.io.File
import java.nio.charset.Charset

import com.intellij.openapi.util.io.FileUtil

object FileUtils {
  def sanitizeFilename(input: String): String = {
    if (input == null || input.isEmpty) return "unnamed_file"
    // 定义非法字符集（覆盖 Windows/Linux/macOS）
    val illegalChars = "[\\/:*?\"<>|]"
    // 移除非法字符和控制字符（ASCII 0-31）
    val sanitized = input.replaceAll(illegalChars, "").replaceAll("\\p{Cntrl}", "") // 移除控制字符

    // 处理空白文件名
    if (sanitized.trim.isEmpty) return "unnamed_file"
    // 移除首尾空格和点（避免隐藏文件）
    sanitized.trim.replaceAll("^[.]+|[.]+$", "")
  }

  def getJavaMainClassName(filePath: String): String = {
    val content          = FileUtil.loadFile(new File(filePath), Charset.forName("UTF-8"))
    val packagePattern   = """package\s+([\w\.]+);""".r
    val packageName      = packagePattern.findFirstMatchIn(content).map(_.group(1)).getOrElse("")
    val mainClassPattern = """public\s+class\s+(\w+)\s*\{""".r
    val mainClassName = mainClassPattern.findFirstMatchIn(content) match {
      case Some(m) => m.group(1)
      case None    => "Main" // 默认类名
    }

    if (packageName.nonEmpty) s"$packageName.$mainClassName" else mainClassName
  }
}
