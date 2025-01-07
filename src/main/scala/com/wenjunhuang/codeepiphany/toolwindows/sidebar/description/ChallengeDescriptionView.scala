package com.wenjunhuang.codeepiphany.toolwindows.sidebar.description

import com.intellij.ide.CopyProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.PopupHandler
import com.intellij.util.ui.{JBInsets, JBUI}
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.SidebarActions
import com.wenjunhuang.codeepiphany.utils.isDebug

import java.awt.Insets
import java.awt.event.{MouseWheelEvent, MouseWheelListener}
import javax.swing.JComponent

class ChallengeDescriptionView(private val myPresenter: ChallengeDescriptionPresenter, private val myProject: Project)
    extends SimpleToolWindowPanel(true)
    with ChallengeDescriptionStyleProvider
    with CopyProvider
    with UiDataProvider
    with Disposable {
  private val myViewer = JCefDescriptionView(myPresenter, this, myProject)

  private val MOUSE_WHEEL_LISTENER = new MouseWheelListener {
    override def mouseWheelMoved(e: MouseWheelEvent): Unit =
      if e.isControlDown then
        e.getWheelRotation match
          case rotation if rotation < 0 => myViewer.zoom = myViewer.zoom * 1.2
          case rotation if rotation > 0 => myViewer.zoom = myViewer.zoom / 1.2
          case _                        => ()
        e.consume()
  }

  private val actionManager = ActionManager.getInstance()
  private val actionGroup   = actionManager.getAction(SidebarActions.GROUP_TOOLBAR).asInstanceOf[ActionGroup]
  private val actionToolbar = actionManager.createActionToolbar(SidebarActions.ACTION_PLACE, actionGroup, true)
  actionToolbar.setTargetComponent(this)

  setToolbar(actionToolbar.getComponent)
  setContent(myViewer.uiComponent)

  myViewer.preferredFocusedComponent.addMouseWheelListener(MOUSE_WHEEL_LISTENER)

  Disposer.register(myPresenter, this)

  if !isDebug then
    PopupHandler.installPopupMenu(
      myViewer.preferredFocusedComponent,
      SidebarActions.GROUP_POPUP,
      SidebarActions.ACTION_PLACE
    )

  override def uiDataSnapshot(dataSink: DataSink): Unit = {
    dataSink.set(ChallengeDescriptionView.DATA_KEY, this)
    dataSink.set(PlatformDataKeys.COPY_PROVIDER, this)
  }

  override def dispose(): Unit =
    myViewer.preferredFocusedComponent.removeMouseWheelListener(MOUSE_WHEEL_LISTENER)
    Disposer.dispose(myViewer)

  def setDescription(content: Option[(String, CodeDojo)]): Unit =
    myViewer.setDescription(content)

  def zoomIn(): Unit = myViewer.zoomIn()

  def zoomOut(): Unit = myViewer.zoomOut()

  def canZoomIn: Boolean = myViewer.canZoomIn

  def canZoomOut: Boolean = myViewer.canZoomOut

  def actualZoom(): Unit = myViewer.actualZoom()

  def zoom: Double = myViewer.zoom

  override def performCopy(dataContext: DataContext): Unit =
    myViewer.performCopy()

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
}

object ChallengeDescriptionView {
  val DATA_KEY: DataKey[ChallengeDescriptionView] = DataKey.create[ChallengeDescriptionView]("DescriptionView")
}
