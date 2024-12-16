package com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank

import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBLabel

import javax.swing.SwingConstants

class UnauthenticatedView extends SimpleToolWindowPanel(true, true) {
  val myLabel = JBLabel("Please login to HackerRank")
  myLabel.setHorizontalAlignment(SwingConstants.CENTER)
  myLabel.setVerticalAlignment(SwingConstants.CENTER)
  setContent(myLabel)
}
