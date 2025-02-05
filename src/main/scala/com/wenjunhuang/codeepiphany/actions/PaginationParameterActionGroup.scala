package com.wenjunhuang.codeepiphany.actions

import cats.Show
import cats.syntax.all.*
import javax.swing.{Icon, JComponent}

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.actions.PaginationParameterActionGroup.*
import com.wenjunhuang.codeepiphany.utils.actions.ParameterProvider
import com.wenjunhuang.codeepiphany.utils.PageSize

class PaginationParameterActionGroup(private val myPageNum: Int = DEFAULT_PAGE_NUMBER) extends DefaultActionGroup {
  private var cache = (0, 0, 0)

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  override def update(e: AnActionEvent): Unit =
    Option(PAGINATION_PROVIDER_KEY.getData(e.getDataContext)) match {
      case None => e.getPresentation.setEnabled(false)
      case Some(provider) =>
        rebuildActions(provider)
    }

  private def rebuildActions(provider: PaginationParameterProvider): Unit = {
    val pageSize    = provider.getPageSize
    val currentPage = provider.getCurrentPage
    val totalItems  = provider.getTotalItems
    val totalPages  = provider.getTotalPages

    if cache != (pageSize, currentPage, totalItems) then
      removeAll()

      add(PageSizeAction())

      add(
        createIconAction(
          AllIcons.General.ArrowLeft,
          if currentPage > 1 then Some(() => provider.setCurrentPage(currentPage - 1)) else None
        )
      )
      if totalPages <= myPageNum then
        for (i <- 1 to totalPages) do
          add(createPageIndexAction(s"$i", currentPage == i, Some(() => provider.setCurrentPage(i))))
      else if currentPage <= myPageNum - 3 then
        for (i <- 1 to myPageNum - 2) do
          add(createPageIndexAction(s"$i", currentPage == i, Some(() => provider.setCurrentPage(i))))
        add(createPageIndexAction("...", false, None))
        add(
          createPageIndexAction(
            s"$totalPages",
            currentPage == totalPages,
            Some(() => provider.setCurrentPage(totalPages))
          )
        )
      else if currentPage >= totalPages - myPageNum + 4 then
        add(createPageIndexAction("1", currentPage == 1, Some(() => provider.setCurrentPage(1))))
        add(createPageIndexAction("...", false, None))
        for (i <- totalPages - myPageNum + 3 to totalPages) do
          add(createPageIndexAction(s"$i", currentPage == i, Some(() => provider.setCurrentPage(i))))
      else
        add(createPageIndexAction("1", currentPage == 1, Some(() => provider.setCurrentPage(1))))
        add(createPageIndexAction("...", false, None))
        for (i <- (currentPage - 1) to (currentPage + myPageNum - 6))
          add(createPageIndexAction(s"$i", currentPage == i, Some(() => provider.setCurrentPage(i))))
        add(createPageIndexAction("...", false, None))
        add(
          createPageIndexAction(
            s"$totalPages",
            currentPage == totalPages,
            Some(() => provider.setCurrentPage(totalPages))
          )
        )

      add(
        createIconAction(
          AllIcons.General.ArrowRight,
          if currentPage < totalPages then Some(() => provider.setCurrentPage(currentPage + 1)) else None
        )
      )

      cache = (pageSize, currentPage, totalItems)
      provider.refresh()

  }

  private def createIconAction(icon: Icon, action: Option[() => Unit]): AnAction =
    new AnAction(icon) with RightAlignedToolbarAction {
      override def actionPerformed(e: AnActionEvent): Unit = action.foreach(_())

      override def update(e: AnActionEvent): Unit =
        e.getPresentation.setEnabled(action.nonEmpty)

      override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
    }

  private def createPageIndexAction(text: String, selected: Boolean, action: Option[() => Unit]): AnAction =
    new ToggleAction(text) with RightAlignedToolbarAction {
      override def isSelected(e: AnActionEvent): Boolean = selected

      override def setSelected(e: AnActionEvent, state: Boolean): Unit =
        if state then action.foreach(_())

      override def displayTextInToolbar(): Boolean = true

      override def update(e: AnActionEvent): Unit = {
        super.update(e)
        e.getPresentation.setEnabled(action.nonEmpty)
      }

      override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
    }
}

object PaginationParameterActionGroup {
  final val DEFAULT_PAGE_NUMBER = 8

  final val PAGINATION_PROVIDER_KEY = DataKey.create[PaginationParameterProvider]("PAGINATION_PROVIDER_KEY")


  trait PaginationParameterProvider extends ParameterProvider[PageSize] {
    def getPageSize: Int
    def getCurrentPage: Int
    def setCurrentPage(page: Int): Unit
    def getTotalPages: Int
    def getTotalItems: Int
    def refresh(): Unit
  }
}

class PageSizeAction extends ComboBoxAction {
  override def createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup =
    Option(PAGINATION_PROVIDER_KEY.getData(dataContext)) match {
      case None => DefaultActionGroup()
      case Some(provider) =>
        DefaultActionGroup(provider.getAllItems.map(item => new RangePageSizeItemAction(item))*)
    }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  override def update(e: AnActionEvent): Unit =
    Option(PAGINATION_PROVIDER_KEY.getData(e.getDataContext)) match {
      case None => e.getPresentation.setEnabled(false)
      case Some(provider) =>
        val presentation = e.getPresentation
        presentation.setEnabled(true)
        provider.getSelectedItems.headOption match {
          case None       => presentation.setText("")
          case Some(item) => presentation.setText(item.show)
        }
    }
}

class RangePageSizeItemAction(private val myItem: PageSize) extends AnAction(myItem.show) {
  override def actionPerformed(e: AnActionEvent): Unit =
    Option(PAGINATION_PROVIDER_KEY.getData(e.getDataContext)).foreach(_.toggleSelection(myItem))

  override def update(e: AnActionEvent): Unit = {
    val presentation = e.getPresentation
    Option(PAGINATION_PROVIDER_KEY.getData(e.getDataContext))
      .map(_.isSelected(myItem))
      .foreach {
        case true  => presentation.setIcon(AllIcons.Actions.Checked)
        case false => presentation.setIcon(null)
      }
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
