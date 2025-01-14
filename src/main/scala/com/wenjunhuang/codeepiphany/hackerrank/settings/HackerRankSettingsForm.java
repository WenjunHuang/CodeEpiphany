package com.wenjunhuang.codeepiphany.hackerrank.settings;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.tabs.JBTabsEx;
import com.intellij.ui.tabs.JBTabsFactory;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.wenjunhuang.codeepiphany.PluginBundle;
import com.wenjunhuang.codeepiphany.model.Language;
import com.wenjunhuang.codeepiphany.model.LanguageVersion;
import com.wenjunhuang.codeepiphany.settings.SettingsUi;
import com.wenjunhuang.codeepiphany.utils.JavaUtils;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import scala.Tuple2;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class HackerRankSettingsForm extends SettingsUi<HackerRankSettings.HackerRankSettingsState> {
    private JBTabsEx myTabs;
    private JPanel rootPanel;
    private JComponent tabPanel;
    private DefaultActionGroup myLanguagesActionGroup;
    private Map<Tuple2<Language, LanguageVersion>, LanguageSettingsPanel> myLanguagesPanels = new HashMap<>();
    private TabInfo myInitTab;

    public HackerRankSettingsForm(Project project, Disposable parentDisposable) {
        super(project);
        Disposer.register(parentDisposable, this);
        myLanguagesActionGroup = new DefaultActionGroup("Languages", null, AllIcons.General.Add);
        Arrays.stream(HackerRankSettingsConfigurable.HACKERRANK_LANGUAGES()).forEach((language) -> {
            myLanguagesActionGroup.add(new LanguageAction(language._1(), language._2()));
        });
        myLanguagesActionGroup.setPopup(true);


        $$$setupUI$$$();
    }

    private void createUIComponents() {
        myTabs = (JBTabsEx) JBTabsFactory.createTabs(myProject());
        myInitTab = new TabInfo(new BorderLayoutPanel().addToTop(
                        new JBLabel(PluginBundle.message("configure.addLanguage.label")))
                .addToCenter(new Spacer())
                .withBorder(JBUI.Borders.emptyTop(5))).setText("Note")
                .setIcon(AllIcons.General.Information)
                .setTabPaneActions(new DefaultActionGroup(myLanguagesActionGroup));

        tabPanel = myTabs.getComponent();
    }

    private class RemoveLanguageAction extends DumbAwareAction {
        private final Language myLanguage;
        private final LanguageVersion myLanguageVersion;

        RemoveLanguageAction(Language language, LanguageVersion languageVersion) {
            super("Close", "Close", AllIcons.Actions.Close);
            myLanguage = language;
            myLanguageVersion = languageVersion;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            var langVer = new Tuple2<>(myLanguage, myLanguageVersion);
            myLanguagesPanels.remove(langVer);
            var tab = myTabs.findInfo(langVer);
            myTabs.removeTab(tab);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(true);
            e.getPresentation().setHoveredIcon(AllIcons.Actions.CloseHovered);
            e.getPresentation().setText("Close");
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }

    private class LanguageAction extends DumbAwareAction {
        private final Language myLanguage;
        private final LanguageVersion myLanguageVersion;

        LanguageAction(Language language, LanguageVersion languageVersion) {
            super(language.show() + languageVersion.version(), null, language.icon());
            myLanguage = language;
            myLanguageVersion = languageVersion;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            var state = new HackerRankSettings.HackerRankLanguageSettingsState();
            state.language_$eq(JavaUtils.toOption(myLanguage));
            state.languageVersion_$eq(JavaUtils.toOption(myLanguageVersion));
            addNewLanguageSetting(state);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            var languages = myLanguagesPanels.keySet();
            e.getPresentation().setEnabledAndVisible(!languages.contains(new Tuple2<>(myLanguage, myLanguageVersion)));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    private void addNewLanguageSetting(HackerRankSettings.HackerRankLanguageSettingsState languageSettingsState) {
        var languageSettings = new LanguageSettingsPanel(myProject());
        languageSettings.reset(languageSettingsState);
        var language = languageSettingsState.language().get();
        var languageVer = languageSettingsState.languageVersion().get();
        var tup = new Tuple2<>(language, languageVer);
        myLanguagesPanels.put(tup, languageSettings);

        var text = language.show() + languageVer.version();
        var newTabInfo = new TabInfo(languageSettings.getComponent())
                .setObject(tup)
                .setText(text)
                .setIcon(language.icon())
                .setTabLabelActions(new DefaultActionGroup(new RemoveLanguageAction(language, languageVer)), text + ".Place")
                .setTabPaneActions(new DefaultActionGroup(myLanguagesActionGroup));
        myTabs.addTab(newTabInfo);
        myTabs.select(newTabInfo, true);
    }

    @Override
    public void reset(@NotNull HackerRankSettings.HackerRankSettingsState settings) {
        myTabs.removeAllTabs();
        myTabs.addTab(myInitTab);
        settings.getLanguageSettings().forEach(this::addNewLanguageSetting);
    }

    @Override
    public boolean isModified(@NotNull HackerRankSettings.HackerRankSettingsState settings) {
        var oldLangs = settings.getLanguageSettings().stream().map(setting -> new Tuple2<>(setting.language().get(), setting.languageVersion().get()))
                .collect(Collectors.toSet());
        var newLangs = myLanguagesPanels.keySet();
        if (!CollectionUtils.isEqualCollection(oldLangs, newLangs)) return true;


        for (var languageSettings : settings.getLanguageSettings()) {
            var language = languageSettings.language().get();
            var languageVersion = languageSettings.languageVersion().get();
            var tup = new Tuple2<>(language, languageVersion);
            var panel = myLanguagesPanels.get(tup);
            if (panel.isModified(languageSettings)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void apply(@NotNull HackerRankSettings.HackerRankSettingsState settings) throws ConfigurationException {
        var states = new ArrayList<HackerRankSettings.HackerRankLanguageSettingsState>();
        for (var languageSettings : myLanguagesPanels.entrySet()) {
            try {
                var state = new HackerRankSettings.HackerRankLanguageSettingsState();
                languageSettings.getValue().apply(state);
                states.add(state);
            } catch (ConfigurationException e) {
                var tab = myTabs.findInfo(languageSettings.getKey());
                myTabs.select(tab, true);
                return;
            }
        }

        settings.languageSettings_$eq(states);
    }

    @Override
    public @NotNull JComponent getComponent() {
        return rootPanel;
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
        rootPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(tabPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
