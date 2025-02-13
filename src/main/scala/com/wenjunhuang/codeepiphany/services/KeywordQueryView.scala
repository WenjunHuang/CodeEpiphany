package com.wenjunhuang.codeepiphany.services

import java.awt.{BorderLayout, Color}
import java.awt.event.{MouseAdapter, MouseEvent}
import javax.swing.{JTable, ScrollPaneConstants}
import javax.swing.table.DefaultTableCellRenderer

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.{PopupHandler, SearchTextField, SimpleTextAttributes}
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.TableView
import com.intellij.util.ui.{EDT, JBUI}
import com.intellij.util.ui.components.BorderLayoutPanel

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.actions.PaginationParameterActionGroup
import com.wenjunhuang.codeepiphany.model.Actions.{CHALLENGES_TABLE_POPUP_GROUP, CHALLENGES_TABLE_POPUP_PLACE, TOOLBAR_PLACE}
import com.wenjunhuang.codeepiphany.model.OrderDirection
import com.wenjunhuang.codeepiphany.utils.ColorUtils
import com.wenjunhuang.codeepiphany.utils.OrderByColumnInfo.nextOrderFilter
import com.wenjunhuang.codeepiphany.utils.actions.{DataSink, UiDataProvider}

class KeywordQueryView[Item](private val myPresenter: KeywordQueryPresenter[?, ?, Item])
    extends SimpleToolWindowPanel(true, true)
    with UiDataProvider {

  private val mySearchTextField       = SearchTextField(true)
  private val myTable                 = createTableView()
  private val myContent               = BorderLayoutPanel()
  private val myQueryRangeActionGroup = PaginationParameterActionGroup()
  private val myQueryRangeToolbar =
    ActionManager.getInstance().createActionToolbar(TOOLBAR_PLACE, myQueryRangeActionGroup, true)

  mySearchTextField.getTextEditor.getEmptyText
    .appendText(
      PluginBundle.message("ui.query.searchHint"),
      new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, ColorUtils.LABEL_GRAY_COLOR)
    )
  mySearchTextField.addDocumentListener(myPresenter.getDocumentAdapter)
  add(mySearchTextField, BorderLayout.NORTH)

  myContent.addToCenter(
    JBScrollPane(
      myTable,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    )
  )
  myQueryRangeToolbar.setTargetComponent(this)
  myContent.addToBottom(myQueryRangeToolbar.getComponent)

  setContent(myContent)

  override def uiDataSnapshot(sink: DataSink): Unit = {
    myPresenter.uiDataSnapshot(sink)
  }

  def refreshPagination(): Unit = {
    if EDT.isCurrentThreadEdt then myQueryRangeToolbar.updateActionsImmediately()
    else ApplicationManager.getApplication.invokeLater(() => myQueryRangeToolbar.updateActionsImmediately())
  }

  private def createTableView(): TableView[Item] = {
    val tableView = new TableView[Item](myPresenter.getQueryResultTableModel)
    tableView.setSelectionModel(myPresenter.getQueryResultTableSelectionModel)
    tableView.setShowGrid(false)
    tableView.setShowColumns(true)
    tableView.getTableHeader.setDefaultRenderer(
      (table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int) => {
        val nameLabel = DefaultTableCellRenderer()
        nameLabel.setBorder(JBUI.Borders.empty(0, 4, 0, 0))
        nameLabel.setText(value.asInstanceOf[String])
        val panel = BorderLayoutPanel()
        panel.setBackground(new Color(0, 0, 0, 0))
        panel.addToCenter(nameLabel)

        val columnInfo = myPresenter.getQueryResultColumns(column)
        if columnInfo.enableOrderBy then
          val sortIcon = DefaultTableCellRenderer()
          columnInfo.getOrderFilter match
            case None                            => sortIcon.setIcon(AllIcons.General.ArrowSplitCenterV)
            case Some(OrderDirection.Ascending)  => sortIcon.setIcon(AllIcons.General.ArrowUp)
            case Some(OrderDirection.Descending) => sortIcon.setIcon(AllIcons.General.ArrowDown)
          panel.addToRight(sortIcon)
        else panel
      }
    )
    tableView.getTableHeader.addMouseListener(new MouseAdapter {
      override def mouseClicked(e: MouseEvent): Unit = {
        val columnIndex = tableView.columnAtPoint(e.getPoint)
        if columnIndex >= 0 then
          val columnInfo = myPresenter.getQueryResultColumns(columnIndex)
          if columnInfo.enableOrderBy then columnInfo.setOrderFilter(nextOrderFilter(columnInfo.getOrderFilter))
          tableView.getTableHeader.revalidate()
      }
    })
    PopupHandler.installRowSelectionTablePopup(
      tableView,
      ActionManager.getInstance().getAction(CHALLENGES_TABLE_POPUP_GROUP).asInstanceOf[ActionGroup],
      CHALLENGES_TABLE_POPUP_PLACE
    )
    tableView
  }

  override def addNotify(): Unit = {
    super.addNotify()
    myPresenter.requery()
  }
}
