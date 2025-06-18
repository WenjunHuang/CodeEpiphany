package com.wenjunhuang.codeepiphany.utils.testCases

import java.awt.{GridBagConstraints, GridBagLayout}
import java.awt.event.ActionEvent
import javax.swing.{JComponent, JPanel, ScrollPaneConstants}
import scala.collection.mutable

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ActionManager, AnAction, AnActionEvent, DefaultActionGroup}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ScrollPaneFactory

import com.wenjunhuang.codeepiphany.{settings, PluginBundle}
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.utils.walkaround.DialogWrapperBridge

object TestCasesDialog {
  private val DIALOG_WIDTH  = 600
  private val DIALOG_HEIGHT = 500
  private val TOOLBAR_ID    = "TestCasesDialog"

  private def createGridBagConstraints: GridBagConstraints = {
    val gbc = new GridBagConstraints()
    gbc.fill = GridBagConstraints.HORIZONTAL
    gbc.weightx = 1.0
    gbc.gridx = 0
    gbc.anchor = GridBagConstraints.NORTHWEST
    gbc
  }
}

class TestCasesDialog(
  private val myProject: Project,
  private val myTestCases: List[ChallengeSettings.TestCase],
  private val myDefaultTestCases: List[ChallengeSettings.TestCase]
) extends DialogWrapperBridge(myProject, false, DialogWrapper.IdeModalityType.IDE) {
  import TestCasesDialog.*

  private val myTestCaseItemPanels = mutable.ListBuffer.from(createTestCaseItemsFromTestCases(myTestCases))

  private val myScrollPane = ScrollPaneFactory.createScrollPane(
    createTestCasesPanel(),
    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  )

  init()
  setTitle(PluginBundle.message("testcases.dialog.title"))
  setSize(DIALOG_WIDTH, DIALOG_HEIGHT)

  private def createTestCaseItemsFromTestCases(testCases: List[ChallengeSettings.TestCase]): List[TestCaseItemPanel] = {
    testCases.zipWithIndex.map { case (testCase, index) =>
      createNewTestCaseItemPanel(index, testCase)
    }
  }

  private def createNewTestCaseItemPanel(index: Int, testCase: ChallengeSettings.TestCase): TestCaseItemPanel = {
    var panel: TestCaseItemPanel = null
    panel = new TestCaseItemPanel(
      myProject,
      PluginBundle.message("testcases.title", index + 1),
      testCase.input,
      testCase.expectedOutput,
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

    gbc.weighty = 1.0
    gbc.fill = GridBagConstraints.BOTH
    panel.add(JPanel(), gbc)
    panel
  }

  private def refreshTestCaseItemPanels(): Unit = {
    myScrollPane.setViewportView(createTestCasesPanel())
  }

  def getTestCases(): List[ChallengeSettings.TestCase] = {
    myTestCaseItemPanels.map(item => ChallengeSettings.TestCase(item.getInputValue, item.getExpectedValue)).toList
  }

  override protected def onOkAction(e: ActionEvent): Unit = {
    close(DialogWrapper.OK_EXIT_CODE)
  }

  override def createTitlePane(): JComponent = {
    val actionGroup = new DefaultActionGroup(
      new AnAction(
        PluginBundle.message("action.CodeEpiphany.Actions.TestCases.Reset.text"),
        PluginBundle.message("action.CodeEpiphany.Actions.TestCases.Reset.description"),
        AllIcons.General.Reset
      ) {
        override def actionPerformed(e: AnActionEvent): Unit = {
          myTestCaseItemPanels.clear()
          myTestCaseItemPanels.addAll(createTestCaseItemsFromTestCases(myDefaultTestCases))
          refreshTestCaseItemPanels()
        }
      },
      new AnAction(
        PluginBundle.message("action.CodeEpiphany.Actions.TestCases.Add.text"),
        PluginBundle.message("action.CodeEpiphany.Actions.TestCases.Add.description"),
        AllIcons.General.Add
      ) {
        override def actionPerformed(e: AnActionEvent): Unit = {
          myTestCaseItemPanels.addOne(
            createNewTestCaseItemPanel(myTestCaseItemPanels.size, ChallengeSettings.TestCase("", ""))
          )
          refreshTestCaseItemPanels()
          val verticalScrollBar = myScrollPane.getVerticalScrollBar
          verticalScrollBar.setValue(verticalScrollBar.getMaximum)
        }
      }
    )

    val toolbar = ActionManager
      .getInstance()
      .createActionToolbar(TOOLBAR_ID, actionGroup, true)
    toolbar.setTargetComponent(myScrollPane)
    toolbar.getComponent
  }

  override def createCenterPanel(): JComponent = myScrollPane
}
