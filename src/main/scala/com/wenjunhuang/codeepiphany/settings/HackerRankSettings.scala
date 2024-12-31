package com.wenjunhuang.codeepiphany.settings

import com.intellij.openapi.components.{BaseState, PersistentStateComponent, State, Storage}
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.{Attribute, OptionTag}
import com.wenjunhuang.codeepiphany.model.{Constants, Language}
import com.wenjunhuang.codeepiphany.settings.HackerRankSettings.*
import org.typelevel.ci.CIString

import scala.annotation.meta.{beanGetter, beanSetter, field, getter}
import scala.beans.BeanProperty

@State(
  name = Constants.HACKERRANK_SETTING,
  storages = Array(new Storage(Constants.HACKERRANK_SETTING_FILE))
)
final class HackerRankSettings(private val myProject: Project)
    extends PersistentStateComponent[HackerRankState] {
  private var state = HackerRankState()

  override def getState: HackerRankState = state

  override def loadState(newState: HackerRankState): Unit =
    state = newState
}

object HackerRankSettings {
  class HackerRankState {
    @(Attribute @field)(converter = classOf[StringOptionConverter])
    var sourceFolder: Option[String] = None

    @(Attribute @field)(converter = classOf[LanguageOptionConverter])
    var language: Option[Language] = None

    @(Attribute @field)(converter = classOf[StringOptionConverter])
    var fileNameTemplate: Option[String] = None

    @(Attribute @field)(converter = classOf[StringOptionConverter])
    var codeTemplate: Option[String] = None
  }

  class LanguageConverter extends Converter[Language] {
    override def fromString(value: String): Language =
      Language.fromCIString(CIString(value)).orNull
    override def toString(value: Language): String =
      value.value
  }

  implicit val languageConverter: Converter[Language] = new LanguageConverter
  implicit val stringConverter: Converter[String] = new Converter[String] {
    override def fromString(value: String): String = value
    override def toString(value: String): String   = value
  }
  
  class OptionConverter[T: Converter] extends Converter[Option[T]] {
    override def fromString(value: String): Option[T] =
      if (value == null || value.isEmpty) None else Some(implicitly[Converter[T]].fromString(value))
    override def toString(value: Option[T]): String =
      value.map(it => implicitly[Converter[T]].toString(it)).orNull
  }

  class StringOptionConverter   extends OptionConverter[String]
  class LanguageOptionConverter extends OptionConverter[Language]

  def getInstance(project: Project): HackerRankSettings =
    project.getService(classOf[HackerRankSettings])
}
