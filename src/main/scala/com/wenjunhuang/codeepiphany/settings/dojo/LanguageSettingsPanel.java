package com.wenjunhuang.codeepiphany.settings.dojo;

import com.intellij.codeInsight.hint.EditorFragmentComponent;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.ui.laf.darcula.ui.DarculaEditorTextFieldBorder;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.EditorTextField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.LineSeparator;
import com.intellij.util.ui.HTMLEditorKitBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.xml.util.XmlStringUtil;
import com.wenjunhuang.codeepiphany.PluginBundle;
import com.wenjunhuang.codeepiphany.model.CodeDojo;
import com.wenjunhuang.codeepiphany.model.Language;
import com.wenjunhuang.codeepiphany.model.LanguageVersion;
import com.wenjunhuang.codeepiphany.model.template.ChallengeFileTemplateHighlighter;
import com.wenjunhuang.codeepiphany.settings.SettingsUi;
import com.wenjunhuang.codeepiphany.utils.IdeUtils;
import com.wenjunhuang.codeepiphany.utils.JavaUtils;
import com.wenjunhuang.codeepiphany.utils.template.VelocityUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import scala.Option;
import scala.util.Left;
import scala.util.Right;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class LanguageSettingsPanel extends SettingsUi<BaseCodeDojoSettings.LanguageSettingsState> {
    private TextFieldWithBrowseButton mySourceFolder;
    private EditorTextField myFileNameEditor;
    private EditorTextField myFileNamePreview;
    private EditorTextField myCodeTemplateEditor;
    private EditorTextField myCodeTemplatePreview;
    private JPanel rootPanel;
    private Splitter myFileNameSplitter;
    private JComponent myFileNameToolbarComponent;
    private JPanel myFileNameLabel;
    private JPanel myCodeTemplateLabel;
    private JComponent myCodeTemplateToolbarComponent;
    private Splitter myCodeTemplateSplitter;
    private JEditorPane myDescription;
    private Language myLanguage;
    private LanguageVersion myLanguageVersion;
    private final BiFunction<Language, LanguageVersion, Object> myDemoTemplateSupplier;
    private final CodeDojo myCodeDojo;
    private final Logger myLogger = Logger.getInstance(LanguageSettingsPanel.class);

    public LanguageSettingsPanel(Project project,
                                 CodeDojo codeDojo,
                                 BiFunction<Language, LanguageVersion, Object> demoTemplateSupplier) {
        super(project);

        myDemoTemplateSupplier = demoTemplateSupplier;
        myCodeDojo = codeDojo;

        $$$setupUI$$$();

        mySourceFolder.addActionListener(new MyBrowseFolderListener(PluginBundle.message("ui.settings.sourceFolder.title"),
                null,
                mySourceFolder,
                project, FileChooserDescriptorFactory.createSingleFolderDescriptor()));


        new ComponentValidator(this)
                .withOutlineProvider(ComponentValidator.CWBB_PROVIDER)
                .withValidator(() -> {
                    String text = mySourceFolder.getText();
                    if (StringUtil.isEmpty(text)) {
                        return new ValidationInfo(PluginBundle.message("ui.settings.sourceFolder.error.empty"), mySourceFolder);
                    }
                    return null;
                }).installOn(mySourceFolder);
        new ComponentValidator(this)
                .withValidator(() -> {
                    String text = myFileNameEditor.getText();
                    if (StringUtil.isEmpty(text)) {
                        return new ValidationInfo(PluginBundle.message("ui.settings.fileName.error.empty"), myFileNameEditor);
                    }
                    return null;
                }).installOn(myFileNameEditor);
        new ComponentValidator(this)
                .withValidator(() -> {
                    var text = myCodeTemplateEditor.getText();
                    if (StringUtil.isEmpty(text))
                        return new ValidationInfo(PluginBundle.message("ui.settings.codeTemplate.error.empty"), myCodeTemplateEditor);
                    return null;
                }).installOn(myCodeTemplateEditor);

        myDescription.setEditorKit(HTMLEditorKitBuilder.simple());
        myDescription.setEditable(false);
        myDescription.addHyperlinkListener(new BrowserHyperlinkListener());

        // get current ide locale
        var i18nLang = IdeUtils.i18nLanguage();
        var file = "/settings/CodeTemplate_" + myCodeDojo.value() + "_" + i18nLang + ".html";
        var fallbackFile = "/settings/CodeTemplate_" + myCodeDojo.value() + ".html";
        try (var fileStream = getClass().getResourceAsStream(file);
             var fallbackStream = getClass().getResourceAsStream(fallbackFile)) {
            var input = Optional.ofNullable(fileStream)
                    .or(() -> Optional.ofNullable(fallbackStream))
                    .orElse(null);
            if (input != null) {
                var description =
                        StringUtil.join(IOUtils.readLines(Objects.requireNonNull(input), StandardCharsets.UTF_8), "");
                description = XmlStringUtil.stripHtml(description);
                description = IdeBundle.message("http.velocity", description);
                myDescription.setText(description);
                myDescription.setCaretPosition(0);
            }
        } catch (Exception ignored) {
        }
    }

    private void createUIComponents() {
        createFileNameGroup();
        createCodeTemplateGroup();
    }

    private void createCodeTemplateGroup() {
        myCodeTemplateSplitter = new Splitter(false);
        myCodeTemplateSplitter.setDividerWidth(2);
        myCodeTemplateSplitter.setPreferredSize(JBUI.size(0, 0));
        myCodeTemplateEditor = new EditorTextField(EditorFactory.getInstance().createDocument(""), myProject(), FileTypes.UNKNOWN,
                false, false);
        myCodeTemplateEditor.setFont(EditorFontType.PLAIN.getGlobalFont());
        myCodeTemplateEditor.addSettingsProvider(editor -> {
            editor.setVerticalScrollbarVisible(true);
            editor.setHorizontalScrollbarVisible(true);
            editor.getSettings().setLineNumbersShown(true);
            editor.setBackgroundColor(EditorFragmentComponent.getBackgroundColor(editor, false));
            editor.setBorder(new DarculaEditorTextFieldBorder(myCodeTemplateEditor, editor));
            editor.setHighlighter(ChallengeFileTemplateHighlighter.createVelocityTemplateLanguageEditorHighlighter(myProject(),
                    JavaUtils.toOption(myLanguage)));
        });

        myCodeTemplatePreview = new EditorTextField(EditorFactory.getInstance().createDocument(""), myProject(), FileTypes.UNKNOWN, true, false);
        myCodeTemplatePreview.setFont(EditorFontType.PLAIN.getGlobalFont());
        myCodeTemplatePreview.addSettingsProvider(editor -> {
            editor.setVerticalScrollbarVisible(true);
            editor.setHorizontalScrollbarVisible(true);
            editor.setBackgroundColor(EditorFragmentComponent.getBackgroundColor(editor, false));
            editor.setBorder(new DarculaEditorTextFieldBorder(myCodeTemplatePreview, editor));
            editor.setHighlighter(ChallengeFileTemplateHighlighter.createLanguageEditorHighlighter(myProject(),
                    JavaUtils.toOption(myLanguage)));
        });

        myCodeTemplateEditor.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                updateCodeTemplatePreview();
            }
        });

        myCodeTemplateSplitter.setFirstComponent(myCodeTemplateEditor);
        myCodeTemplateSplitter.setSecondComponent(myCodeTemplatePreview);
        var actionGroup = createCodeTemplateActionGroup();

        var codeTemplateToolbar = ((ActionManagerEx) ActionManager
                .getInstance())
                .createActionToolbar("CodeTemplate", actionGroup, true, false, false);
        myCodeTemplateToolbarComponent = codeTemplateToolbar.getComponent();
        myCodeTemplateToolbarComponent.setBorder(JBUI.Borders.empty());
        codeTemplateToolbar.setTargetComponent(myCodeTemplateEditor);
    }

    @NotNull
    private DefaultActionGroup createCodeTemplateActionGroup() {
        var togglePreview = new ToggleAction(
                PluginBundle.message("ui.settings.codeTemplate.action.togglePreview"),
                null,
                AllIcons.Actions.ToggleVisibility) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return myCodeTemplatePreview.isVisible();
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                myCodeTemplatePreview.setVisible(state);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        };

        var applyTemplate =
                new DumbAwareAction(
                        PluginBundle.message("ui.settings.codeTemplate.action.useDefaultTemplate"),
                        null,
                        AllIcons.Actions.Refresh
                ) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        var versionedPath = buildCodeTemplatePath(true);
                        var fallbackPath = buildCodeTemplatePath(false);
                        var defaultPath = "/templates/code.ft";
                        try (var versionedStream = getClass().getResourceAsStream(versionedPath);
                             var fallbackStream = getClass().getResourceAsStream(fallbackPath);
                             var defaultStream = getClass().getResourceAsStream(defaultPath)) {

                            // 优先尝试带版本号的模板
                            var resourceStream = Optional.ofNullable(versionedStream)
                                    .or(() -> Optional.ofNullable(fallbackStream))
                                    .or(() -> Optional.ofNullable(defaultStream))
                                    .orElseThrow(() -> new IllegalStateException("Template not found"));

                            var template =
                                    StringUtil.join(IOUtils.readLines(resourceStream, StandardCharsets.UTF_8), LineSeparator.LF.getSeparatorString());
                            myCodeTemplateEditor.setText(template);

                        } catch (Exception ex) {
                            myLogger.warn("Failed to load default code template for " + myCodeDojo.value() + " " + myLanguage.value() + " " + myLanguageVersion.version(), ex);
                        }
                    }

                    @NotNull
                    @Override
                    public ActionUpdateThread getActionUpdateThread() {
                        return ActionUpdateThread.EDT;
                    }
                };

        var actionGroup = new DefaultActionGroup();
        actionGroup.add(togglePreview);
        actionGroup.add(applyTemplate);
        return actionGroup;
    }

    private String buildCodeTemplatePath(boolean includeVersion) {
        String base = "/templates/" + myCodeDojo.value() + "/code/";
        String filename = myLanguage.fileExt() +
                (includeVersion ? myLanguageVersion.version() : "") +
                ".ft";
        return Paths.get(base, filename).toString().replace("\\", "/");
    }

    private String buildFileNameTemplatePath(boolean includeVersion) {
        String base = "/templates/" + myCodeDojo.value() + "/filename/";
        String filename = myLanguage.fileExt() +
                (includeVersion ? myLanguageVersion.version() : "") +
                ".ft";
        return Paths.get(base, filename).toString().replace("\\", "/");
    }

    private void updateCodeTemplatePreview() {

        var result = VelocityUtils.generateContent(myCodeTemplateEditor.getText(),
                myLanguage,
                myDemoTemplateSupplier.apply(myLanguage, myLanguageVersion));

        ComponentValidator.getInstance(myCodeTemplateEditor).ifPresent(validator -> {
            if (result.isLeft()) {
                var left = (Left<Exception, String>) result;
                validator.updateInfo(new ValidationInfo(left.value().getMessage(), myCodeTemplateEditor));
                myCodeTemplatePreview.setText(PluginBundle.message("ui.settings.codeTemplate.error.invalid"));
            } else {
                var right = (Right<Exception, String>) result;
                validator.updateInfo(null);
                myCodeTemplatePreview.setText(right.value());
            }
        });
    }

    private void updateFileNamePreview() {
        var result = VelocityUtils.generateContent(myFileNameEditor.getText(), myLanguage, myDemoTemplateSupplier.apply(myLanguage, myLanguageVersion));
        if (result.isLeft()) {
            var left = (Left<Exception, String>) result;

            ComponentValidator.getInstance(myFileNameEditor).ifPresent(validator -> validator.updateInfo(new ValidationInfo(left.value().getMessage(), myFileNameEditor)));

            myFileNamePreview.setText(PluginBundle.message("ui.settings.fileName.error.invalid"));
        } else {
            var right = (Right<Exception, String>) result;
            ComponentValidator.getInstance(myFileNameEditor).ifPresent(validator -> validator.updateInfo(null));
            var content = right.value().trim();
            myFileNamePreview.setText(content + "." + myLanguage.fileExt());
        }
    }

    private void createFileNameGroup() {
        myFileNameSplitter = new Splitter(false);
        myFileNameSplitter.setDividerWidth(2);
        myFileNameSplitter.setPreferredSize(JBUI.size(0, 0));
        myFileNameEditor = new EditorTextField(EditorFactory.getInstance().createDocument(""), myProject(), FileTypes.UNKNOWN,
                false, true);
        myFileNameEditor.addSettingsProvider(editor -> editor.setHighlighter(ChallengeFileTemplateHighlighter.createVelocityTemplatePlainTextHighlighter(myProject())));
        myFileNamePreview = new EditorTextField(EditorFactory.getInstance().createDocument(""), myProject(), FileTypes.UNKNOWN, true, true);

        myFileNameEditor.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                updateFileNamePreview();
            }
        });
        myFileNameSplitter.setFirstComponent(myFileNameEditor);
        myFileNameSplitter.setSecondComponent(myFileNamePreview);
        var actionGroup = createFileNameActionGroup();

        var fileNameToolbar = ((ActionManagerEx) ActionManager
                .getInstance())
                .createActionToolbar("CodeDojoSetting.FileName", actionGroup, true, false, false);
        myFileNameToolbarComponent = fileNameToolbar.getComponent();
        myFileNameToolbarComponent.setBorder(JBUI.Borders.empty());
        fileNameToolbar.setTargetComponent(myFileNameEditor);
    }

    @NotNull
    private DefaultActionGroup createFileNameActionGroup() {
        var togglePreview = new ToggleAction(
                PluginBundle.message("ui.settings.fileName.action.togglePreview"),
                null,
                AllIcons.Actions.ToggleVisibility) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return myFileNamePreview.isVisible();
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                myFileNamePreview.setVisible(state);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        };

        var applyTemplate =
                new DumbAwareAction(
                        PluginBundle.message("ui.settings.fileName.action.useDefaultTemplate"),
                        null,
                        AllIcons.Actions.Refresh) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        var versionedPath = buildFileNameTemplatePath(true);
                        var fallbackPath = buildFileNameTemplatePath(false);
                        var defaultPath = "/templates/filename.ft";
                        try (var versionedStream = getClass().getResourceAsStream(versionedPath);
                             var fallbackStream = getClass().getResourceAsStream(fallbackPath);
                             var defaultStream = getClass().getResourceAsStream(defaultPath)) {

                            // 优先尝试带版本号的模板
                            var resourceStream = Optional.ofNullable(versionedStream)
                                    .or(() -> Optional.ofNullable(fallbackStream))
                                    .or(() -> Optional.ofNullable(defaultStream))
                                    .orElseThrow(() -> new IllegalStateException("Template not found"));

                            var template =
                                    StringUtil.join(IOUtils.readLines(resourceStream, StandardCharsets.UTF_8), "");
                            myFileNameEditor.setText(template);

                        } catch (Exception ex) {
                            myLogger.warn("Failed to load default filename template for " + myCodeDojo.value() + " " + myLanguage.value() + " " + myLanguageVersion.version(), ex);
                        }
                    }
                };

        var actionGroup = new DefaultActionGroup();
        actionGroup.add(togglePreview);
        actionGroup.add(applyTemplate);
        return actionGroup;
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return rootPanel;
    }

    @Override
    public void apply(@NotNull BaseCodeDojoSettings.LanguageSettingsState state) throws ConfigurationException {
        var validationInfos = new ArrayList<ValidationInfo>();
        Stream.of(mySourceFolder, myFileNameEditor, myCodeTemplateEditor)
                .map(ComponentValidator::getInstance)
                .forEach(validator -> {
                    validator.ifPresent(v -> {
                        v.revalidate();
                        ValidationInfo result = v.getValidationInfo();
                        if (result != null) validationInfos.add(result);
                    });
                });
        if (validationInfos.isEmpty()) {
            state.codeTemplate_$eq(Option.apply(myCodeTemplateEditor.getText()));
            state.fileNameTemplate_$eq(Option.apply(myFileNameEditor.getText()));
            state.language_$eq(Option.apply(myLanguage));
            state.languageVersion_$eq(Option.apply(myLanguageVersion));
            state.sourceFolder_$eq(Option.apply(mySourceFolder.getText()));
        } else {
            throw new ConfigurationException("Invalid settings");
        }
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return super.enableSearch(option);
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return mySourceFolder;
    }

    @Override
    public boolean isModified(BaseCodeDojoSettings.LanguageSettingsState state) {
        var sourceFolder = StringUtil.equals(state.sourceFolder().getOrElse(() -> ""), mySourceFolder.getText());
        var codeTemplate = StringUtil.equals(state.codeTemplate().getOrElse(() -> ""), myCodeTemplateEditor.getText());
        var fileName = StringUtil.equals(state.fileNameTemplate().getOrElse(() -> ""), myFileNameEditor.getText());
        return !sourceFolder || !codeTemplate || !fileName;
    }

    @Override
    public void reset(BaseCodeDojoSettings.LanguageSettingsState state) {
        mySourceFolder.setText(null);
        myLanguage = state.language().getOrElse(() -> null);
        myLanguageVersion = state.languageVersion().getOrElse(() -> null);
        myFileNameEditor.setText(null);
        myCodeTemplateEditor.setText(null);

        JavaUtils.toOptional(state.sourceFolder()).ifPresent(mySourceFolder::setText);
        JavaUtils.toOptional(state.codeTemplate()).ifPresent(myCodeTemplateEditor::setText);
        JavaUtils.toOptional(state.fileNameTemplate()).ifPresent(myFileNameEditor::setText);
    }

    private class MyBrowseFolderListener extends ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> {

        MyBrowseFolderListener(@NlsContexts.DialogTitle String title,
                               @NlsContexts.Label String description,
                               TextFieldWithBrowseButton textField,
                               Project project,
                               FileChooserDescriptor fileChooserDescriptor) {
            super(title, description, textField, project, fileChooserDescriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        }

        @Override
        protected VirtualFile getInitialFile() {
            // suggest project base dir only if nothing is typed in the component.
            String text = getComponentText();
            if (text.isEmpty()) {
                VirtualFile file = myProject().getBaseDir();
                if (file != null) {
                    return file;
                }
            }
            return super.getInitialFile();
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(6, 2, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("messages/PluginBundle", "ui.settings.sourceFolder.label"));
        rootPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mySourceFolder = new TextFieldWithBrowseButton();
        rootPanel.add(mySourceFolder, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        myFileNameSplitter.setDividerWidth(2);
        myFileNameSplitter.setLackOfSpaceStrategy(Splitter.LackOfSpaceStrategy.HONOR_THE_FIRST_MIN_SIZE);
        myFileNameSplitter.setShowDividerControls(true);
        myFileNameSplitter.setShowDividerIcon(false);
        rootPanel.add(myFileNameSplitter, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        myFileNameLabel = new JPanel();
        myFileNameLabel.setLayout(new BorderLayout(0, 0));
        rootPanel.add(myFileNameLabel, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("messages/PluginBundle", "ui.settings.fileName.title"));
        myFileNameLabel.add(label2, BorderLayout.WEST);
        final Spacer spacer1 = new Spacer();
        myFileNameLabel.add(spacer1, BorderLayout.CENTER);
        myFileNameLabel.add(myFileNameToolbarComponent, BorderLayout.EAST);
        myCodeTemplateLabel = new JPanel();
        myCodeTemplateLabel.setLayout(new BorderLayout(0, 0));
        rootPanel.add(myCodeTemplateLabel, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("messages/PluginBundle", "ui.settings.codeTemplate.label"));
        myCodeTemplateLabel.add(label3, BorderLayout.WEST);
        final Spacer spacer2 = new Spacer();
        myCodeTemplateLabel.add(spacer2, BorderLayout.CENTER);
        myCodeTemplateLabel.add(myCodeTemplateToolbarComponent, BorderLayout.EAST);
        rootPanel.add(myCodeTemplateSplitter, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        rootPanel.add(scrollPane1, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(-1, 180), null, 0, false));
        scrollPane1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        myDescription = new JEditorPane();
        myDescription.setEditable(false);
        myDescription.setMargin(new Insets(0, 0, 0, 0));
        scrollPane1.setViewportView(myDescription);
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle;
        try {
            Class<?> thisClass = this.getClass();
            if ($$$cachedGetBundleMethod$$$ == null) {
                Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
                $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
            }
            bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(path);
        }
        return bundle.getString(key);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
