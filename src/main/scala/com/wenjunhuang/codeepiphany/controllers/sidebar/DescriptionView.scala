package com.wenjunhuang.codeepiphany.controllers.sidebar

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.{DataSink, UiDataProvider}
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefApp
import com.wenjunhuang.codeepiphany.controllers.sidebar.jcef.JCefDescriptionView
import com.wenjunhuang.codeepiphany.model.QuestionStorage.QuestionItem
import org.intellij.images.options.OptionsManager

import java.awt.event.{MouseWheelEvent, MouseWheelListener}
import javax.swing.JPanel

class DescriptionView(private val myProject: Project, private val myPresenter: DescriptionPresenter) extends JPanel() with UiDataProvider with Disposable {
  private val myViewer = JCefDescriptionView(myProject, myPresenter)
  private val MOUSE_WHEEL_LISTENER = new MouseWheelListener {
    override def mouseWheelMoved(e: MouseWheelEvent): Unit =  {
      if e.isControlDown then
        val rotation = e.getWheelRotation
        if rotation < 0 then
          myViewer.set
    }
  }

  override def uiDataSnapshot(dataSink: DataSink): Unit = ???

  override def dispose(): Unit =

  def updateCurrentQuestion(question: QuestionItem): Unit = {}

}
