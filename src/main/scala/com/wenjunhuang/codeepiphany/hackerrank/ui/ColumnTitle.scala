package com.wenjunhuang.codeepiphany.hackerrank.ui

enum ColumnTitle(val title: String) {
  case Status extends ColumnTitle("Status")
  case Title extends ColumnTitle("Title")
  case Difficulty extends ColumnTitle("Difficulty")
  case MaxScore extends ColumnTitle("Max Score")
  case SuccessRate extends ColumnTitle("Success Rate")
}
