package com.wenjunhuang.codeepiphany.editor.extensions

import java.awt.{AWTEvent, EventQueue}
import java.awt.event.{AWTEventListener, KeyAdapter}
import java.beans.PropertyChangeListener
import java.util
import javax.swing.{JComponent, JLayeredPane}

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager}
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.util.{Disposer, Key}
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.ui.components.JBLayeredPane
import com.intellij.util.Alarm
import com.intellij.util.ui.{StartupUiUtil, UIUtil}

import com.wenjunhuang.codeepiphany.editor.extensions.ChallengeEditor.*
import com.wenjunhuang.codeepiphany.model.Actions

class ChallengeEditor(private val myDelegate: TextEditor, private val myName: String = "ChallengeEditor")
    extends TextEditor {

  private lazy val myUi = MyUi()

  EventQueue.invokeLater(() => {
    myUi
  })

  Disposer.register(this, myDelegate)

  override def getEditor: Editor = myDelegate.getEditor

  override def canNavigateTo(navigatable: Navigatable): Boolean = myDelegate.canNavigateTo(navigatable)

  override def navigateTo(navigatable: Navigatable): Unit = myDelegate.navigateTo(navigatable)

  override def isEditorLoaded: Boolean = myDelegate.isEditorLoaded

  override def getComponent: JComponent = myUi.myLayeredPane

  override def getName: String = myName

  override def getFile: VirtualFile = myDelegate.getFile

  override def getPreferredFocusedComponent: JComponent = myDelegate.getPreferredFocusedComponent

  override def setState(state: FileEditorState): Unit = myDelegate.setState(state)

  override def getState(level: FileEditorStateLevel): FileEditorState = myDelegate.getState(level)

  override def isModified: Boolean = myDelegate.isModified

  override def isValid: Boolean = myDelegate.isValid

  override def addPropertyChangeListener(listener: PropertyChangeListener): Unit =
    myDelegate.addPropertyChangeListener(listener)

  override def removePropertyChangeListener(listener: PropertyChangeListener): Unit =
    myDelegate.removePropertyChangeListener(listener)

  override def dispose(): Unit = {}

  override def getUserData[T](key: Key[T]): T = myDelegate.getUserData(key)

  override def putUserData[T](key: Key[T], value: T): Unit = myDelegate.putUserData(key, value)

  override def setState(state: FileEditorState, exactState: Boolean): Unit = myDelegate.setState(state, exactState)

  override def selectNotify(): Unit = myDelegate.selectNotify()

  override def deselectNotify(): Unit = myDelegate.deselectNotify()

  override def getBackgroundHighlighter: BackgroundEditorHighlighter = myDelegate.getBackgroundHighlighter

  override def getCurrentLocation: FileEditorLocation = myDelegate.getCurrentLocation

  override def getStructureViewBuilder: StructureViewBuilder = myDelegate.getStructureViewBuilder

  override def getFilesToRefresh: util.List[VirtualFile] = myDelegate.getFilesToRefresh

  override def getTabActions: ActionGroup = myDelegate.getTabActions

  private def createActionGroup(): ActionGroup = {
    val ag = ActionManager.getInstance().getAction(Actions.CHALLENGE_EDITOR_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
    ag
  }

  private def registerToolbarListener(actualComponent: JComponent, toolbar: LayoutActionsFloatingToolbar): Unit = {
    StartupUiUtil.addAwtListener(AWTEvent.MOUSE_MOTION_EVENT_MASK, ChallengeEditor.this, MyMouseListener(toolbar))
    UIUtil.findComponentOfType(actualComponent, classOf[EditorComponentImpl]) match
      case null =>
      case actualEditor =>
        val editorKeyListener = new KeyAdapter {
          override def keyTyped(e: java.awt.event.KeyEvent): Unit = {
            toolbar.scheduleHide()
          }
        }
        actualEditor.getEditor.getContentComponent.addKeyListener(editorKeyListener)
        Disposer.register(
          ChallengeEditor.this,
          () => actualEditor.getEditor.getContentComponent.removeKeyListener(editorKeyListener)
        )
  }

  private class MyUi {
    private val myEditorComponent = myDelegate.getComponent
    val myLayeredPane             = MyEditorLayeredComponentWrapper(myEditorComponent)
    private val myToolbar = LayoutActionsFloatingToolbar(myLayeredPane, createActionGroup(), ChallengeEditor.this)

    myLayeredPane.add(myEditorComponent, JLayeredPane.DEFAULT_LAYER)
    myLayeredPane.add(myToolbar, JLayeredPane.POPUP_LAYER)

    registerToolbarListener(myEditorComponent, myToolbar)
  }

  private class MyMouseListener(private val myToolbar: LayoutActionsFloatingToolbar) extends AWTEventListener {
    private val myAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, ChallengeEditor.this)

    override def eventDispatched(event: AWTEvent): Unit = {
      val isMouseOutsideToolbar = myToolbar.getMousePosition == null
      if getComponent.getMousePosition != null then
        myAlarm.cancelAllRequests()
        myToolbar.scheduleShow()
        if isMouseOutsideToolbar then myAlarm.addRequest(() => myToolbar.scheduleHide(), 1400)
      else if isMouseOutsideToolbar then myToolbar.scheduleHide()
    }
  }
}

object ChallengeEditor {
  private final val TOOLBAR_RIGHT_PADDING = 25
  private final val TOOLBAR_TOP_PADDING   = 20

  class MyEditorLayeredComponentWrapper(private val myEditorComponent: JComponent) extends JBLayeredPane {
    override def doLayout(): Unit = {
      val bounds = getBounds
      getComponents.foreach { component =>
        if component == myEditorComponent then component.setBounds(0, 0, bounds.width, bounds.height)
        else
          val preferredComponentSize = component.getPreferredSize
          var x                      = 0
          var y                      = 0
          if component.isInstanceOf[LayoutActionsFloatingToolbar] then
            x = bounds.width - preferredComponentSize.width - TOOLBAR_RIGHT_PADDING
            y = TOOLBAR_TOP_PADDING
          component.setBounds(x, y, preferredComponentSize.width, preferredComponentSize.height)
      }

    }
  }
}
