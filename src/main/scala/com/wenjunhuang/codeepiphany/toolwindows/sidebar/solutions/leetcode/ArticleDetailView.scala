package com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode

import java.awt.Insets
import java.awt.event.{ MouseWheelEvent, MouseWheelListener }
import javax.swing.JComponent

import com.intellij.ide.CopyProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.PopupHandler
import com.intellij.util.ui.{ JBInsets, JBUI }

import com.wenjunhuang.codeepiphany.actions.webview.WebviewActionProvider
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.WebViewStyleProvider
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.SidebarActions
import com.wenjunhuang.codeepiphany.utils.actions.{ DataSink, UiDataProvider }
import com.wenjunhuang.codeepiphany.utils.isDebug

class ArticleDetailView(private val myPresenter: ArticleDetailPresenter, private val myProject: Project)
    extends SimpleToolWindowPanel(true)
    with WebViewStyleProvider
    with CopyProvider
    with UiDataProvider
    with WebviewActionProvider
    with Disposable {
  private val myView = ArticleJCefView(myPresenter, this, myProject)

  private val MOUSE_WHEEL_LISTENER = new MouseWheelListener {
    override def mouseWheelMoved(e: MouseWheelEvent): Unit =
      if e.isControlDown then
        e.getWheelRotation match
          case rotation if rotation < 0 => myView.zoom = myView.zoom * 1.2
          case rotation if rotation > 0 => myView.zoom = myView.zoom / 1.2
          case _                        => ()
        e.consume()
  }

  private val actionManager = ActionManager.getInstance()
  private val actionGroup   = actionManager.getAction(SidebarActions.GROUP_TOOLBAR).asInstanceOf[ActionGroup]
  private val actionToolbar = actionManager.createActionToolbar(SidebarActions.ACTION_PLACE, actionGroup, true)
  actionToolbar.setTargetComponent(this)

  setToolbar(actionToolbar.getComponent)
  setContent(myView.uiComponent)

  myView.preferredFocusedComponent.addMouseWheelListener(MOUSE_WHEEL_LISTENER)

  Disposer.register(myPresenter, this)

  if !isDebug then
    PopupHandler.installPopupMenu(
      myView.preferredFocusedComponent,
      SidebarActions.GROUP_POPUP,
      SidebarActions.ACTION_PLACE
    )

  override def uiDataSnapshot(dataSink: DataSink): Unit = {
    dataSink.set(WebviewActionProvider.DATA_KEY, this)
    dataSink.set(PlatformDataKeys.COPY_PROVIDER, this)
  }

  override def dispose(): Unit =
    myView.preferredFocusedComponent.removeMouseWheelListener(MOUSE_WHEEL_LISTENER)
    Disposer.dispose(myView)

  def setArticleContent(content: Option[(String, CodeDojo)]): Unit =
    myView.setArticleContent(content)


  override def performCopy(dataContext: DataContext): Unit =
    myView.performCopy()

  override def isCopyEnabled(dataContext: DataContext): Boolean = true

  override def isCopyVisible(dataContext: DataContext): Boolean = true

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.EDT

  override def bodyPadding: Option[(Int, Int, Int, Int)] = {
    // Align the HTML body to the left with the toolbar's first action and
    // ensure it has the same vertical padding as the toolbar for a visually pleasing layout.
    val insets = JBInsets.addInsets(
      Option(actionToolbar.getComponent.getInsets())
        .map(JBInsets.create)
        .getOrElse(JBUI.emptyInsets()),
      Option(actionToolbar.getComponent.getComponent(0)).map {
        case child: JComponent =>
          JBInsets.create(child.getInsets())
        case _ =>
          JBUI.emptyInsets()
      }.getOrElse(JBUI.emptyInsets())
    )
    Some(insets.top, insets.right, insets.bottom, insets.left)
  }

  override def zoomIn(): Unit = myView.zoomIn()

  override def zoomOut(): Unit = myView.zoomOut()

  override def canZoomIn: Boolean = myView.canZoomIn

  override def canZoomOut: Boolean = myView.canZoomOut

  override def actualZoom(): Unit = myView.actualZoom()

  override def zoom: Double = myView.zoom
}
