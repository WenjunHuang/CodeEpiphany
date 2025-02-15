package com.wenjunhuang.codeepiphany.luogu.ui

enum LuoGuTableColumnTitle(val title: String) {
  case Id         extends LuoGuTableColumnTitle("Id")
  case Title      extends LuoGuTableColumnTitle("Title")
  case Solution   extends LuoGuTableColumnTitle("Solution")
  case Difficulty extends LuoGuTableColumnTitle("Difficulty")
  case Acceptance extends LuoGuTableColumnTitle("Acceptance")
  case Frequency  extends LuoGuTableColumnTitle("Frequency")
}
