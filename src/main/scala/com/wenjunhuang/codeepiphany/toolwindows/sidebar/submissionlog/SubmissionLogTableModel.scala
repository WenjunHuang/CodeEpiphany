package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog

import com.intellij.openapi.actionSystem.{ ActionGroup, ActionManager, DataSink, UiDataProvider }
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.table.TableView
import com.intellij.ui.PopupHandler
import com.intellij.util.ui.{ ColumnInfo, ListTableModel }
import com.intellij.util.ui.table.IconTableCellRenderer
import com.wenjunhuang.codeepiphany.model.{ Actions, CodeDojo }
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionLogTableModel.COLUMNS

import java.time.format.DateTimeFormatter
import javax.swing.{ Icon, JTable, ListSelectionModel }
import javax.swing.table.TableCellRenderer

class SubmissionLogTableModel extends ListTableModel[SubmissionLogEntry] {
  setColumnInfos(COLUMNS.asInstanceOf[Array[ColumnInfo[?, ?]]])

  def createTableView(setDataSink: DataSink => Unit): TableView[SubmissionLogEntry] = {
    val tableView = new TableView(this) with UiDataProvider {
      override def uiDataSnapshot(dataSink: DataSink): Unit = setDataSink(dataSink)
    }
    tableView.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
    tableView.setShowGrid(false)
    tableView.setShowColumns(true)

    PopupHandler.installRowSelectionTablePopup(
      tableView,
      ActionManager.getInstance().getAction(Actions.SUBMISSIONS_TABLE_POPUP_GROUP).asInstanceOf[ActionGroup],
      Actions.SUBMISSIONS_TABLE_POPUP_PLACE
    )

    tableView
  }
}

object SubmissionLogTableModel {
  val COLUMNS: Array[ColumnInfo[SubmissionLogEntry, ?]] = Array(
    new ColumnInfo[SubmissionLogEntry, CodeDojo](PluginBundle.message("submissionLog.ui.dojo.title")) {
      override def valueOf(item: SubmissionLogEntry): CodeDojo = item.dojo

      override def getPreferredStringValue: String = PluginBundle.message("submissionLog.ui.dojo.title")

      override def getRenderer(item: SubmissionLogEntry): TableCellRenderer =
        new IconTableCellRenderer[SubmissionLogEntry]() {
          override def getIcon(value: SubmissionLogEntry, table: JTable, row: Int): Icon =
            value.dojo.getIcon.orNull

          override def isCenterAlignment: Boolean = true

          override def getText: String = null
        }
    },
    new ColumnInfo[SubmissionLogEntry, String](PluginBundle.message("submissionLog.ui.challenge.title")) {
      override def valueOf(item: SubmissionLogEntry): String = item.challengeTitle

      override def getPreferredStringValue: String = StringUtil.repeat("W", 30)
    },
    new ColumnInfo[SubmissionLogEntry, String](PluginBundle.message("submissionLog.ui.solution.title")) {
      override def valueOf(item: SubmissionLogEntry): String = item.solution
      override def getPreferredStringValue: String           = StringUtil.repeat("W", 15)
    },
    new ColumnInfo[SubmissionLogEntry, String](PluginBundle.message("submissionLog.ui.difficulty.title")) {
      override def valueOf(item: SubmissionLogEntry): String = s"${item.difficulty.showAsHtml}"
      override def getPreferredStringValue: String           = StringUtil.repeat("W", 15)
    },
    new ColumnInfo[SubmissionLogEntry, String](PluginBundle.message("submissionLog.ui.language.title")) {
      override def valueOf(item: SubmissionLogEntry): String = s"${item.language.show}${item.languageVersion.version}"
      override def getPreferredStringValue: String           = StringUtil.repeat("W", 15)
    },
    new ColumnInfo[SubmissionLogEntry, String](PluginBundle.message("submissionLog.ui.result.title")) {
      override def valueOf(item: SubmissionLogEntry): String = s"${item.result.showAsHtml}"
      override def getPreferredStringValue: String           = StringUtil.repeat("W", 15)
    },
    new ColumnInfo[SubmissionLogEntry, String](PluginBundle.message("submissionLog.ui.submissionDateTime.title")) {
      override def valueOf(item: SubmissionLogEntry): String =
        s"${item.submissionDateTime.map(_.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).getOrElse("")}"
      override def getPreferredStringValue: String = StringUtil.repeat("W", 20)
    },
    new ColumnInfo[SubmissionLogEntry, String](PluginBundle.message("submissionLog.ui.resultDateTime.title")) {
      override def valueOf(item: SubmissionLogEntry): String =
        s"${item.resultDateTime.map(_.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).getOrElse("")}"
      override def getPreferredStringValue: String = StringUtil.repeat("W", 20)
    }
  )
}
