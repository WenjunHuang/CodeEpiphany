package com.wenjunhuang.codeepiphany

import com.intellij.openapi.options.Configurable

import javax.swing.{ JComponent, JPanel }

class CodeEpiphanyConfigurable extends Configurable {
  override def getDisplayName: String = "Code Epiphany"

  override def createComponent(): JComponent = JPanel()

  override def isModified: Boolean = false

  override def apply(): Unit = {}
}
