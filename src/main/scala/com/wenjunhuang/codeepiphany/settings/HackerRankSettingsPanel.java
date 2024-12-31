package com.wenjunhuang.codeepiphany.settings;

import com.intellij.codeInsight.hint.EditorFragmentComponent;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.EditorSettingsProvider;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.JBUI;
import com.wenjunhuang.codeepiphany.PluginBundle;
import com.wenjunhuang.codeepiphany.model.Language;
import com.wenjunhuang.codeepiphany.utils.JavaUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;

import scala.jdk.OptionConverters;

public class HackerRankSettingsPanel extends SettingsPanel {
    private TextFieldWithBrowseButton mySourceFolder;
    private EditorTextField myFileNameEditor;
    private EditorTextField myFileNamePreview;
    private EditorTextField myCodeTemplateEditor;
    private EditorTextField myCodeTemplatePreview;
    private JPanel rootPanel;
    private Splitter myFileNameSplitter;
    private ActionToolbarImpl myFileNameToolbar;
    private ComboBox<Language> myLanguages;
    private JPanel myFileNameLabel;
    private JPanel myCodeTemplateLabel;
    private ActionToolbarImpl myCodeTemplateToolbar;
    private Splitter myCodeTemplateSplitter;

    public HackerRankSettingsPanel(Project project) {
        super(project);
        $$$setupUI$$$();

        initLanguageComboBox();
        reset();

        new ComponentValidator(myDisposable())
                .withValidator(() -> {
                    String text = mySourceFolder.getText();
                    if (StringUtil.isEmpty(text)) {
                        return new ValidationInfo(PluginBundle.message("hackerrank.ui.settings.sourceFolder.error.empty"), mySourceFolder);
                    }
                    return null;
                }).installOn(mySourceFolder);
    }

    private void initLanguageComboBox() {
        myLanguages.setModel(new DefaultComboBoxModel<>(Language.values()));
        myLanguages.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            if (value == null) {
                return new JBLabel(PluginBundle.message("hackerrank.ui.settings.language.hint"));
            } else return new JBLabel(value.show(), value.icon(), SwingConstants.LEFT);
        });
    }

    private void createUIComponents() {
        createFileNameGroup();
        createCodeTemplateGroup();
    }

    private void createCodeTemplateGroup() {
        myCodeTemplateSplitter = new Splitter(false);
        myCodeTemplateEditor = new EditorTextField(EditorFactory.getInstance().createDocument(""), myProject(), FileTypes.UNKNOWN,
                false, false);
        myCodeTemplateEditor.setFont(EditorFontType.PLAIN.getGlobalFont());
        myCodeTemplateEditor.setBorder(new LineBorder(JBColor.border()));
        myCodeTemplateEditor.addSettingsProvider(editor -> {
            editor.setVerticalScrollbarVisible(true);
            editor.setHorizontalScrollbarVisible(true);
            editor.setBackgroundColor(EditorFragmentComponent.getBackgroundColor(editor, false));
            editor.setBorder(JBUI.Borders.empty());
        });

        myCodeTemplatePreview = new EditorTextField(EditorFactory.getInstance().createDocument(""), myProject(), FileTypes.UNKNOWN, true, false);
        myCodeTemplateSplitter.setFirstComponent(myCodeTemplateEditor);
        myCodeTemplateSplitter.setSecondComponent(myCodeTemplatePreview);
        ToggleAction togglePreview = new ToggleAction(
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

        AnAction applyTemplate =
                new DumbAwareAction(
                        PluginBundle.message("hackerrank.ui.settings.codeTemplate.action.useDefaultTemplate"),
                        null,
                        AllIcons.Actions.Refresh
                ) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        Language language = myLanguages.getItem();
                    }

                };

        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(togglePreview);
        actionGroup.add(applyTemplate);

        myCodeTemplateToolbar = (ActionToolbarImpl) ((ActionManagerEx) ActionManager
                .getInstance())
                .createActionToolbar("HackerRankSetting.CodeTemplate", actionGroup, true, false, false);
        myCodeTemplateToolbar.setActionButtonBorder(JBUI.Borders.empty(0, 0, 0, 5));
        myCodeTemplateToolbar.setBorder(JBUI.Borders.empty());
        myCodeTemplateToolbar.setTargetComponent(myCodeTemplateEditor);
    }

    private void createFileNameGroup() {
        myFileNameSplitter = new Splitter(false);
        myFileNameEditor = new EditorTextField(EditorFactory.getInstance().createDocument(""), myProject(), FileTypes.UNKNOWN,
                false, true);
        myFileNamePreview = new EditorTextField(EditorFactory.getInstance().createDocument(""), myProject(), FileTypes.UNKNOWN, true, true);
        myFileNameSplitter.setFirstComponent(myFileNameEditor);
        myFileNameSplitter.setSecondComponent(myFileNamePreview);
        ToggleAction togglePreview = new ToggleAction(
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

        AnAction applyTemplate =
                new DumbAwareAction(
                        PluginBundle.message("hackerrank.ui.settings.fileName.action.useDefaultTemplate"),
                        null,
                        AllIcons.Actions.Refresh
                ) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        Language language = myLanguages.getItem();
                    }

                };

        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(togglePreview);
        actionGroup.add(applyTemplate);

        myFileNameToolbar = (ActionToolbarImpl) ((ActionManagerEx) ActionManager
                .getInstance())
                .createActionToolbar("HackerRankSetting.FileName", actionGroup, true, false, false);
        myFileNameToolbar.setActionButtonBorder(JBUI.Borders.empty(0, 0, 0, 5));
        myFileNameToolbar.setBorder(JBUI.Borders.empty());
    }

    @Override
    public JComponent getRootPanel() {
        return rootPanel;
    }

    @Override
    public void apply() throws ConfigurationException {
        ArrayList<ValidationInfo> validationInfos = new ArrayList<>();
        ComponentValidator.getInstance(mySourceFolder).ifPresent(validator -> {
                    validator.revalidate();
                    ValidationInfo result = validator.getValidationInfo();
                    if (result != null) validationInfos.add(result);
                }
        );

//        if (!validationInfos.isEmpty()) {
//            throw new ConfigurationException(validationInfos.get(0).message);
//        }

    }

    @Override
    public boolean isModified() {
        HackerRankSettings settings = HackerRankSettings.getInstance(myProject());
        HackerRankSettings.HackerRankState state = settings.getState();
        return !(JavaUtils.toOptional(state.sourceFolder()).equals(Optional.of(mySourceFolder.getText())) &&
                JavaUtils.toOptional(state.language()).equals(Optional.of(myLanguages.getSelectedItem())) &&
                JavaUtils.toOptional(state.codeTemplate()).equals(Optional.of(myCodeTemplateEditor.getText())) &&
                JavaUtils.toOptional(state.fileNameTemplate()).equals(Optional.of(myFileNameEditor.getText())));

    }

    @Override
    public void reset() {
        mySourceFolder.setText(null);
        myLanguages.setSelectedItem(null);
        myFileNameEditor.setText(null);
        myCodeTemplateEditor.setText(null);

        HackerRankSettings settings = HackerRankSettings.getInstance(myProject());
        HackerRankSettings.HackerRankState state = settings.getState();
        JavaUtils.toOptional(state.sourceFolder()).ifPresent(mySourceFolder::setText);
        JavaUtils.toOptional(state.language()).ifPresent(language -> myLanguages.setSelectedItem(language));
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
        rootPanel.setLayout(new GridLayoutManager(7, 2, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("messages/PluginBundle", "hackerrank.ui.settings.sourceFolder.label"));
        rootPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        rootPanel.add(spacer1, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        mySourceFolder = new TextFieldWithBrowseButton();
        rootPanel.add(mySourceFolder, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("messages/PluginBundle", "hackerrank.ui.settings.language.label"));
        rootPanel.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myFileNameSplitter.setDividerWidth(2);
        rootPanel.add(myFileNameSplitter, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        myLanguages = new ComboBox();
        rootPanel.add(myLanguages, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        myFileNameLabel = new JPanel();
        myFileNameLabel.setLayout(new BorderLayout(0, 0));
        rootPanel.add(myFileNameLabel, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("messages/PluginBundle", "hackerrank.ui.settings.fileName.title"));
        myFileNameLabel.add(label3, BorderLayout.WEST);
        final Spacer spacer2 = new Spacer();
        myFileNameLabel.add(spacer2, BorderLayout.CENTER);
        myFileNameLabel.add(myFileNameToolbar, BorderLayout.EAST);
        myCodeTemplateLabel = new JPanel();
        myCodeTemplateLabel.setLayout(new BorderLayout(0, 0));
        rootPanel.add(myCodeTemplateLabel, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, this.$$$getMessageFromBundle$$$("messages/PluginBundle", "hackerrank.ui.settings.codeTemplate.label"));
        myCodeTemplateLabel.add(label4, BorderLayout.WEST);
        final Spacer spacer3 = new Spacer();
        myCodeTemplateLabel.add(spacer3, BorderLayout.CENTER);
        myCodeTemplateLabel.add(myCodeTemplateToolbar, BorderLayout.EAST);
        rootPanel.add(myCodeTemplateSplitter, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 300), null, 0, false));
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
