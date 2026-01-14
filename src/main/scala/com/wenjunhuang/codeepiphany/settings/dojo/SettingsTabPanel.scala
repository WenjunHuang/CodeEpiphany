package com.wenjunhuang.codeepiphany.settings.dojo

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.{ ActionUpdateThread, AnActionEvent, DefaultActionGroup }
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.{ DumbAwareAction, Project }
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.tabs.{ JBTabsEx, JBTabsFactory, TabInfo }
import com.intellij.uiDesigner.core.Spacer
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.model.{ CodeDojo, Language, LanguageVersion }
import com.wenjunhuang.codeepiphany.settings.{ CodeEpiphanySettings, SettingsUi }
import org.jetbrains.annotations.NotNull

import java.util
import javax.swing.*
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.jdk.FunctionConverters.*
import scala.util.boundary

class SettingsTabPanel(
  project: Project,
  private val myCodeDojo: CodeDojo,
  languages: Seq[(Language, LanguageVersion)],
  private val myDemoTemplateSupplier: (Language, LanguageVersion) => Any,
  parentDisposable: Disposable
) extends SettingsUi[BaseCodeDojoSettings.CodeDojoSettingsState](project) {
  private val myLanguagesPanels      = mutable.Map[(Language, LanguageVersion), LanguageSettingsPanel]()
  private val myLanguagesActionGroup = new DefaultActionGroup("Languages", null, AllIcons.General.Add)

  private val myTabs = JBTabsFactory.createTabs(myProject).asInstanceOf[JBTabsEx]
  private val myInitTab = new TabInfo(
    new BorderLayoutPanel()
      .addToTop(new JBLabel(PluginBundle.message("configure.addLanguage.label")).setAllowAutoWrapping(true))
      .addToCenter(new Spacer)
      .withBorder(JBUI.Borders.emptyTop(5))
  ).setText(PluginBundle.message("configure.addLanguage.text"))
    .setIcon(AllIcons.General.Information)
    .setTabPaneActions(new DefaultActionGroup(myLanguagesActionGroup))
  private val rootPanel = new BorderLayoutPanel().addToCenter(myTabs.getComponent)

  languages.foreach(language => myLanguagesActionGroup.add(new LanguageAction(language._1, language._2)))
  myLanguagesActionGroup.setPopup(true)
  Disposer.register(parentDisposable, this)

  private def addNewLanguageSetting(languageSettingsState: BaseCodeDojoSettings.LanguageSettingsState): Unit = {
    val languageSettings = new LanguageSettingsPanel(myProject, myCodeDojo, myDemoTemplateSupplier.asJavaBiFunction)
    Disposer.register(this, languageSettings)
    languageSettings.reset(languageSettingsState)
    val language    = languageSettingsState.language.get
    val languageVer = languageSettingsState.languageVersion.get
    val tup         = (language, languageVer)
    myLanguagesPanels.put(tup, languageSettings)
    val text = Language.prettyPrint(language, languageVer)
    val newTabInfo = new TabInfo(languageSettings.getComponent)
      .setObject(tup)
      .setText(text)
      .setIcon(language.icon)
      .setTabLabelActions(new DefaultActionGroup(new RemoveLanguageAction(language, languageVer)), text + ".Place")
      .setTabPaneActions(new DefaultActionGroup(myLanguagesActionGroup))
    myTabs.addTab(newTabInfo)
    myTabs.select(newTabInfo, true)
  }

  override def reset(@NotNull settings: BaseCodeDojoSettings.CodeDojoSettingsState): Unit = {
    myTabs.removeAllTabs()
    myTabs.addTab(myInitTab)
    settings.languageSettings.forEach(this.addNewLanguageSetting)
  }

  override def isModified(@NotNull settings: BaseCodeDojoSettings.CodeDojoSettingsState): Boolean = {
    val oldLangs = settings.languageSettings.asScala
      .map((setting: BaseCodeDojoSettings.LanguageSettingsState) => (setting.language.get, setting.languageVersion.get))
      .toSet

    val newLangs = myLanguagesPanels.keySet
    boundary {
      if (oldLangs != newLangs) boundary.break(true)
      else {
        for (languageSettings <- settings.languageSettings.asScala) {
          myLanguagesPanels.get((languageSettings.language.get, languageSettings.languageVersion.get)) match {
            case Some(panel) =>
              if (panel.isModified(languageSettings)) boundary.break(true)
            case None =>
          }
        }
        false
      }
    }
  }

  @throws[ConfigurationException]
  override def apply(settings: BaseCodeDojoSettings.CodeDojoSettingsState): Unit = {
    val states = new util.ArrayList[BaseCodeDojoSettings.LanguageSettingsState]
    boundary {
      for ((key, value) <- myLanguagesPanels) {
        try {
          val state = new BaseCodeDojoSettings.LanguageSettingsState
          value.apply(state)
          states.add(state)
        } catch {
          case e: ConfigurationException =>
            val tab = myTabs.findInfo(key)
            assert(tab != null)
            myTabs.select(tab, true)
            boundary.break()
        }
      }
      settings.languageSettings = states
      myProject.getMessageBus.syncPublisher(CodeEpiphanySettings.TOPIC).changed()
    }
  }

  override def getComponent: JComponent = rootPanel

  private class RemoveLanguageAction(private val myLanguage: Language, private val myLanguageVersion: LanguageVersion)
      extends DumbAwareAction("Close", "Close", AllIcons.Actions.Close) {
    override def actionPerformed(e: AnActionEvent): Unit = {
      val langVer = (myLanguage, myLanguageVersion)
      myLanguagesPanels.remove(langVer)
      val tab = myTabs.findInfo(langVer)
      myTabs.removeTab(tab)
    }
    override def update(e: AnActionEvent): Unit = {
      e.getPresentation.setEnabledAndVisible(true)
      e.getPresentation.setHoveredIcon(AllIcons.Actions.CloseHovered)
      e.getPresentation.setText("Close")
    }
    override def getActionUpdateThread = ActionUpdateThread.EDT
  }

  private class LanguageAction(private val myLanguage: Language, private val myLanguageVersion: LanguageVersion)
      extends DumbAwareAction(Language.prettyPrint(myLanguage, myLanguageVersion), null, myLanguage.icon) {
    override def actionPerformed(e: AnActionEvent): Unit = {
      val state = new BaseCodeDojoSettings.LanguageSettingsState
      state.language = Some(myLanguage)
      state.languageVersion = Some(myLanguageVersion)
      addNewLanguageSetting(state)
    }
    override def update(e: AnActionEvent): Unit = {
      val languages = myLanguagesPanels.keySet
      e.getPresentation.setEnabledAndVisible(!languages.contains((myLanguage, myLanguageVersion)))
    }
    override def getActionUpdateThread = ActionUpdateThread.EDT
  }
}
