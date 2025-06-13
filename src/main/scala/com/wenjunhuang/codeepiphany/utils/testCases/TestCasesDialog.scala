package com.wenjunhuang.codeepiphany.utils.testCases

import java.awt.{ GridBagConstraints, GridBagLayout }
import java.awt.event.ActionEvent
import javax.swing.{ JComponent, JPanel, ScrollPaneConstants }
import scala.collection.mutable

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ ActionManager, AnAction, AnActionEvent, DefaultActionGroup }
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ui.JBUI

import com.wenjunhuang.codeepiphany.utils.walkaround.DialogWrapperBridge
import com.wenjunhuang.codeepiphany.PluginBundle

object TestCasesDialog {
  private val DIALOG_WIDTH  = 600
  private val DIALOG_HEIGHT = 400
  private val TOOLBAR_ID    = "TestCasesDialog"

  type TestCase = (String, String)

  private def createGridBagConstraints: GridBagConstraints = {
    val gbc = new GridBagConstraints()
    gbc.fill = GridBagConstraints.HORIZONTAL
    gbc.weightx = 1.0
    gbc.gridx = 0
    gbc.anchor = GridBagConstraints.NORTHWEST
    gbc
  }
}

class TestCasesDialog(private val myProject: Project, private val myInitialTestCases: List[TestCasesDialog.TestCase])
    extends DialogWrapperBridge(myProject, false, DialogWrapper.IdeModalityType.MODELESS) {
  import TestCasesDialog.*

  private val myTestCaseItemPanels = mutable.ListBuffer.from(createTestCaseItemsFromTestCases())

  private val myScrollPane = ScrollPaneFactory.createScrollPane(
    createTestCasesPanel(),
    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  )

  init()
  setSize(DIALOG_WIDTH, DIALOG_HEIGHT)

  private def createTestCaseItemsFromTestCases(): List[TestCaseItemPanel] = {
    myInitialTestCases.zipWithIndex.map { case ((input, expected), index) =>
      createNewTestCaseItemPanel(index, input, expected)
    }
  }

  private def createNewTestCaseItemPanel(index: Int, input: String, expected: String): TestCaseItemPanel = {
    var panel: TestCaseItemPanel = null
    panel = new TestCaseItemPanel(
      myProject,
      PluginBundle.message("testcases.title", index + 1),
      input,
      expected,
      () => {
        myTestCaseItemPanels.remove(myTestCaseItemPanels.indexOf(panel))
        refreshTestCaseItemPanels()
      }
    )
    panel
  }

  private def createTestCasesPanel(): JComponent = {
    val panel = new JPanel(new GridBagLayout())
    val gbc   = createGridBagConstraints

    myTestCaseItemPanels.foreach { item =>
      panel.add(item.getComponent, gbc)
    }

    panel
  }

  private def refreshTestCaseItemPanels(): Unit = {
    myScrollPane.setViewportView(createTestCasesPanel())
  }

  override protected def onOkAction(e: ActionEvent): Unit = {}

  override def createTitlePane(): JComponent = {
    val actionGroup = new DefaultActionGroup(
      new AnAction(
        PluginBundle.message("action.CodeEpiphany.Actions.TestCases.Reset.text"),
        PluginBundle.message("action.CodeEpiphany.Actions.TestCases.Reset.description"),
        AllIcons.General.Reset
      ) {
        override def actionPerformed(e: AnActionEvent): Unit = {
          myTestCaseItemPanels.clear()
          myTestCaseItemPanels.addAll(createTestCaseItemsFromTestCases())
          refreshTestCaseItemPanels()
        }
      },
      new AnAction(
        PluginBundle.message("action.CodeEpiphany.Actions.TestCases.Add.text"),
        PluginBundle.message("action.CodeEpiphany.Actions.TestCases.Add.description"),
        AllIcons.General.Add
      ) {
        override def actionPerformed(e: AnActionEvent): Unit = {
          myTestCaseItemPanels.addOne(createNewTestCaseItemPanel(myTestCaseItemPanels.size, "", ""))
          refreshTestCaseItemPanels()
          val verticalScrollBar = myScrollPane.getVerticalScrollBar
          verticalScrollBar.setValue(verticalScrollBar.getMaximum)
        }
      }
    )

    ActionManager
      .getInstance()
      .createActionToolbar(TOOLBAR_ID, actionGroup, true)
      .getComponent
  }

  override def createCenterPanel(): JComponent = myScrollPane
}
