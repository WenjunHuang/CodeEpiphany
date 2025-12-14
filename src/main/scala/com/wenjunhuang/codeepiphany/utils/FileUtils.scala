package com.wenjunhuang.codeepiphany.utils

object FileUtils {
  def sanitizeFilename(input: String): String = {
    if (input == null || input.isEmpty) return "unnamed_file"
    // 定义非法字符集（覆盖 Windows/Linux/macOS）
    val illegalChars = "[\\\\/:*?\"<>|]"
    // 移除非法字符和控制字符（ASCII 0-31）
    val sanitized = input.replaceAll(illegalChars, "").replaceAll("\\p{Cntrl}", "") // 移除控制字符

    // 处理空白文件名
    if (sanitized.trim.isEmpty) return "unnamed_file"
    // 移除首尾空格和点（避免隐藏文件）
    sanitized.trim.replaceAll("^[.]+|[.]+$", "")
  }
}
