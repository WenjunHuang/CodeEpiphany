package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.leetcode

import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.wenjunhuang.codeepiphany.utils.ColorUtils
import com.wenjunhuang.codeepiphany.utils.ui.BackgroundRoundedPanel

import java.awt.BorderLayout
import javax.swing.JComponent

class LeetCodeErrorResultForm(errorText: String) {
  private val myErrorPane = BackgroundRoundedPanel(12, BorderLayout())
  private val myLabel     = new JBTextArea(errorText)

  myLabel.setMinimumSize(JBUI.emptySize())
  myLabel.setLineWrap(true)
  myLabel.setForeground(ColorUtils.ERROR_FOREGROUND)
  myLabel.setOpaque(false)
  myErrorPane.setBackground(ColorUtils.ERROR_BACKGROUND)
  myErrorPane.setBorder(JBUI.Borders.empty(5))
  myErrorPane.add(myLabel, BorderLayout.CENTER)

  def getComponent: JComponent = myErrorPane
}

object LeetCodeErrorResultForm {}
