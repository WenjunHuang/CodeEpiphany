package com.wenjunhuang.codeepiphany.services

import javax.swing.event.DocumentEvent
import javax.swing.JComponent

import com.intellij.openapi.project.Project
import com.intellij.ui.DocumentAdapter

abstract class KeywordQueryPresenter[UIBoostrapParameters, ResultItem](project: Project, boostrap: UIBoostrapParameters)
    extends BaseQueryPresenter[UIBoostrapParameters, String, ResultItem](project, boostrap) {
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
      myQueryStateManager.update(_.updateCriteria(_ => keyword))
      requery()
  }
}
