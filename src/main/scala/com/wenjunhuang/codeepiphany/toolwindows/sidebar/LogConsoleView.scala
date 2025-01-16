package com.wenjunhuang.codeepiphany.toolwindows.sidebar

import cats.effect.Async

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.concurrency.ThreadingAssertions

import com.wenjunhuang.codeepiphany.model.Constants
import com.wenjunhuang.codeepiphany.utils.implicits.*

class LogConsoleView(private val myProject: Project) extends SimpleToolWindowPanel(false, true) {

  private val myConsoleView: ConsoleView = TextConsoleBuilderFactory.getInstance().createBuilder(myProject).getConsole

  init()

  private def init(): Unit = {
    val actionGroup = DefaultActionGroup(myConsoleView.createConsoleActions()*)
    val toolbar = ActionManager
      .getInstance()
      .createActionToolbar(Constants.ACTION_PREFIX + ".ConsoleView", actionGroup, true)

    toolbar.setTargetComponent(myConsoleView.getComponent)
    setContent(myConsoleView.getComponent)
    setToolbar(toolbar.getComponent)

    Disposer.register(myProject, myConsoleView)
  }

  override def uiDataSnapshot(sink: DataSink): Unit = {
    super.uiDataSnapshot(sink)
    sink.set(LogConsoleView.CONSOLE_VIEW_KEY, myConsoleView)
  }
}

object LogConsoleView {
  final val DISPLAY_NAME     = "Console"
  final val CONSOLE_VIEW_KEY = DataKey.create[ConsoleView]("ConsoleViewKey")

  def getConsoleView(project: Project): ConsoleView = {
    ThreadingAssertions.assertEventDispatchThread()
    val logConsoleView = ToolWindowManager
      .getInstance(project)
      .getToolWindow(SidebarWindowFactory.TOOL_WINDOW_ID)
      .getContentManager
      .findContent(DISPLAY_NAME)
      .getComponent
      .asInstanceOf[LogConsoleView]
    CONSOLE_VIEW_KEY.getData(DataManager.getInstance().getDataContext(logConsoleView))
  }

  def getConsoleViewF[F[_]: Async](project: Project): F[ConsoleView] = Async[F].delay(getConsoleView(project)).evalOnEDTAny()
}
