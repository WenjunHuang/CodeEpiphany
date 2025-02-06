package com.wenjunhuang.codeepiphany.utils.ui

import cats.syntax.all.*
import javax.swing.{JEditorPane, SwingConstants}

import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.util.ui.{HTMLEditorKitBuilder, JBUI}
import com.intellij.util.ui.components.BorderLayoutPanel

import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.PluginBundle

class UnauthenticatedView(private val myCodeDojo: CodeDojo,
                          private val myTips:Option[String] = None) extends SimpleToolWindowPanel(true, true) {
  val myLabel = JBLabel(PluginBundle.message("unauthenticatedView.message", myCodeDojo.show))

  myTips match {
    case None =>
      myLabel.setHorizontalAlignment(SwingConstants.CENTER)
      myLabel.setVerticalAlignment(SwingConstants.CENTER)
      setContent(myLabel)
    case Some(tips) =>
      val tipsLabel = JEditorPane()
      tipsLabel.setFont(JBUI.Fonts.label().biggerOn(1.5))
      tipsLabel.setEditorKit(HTMLEditorKitBuilder.simple());
      tipsLabel.setEditable(false);
      tipsLabel.addHyperlinkListener(BrowserHyperlinkListener())
      tipsLabel.setText(tips)
      val panel = BorderLayoutPanel(0,5)
      panel.setBorder(JBUI.Borders.empty(5))
      panel.addToTop(myLabel)
      panel.addToCenter(tipsLabel)
      setContent(panel)
  }
}
