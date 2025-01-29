package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.hackerrank

import java.awt.BorderLayout

import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI

import com.wenjunhuang.codeepiphany.database.tables.records.SolutionSubmissionRecord
import com.wenjunhuang.codeepiphany.model.SubmissionResult
import com.wenjunhuang.codeepiphany.utils.ui.BackgroundRoundedPanel
import com.wenjunhuang.codeepiphany.utils.ColorUtils

object HackerRankResultHelper {
  def setupMessagePane(pane: BackgroundRoundedPanel, result: Option[SubmissionResult], record: SolutionSubmissionRecord): Unit = {
    result match
      case Some(SubmissionResult.CompilationError) =>
        val textArea = new JBTextArea(record.getMessage)
        textArea.setMinimumSize(JBUI.emptySize)
        textArea.setLineWrap(true)
        textArea.setForeground(ColorUtils.ERROR_FOREGROUND)
        textArea.setOpaque(false)
        pane.setLayout(BorderLayout())
        pane.setBackground(ColorUtils.ERROR_BACKGROUND)
        pane.setBorder(JBUI.Borders.empty(5))
        pane.add(textArea, BorderLayout.CENTER)
      case _ => pane.setVisible(false)
  }
}
