package com.wenjunhuang.codeepiphany.hackerrank.settings;

import com.intellij.codeInsight.hint.EditorFragmentComponent;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.ui.laf.darcula.ui.DarculaEditorTextFieldBorder;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.EditorTextField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.HTMLEditorKitBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.xml.util.XmlStringUtil;
import com.wenjunhuang.codeepiphany.PluginBundle;
import com.wenjunhuang.codeepiphany.model.Language;
import com.wenjunhuang.codeepiphany.model.LanguageVersion;
import com.wenjunhuang.codeepiphany.model.template.ChallengeFileTemplateHighlighter;
import com.wenjunhuang.codeepiphany.settings.SettingsUi;
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
import java.util.ArrayList;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class LanguageSettingsPanel extends SettingsUi<HackerRankSettings.HackerRankLanguageSettingsState> {
    private TextFieldWithBrowseButton mySourceFolder;
    private EditorTextField myFileNameEditor;
    private EditorTextField myFileNamePreview;
    private EditorTextField myCodeTemplateEditor;
    private EditorTextField myCodeTemplatePreview;
    private JPanel rootPanel;
    private Splitter myFileNameSplitter;
    private ActionToolbarImpl myFileNameToolbar;
    private JPanel myFileNameLabel;
    private JPanel myCodeTemplateLabel;
    private ActionToolbarImpl myCodeTemplateToolbar;
    private Splitter myCodeTemplateSplitter;
    private JEditorPane myDescription;
    private Language myLanguage;
    private LanguageVersion myLanguageVersion;

    public LanguageSettingsPanel(Project project) {
        super(project);
        $$$setupUI$$$();

        mySourceFolder.addBrowseFolderListener(PluginBundle.message("hackerrank.ui.settings.sourceFolder.title"),
                null,
                project, FileChooserDescriptorFactory.createSingleFolderDescriptor());


        new ComponentValidator(this)
                .withOutlineProvider(ComponentValidator.CWBB_PROVIDER)
                .withValidator(() -> {
                    String text = mySourceFolder.getText();
                    if (StringUtil.isEmpty(text)) {
                        return new ValidationInfo(PluginBundle.message("hackerrank.ui.settings.sourceFolder.error.empty"), mySourceFolder);
                    }
                    return null;
                }).installOn(mySourceFolder);
        new ComponentValidator(this)
                .withValidator(() -> {
                    String text = myFileNameEditor.getText();
                    if (StringUtil.isEmpty(text)) {
                        return new ValidationInfo(PluginBundle.message("hackerrank.ui.settings.fileName.error.empty"), myFileNameEditor);
                    }
                    return null;
                }).installOn(myFileNameEditor);
        new ComponentValidator(this)
                .withValidator(() -> {
                    var text = myCodeTemplateEditor.getText();
                    if (StringUtil.isEmpty(text))
                        return new ValidationInfo(PluginBundle.message("hackerrank.ui.settings.codeTemplate.error.empty"), myCodeTemplateEditor);
                    return null;
                }).installOn(myCodeTemplateEditor);

        myDescription.setEditorKit(HTMLEditorKitBuilder.simple());
        myDescription.setEditable(false);
        myDescription.addHyperlinkListener(new BrowserHyperlinkListener());

        try {
            var description =
                    StringUtil.join(IOUtils.readLines(Objects.requireNonNull(getClass().getResourceAsStream("/settings/TemplateDescription.html")), StandardCharsets.UTF_8), "");
            description = XmlStringUtil.stripHtml(description);
            description = IdeBundle.message("http.velocity", description);
            myDescription.setText(description);
            myDescription.setCaretPosition(0);
        } catch (Exception ignored) {
        }
    }

//    private void initLanguageComboBox() {
//        myLanguages.setModel(new DefaultComboBoxModel<>(HackerRankSettingsConfigurable.HACKERRANK_LANGUAGES()));
//        myLanguages.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
//            if (value == null) {
//                return new JBLabel(PluginBundle.message("hackerrank.ui.settings.language.hint"));
//            } else return new JBLabel(value._1.show() + value._2.version(), value._1.icon(), SwingConstants.LEFT);
//        });
//        myLanguages.addItemListener(e -> {
//            if (e.getStateChange() == ItemEvent.SELECTED) {
//                Optional.ofNullable((EditorEx) myCodeTemplateEditor.getEditor()).ifPresent(editor -> editor.setHighlighter(ChallengeFileTemplateHighlighter.createVelocityTemplateLanguageEditorHighlighter(myProject(),
//                        JavaUtils.toOption(myLanguages.getItem()).map(Tuple2::_1))));
//                Optional.ofNullable((EditorEx) myCodeTemplatePreview.getEditor())
//                        .ifPresent(preview ->
//                                preview.setHighlighter(ChallengeFileTemplateHighlighter.createLanguageEditorHighlighter(myProject(),
//                                        JavaUtils.toOption(myLanguages.getItem()).map(Tuple2::_1))));
//
//                updateCodeTemplatePreview();
//                updateFileNamePreview();
//            }
//        });
//    }

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

        myCodeTemplateToolbar = (ActionToolbarImpl) ((ActionManagerEx) ActionManager
                .getInstance())
                .createActionToolbar("HackerRankSetting.CodeTemplate", actionGroup, true, false, false);
        myCodeTemplateToolbar.setActionButtonBorder(JBUI.Borders.empty(0, 0, 0, 5));
        myCodeTemplateToolbar.setBorder(JBUI.Borders.empty());
        myCodeTemplateToolbar.setTargetComponent(myCodeTemplateEditor);
    }

    @NotNull
    private DefaultActionGroup createCodeTemplateActionGroup() {
        var togglePreview = new ToggleAction(
                PluginBundle.message("hackerrank.ui.settings.codeTemplate.action.togglePreview"),
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
                return ActionUpdateThread.BGT;
            }
        };

        var applyTemplate =
                new DumbAwareAction(
                        PluginBundle.message("hackerrank.ui.settings.codeTemplate.action.useDefaultTemplate"),
                        null,
                        AllIcons.Actions.Refresh
                ) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        var template = FileTemplateManager.getInstance(myProject())
                                .findInternalTemplate("hackerrank_code." + myLanguage.fileExt());
                        if (template != null) {
                            myCodeTemplateEditor.setText(template.getText());
                        }
                    }

                    @NotNull
                    @Override
                    public ActionUpdateThread getActionUpdateThread() {
                        return ActionUpdateThread.BGT;
                    }
                };

        var actionGroup = new DefaultActionGroup();
        actionGroup.add(togglePreview);
        actionGroup.add(applyTemplate);
        return actionGroup;
    }

    private void updateCodeTemplatePreview() {

        var result = VelocityUtils.generateContent(myCodeTemplateEditor.getText(),
                HackerRankSettingsConfigurable.getDemoTemplate(myLanguage, myLanguageVersion).get());

        ComponentValidator.getInstance(myCodeTemplateEditor).ifPresent(validator -> {
            switch (result) {
                case Left<Exception, String> left -> {
                    validator.updateInfo(new ValidationInfo(left.value().getMessage(), myCodeTemplateEditor));
                    myCodeTemplatePreview.setText(PluginBundle.message("hackerrank.ui.settings.codeTemplate.error.invalid"));
                }
                case Right<Exception, String> right -> {
                    validator.updateInfo(null);
                    myCodeTemplatePreview.setText(right.value());
                }
                default -> {
                }
            }
        });
    }

    private void updateFileNamePreview() {
        var result = VelocityUtils.generateContent(myFileNameEditor.getText(),
                HackerRankSettingsConfigurable.getDemoTemplate(myLanguage, myLanguageVersion).get());
        switch (result) {
            case Left<Exception, String> left -> {
                ComponentValidator.getInstance(myFileNameEditor).ifPresent(validator -> validator.updateInfo(new ValidationInfo(left.value().getMessage(), myFileNameEditor)));

                myFileNamePreview.setText(PluginBundle.message("hackerrank.ui.settings.fileName.error.invalid"));
            }
            case Right<Exception, String> right -> {
                ComponentValidator.getInstance(myFileNameEditor).ifPresent(validator -> validator.updateInfo(null));
                var content = right.value().trim();
                myFileNamePreview.setText(content + "." + myLanguage.fileExt());
            }
            default -> {
            }
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

        myFileNameToolbar = (ActionToolbarImpl) ((ActionManagerEx) ActionManager
                .getInstance())
                .createActionToolbar("HackerRankSetting.FileName", actionGroup, true, false, false);
        myFileNameToolbar.setActionButtonBorder(JBUI.Borders.empty(0, 0, 0, 5));
        myFileNameToolbar.setBorder(JBUI.Borders.empty());
        myFileNameToolbar.setTargetComponent(myFileNameEditor);
    }

    @NotNull
    private DefaultActionGroup createFileNameActionGroup() {
        var togglePreview = new ToggleAction(
                PluginBundle.message("hackerrank.ui.settings.fileName.action.togglePreview"),
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
                        PluginBundle.message("hackerrank.ui.settings.fileName.action.useDefaultTemplate"),
                        null,
                        AllIcons.Actions.Refresh) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        var template = FileTemplateManager.getInstance(myProject())
                                .findInternalTemplate("hackerrank_filename." + myLanguage.fileExt());
                        if (template != null) {
                            myFileNameEditor.setText(template.getText());
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
    public void apply(@NotNull HackerRankSettings.HackerRankLanguageSettingsState state) throws ConfigurationException {
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
    public boolean isModified(HackerRankSettings.HackerRankLanguageSettingsState state) {
        var sourceFolder = StringUtil.equals(state.sourceFolder().getOrElse(() -> ""), mySourceFolder.getText());
        var codeTemplate = StringUtil.equals(state.codeTemplate().getOrElse(() -> ""), myCodeTemplateEditor.getText());
        var fileName = StringUtil.equals(state.fileNameTemplate().getOrElse(() -> ""), myFileNameEditor.getText());
        return !sourceFolder || !codeTemplate || !fileName;
    }

    @Override
    public void reset(HackerRankSettings.HackerRankLanguageSettingsState state) {
        mySourceFolder.setText(null);
        myLanguage = state.language().getOrElse(() -> null);
        myLanguageVersion = state.languageVersion().getOrElse(() -> null);
        myFileNameEditor.setText(null);
        myCodeTemplateEditor.setText(null);

        JavaUtils.toOptional(state.sourceFolder()).ifPresent(mySourceFolder::setText);
        JavaUtils.toOptional(state.codeTemplate()).ifPresent(myCodeTemplateEditor::setText);
        JavaUtils.toOptional(state.fileNameTemplate()).ifPresent(myFileNameEditor::setText);
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
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("messages/PluginBundle", "hackerrank.ui.settings.sourceFolder.label"));
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
        rootPanel.add(myFileNameLabel, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("messages/PluginBundle", "hackerrank.ui.settings.fileName.title"));
        myFileNameLabel.add(label2, BorderLayout.WEST);
        final Spacer spacer1 = new Spacer();
        myFileNameLabel.add(spacer1, BorderLayout.CENTER);
        myFileNameLabel.add(myFileNameToolbar, BorderLayout.EAST);
        myCodeTemplateLabel = new JPanel();
        myCodeTemplateLabel.setLayout(new BorderLayout(0, 0));
        rootPanel.add(myCodeTemplateLabel, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("messages/PluginBundle", "hackerrank.ui.settings.codeTemplate.label"));
        myCodeTemplateLabel.add(label3, BorderLayout.WEST);
        final Spacer spacer2 = new Spacer();
        myCodeTemplateLabel.add(spacer2, BorderLayout.CENTER);
        myCodeTemplateLabel.add(myCodeTemplateToolbar, BorderLayout.EAST);
        rootPanel.add(myCodeTemplateSplitter, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(-1, 300), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        rootPanel.add(scrollPane1, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(-1, 200), null, 0, false));
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
