package com.wenjunhuang.codeepiphany.hackerrank.ui

enum HackerRankTableColumnTitle(val title: String) {
  case Status      extends HackerRankTableColumnTitle("Status")
  case Title       extends HackerRankTableColumnTitle("Title")
  case Difficulty  extends HackerRankTableColumnTitle("Difficulty")
  case MaxScore    extends HackerRankTableColumnTitle("Max Score")
  case SuccessRate extends HackerRankTableColumnTitle("Success Rate")
}
