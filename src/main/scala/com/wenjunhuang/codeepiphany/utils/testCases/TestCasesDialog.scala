package com.wenjunhuang.codeepiphany.utils.testCases

import com.intellij.execution.configurations.{ConfigurationFactory, ConfigurationType, ConfigurationTypeUtil, RunConfiguration}
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.{ExecutorRegistry, ProgramRunnerUtil, RunManager}
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ActionManager, AnAction, AnActionEvent, DefaultActionGroup}
import com.intellij.openapi.application.{ApplicationManager, ModalityState}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.ScrollPaneFactory
import com.wenjunhuang.codeepiphany.model.{CodeDojo, Language}
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.utils.FileUtils
import com.wenjunhuang.codeepiphany.utils.walkaround.DialogWrapperBridge
import com.wenjunhuang.codeepiphany.{PluginBundle, settings}

import java.awt.event.ActionEvent
import java.awt.{GridBagConstraints, GridBagLayout}
import java.io.File
import java.net.URLDecoder
import javax.swing.{JComponent, JPanel, ScrollPaneConstants}
import scala.collection.mutable
import scala.jdk.FunctionConverters.*

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
  private val myDefaultTestCases: List[ChallengeSettings.TestCase],
  private val myLanguage: Language,
  private val myCodeDojo: CodeDojo,
  private val myRunConfigTitle: String,
  private val mySourceFile: String
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
      },
      createRunTestAction(testCase) match {
        case null => null
        case f    => f.asJavaConsumer
      }
    )
    panel
  }

  private def createRunTestAction(testCase: ChallengeSettings.TestCase): String => Unit = {
    myCodeDojo match {
      case CodeDojo.LeetCode | CodeDojo.LeetCodeCN =>
        null
      case _ =>
        myLanguage match {
          case Language.Python           => createPythonTestRunner()
          case Language.Java             => createJavaTestRunner()
          case Language.Cpp | Language.C => createCppTestRunner()
          case Language.GO               => createGoTestRunner()
          case _                         => null
        }
    }
  }

  private def createPythonTestRunner(): String => Unit = {
    ConfigurationTypeUtil.findConfigurationType("PythonConfigurationType") match {
      case null => null
      case pythonConfigType =>
        (input: String) => runPythonTest(input, pythonConfigType)
    }
  }

  private def createJavaTestRunner(): String => Unit = {
    ConfigurationTypeUtil.findConfigurationType("Java Scratch") match {
      case null => null
      case javaConfigType =>
        (input: String) => runJavaTest(input, javaConfigType)
    }
  }

  private def createCppTestRunner(): String => Unit = {
    ConfigurationTypeUtil.findConfigurationType("CppFileRunConfiguration") match {
      case null => null
      case cppConfigType =>
        (input: String) => runCppTest(input, cppConfigType)
    }
  }

  private def createGoTestRunner(): String => Unit = {
    ConfigurationTypeUtil.findConfigurationType("GoApplicationRunConfiguration") match {
      case null => null
      case goConfigType =>
        (input: String) => runGoTest(input, goConfigType)
    }
  }

  private def runPythonTest(input: String, configType: ConfigurationType): Unit = {
    runTest(
      input,
      configType,
      (runConfig, stdinFile) => {
        val cls = runConfig.getClass
        cls.getMethod("setRedirectInput", classOf[Boolean]).invoke(runConfig, true.asInstanceOf[Object])
        cls.getMethod("setScriptName", classOf[String]).invoke(runConfig, new File(mySourceFile).getAbsolutePath)
        cls.getMethod("setInputFile", classOf[String]).invoke(runConfig, stdinFile.getAbsolutePath)
      }
    )
  }

  private def runJavaTest(input: String, configType: ConfigurationType): Unit = {
    runTest(
      input,
      configType,
      (runConfig, stdinFile) => {
        val cls    = runConfig.getClass
        val option = cls.getMethod("getInputRedirectOptions").invoke(runConfig)
        option.getClass
          .getMethod("setRedirectInput", classOf[Boolean])
          .invoke(option, true.asInstanceOf[Object])
        option.getClass
          .getMethod("setRedirectInputPath", classOf[String])
          .invoke(option, stdinFile.getAbsolutePath)
        cls
          .getMethod("setMainClassName", classOf[String])
          .invoke(runConfig, FileUtils.getJavaMainClassName(mySourceFile))
        cls
          .getMethod("setScratchFileUrl", classOf[String])
          .invoke(runConfig, URLDecoder.decode(new File(mySourceFile).toPath.toAbsolutePath.toUri.toString, "UTF-8"))
      }
    )
  }

  private def runCppTest(input: String, configType: ConfigurationType): Unit = {
    runTest(
      input,
      configType,
      (runConfig, stdinFile) => {
        val cls = runConfig.getClass
        cls.getMethod("setRedirectInput", classOf[Boolean]).invoke(runConfig, true.asInstanceOf[Object])
        cls.getMethod("setRedirectInputPath", classOf[String]).invoke(runConfig, stdinFile.getAbsolutePath)
        val option = cls.getMethod("getOptions").invoke(runConfig)
        option.getClass.getMethod("setSourceFile", classOf[String]).invoke(option, mySourceFile)
      }
    )
  }

  private def runGoTest(input: String, configType: ConfigurationType): Unit = {
    runTest(
      input,
      configType,
      (runConfig, stdinFile) => {
        val cls = runConfig.getClass
        cls.getMethod("setRedirectInput", classOf[Boolean]).invoke(runConfig, true.asInstanceOf[Object])
        cls.getMethod("setRedirectInputPath", classOf[String]).invoke(runConfig, stdinFile.getAbsolutePath)
        cls.getMethod("setFilePathsString", classOf[String]).invoke(runConfig, new File(mySourceFile).getAbsolutePath)
      }
    )
  }

  private type Configurator = (RunConfiguration, File) => Unit

  private def runTest(input: String, configType: ConfigurationType, configure: Configurator): Unit = {
    close(DialogWrapper.OK_EXIT_CODE, true)
    ApplicationManager.getApplication.invokeLater(
      new Runnable {
        override def run(): Unit = {
          val factory   = configType.getConfigurationFactories.head
          val runConfig = factory.createTemplateConfiguration(myProject)
          runConfig.setName(s"$myRunConfigTitle")

          // Apply language-specific configuration
          configure(runConfig, createStdinFile(input))

          executeRunConfiguration(runConfig, factory)
        }
      },
      ModalityState.nonModal()
    )
  }

  private def createStdinFile(input: String): File = {
    val stdinFile = new File(new File(mySourceFile).getParentFile, s"${myRunConfigTitle}_test.txt")
    FileUtil.writeToFile(stdinFile, input)
    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(stdinFile)
    stdinFile
  }

  private def executeRunConfiguration(runConfig: RunConfiguration, factory: ConfigurationFactory): Unit = {
    val rm               = RunManager.getInstance(myProject)
    val runnerAndSetting = rm.createConfiguration(runConfig, factory)
    rm.addConfiguration(runnerAndSetting)
    rm.setSelectedConfiguration(runnerAndSetting)

    val executor = ExecutorRegistry.getInstance().getExecutorById(DefaultRunExecutor.EXECUTOR_ID)
    ProgramRunnerUtil.executeConfiguration(runnerAndSetting, executor)
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

  def getTestCases: List[ChallengeSettings.TestCase] = {
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
