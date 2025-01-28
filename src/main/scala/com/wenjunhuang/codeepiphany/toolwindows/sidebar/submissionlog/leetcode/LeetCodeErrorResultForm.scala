package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.leetcode

import java.awt.BorderLayout
import javax.swing.JComponent

import com.intellij.ui.components.JBTextArea
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI

import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.leetcode.LeetCodeErrorResultForm.{BACKGROUND, FOREGROUND}
import com.wenjunhuang.codeepiphany.utils.ui.BackgroundRoundedPanel

class LeetCodeErrorResultForm(errorText: String) {
  private val myErrorPane = BackgroundRoundedPanel(12, BorderLayout())
  private val myLabel     = new JBTextArea(errorText)

  myLabel.setMinimumSize(JBUI.emptySize())
  myLabel.setLineWrap(true)
  myLabel.setForeground(FOREGROUND)
  myLabel.setOpaque(false)
  myErrorPane.setBackground(BACKGROUND)
  myErrorPane.setBorder(JBUI.Borders.empty(5))
  myErrorPane.add(myLabel,BorderLayout.CENTER)

  def getComponent: JComponent = myErrorPane
}
object LeetCodeErrorResultForm {
  private val FOREGROUND = JBColor(0xeb353a, 0xee615e)
  private val BACKGROUND = JBColor(0xfdeff0, 0x362b2a)
}
