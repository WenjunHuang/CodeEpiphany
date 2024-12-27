package com.wenjunhuang.codeepiphany.hackerrank

import cats.effect.SyncIO
import com.intellij.icons.AllIcons
import com.intellij.ide.fileTemplates.{ FileTemplate, FileTemplateManager }
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ex.ActionManagerEx
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.actionSystem.{
  ActionManager,
  AnActionEvent,
  DefaultActionGroup,
  ToggleAction
}
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.{ DocumentEvent, DocumentListener }
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.{ LayerDescriptor, LayeredLexerEditorHighlighter }
import com.intellij.openapi.editor.highlighter.{ EditorHighlighter, EditorHighlighterFactory }
import com.intellij.openapi.editor.{ Document, EditorFactory }
import com.intellij.openapi.fileTypes.{
  FileTypeManager,
  FileTypes,
  PlainSyntaxHighlighter,
  SyntaxHighlighterFactory
}
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.observable.util.BindUtil
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.{ DumbAwareAction, Project }
import com.intellij.openapi.ui.{ ComboBox, DialogPanel, Splitter }
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.hackerrank.HackerRankSettingsConfigurable.LANGUAGES
import com.wenjunhuang.codeepiphany.hackerrank.model.demo.DEMOS
import com.wenjunhuang.codeepiphany.model.template.ChallengeFileTemplateHighlighter
import com.wenjunhuang.codeepiphany.model.template.lexer.ChallengeFileTemplateTokenType

import com.wenjunhuang.codeepiphany.model.{ CodeDojo, Language, LanguageVersion }
import com.wenjunhuang.codeepiphany.utils.ui.dsl.*
import org.typelevel.log4cats.{ Logger, LoggerFactory }
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.template.VelocityUtils

import java.awt.{ BorderLayout, Dimension, FlowLayout, GridBagConstraints, GridBagLayout }
import javax.swing.{ JComponent, JList, JPanel, ListCellRenderer, SwingConstants }
import scala.compiletime.uninitialized

class HackerRankSettingsConfigurable(private val myProject: Project)
    extends Configurable
    with Disposable {
  private val myLogger: Logger[SyncIO] = LoggerFactory[SyncIO].getLogger
  private val mySettings               = HackerRankSettings.getInstance(myProject)
  private val myVelocityFileType =
    FileTypeManager.getInstance().getFileTypeByExtension("ft")

  private var myLanguageComboBox: ComboBox[Language] = uninitialized
  private val myCodeFileNameDocument        = EditorFactory.getInstance().createDocument("")
  private val myCodeFileNameEditor          = createFileNameEditor(myCodeFileNameDocument, false)
  private val myCodeFileNamePreviewDocument = EditorFactory.getInstance().createDocument("")
  private val myCodeFileNamePreviewEditor =
    createFileNameEditor(myCodeFileNamePreviewDocument, true)
  myCodeFileNameDocument.addDocumentListener(
    new DocumentListener() {
      override def documentChanged(event: DocumentEvent): Unit = {
        val language = myLanguageComboBox.getItem
        DEMOS.get(language) match {
          case Some(demo) =>
            VelocityUtils
              .generateContent(event.getDocument.getText, Map("challenge" -> demo)) match {
              case Right(content) =>
                WriteAction.run(() =>
                  myCodeFileNamePreviewDocument.setText(
                    s"${StringUtil.convertLineSeparators(content)}.${language.fileExt}"
                  )
                )
              case Left(e) =>
                myLogger.warn(e)(s"Failed to generate content for ${language.show}").unsafeRunSync()
            }
          case None =>
        }
      }
    },
    this
  )

  private val myCodeSourceTemplateDocument        = EditorFactory.getInstance().createDocument("")
  private val myCodeSourceTemplatePreviewDocument = EditorFactory.getInstance().createDocument("")

  myCodeSourceTemplateDocument.addDocumentListener(
    new DocumentListener() {
      override def documentChanged(event: DocumentEvent): Unit = {
        val language = myLanguageComboBox.getItem
        DEMOS.get(language) match {
          case Some(demo) =>
            VelocityUtils.generateContent(event.getDocument.getText, Map("challenge" -> demo)) match
              case Right(content) =>
                WriteAction.run(() =>
                  myCodeSourceTemplatePreviewDocument.setText(
                    StringUtil.convertLineSeparators(content)
                  )
                )
              case Left(e) =>
                myLogger.warn(e)(s"Failed to generate content for ${language.show}").unsafeRunSync()
          case None =>
        }
      }
    },
    this
  )

  private val myCodeSourceTemplateEditor =
    createCodeTemplateEditor(myCodeSourceTemplateDocument, false)
  private val myCodeSourceTemplatePreviewEditor =
    createCodeTemplateEditor(myCodeSourceTemplatePreviewDocument, true)

  private val myCodePreviewVisible         = AtomicBooleanProperty(true)
  private val myCodeFileNamePreviewVisible = AtomicBooleanProperty(true)

  private var myPanel: DialogPanel = uninitialized

  override def getDisplayName: String =
    PluginBundle.message("hackerrank.settings.title")

  override def createComponent(): JComponent = {
    if myPanel == null then
      myPanel = dialogPanel {
        row("Source Folder:") {
          textFieldWithBrowseButton(
            "Choose the folder where you want to save your HackerRank solutions",
            myProject,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
          ).bindText(
            () => mySettings.getState.sourceFolder,
            (s: String) => mySettings.getState.sourceFolder = s
          ).align(AlignX.FILL.INSTANCE)
            .resizableColumn()
            .getComponent
        }

        row("Language:") {
          myLanguageComboBox = comboBox(
            LANGUAGES,
            (
              list: JList[? <: Language],
              value: Language,
              index: Int,
              isSelected: Boolean,
              cellHasFocus: Boolean
            ) =>
              if value == null then
                JBLabel(PluginBundle.message("hackerrank.ui.settings.language.hint"))
              else JBLabel(value.show, value.icon, SwingConstants.LEFT)
          ).bindItem(
            () => Option(HackerRankSettings.getInstance(myProject).getState.language).orNull,
            (l: Language) => HackerRankSettings.getInstance(myProject).getState.language = l
          ).align(AlignX.FILL.INSTANCE)
            .resizableColumn()
            .getComponent
        }

        row() {
          val togglePreview =
            new ToggleAction(
              PluginBundle.message("hackerrank.ui.settings.togglePreview"),
              null,
              AllIcons.Actions.ToggleVisibility
            ) {
              override def isSelected(e: AnActionEvent): Boolean = myCodeFileNamePreviewVisible.get

              override def setSelected(e: AnActionEvent, state: Boolean): Unit =
                myCodeFileNamePreviewVisible.set(state)
            }

          val applyTemplate =
            new DumbAwareAction(
              PluginBundle.message("hackerrank.ui.settings.useDefaultTemplate"),
              null,
              AllIcons.Actions.Refresh
            ) {
              override def actionPerformed(e: AnActionEvent): Unit = {
                val language = myLanguageComboBox.getItem
                updateCodeFileNameTemplateDocumentText(language)
              }
            }

          val panel       = JPanel(BorderLayout())
          val actionGroup = DefaultActionGroup()
          actionGroup.add(togglePreview)
          actionGroup.add(applyTemplate)

          val toolbar = ActionManager
            .getInstance()
            .asInstanceOf[ActionManagerEx]
            .createActionToolbar("HackerRankSetting.FileName", actionGroup, true, false, false)
            .asInstanceOf[ActionToolbarImpl]
          toolbar.setActionButtonBorder(JBUI.Borders.empty(0, 0, 0, 5))
          toolbar.setBorder(JBUI.Borders.empty(2, 0, 5, 0))
          toolbar.setTargetComponent(panel)

          val editor  = myCodeFileNameEditor.getComponent
          val preview = myCodeFileNamePreviewEditor.getComponent

          BindUtil.bindVisible(preview, myCodeFileNamePreviewVisible)
          val splitter = Splitter(false, 0.6)
          splitter.setFirstComponent(editor)
          splitter.setSecondComponent(preview)

          panel.add(toolbar, BorderLayout.NORTH)
          panel.add(splitter, BorderLayout.CENTER)
          cell(panel)
            .label("File Name:", LabelPosition.TOP)
            .align(AlignX.FILL.INSTANCE)
            .resizableColumn()
        }

        row() {
          val togglePreview =
            new ToggleAction(
              PluginBundle.message("hackerrank.ui.settings.togglePreview"),
              null,
              AllIcons.Actions.ToggleVisibility
            ) {
              override def isSelected(e: AnActionEvent): Boolean = myCodePreviewVisible.get

              override def setSelected(e: AnActionEvent, state: Boolean): Unit =
                myCodePreviewVisible.set(state)
            }

          val applyTemplate =
            new DumbAwareAction(
              PluginBundle.message("hackerrank.ui.settings.useDefaultTemplate"),
              null,
              AllIcons.Actions.Refresh
            ) {
              override def actionPerformed(e: AnActionEvent): Unit = {
                val language = myLanguageComboBox.getItem
                updateCodeTemplateDocumentTextToTemplate(language)
              }
            }

          val panel = JPanel(BorderLayout())

          val actionGroup = DefaultActionGroup()
          actionGroup.add(togglePreview)
          actionGroup.add(applyTemplate)

          val toolbar = ActionManager
            .getInstance()
            .asInstanceOf[ActionManagerEx]
            .createActionToolbar("HackerRankSetting.FileName", actionGroup, true, false, false)
            .asInstanceOf[ActionToolbarImpl]
          toolbar.setActionButtonBorder(JBUI.Borders.empty())
          toolbar.setBorder(JBUI.Borders.empty(2, 0, 5, 0))
          toolbar.setTargetComponent(panel)

          val templateEditor  = myCodeSourceTemplateEditor.getComponent
          val templatePreview = myCodeSourceTemplatePreviewEditor.getComponent

          BindUtil.bindVisible(templatePreview, myCodePreviewVisible)
          val splitter = Splitter(false, 0.6f)
          splitter.setFirstComponent(templateEditor)
          splitter.setSecondComponent(templatePreview)

          panel.add(toolbar.getComponent, BorderLayout.NORTH)
          panel.add(splitter, BorderLayout.CENTER)
          panel.setPreferredSize(JBUI.size(400, 300))
          cell(panel)
            .label("Code Template:", LabelPosition.TOP)
            .align(AlignX.FILL.INSTANCE)
            .resizableColumn()
        }
      }

    myPanel
  }

  override def isModified: Boolean = Option(myPanel).exists(_.isModified)

  override def apply(): Unit =
    myPanel.apply()

  override def reset(): Unit =
    myPanel.reset()

  private def createFileNameEditor(document: Document, preview: Boolean): EditorEx = {
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

    editor.asInstanceOf[EditorEx]
  }

  private def createCodeTemplateEditor(document: Document, viewer: Boolean): EditorEx = {
    val editorFactory = EditorFactory.getInstance()
    val editor =
      if !viewer then editorFactory.createEditor(document, myProject)
      else editorFactory.createViewer(document, myProject)

    val editorSettings = editor.getSettings
    editorSettings.setVirtualSpace(false)
    editorSettings.setLineMarkerAreaShown(false)
    editorSettings.setIndentGuidesShown(false)
    editorSettings.setFoldingOutlineShown(false)
    editorSettings.setLineNumbersShown(false)
    editorSettings.setAdditionalColumnsCount(0)
    editorSettings.setAdditionalLinesCount(0)
    editorSettings.setCaretRowShown(false)

    editor.asInstanceOf[EditorEx]
  }

  override def disposeUIResources(): Unit = {
    val editorFactory = EditorFactory.getInstance()
    editorFactory.releaseEditor(myCodeFileNameEditor)
    editorFactory.releaseEditor(myCodeFileNamePreviewEditor)
    editorFactory.releaseEditor(myCodeSourceTemplateEditor)
    editorFactory.releaseEditor(myCodeSourceTemplatePreviewEditor)

    Disposer.dispose(this)
  }

  private def getLanguageTemplate(language: Language): FileTemplate = {
    val templateName = s"${CodeDojo.HackerRank.value}_code.${language.fileExt}"
    val template     = FileTemplateManager.getInstance(myProject).findInternalTemplate(templateName)
    template
  }

  private def getFileNameTemplate(language: Language): FileTemplate = {
    val templateName = s"${CodeDojo.HackerRank.value}_filename.${language.fileExt}"
    val template     = FileTemplateManager.getInstance(myProject).findInternalTemplate(templateName)
    template
  }

  private def updateCodeFileNameTemplateDocumentText(language: Language): Unit = {
    val template = getFileNameTemplate(language)
    if template != null then
      val highlighterEdit    = createHighlighter(language)
      val highlighterPreview = createHighlighter(language)
      WriteAction.run { () =>
        myCodeFileNameDocument.setText(template.getText.trim)
        myCodeFileNameEditor.setHighlighter(highlighterEdit)
        myCodeFileNamePreviewEditor.setHighlighter(highlighterPreview)
      }

  }
  private def updateCodeTemplateDocumentTextToTemplate(language: Language): Unit = {
    val template = getLanguageTemplate(language)
    if template != null then
      val highlighterEdit    = createHighlighter(language)
      val highlighterPreview = createHighlighter(language)
      WriteAction.run { () =>
        myCodeSourceTemplateDocument.setText(template.getText)
        myCodeSourceTemplateEditor.setHighlighter(highlighterEdit)
        myCodeSourceTemplatePreviewEditor.setHighlighter(highlighterPreview)
      }
  }

  private def createHighlighter(language: Language): EditorHighlighter =
    if myVelocityFileType != FileTypes.UNKNOWN then
      EditorHighlighterFactory
        .getInstance()
        .createEditorHighlighter(myProject, LightVirtualFile(s"template.${language.fileExt}.ft"))
    else
      val fileType =
        Option(FileTypeManager.getInstance().getFileTypeByExtension(language.fileExt)).map {
          fileType => if fileType == FileTypes.UNKNOWN then FileTypes.PLAIN_TEXT else fileType
        }.getOrElse(FileTypes.PLAIN_TEXT)

      val originalHighlighter =
        Option(SyntaxHighlighterFactory.getSyntaxHighlighter(fileType, myProject, null))
          .getOrElse(PlainSyntaxHighlighter())
      val highlighter = LayeredLexerEditorHighlighter(
        ChallengeFileTemplateHighlighter(),
        EditorColorsManager.getInstance().getGlobalScheme
      )
      highlighter.registerLayer(
        ChallengeFileTemplateTokenType.TEXT,
        LayerDescriptor(originalHighlighter, "")
      )

      highlighter

  override def dispose(): Unit = {}
}

object HackerRankSettingsConfigurable {
  val LANGUAGES: List[Language] = Language.values.toList
}
