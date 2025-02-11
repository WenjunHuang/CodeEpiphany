package com.wenjunhuang.codeepiphany.settings.dojo;

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
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.wenjunhuang.codeepiphany.PluginBundle;
import com.wenjunhuang.codeepiphany.model.CodeDojo;
import com.wenjunhuang.codeepiphany.model.Language;
import com.wenjunhuang.codeepiphany.model.LanguageVersion;
import com.wenjunhuang.codeepiphany.settings.CodeEpiphanySettings;
import com.wenjunhuang.codeepiphany.settings.SettingsUi;
import com.wenjunhuang.codeepiphany.utils.JavaUtils;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import scala.Tuple2;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class CodeDojoSettingsForm extends SettingsUi<BaseCodeDojoSettings.CodeDojoSettingsState> {
    private JBTabsEx myTabs;
    private JPanel rootPanel;
    private final DefaultActionGroup myLanguagesActionGroup;
    private final Map<Tuple2<Language, LanguageVersion>, LanguageSettingsPanel> myLanguagesPanels = new HashMap<>();
    private TabInfo myInitTab;
    private final BiFunction<Language, LanguageVersion, Object> myDemoTemplateSupplier;
    private final CodeDojo myCodeDojo;

    public CodeDojoSettingsForm(Project project,
                                CodeDojo codeDojo,
                                Collection<Tuple2<Language, LanguageVersion>> languages,
                                BiFunction<Language, LanguageVersion, Object> demoTemplateSupplier,
                                Disposable parentDisposable) {
        super(project);
        Disposer.register(parentDisposable, this);

        myLanguagesActionGroup = new DefaultActionGroup("Languages", null, AllIcons.General.Add);
        languages.forEach((language) -> {
            myLanguagesActionGroup.add(new LanguageAction(language._1(), language._2()));
        });
        myLanguagesActionGroup.setPopup(true);
        myDemoTemplateSupplier = demoTemplateSupplier;
        myCodeDojo = codeDojo;


        createUIComponents();
    }

    private void createUIComponents() {
        myTabs = (JBTabsEx) JBTabsFactory.createTabs(myProject());
        myInitTab = new TabInfo(new BorderLayoutPanel().addToTop(
                        new JBLabel(PluginBundle.message("configure.addLanguage.label")).setAllowAutoWrapping(true))
                .addToCenter(new Spacer())
                .withBorder(JBUI.Borders.emptyTop(5))).setText(PluginBundle.message("configure.addLanguage.text"))
                .setIcon(AllIcons.General.Information)
                .setTabPaneActions(new DefaultActionGroup(myLanguagesActionGroup));

        JComponent tabPanel = myTabs.getComponent();
        rootPanel = new BorderLayoutPanel().addToCenter(tabPanel);
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
            super(Language.prettyPrint(language, languageVersion), null, language.icon());
            myLanguage = language;
            myLanguageVersion = languageVersion;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            var state = new BaseCodeDojoSettings.LanguageSettingsState();
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

    private void addNewLanguageSetting(BaseCodeDojoSettings.LanguageSettingsState languageSettingsState) {
        var languageSettings = new LanguageSettingsPanel(myProject(),
                myCodeDojo,
                myDemoTemplateSupplier);
        Disposer.register(this, languageSettings);
        languageSettings.reset(languageSettingsState);
        var language = languageSettingsState.language().get();
        var languageVer = languageSettingsState.languageVersion().get();
        var tup = new Tuple2<>(language, languageVer);
        myLanguagesPanels.put(tup, languageSettings);

        var text = Language.prettyPrint(language, languageVer);
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
    public void reset(@NotNull BaseCodeDojoSettings.CodeDojoSettingsState settings) {
        myTabs.removeAllTabs();
        myTabs.addTab(myInitTab);
        settings.getLanguageSettings().forEach(this::addNewLanguageSetting);
    }

    @Override
    public boolean isModified(@NotNull BaseCodeDojoSettings.CodeDojoSettingsState settings) {
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
    public void apply(@NotNull BaseCodeDojoSettings.CodeDojoSettingsState settings) throws ConfigurationException {
        var states = new ArrayList<BaseCodeDojoSettings.LanguageSettingsState>();
        for (var languageSettings : myLanguagesPanels.entrySet()) {
            try {
                var state = new BaseCodeDojoSettings.LanguageSettingsState();
                languageSettings.getValue().apply(state);
                states.add(state);
            } catch (ConfigurationException e) {
                var tab = myTabs.findInfo(languageSettings.getKey());
                assert tab != null;
                myTabs.select(tab, true);
                return;
            }
        }

        settings.languageSettings_$eq(states);
        myProject().getMessageBus().syncPublisher(CodeEpiphanySettings.TOPIC()).changed();
    }

    @Override
    public @NotNull JComponent getComponent() {
        return rootPanel;
    }
}
