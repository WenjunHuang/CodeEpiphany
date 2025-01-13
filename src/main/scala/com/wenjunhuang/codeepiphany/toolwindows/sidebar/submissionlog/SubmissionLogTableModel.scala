package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ ActionGroup, ActionManager, DataSink, UiDataProvider }
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.table.TableView
import com.intellij.ui.PopupHandler
import com.intellij.util.ui.{ ColumnInfo, JBUI, ListTableModel }
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.table.IconTableCellRenderer
import com.wenjunhuang.codeepiphany.model.{ Actions, CodeDojo, OrderDirection }
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.model.OrderDirection.{ Ascending, Descending }
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionLogTableModel.{
  nextOrderFilter,
  OrderByColumnInfo
}

import java.awt.{ Color, Component }
import java.awt.event.{ MouseAdapter, MouseEvent }
import java.time.format.DateTimeFormatter
import javax.swing.{ Icon, JTable, ListSelectionModel }
import javax.swing.table.{ DefaultTableCellRenderer, TableCellRenderer }

class SubmissionLogTableModel(private val myPresenter: SubmissionLogPresenter)
    extends ListTableModel[SubmissionLogEntry] {

  private val myColumns: Array[OrderByColumnInfo[SubmissionLogEntry, ?]] = Array(
    new OrderByColumnInfo[SubmissionLogEntry, CodeDojo](PluginBundle.message("submissionLog.ui.dojo.title")) {
      override def valueOf(item: SubmissionLogEntry): CodeDojo = item.dojo

      override def getPreferredStringValue: String = PluginBundle.message("submissionLog.ui.dojo.title")

      override def getRenderer(item: SubmissionLogEntry): TableCellRenderer =
        new IconTableCellRenderer[CodeDojo]() {
          override def getIcon(value: CodeDojo, table: JTable, row: Int): Icon =
            value.getIcon.orNull

          override def isCenterAlignment: Boolean = true

          override def getText: String = null
        }

      override def getOrderFilter: Option[OrderDirection] = myPresenter.getCodeDojoOrderFilter

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        filter match
          case None         => myPresenter.clearOrderFilter()
          case Some(filter) => myPresenter.setCodeDojoOrderFilter(filter)
    },
    new OrderByColumnInfo[SubmissionLogEntry, String](PluginBundle.message("submissionLog.ui.challenge.title")) {
      override def valueOf(item: SubmissionLogEntry): String = item.challengeTitle

      override def getPreferredStringValue: String = StringUtil.repeat("W", 30)

      override def enableOrderBy: Boolean = false

    },
    new OrderByColumnInfo[SubmissionLogEntry, String](PluginBundle.message("submissionLog.ui.solution.title")) {
      override def valueOf(item: SubmissionLogEntry): String = item.solution
      override def getPreferredStringValue: String           = StringUtil.repeat("W", 15)
      override def enableOrderBy: Boolean                    = false
    },
    new OrderByColumnInfo[SubmissionLogEntry, String](PluginBundle.message("submissionLog.ui.difficulty.title")) {
      override def valueOf(item: SubmissionLogEntry): String = s"${item.difficulty.showAsHtml}"
      override def getPreferredStringValue: String           = StringUtil.repeat("W", 15)

      override def getOrderFilter: Option[OrderDirection] =
        myPresenter.getDifficultyOrderFilter

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        filter match
          case None         => myPresenter.clearOrderFilter()
          case Some(filter) => myPresenter.setDifficultyOrderFilter(filter)
    },
    new OrderByColumnInfo[SubmissionLogEntry, String](PluginBundle.message("submissionLog.ui.language.title")) {
      override def valueOf(item: SubmissionLogEntry): String = s"${item.language.show}${item.languageVersion.version}"
      override def getPreferredStringValue: String           = StringUtil.repeat("W", 15)

      override def getOrderFilter: Option[OrderDirection] = myPresenter.getLanguageOrderFilter

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        filter match
          case None         => myPresenter.clearOrderFilter()
          case Some(filter) => myPresenter.setLanguageOrderFilter(filter)
    },
    new OrderByColumnInfo[SubmissionLogEntry, String](PluginBundle.message("submissionLog.ui.result.title")) {
      override def valueOf(item: SubmissionLogEntry): String = s"${item.result.showAsHtml}"
      override def getPreferredStringValue: String           = StringUtil.repeat("W", 15)

      override def getOrderFilter: Option[OrderDirection] = myPresenter.getSubmissionResultOrderFilter

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        filter match
          case None         => myPresenter.clearOrderFilter()
          case Some(filter) => myPresenter.setSubmissionResultOrderFilter(filter)
    },
    new OrderByColumnInfo[SubmissionLogEntry, String](
      PluginBundle.message("submissionLog.ui.submissionDateTime.title")
    ) {
      override def valueOf(item: SubmissionLogEntry): String =
        s"${item.submissionDateTime.map(_.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).getOrElse("")}"
      override def getPreferredStringValue: String = StringUtil.repeat("W", 20)

      override def getOrderFilter: Option[OrderDirection] = myPresenter.getSubmissionDateTimeOrderFilter

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        filter match
          case None         => myPresenter.clearOrderFilter()
          case Some(filter) => myPresenter.setSubmissionDateTimeOrderFilter(filter)
    },
    new OrderByColumnInfo[SubmissionLogEntry, String](PluginBundle.message("submissionLog.ui.resultDateTime.title")) {
      override def valueOf(item: SubmissionLogEntry): String =
        s"${item.resultDateTime.map(_.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).getOrElse("")}"
      override def getPreferredStringValue: String = StringUtil.repeat("W", 20)

      override def getOrderFilter: Option[OrderDirection] = myPresenter.getResultDateTimeOrderFilter

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        filter match
          case None         => myPresenter.clearOrderFilter()
          case Some(filter) => myPresenter.setResultDateTimeOrderFilter(filter)
    }
  )
  setColumnInfos(myColumns.asInstanceOf[Array[ColumnInfo[?, ?]]])

  def createTableView(setDataSink: DataSink => Unit): TableView[SubmissionLogEntry] = {
    val tableView = new TableView(this) with UiDataProvider {
      override def uiDataSnapshot(dataSink: DataSink): Unit = setDataSink(dataSink)
    }
    tableView.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
    tableView.setShowGrid(false)
    tableView.setShowColumns(true)
    tableView.getTableHeader.setDefaultRenderer(new TableCellRenderer {
      override def getTableCellRendererComponent(
        table: JTable,
        value: Any,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
      ): Component = {
        val nameLabel = DefaultTableCellRenderer()
        nameLabel.setBorder(JBUI.Borders.empty(0, 4, 0, 0))
        nameLabel.setText(value.asInstanceOf[String])
        val panel = BorderLayoutPanel()
        panel.setBackground(new Color(0, 0, 0, 0))
        panel.addToCenter(nameLabel)

        if myColumns(column).enableOrderBy then
          val sortIcon = DefaultTableCellRenderer()
          myColumns(column).getOrderFilter match
            case None                            => sortIcon.setIcon(AllIcons.General.ArrowSplitCenterV)
            case Some(OrderDirection.Ascending)  => sortIcon.setIcon(AllIcons.General.ArrowUp)
            case Some(OrderDirection.Descending) => sortIcon.setIcon(AllIcons.General.ArrowDown)
          panel.addToRight(sortIcon)
        else

          panel
      }
    })
    tableView.getTableHeader.addMouseListener(new MouseAdapter {
      override def mouseClicked(e: MouseEvent): Unit = {
        val columnIndex = tableView.columnAtPoint(e.getPoint)
        if columnIndex >= 0 then
          val columnInfo = myColumns(columnIndex)
          if columnInfo.enableOrderBy then columnInfo.setOrderFilter(nextOrderFilter(columnInfo.getOrderFilter))
          tableView.getTableHeader.revalidate()
      }
    })

    PopupHandler.installRowSelectionTablePopup(
      tableView,
      ActionManager.getInstance().getAction(Actions.SUBMISSIONS_TABLE_POPUP_GROUP).asInstanceOf[ActionGroup],
      Actions.SUBMISSIONS_TABLE_POPUP_PLACE
    )

    tableView
  }
}

object SubmissionLogTableModel {

  abstract class OrderByColumnInfo[Item, Aspect](name: String) extends ColumnInfo[Item, Aspect](name) {
    def enableOrderBy: Boolean                               = true
    def getOrderFilter: Option[OrderDirection]               = None
    def setOrderFilter(filter: Option[OrderDirection]): Unit = {}
  }

  def nextOrderFilter(filter: Option[OrderDirection]): Option[OrderDirection] = filter match
    case None             => Some(Ascending)
    case Some(Ascending)  => Some(Descending)
    case Some(Descending) => None

}
