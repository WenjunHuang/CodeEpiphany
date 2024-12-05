package com.wenjunhuang.codeepiphany.controllers.sidebar

import com.intellij.ide.CopyProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.colors.{ EditorColors, EditorColorsManager }
import com.intellij.openapi.project.Project
import com.intellij.ui.{ JBColor, PopupHandler }
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.{ JBInsets, JBUI, UIUtil }
import com.wenjunhuang.codeepiphany.controllers.sidebar.jcef.{ DescriptionStyleProvider, JCefDescriptionView }
import com.wenjunhuang.codeepiphany.model.QuestionStorage.QuestionItem
import com.wenjunhuang.codeepiphany.utils.isDebug

import java.awt.{ BorderLayout, Insets }
import java.awt.event.{ MouseWheelEvent, MouseWheelListener }
import javax.swing.{ JComponent, JPanel }

class DescriptionView(private val myProject: Project, private val myPresenter: DescriptionPresenter)
    extends JPanel()
    with DescriptionStyleProvider
    with CopyProvider
    with UiDataProvider
    with Disposable {
  private val myViewer = JCefDescriptionView(myProject, myPresenter, this)

  private val MOUSE_WHEEL_LISTENER = new MouseWheelListener {
    override def mouseWheelMoved(e: MouseWheelEvent): Unit =
      if e.isControlDown then
        e.getWheelRotation match
          case rotation if rotation < 0 => myViewer.zoom = myViewer.zoom * 1.2
          case rotation if rotation > 0 => myViewer.zoom = myViewer.zoom / 1.2
          case _                        => ()
        e.consume()
  }

  setLayout(new BorderLayout())
  private val actionManager = ActionManager.getInstance()
  private val actionGroup   = actionManager.getAction(SidebarActions.GROUP_TOOLBAR).asInstanceOf[ActionGroup]
  private val actionToolbar = actionManager.createActionToolbar(SidebarActions.ACTION_PLACE, actionGroup, true)
  actionToolbar.setTargetComponent(this)

  setBackground(JBColor.`lazy` { () =>
    Option(EditorColorsManager.getInstance().getGlobalScheme.getColor(EditorColors.PREVIEW_BACKGROUND)).getOrElse(
      EditorColorsManager.getInstance().getGlobalScheme.getDefaultBackground
    )
  })

  private val toolbarPanel = actionToolbar.getComponent
  toolbarPanel.setBackground(JBColor.`lazy`(() => Option(getBackground).getOrElse(UIUtil.getPanelBackground)))

  private val topPanel = NonOpaquePanel(BorderLayout())
  topPanel.add(toolbarPanel, BorderLayout.WEST)
  add(topPanel, BorderLayout.NORTH)
  add(myViewer.uiComponent, BorderLayout.CENTER)

  myViewer.preferredFocusedComponent.addMouseWheelListener(MOUSE_WHEEL_LISTENER)

  if !isDebug then PopupHandler.installPopupMenu(myViewer.preferredFocusedComponent, SidebarActions.GROUP_POPUP, SidebarActions.ACTION_PLACE)

  override def uiDataSnapshot(dataSink: DataSink): Unit = {
    dataSink.set(DescriptionView.DATA_KEY, this)
    dataSink.set(PlatformDataKeys.COPY_PROVIDER, this)

  }

  override def dispose(): Unit =
    myViewer.preferredFocusedComponent.removeMouseWheelListener(MOUSE_WHEEL_LISTENER)

  def updateCurrentQuestion(question: QuestionItem): Unit =
    myViewer.updateCurrentQuestion(question)

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
    val insets = JBInsets.addInsets(
      Option(toolbarPanel.getInsets())
        .map(JBInsets.create)
        .getOrElse(JBUI.emptyInsets()),
      Option(toolbarPanel.getComponent(0)).map {
        case child: JComponent =>
          JBInsets.create(child.getInsets())
        case _                 =>
          JBUI.emptyInsets()
      }.getOrElse(JBUI.emptyInsets())
    )
    Some(insets.top, insets.right, insets.bottom, insets.left)
  }
}

object DescriptionView {
  val DATA_KEY: DataKey[DescriptionView] = DataKey.create[DescriptionView]("DescriptionView")
}
