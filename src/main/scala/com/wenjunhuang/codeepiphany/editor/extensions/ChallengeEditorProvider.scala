package com.wenjunhuang.codeepiphany.editor.extensions

import org.jdom.Element

import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider
import com.intellij.openapi.project.{ DumbAware, Project }
import com.intellij.openapi.vfs.VirtualFile
import scala.jdk.CollectionConverters.*

import com.intellij.openapi.util.Key

import com.wenjunhuang.codeepiphany.editor.actions.{
  SolutionSelectionAction,
  SurroundSubmissionRegionAction,
  TestCasesEditionAction
}
import com.wenjunhuang.codeepiphany.editor.actions.RunTestAction.{ RUNTEST_PROVIDER_KEY, RunTestProvider }
import com.wenjunhuang.codeepiphany.editor.actions.SolutionSelectionAction.SOLUTION_PROVIDER_KEY
import com.wenjunhuang.codeepiphany.editor.actions.SubmitCodeAction.{ SUBMITCODE_PROVIDER_KEY, SubmitCodeProvider }
import com.wenjunhuang.codeepiphany.editor.actions.SurroundSubmissionRegionAction.SURROUND_PROVIDER_KEY
import com.wenjunhuang.codeepiphany.editor.actions.TestCasesEditionAction.TESTCASES_PROVIDER_KEY
import com.wenjunhuang.codeepiphany.editor.extensions.ChallengeEditorProvider.FILEEDITOR_CODEDOJO_KEY
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.utils.walkaround.FileEditorProviderBridge

/** 挑战编辑器提供者 负责创建和管理挑战相关的编辑器
  */
class ChallengeEditorProvider extends FileEditorProviderBridge with DumbAware {
  private val delegate = PsiAwareTextEditorProvider()

  override def accept(project: Project, file: VirtualFile): Boolean = {
    delegate.accept(project, file) &&
    ChallengeSettings.getInstance(project).findChallengeId(file).isDefined
  }

  override def getPolicy: FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

  override def createEditor(project: Project, file: VirtualFile): FileEditor = {
    val textEditor = delegate.createEditor(project, file).asInstanceOf[TextEditor]
    setupEditor(textEditor, project, file)
  }

  override def readState(element: Element, project: Project, file: VirtualFile): FileEditorState = {
    delegate.readState(element, project, file)
  }

  override def writeState(state: FileEditorState, project: Project, element: Element): Unit = {
    delegate.writeState(state, project, element)
  }

  override def getEditorTypeId: String = s"LeetCodeEpiphany.${delegate.getEditorTypeId}"

  /** 设置编辑器
    * @param editor
    *   文本编辑器
    * @param project
    *   项目
    * @param file
    *   虚拟文件
    * @return
    *   配置好的编辑器
    */
  private def setupEditor(editor: TextEditor, project: Project, file: VirtualFile): TextEditor = {
    ChallengeSettings.getInstance(project).findChallengeId(file) match {
      case Some(challenge) => configureEditor(editor, project, file, challenge)
      case None            => editor // 不应该发生
    }
  }

  /** 配置编辑器
    * @param editor
    *   文本编辑器
    * @param project
    *   项目
    * @param file
    *   虚拟文件
    * @param challenge
    *   挑战信息
    * @return
    *   配置好的编辑器
    */
  private def configureEditor(
    editor: TextEditor,
    project: Project,
    file: VirtualFile,
    challenge: ChallengeSettings.ChallengeSettingsStateItem
  ): TextEditor = {
    val editorWrapper = ChallengeEditor(editor)

    // 配置提交代码提供者
    editorWrapper.putUserData(SUBMITCODE_PROVIDER_KEY, SubmitCodeProvider.createProvider(file, project, challenge.dojo))
    if (challenge.dojo == CodeDojo.LeetCode || challenge.dojo == CodeDojo.LeetCodeCN) {
      // 如果是 LeetCode 的挑战，配置测试代码
      editorWrapper.putUserData(RUNTEST_PROVIDER_KEY, RunTestProvider.createProvider(file, project, challenge.dojo))
    }
    editorWrapper.putUserData(FILEEDITOR_CODEDOJO_KEY, challenge.dojo)
    editorWrapper.putUserData(
      TESTCASES_PROVIDER_KEY,
      new TestCasesEditionAction.TestCasesEditionProvider {

        override def getDefaultTestCases: List[ChallengeSettings.TestCase] = {
          challenge.defaultTestCases.asScala.toList
        }

        override def getTestCases: List[ChallengeSettings.TestCase] = {
          ChallengeSettings.getInstance(project).findChallengeId(file) match {
            case Some(challenge) => challenge.testCases.asScala.toList
            case None            => List.empty
          }
        }

        override def updateTestCases(testCases: List[ChallengeSettings.TestCase]): Unit = {
          ChallengeSettings.getInstance(project).updateChallengeTestCases(file, testCases.asJava)
        }
      }
    )

    // 配置解决方案选择提供者
    val solutionProvider = SolutionSelectionAction.createSolutionSelectionProvider(project, file)
    editorWrapper.putUserData(SOLUTION_PROVIDER_KEY, solutionProvider)

    // 配置提交区域环绕提供者
    editorWrapper.putUserData(
      SURROUND_PROVIDER_KEY,
      SurroundSubmissionRegionAction.createProvider(editorWrapper.getEditor.asInstanceOf[EditorEx], project)
    )

    editorWrapper
  }
}
object ChallengeEditorProvider {
  val FILEEDITOR_CODEDOJO_KEY: Key[CodeDojo] = Key.create("ChallengeEditorCodeDojo")
}
