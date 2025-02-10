package com.wenjunhuang.codeepiphany.utils.ui

import cats.syntax.all.*
import javax.swing.{ JEditorPane, JLayeredPane, SwingConstants }

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.{ JBLabel, JBLayeredPane }
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.util.ui.{ HTMLEditorKitBuilder, JBUI }
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.xml.util.HtmlUtil

import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.PluginBundle

class UnauthenticatedView(private val myCodeDojo: CodeDojo, private val myTips: Option[String] = None)
    extends SimpleToolWindowPanel(true, true)
    with DumbAware {

  private val tipsLabel = JEditorPane()
  tipsLabel.setFont(JBUI.Fonts.label().biggerOn(1.5))
  tipsLabel.setEditorKit(HTMLEditorKitBuilder.simple());
  tipsLabel.setEditable(false);
  tipsLabel.addHyperlinkListener(BrowserHyperlinkListener())
  tipsLabel.setBackground(null)
  myTips match {
    case None =>
      tipsLabel.setText(s"<p>${PluginBundle.message("unauthenticatedView.message", myCodeDojo.show)}</p>")
    case Some(tips) =>
      tipsLabel.setText(
        s"<p>${PluginBundle.message("unauthenticatedView.message", myCodeDojo.show)}</p><br/><p>$tips</p>"
      )
  }
  setContent(tipsLabel)
}
