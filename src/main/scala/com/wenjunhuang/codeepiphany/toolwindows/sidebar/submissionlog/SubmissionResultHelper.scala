package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog

import java.awt.BorderLayout

import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI

import com.wenjunhuang.codeepiphany.database.tables.records.SolutionSubmissionRecord
import com.wenjunhuang.codeepiphany.model.SubmissionResult
import com.wenjunhuang.codeepiphany.utils.ui.BackgroundRoundedPanel
import com.wenjunhuang.codeepiphany.utils.ColorUtils

object SubmissionResultHelper {
  def setupMessagePane(
    pane: BackgroundRoundedPanel,
    result: Option[SubmissionResult],
    record: SolutionSubmissionRecord
  ): Unit = {
    result match
      case None => pane.setVisible(false)
      case Some(result) =>
        val message = record.getMessage
        if StringUtil.isNotEmpty(message) then
          val textArea = new JBTextArea(message)
          textArea.setMinimumSize(JBUI.emptySize)
          textArea.setLineWrap(true)
          textArea.setOpaque(false)
          pane.setLayout(BorderLayout())
          pane.setBorder(JBUI.Borders.empty(5))
          pane.add(textArea, BorderLayout.CENTER)
          result match
            case SubmissionResult.Success =>
              pane.setBackground(ColorUtils.SUCCESS_BACKGROUND)
              textArea.setForeground(ColorUtils.SUCCESS_FOREGROUND)
            case _ =>
              textArea.setForeground(ColorUtils.ERROR_FOREGROUND)
              pane.add(textArea, BorderLayout.CENTER)
  }
}
