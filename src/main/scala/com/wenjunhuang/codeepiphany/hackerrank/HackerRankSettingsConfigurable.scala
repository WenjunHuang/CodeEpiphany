package com.wenjunhuang.codeepiphany.hackerrank
import com.intellij.icons.AllIcons
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.impl.FileTemplateHighlighter
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.actionSystem.{ ActionPlaces, AnActionEvent }
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.{ DocumentEvent, DocumentListener }
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.{ LayerDescriptor, LayeredLexerEditorHighlighter }
import com.intellij.openapi.editor.highlighter.{ EditorHighlighter, EditorHighlighterFactory }
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.{ Document, Editor, EditorFactory }
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileTypes.{
  FileTypeManager,
  FileTypes,
  PlainSyntaxHighlighter,
  SyntaxHighlighterFactory
}
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.{ DumbAwareAction, Project }
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.psi.{ PsiDocumentManager, PsiFile }
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.hackerrank.HackerRankSettingsConfigurable.DEMO_CODE
import com.wenjunhuang.codeepiphany.model.template.{ ChallengeFileTemplateHighlighter, ChallengeFileTemplateTokenType }
import com.wenjunhuang.codeepiphany.utils.ui.dsl.*

import java.awt.{ BorderLayout, Dimension }
import javax.swing.JComponent
import scala.compiletime.uninitialized

class HackerRankSettingsConfigurable(private val myProject: Project) extends Configurable {
  private val myVelocityFileType =
    FileTypeManager.getInstance().getFileTypeByExtension("ft")

  private var myTFSourceFolder: TextFieldWithBrowseButton = uninitialized

  private var myCodeFileNameEditor: Editor = uninitialized

  private var myCodeSourceTemplate: Option[FileTemplate] = None
  private var myCodeSourceTemplateEditor: Editor         = uninitialized
  private val myCodePreviewVisible                       = AtomicBooleanProperty(true)
  private val myPanel = dialogPanel {
    row("Source Folder:") {
      myTFSourceFolder = textFieldWithBrowseButton(
        "Choose the folder where you want to save your HackerRank solutions",
        myProject,
        FileChooserDescriptorFactory.createSingleFolderDescriptor()
      ).align(AlignX.FILL.INSTANCE)
        .resizableColumn()
        .getComponent
    }

    row() {
      comboBox(List("Java", "Kotlin", "Python", "Scala"))
        .align(AlignX.FILL.INSTANCE)
        .resizableColumn()
        .label("Language:")
      comboBox(List("8", "11", "14", "17"))
        .align(AlignX.FILL.INSTANCE)
        .resizableColumn()
        .label("Version:")
    }.layout(RowLayout.PARENT_GRID)

    row() {
      val togglePreview =
        new DumbAwareAction("Preview", "Toggle Preview", AllIcons.General.PreviewHorizontally) {
          override def actionPerformed(e: AnActionEvent): Unit =
            myCodePreviewVisible.set(!myCodePreviewVisible.get)
        }

      val l = JBLabel("Code File Name:")
      val s = l.getPreferredSize.height

      val component = ActionButton(
        togglePreview,
        togglePreview.getTemplatePresentation.clone(),
        ActionPlaces.UNKNOWN,
        Dimension(s, s)
      )
      l.setCopyable(true)

      component.setBorder(JBUI.Borders.emptyLeft(5))
      l.add(component, BorderLayout.EAST)
      val nameDoc = EditorFactory.getInstance().createDocument("${CHALLENGE_ID}.java")
      myCodeFileNameEditor = createFileNameEditor(nameDoc, false)
      cell(myCodeFileNameEditor.getComponent)
        .label(l, LabelPosition.TOP)
        .align(AlignX.FILL.INSTANCE)
        .resizableColumn()
    }

    row() {
      myCodeSourceTemplateEditor = createCodeTemplateEditor(None, false)
      cell(myCodeSourceTemplateEditor.getComponent)
        .label("Code File Template:", LabelPosition.TOP)
        .align(AlignX.FILL.INSTANCE)
        .resizableColumn()
    }.rowComment("Choose the template for the code file")

//      val right = dialogPanel {
//        row() {
//          val label = JBLabel("File Name:")
//          label.setCopyable(true)
//          textField()
//            .label(label, LabelPosition.TOP)
//            .align(AlignX.FILL.INSTANCE)
//            .resizableColumn()
//
//        }
//
//        row() {
//          myCodeSourceTemplateEditor = createCodeTemplateEditor(None,true)
//          cell(myCodeSourceTemplateEditor.getComponent)
//            .label("Code File:", LabelPosition.TOP)
//            .align(AlignX.FILL.INSTANCE)
//            .resizableColumn()
//        }.rowComment("Choose the template for the code file")
//      }

//      BindUtil.bindVisible(right, myCodePreviewVisible)

//      splitter(left, right)
//        .align(AlignX.FILL.INSTANCE)
//        .resizableColumn()
//    }

//    row() {
//      splitter(templatePanel.getComponent, previewPanel.getComponent)
//        .align(AlignX.FILL.INSTANCE)
//        .resizableColumn()
//    }
  }

  override def getDisplayName: String =
    PluginBundle.message("hackerrank.settings.title")

  override def createComponent(): JComponent = myPanel

  override def isModified: Boolean = myPanel.isModified

  override def apply(): Unit = {}

  override def reset(): Unit = {
    val settings = HackerRankSettings.getInstance(myProject)
  }

  private def createFileNameEditor(document: Document, preview: Boolean): Editor = {
    val editorFactory = EditorFactory.getInstance()
    val editor =
      if preview then editorFactory.createViewer(document, myProject)
      else editorFactory.createEditor(document, myProject)
    editor.asInstanceOf[EditorEx].setOneLineMode(true)

    val editorSettings = editor.getSettings
    editorSettings.setVirtualSpace(false)
    editorSettings.setLineMarkerAreaShown(false)
    editorSettings.setIndentGuidesShown(false)
    editorSettings.setFoldingOutlineShown(false)
    editorSettings.setLineNumbersShown(false)
    editorSettings.setAdditionalColumnsCount(0)
    editorSettings.setAdditionalLinesCount(0)
    editorSettings.setCaretRowShown(false)
    editorSettings.setAdditionalPageAtBottom(true)

    editor
  }

  private def createCodeTemplateEditor(file: Option[PsiFile], viewer: Boolean): Editor = {
    val editorFactory = EditorFactory.getInstance()
    val doc           = createDocument(file)
    val editor =
      if !viewer then editorFactory.createEditor(doc, myProject)
      else editorFactory.createViewer(doc, myProject)

    val editorSettings = editor.getSettings
    editorSettings.setVirtualSpace(false)
    editorSettings.setLineMarkerAreaShown(false)
    editorSettings.setIndentGuidesShown(false)
    editorSettings.setFoldingOutlineShown(false)
    editorSettings.setLineNumbersShown(false)
    editorSettings.setAdditionalColumnsCount(3)
    editorSettings.setAdditionalLinesCount(6)
    editorSettings.setCaretRowShown(false)

    editor.getDocument.addDocumentListener(
      new DocumentListener {
        override def documentChanged(event: DocumentEvent): Unit =
          onTextChanged()
      },
      editor.asInstanceOf[EditorImpl].getDisposable
    )

    editor.asInstanceOf[EditorEx].setHighlighter(createHighlighter())

    editor
  }

  private def onTextChanged(): Unit = {}

  private def createDocument(file: Option[PsiFile]): Document =
    file match {
      case Some(f) => PsiDocumentManager.getInstance(myProject).getDocument(f)
      case None    => EditorFactory.getInstance().createDocument(DEMO_CODE)
    }

  private def createHighlighter(): EditorHighlighter =
    if myCodeSourceTemplate.isDefined && myVelocityFileType != FileTypes.UNKNOWN
    then
      EditorHighlighterFactory
        .getInstance()
        .createEditorHighlighter(
          myProject,
          LightVirtualFile(s"template.${myCodeSourceTemplate.map(_.getExtension)}.ft")
        )
    else
      val fileType = myCodeSourceTemplate
        .map(tmp => FileTypeManager.getInstance().getFileTypeByExtension(tmp.getExtension))
        .getOrElse(FileTypeManager.getInstance.getFileTypeByExtension("java"))

      val originalHighlighter =
        Option(
          SyntaxHighlighterFactory
            .getSyntaxHighlighter(fileType, null, null)
        ).getOrElse(PlainSyntaxHighlighter())
      val scheme = EditorColorsManager.getInstance().getGlobalScheme
//      val highlighter = LexerEditorHighlighter(originalHighlighter, scheme)
      val highlighter =
        LayeredLexerEditorHighlighter(ChallengeFileTemplateHighlighter(), scheme)
      highlighter.registerLayer(ChallengeFileTemplateTokenType.TEXT, LayerDescriptor(originalHighlighter, ""))

      highlighter

}

object HackerRankSettingsConfigurable {
  val DEMO_CODE =
    """package leetcode.editor.cn;
      |
      |public class FindACorrespondingNodeOfABinaryTreeInACloneOfThatTree {
      |    public static void main(String[] args) {
      |        Solution solution = new FindACorrespondingNodeOfABinaryTreeInACloneOfThatTree().new Solution();
      |    }
      |    //leetcode submit region begin(Prohibit modification and deletion)
      |
      |    /**
      |     * Definition for a binary tree node.
      |     * public class TreeNode {
      |     * int val;
      |     * TreeNode left;
      |     * TreeNode right;
      |     * TreeNode(int x) { val = x; }
      |     * }
      |     */
      |
      |    class Solution {
      |        public final TreeNode getTargetCopy(final TreeNode original, final TreeNode cloned, final TreeNode target) {
      |            return traverse(original, cloned, target);
      |
      |//            return getTargetCopyImpl(original, cloned, target);
      |        }
      |
      |        /**
      |         * 分解问题
      |         */
      |        private TreeNode getTargetCopyImpl(final TreeNode original, final TreeNode cloned, final TreeNode target) {
      |            if (original == null) return null;
      |            if (original == target) return cloned;
      |
      |            TreeNode left = getTargetCopyImpl(original.left, cloned.left, target);
      |            if (left != null) return left;
      |            return getTargetCopyImpl(original.right, cloned.right, target);
      |        }
      |
      |        private TreeNode res = null;
      |
      |        private TreeNode traverse(final TreeNode original, final TreeNode cloned, final TreeNode target) {
      |            res = null;
      |            traverseImpl(original, cloned, target);
      |            return res;
      |        }
      |
      |        private void traverseImpl(final TreeNode original, final TreeNode cloned, final TreeNode target) {
      |            if (original == null || res != null) return;
      |            if (original == target) {
      |                res = cloned;
      |            } else {
      |                traverseImpl(original.left, cloned.left, target);
      |                traverseImpl(original.right, cloned.right, target);
      |            }
      |
      |        }
      |    }
      |//leetcode submit region end(Prohibit modification and deletion)
      |
      |}
      |""".stripMargin.replace("\r\n", "\n")
}
