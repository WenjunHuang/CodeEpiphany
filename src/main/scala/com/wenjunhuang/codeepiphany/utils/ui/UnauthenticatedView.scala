package com.wenjunhuang.codeepiphany.utils.ui

import cats.syntax.all.*
import javax.swing.SwingConstants

import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBLabel

import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.PluginBundle

class UnauthenticatedView(private val myCodeDojo: CodeDojo) extends SimpleToolWindowPanel(true, true) {
  val myLabel = JBLabel(PluginBundle.message("unauthenticatedView.message", myCodeDojo.show))
  myLabel.setHorizontalAlignment(SwingConstants.CENTER)
  myLabel.setVerticalAlignment(SwingConstants.CENTER)
  setContent(myLabel)
}
