package com.wenjunhuang.codeepiphany.actions

import cats.syntax.all.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.wenjunhuang.codeepiphany.actions.PaginationParameterActionGroup.*
import com.wenjunhuang.codeepiphany.utils.PageSize
import com.wenjunhuang.codeepiphany.utils.actions.{ActionCompatible, DataKeyNotNull, ParameterProvider}

import javax.swing.{Icon, JComponent}

class PaginationParameterActionGroup(private val myPageNum: Int = DEFAULT_PAGE_NUMBER)
    extends DefaultActionGroup
    with ActionCompatible {
  private var cache = (0, 0, 0)

  override def update(e: AnActionEvent): Unit =
    Option(PAGINATION_PROVIDER_KEY.getData(e.getDataContext)) match {
      case None           => e.getPresentation.setEnabled(false)
      case Some(provider) => rebuildActions(provider)
    }

  private def rebuildActions(provider: PaginationParameterProvider): Unit = {
    val (pageSize, currentPage, totalItems, totalPages) =
      (provider.getPageSize, provider.getCurrentPage, provider.getTotalItems, provider.getTotalPages)

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
        (1 to totalPages).foreach(i =>
          add(createPageIndexAction(s"$i", currentPage == i, Some(() => provider.setCurrentPage(i))))
        )
      else if currentPage <= myPageNum - 3 then
        (1 to myPageNum - 2).foreach(i =>
          add(createPageIndexAction(s"$i", currentPage == i, Some(() => provider.setCurrentPage(i))))
        )
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
        (totalPages - myPageNum + 3 to totalPages).foreach(i =>
          add(createPageIndexAction(s"$i", currentPage == i, Some(() => provider.setCurrentPage(i))))
        )
      else
        add(createPageIndexAction("1", currentPage == 1, Some(() => provider.setCurrentPage(1))))
        add(createPageIndexAction("...", false, None))
        ((currentPage - 1) to (currentPage + myPageNum - 6)).foreach(i =>
          add(createPageIndexAction(s"$i", currentPage == i, Some(() => provider.setCurrentPage(i))))
        )
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
    new AnAction(icon) with RightAlignedToolbarAction with ActionCompatible {
      override def actionPerformed(e: AnActionEvent): Unit = action.foreach(_())
      override def update(e: AnActionEvent): Unit          = e.getPresentation.setEnabled(action.nonEmpty)
    }

  private def createPageIndexAction(text: String, selected: Boolean, action: Option[() => Unit]): AnAction =
    new ToggleAction(text) with RightAlignedToolbarAction with ActionCompatible {
      override def isSelected(e: AnActionEvent): Boolean               = selected
      override def setSelected(e: AnActionEvent, state: Boolean): Unit = if state then action.foreach(_())
      override def displayTextInToolbar(): Boolean                     = true
      override def update(e: AnActionEvent): Unit = {
        super.update(e)
        e.getPresentation.setEnabled(action.nonEmpty)
      }
    }
}

object PaginationParameterActionGroup {
  final val DEFAULT_PAGE_NUMBER     = 8
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

class PageSizeAction extends ComboBoxAction with ActionCompatible with DataKeyNotNull(PAGINATION_PROVIDER_KEY) {
  override def createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup = {
    val provider = getValue(dataContext)
    DefaultActionGroup(provider.getAllItems.map(item => new RangePageSizeItemAction(item))*)
  }

  override def update(e: AnActionEvent): Unit =
    if isSatisfied(e) then
      val presentation = e.getPresentation
      presentation.setEnabled(true)
      val provider = getValue(e)
      provider.getSelectedItems.headOption match {
        case None       => presentation.setText("")
        case Some(item) => presentation.setText(item.show)
      }
    else e.getPresentation.setEnabled(false)
}

class RangePageSizeItemAction(private val myItem: PageSize)
    extends AnAction(myItem.show)
    with ActionCompatible
    with DataKeyNotNull(PAGINATION_PROVIDER_KEY) {
  override def actionPerformed(e: AnActionEvent): Unit =
    getValue(e).toggleSelection(myItem)

  override def update(e: AnActionEvent): Unit = {
    val presentation = e.getPresentation
    if isSatisfied(e) then
      presentation.setEnabled(true)
      val provider = getValue(e)
      if provider.isSelected(myItem) then presentation.setIcon(AllIcons.General.InspectionsOK)
      else presentation.setIcon(null)
    else
      presentation.setEnabled(false)
      presentation.setIcon(null)
  }

}
