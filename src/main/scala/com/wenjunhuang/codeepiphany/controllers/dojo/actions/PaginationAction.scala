package com.wenjunhuang.codeepiphany.controllers.dojo.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.PaginationAction.INDEX_COUNT
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.keys.PAGINATION_PROVIDER_KEY

import javax.swing.Icon

class PaginationAction extends DefaultActionGroup {
  private var cache = (0, 0, 0)

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  override def update(e: AnActionEvent): Unit =
    Option(PAGINATION_PROVIDER_KEY.getData(e.getDataContext)) match {
      case None => e.getPresentation.setEnabled(false)
      case Some(provider) =>
        rebuildActions(provider)
    }

  private def rebuildActions(provider: PaginationProvider): Unit = {
    val pageSize    = provider.getPageSize
    val currentPage = provider.getCurrentPage
    val totalItems  = provider.getTotalItems
    val totalPages  = provider.getTotalPages

    if cache != (pageSize, currentPage, totalItems) then
      removeAll()

      add(createIconAction(AllIcons.General.ArrowLeft, if currentPage > 1 then Some(() => provider.setCurrentPage(currentPage - 1)) else None))
      if totalPages <= INDEX_COUNT then for (i <- 1 to totalPages) do add(createPageIndexAction(s"$i", currentPage == i, Some(() => provider.setCurrentPage(i))))
      else if currentPage <= INDEX_COUNT - 3 then
        for (i <- 1 to INDEX_COUNT - 2) do add(createPageIndexAction(s"$i", currentPage == i, Some(() => provider.setCurrentPage(i))))
        add(createPageIndexAction("...", false, None))
        add(createPageIndexAction(s"$totalPages", currentPage == totalPages, Some(() => provider.setCurrentPage(totalPages))))
      else if currentPage >= totalPages - INDEX_COUNT + 4 then
        add(createPageIndexAction("1", currentPage == 1, Some(() => provider.setCurrentPage(1))))
        add(createPageIndexAction("...", false, None))
        for (i <- totalPages - INDEX_COUNT + 3 to totalPages) do add(createPageIndexAction(s"$i", currentPage == i, Some(() => provider.setCurrentPage(i))))
      else
        add(createPageIndexAction("1", currentPage == 1, Some(() => provider.setCurrentPage(1))))
        add(createPageIndexAction("...", false, None))
        for (i <- (currentPage - 1) to (currentPage + INDEX_COUNT - 6))
          add(createPageIndexAction(s"$i", currentPage == i, Some(() => provider.setCurrentPage(i))))
        add(createPageIndexAction("...", false, None))
        add(createPageIndexAction(s"$totalPages", currentPage == totalPages, Some(() => provider.setCurrentPage(totalPages))))

      add(createIconAction(AllIcons.General.ArrowRight, if currentPage < totalPages then Some(() => provider.setCurrentPage(currentPage + 1)) else None))

      cache = (pageSize, currentPage, totalItems)
      provider.refresh()

  }

  private def createIconAction(icon: Icon, action: Option[() => Unit]): AnAction =
    new AnAction(icon) {
      override def actionPerformed(e: AnActionEvent): Unit = action.foreach(_())

      override def update(e: AnActionEvent): Unit = {
        e.getPresentation.setEnabled(action.nonEmpty)
      }

      override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
    }

  private def createPageIndexAction(text: String, selected: Boolean, action: Option[() => Unit]): AnAction =
    new ToggleAction(text) {
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

object PaginationAction {
  final val INDEX_COUNT = 8
}
