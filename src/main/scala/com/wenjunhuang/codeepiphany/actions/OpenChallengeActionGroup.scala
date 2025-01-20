package com.wenjunhuang.codeepiphany.actions

import com.intellij.openapi.actionSystem.*

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup.*
import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion}

class OpenChallengeActionGroup extends ActionGroup {
  override def getChildren(e: AnActionEvent): Array[AnAction] = {
    Option(CHALLENGE_PROVIDER_KEY.getData(e.getDataContext)) match
      case None => Array.empty
      case Some(provider) =>
        provider.getLanguages.map((language, languageVersion) => LanguageAction(language, languageVersion)).toArray
  }

  override def update(e: AnActionEvent): Unit = {
    val presentation = e.getPresentation
    presentation.setEnabled(true)

    for
      challengeProvider <- Option(CHALLENGE_PROVIDER_KEY.getData(e.getDataContext))
    yield
      if !challengeProvider.currentSelectedCanBeOpened then presentation.setEnabled(false)
      else
        challengeProvider.getLanguages match
          case Nil => presentation.setEnabled(false)
          case _ :: Nil =>
            presentation.setPopupGroup(false)
          case _ =>
            presentation.setPopupGroup(true)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}

object OpenChallengeActionGroup {
  final val CHALLENGE_PROVIDER_KEY = DataKey.create[OpenChallengeProvider]("CHALLENGE_PROVIDER_KEY")

  private class LanguageAction(private val myLanguage: Language, private val myLanguageVersion: LanguageVersion)
      extends AnAction(s"${myLanguage.show}${myLanguageVersion.version}", null, myLanguage.icon) {
    override def actionPerformed(e: AnActionEvent): Unit = {
      Option(CHALLENGE_PROVIDER_KEY.getData(e.getDataContext)) match
        case None           =>
        case Some(provider) => provider.openCurrentSelectedChallenge(myLanguage, myLanguageVersion)
    }

    override def update(e: AnActionEvent): Unit = {
      Option(CHALLENGE_PROVIDER_KEY.getData(e.getDataContext)) match
        case None => e.getPresentation.setEnabledAndVisible(false)
        case Some(provider) =>
          val presentation = e.getPresentation
          if !provider.currentSelectedCanBeOpened then presentation.setEnabled(false)
          else
            presentation.setEnabledAndVisible(true)
            provider.getLanguages match
              case one :: Nil if one == myLanguage =>
                presentation.setText(PluginBundle.message("group.CodeEpiphany.Dojos.Actions.OpenChallengeGroup.text"))
                presentation.setDescription(
                  PluginBundle.message("group.CodeEpiphany.Dojos.Actions.OpenChallengeGroup.description")
                )
              case _ =>
                presentation.setText(myLanguage.show)
                presentation.setDescription(myLanguage.show)
    }

    override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
  }

  trait OpenChallengeProvider {
    def openCurrentSelectedChallenge(language: Language, languageVersion: LanguageVersion): Unit

    def currentSelectedCanBeOpened: Boolean

    def getLanguages: List[(Language, LanguageVersion)]
  }
}
