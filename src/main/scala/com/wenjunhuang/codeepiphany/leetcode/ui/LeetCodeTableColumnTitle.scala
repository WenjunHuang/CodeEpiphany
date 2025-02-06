package com.wenjunhuang.codeepiphany.leetcode.ui

enum LeetCodeTableColumnTitle(val title: String) {
  case Status     extends LeetCodeTableColumnTitle("Status")
  case Title      extends LeetCodeTableColumnTitle("Title")
  case Solution   extends LeetCodeTableColumnTitle("Solution")
  case Difficulty extends LeetCodeTableColumnTitle("Difficulty")
  case Acceptance extends LeetCodeTableColumnTitle("Acceptance")
  case Frequency  extends LeetCodeTableColumnTitle("Frequency")
}
