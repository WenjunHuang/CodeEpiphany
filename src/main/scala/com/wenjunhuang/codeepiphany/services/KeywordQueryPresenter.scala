package com.wenjunhuang.codeepiphany.services

import javax.swing.event.DocumentEvent
import javax.swing.JComponent

import com.intellij.openapi.project.Project
import com.intellij.ui.DocumentAdapter

import com.wenjunhuang.codeepiphany.services.KeywordQueryPresenter.KeywordHolder

abstract class KeywordQueryPresenter[UIBoostrapParameters, T: KeywordHolder, ResultItem](
  project: Project,
  boostrap: UIBoostrapParameters
) extends BaseQueryPresenter[UIBoostrapParameters, T, ResultItem](project, boostrap) {
  protected var myView: Option[KeywordQueryView[ResultItem]] = None

  override def getViewComponent: JComponent = synchronized {
    myView match {
      case Some(view) => view.getComponent
      case None =>
        val view = KeywordQueryView[ResultItem](this)
        myView = Some(view)
        view.getComponent
    }
  }

  override protected def refreshPagination(): Unit = {
    myView.foreach(_.refreshPagination())
  }

  def getDocumentAdapter: DocumentAdapter = (e: DocumentEvent) => {
    val keyword = e.getDocument.getText(0, e.getDocument.getLength)
    if keyword.nonEmpty then
      myQueryStateManager.update(_.updateCriteria(it => summon[KeywordHolder[T]].updateKeyword(it, keyword)))
      requery(true)
  }

  override protected def updateQueryUI(context: QueryContext[T]): Unit =
    myView.foreach(_.setSearchText(summon[KeywordHolder[T]].keyword(context.criteria)))
}

object KeywordQueryPresenter {
  trait KeywordHolder[T] {
    def keyword(v: T): String
    def updateKeyword(v: T, keyword: String): T
  }
}
