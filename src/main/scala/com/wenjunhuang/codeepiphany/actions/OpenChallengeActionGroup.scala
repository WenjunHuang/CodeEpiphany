package com.wenjunhuang.codeepiphany.actions

import com.intellij.openapi.actionSystem.*
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup.*
import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.utils.actions.{ActionCompatible, DataKeyNotNull}

class OpenChallengeActionGroup extends ActionGroup with DataKeyNotNull(CHALLENGE_PROVIDER_KEY) with ActionCompatible {
  override def getChildren(e: AnActionEvent): Array[AnAction] = {
    Option(CHALLENGE_PROVIDER_KEY.getData(e.getDataContext)) match
      case None => Array.empty
      case Some(provider) =>
        provider.getLanguages.map((language, languageVersion) => LanguageAction(language, languageVersion)).toArray
  }

  override def update(e: AnActionEvent): Unit = {
    val presentation = e.getPresentation
    if isSatisfied(e) then
      presentation.setEnabled(true)
      val provider = getValue(e)

      if !provider.currentSelectedCanBeOpened then presentation.setEnabled(false)
      else
        provider.getLanguages match
          case Nil => presentation.setEnabled(false)
          case _ :: Nil =>
            presentation.setPopupGroup(false)
          case _ =>
            presentation.setPopupGroup(true)
  }
}

object OpenChallengeActionGroup {
  final val CHALLENGE_PROVIDER_KEY = DataKey.create[OpenChallengeProvider]("CHALLENGE_PROVIDER_KEY")

  private class LanguageAction(private val myLanguage: Language, private val myLanguageVersion: LanguageVersion)
      extends AnAction(Language.prettyPrint(myLanguage, myLanguageVersion), null, myLanguage.icon)
      with DataKeyNotNull(CHALLENGE_PROVIDER_KEY)
      with ActionCompatible {
    override def actionPerformed(e: AnActionEvent): Unit = {
      getValue(e).openCurrentSelectedChallenge(myLanguage, myLanguageVersion)
    }

    override def update(e: AnActionEvent): Unit = {
      val presentation = e.getPresentation
      if isSatisfied(e) then
        val provider = getValue(e)
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
              presentation.setText(Language.prettyPrint(myLanguage, myLanguageVersion))
              presentation.setDescription(Language.prettyPrint(myLanguage, myLanguageVersion))
      else presentation.setEnabledAndVisible(false)
    }
  }

  trait OpenChallengeProvider {
    def openCurrentSelectedChallenge(language: Language, languageVersion: LanguageVersion): Unit

    def currentSelectedCanBeOpened: Boolean

    def getLanguages: List[(Language, LanguageVersion)]
  }
}
