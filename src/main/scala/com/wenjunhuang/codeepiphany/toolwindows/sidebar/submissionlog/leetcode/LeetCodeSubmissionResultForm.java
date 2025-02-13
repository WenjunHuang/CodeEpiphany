package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.leetcode;

import com.intellij.codeInsight.hint.EditorFragmentComponent;
import com.intellij.ide.ui.laf.darcula.ui.DarculaEditorTextFieldBorder;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.EditorTextField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.HTMLEditorKitBuilder;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.wenjunhuang.codeepiphany.PluginBundle;
import com.wenjunhuang.codeepiphany.database.tables.records.LeetcodeSubmissionRecord;
import com.wenjunhuang.codeepiphany.database.tables.records.SolutionSubmissionRecord;
import com.wenjunhuang.codeepiphany.model.CodeDojo;
import com.wenjunhuang.codeepiphany.model.Language;
import com.wenjunhuang.codeepiphany.model.LanguageVersion;
import com.wenjunhuang.codeepiphany.model.SubmissionResult;
import com.wenjunhuang.codeepiphany.model.template.ChallengeFileTemplateHighlighter;
import com.wenjunhuang.codeepiphany.utils.JavaUtils;
import org.typelevel.ci.CIString;
import scala.Option;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.lang.reflect.Method;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class LeetCodeSubmissionResultForm {

    private JLabel myResult;
    private JLabel myTestcases;
    private JLabel mySubmitDateTime;
    private JLabel myLanguage;
    private EditorTextField mySubmitCode;
    private JPanel myPanel;
    private JComponent myResultComponent;
    private JLabel myCodeLabel;
    private JEditorPane myViewInBrowser;

    public LeetCodeSubmissionResultForm(
            Language language,
            LanguageVersion languageVersion,
            String challengeSlug,
            CodeDojo leetCodeDojo,
            SolutionSubmissionRecord submissionRecord,
            LeetcodeSubmissionRecord leetcodeSubmissionRecord
    ) {
        mySubmitCode = new EditorTextField(
                EditorFactory.getInstance().createDocument(submissionRecord.getSubmitcode()), null, FileTypeManager.getInstance().getFileTypeByExtension(language.fileExt()), true, false);
        mySubmitCode.setFont(EditorFontType.PLAIN.getGlobalFont());
        mySubmitCode.addSettingsProvider(editor -> {
            editor.setVerticalScrollbarVisible(true);
            editor.setHorizontalScrollbarVisible(true);
            editor.getSettings().setLineNumbersShown(true);
            editor.setBackgroundColor(EditorFragmentComponent.getBackgroundColor(editor, false));
            editor.setBorder(new DarculaEditorTextFieldBorder(mySubmitCode, editor));
            editor.setHighlighter(ChallengeFileTemplateHighlighter.createLanguageEditorHighlighter(null, Option.apply(language)));
        });
        var result = JavaUtils.toOptional(SubmissionResult.fromCIString(CIString.apply(submissionRecord.getResult())));
        result.ifPresentOrElse(
                submissionResult -> {
                    myResultComponent = LeetCodeSubmissionResultFormHelper.createFromSubmissionType(submissionResult, submissionRecord, leetcodeSubmissionRecord);
                },
                () -> myResultComponent = new JLabel("Unknown Result type")
        );

        $$$setupUI$$$();
        myResult.setText(result.map(SubmissionResult::showAsHtml).orElse("Unknown"));
        myResult.setFont(JBFont.create(myResult.getFont()).biggerOn(1.5f));
        mySubmitDateTime.setText(PluginBundle.message("leetcode.submissionResult.submitDateTime.text", submissionRecord.getSubmitdatetime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

        var passed = leetcodeSubmissionRecord.getTotalcorrect();
        var testcases = leetcodeSubmissionRecord.getTotaltestcases();

        if (passed == null || testcases == null)
            myTestcases.setVisible(false);
        else
            myTestcases.setText(PluginBundle.message("leetcode.submissionResult.testcases.text", passed, testcases));

        myCodeLabel.setBorder(JBUI.Borders.emptyRight(2));
        myLanguage.setText(Language.prettyPrint(language, languageVersion));
        myLanguage.setBorder(
                JBUI.Borders.compound(
                        JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(), 0, 1, 0, 0),
                        JBUI.Borders.emptyLeft(2)
                ));
        var submissionId = submissionRecord.getDojosubmissionid();
        if (submissionId != null) {
            var link = "https://" + leetCodeDojo.domain().toString() + "/problems/" + challengeSlug + "/submissions/" + submissionId + "/";

            myViewInBrowser.setEditorKit(HTMLEditorKitBuilder.simple());
            myViewInBrowser.setEditable(false);
            myViewInBrowser.addHyperlinkListener(new BrowserHyperlinkListener());
            myViewInBrowser.setText("<html><body><a href='" + link + "'>View In Browser</a></body></html>");
        } else {
            myViewInBrowser.setVisible(false);
        }
    }

    public JComponent getComponent() {
        return myPanel;
    }

    private void createUIComponents() {
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
        myPanel = new JPanel();
        myPanel.setLayout(new GridLayoutManager(5, 4, new Insets(0, 0, 0, 0), 2, 5));
        myPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        myTestcases = new JLabel();
        myTestcases.setText("Testcase");
        myPanel.add(myTestcases, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myResult = new JLabel();
        myResult.setText("Label");
        myPanel.add(myResult, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        myPanel.add(spacer1, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        mySubmitDateTime = new JLabel();
        mySubmitDateTime.setText("Label");
        myPanel.add(mySubmitDateTime, new GridConstraints(1, 0, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mySubmitCode.setViewer(false);
        myPanel.add(mySubmitCode, new GridConstraints(4, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        myPanel.add(myResultComponent, new GridConstraints(2, 0, 1, 4, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        myPanel.add(panel1, new GridConstraints(3, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        myCodeLabel = new JLabel();
        this.$$$loadLabelText$$$(myCodeLabel, this.$$$getMessageFromBundle$$$("messages/PluginBundle", "leetcode.submissionResult.code.text"));
        panel1.add(myCodeLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel1.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        myLanguage = new JLabel();
        myLanguage.setText("Label");
        panel1.add(myLanguage, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myViewInBrowser = new JEditorPane();
        myPanel.add(myViewInBrowser, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
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
        return myPanel;
    }


}
