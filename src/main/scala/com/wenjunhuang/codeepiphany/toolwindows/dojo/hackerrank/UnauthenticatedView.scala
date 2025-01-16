package com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank

import javax.swing.SwingConstants

import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBLabel

class UnauthenticatedView extends SimpleToolWindowPanel(true, true) {
  val myLabel = JBLabel("Please login to HackerRank")
  myLabel.setHorizontalAlignment(SwingConstants.CENTER)
  myLabel.setVerticalAlignment(SwingConstants.CENTER)
  setContent(myLabel)
}
