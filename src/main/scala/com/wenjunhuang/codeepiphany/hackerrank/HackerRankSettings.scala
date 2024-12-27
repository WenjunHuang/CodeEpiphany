package com.wenjunhuang.codeepiphany.hackerrank

import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.components.{PersistentStateComponent, Service, State, Storage}
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.OptionTag
import com.wenjunhuang.codeepiphany.hackerrank.HackerRankSettings.*
import com.wenjunhuang.codeepiphany.model.{Constants, Language}
import org.typelevel.ci.CIString

import java.util
import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Service(Array(Level.PROJECT))
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
    @BeanProperty
    var sourceFolder: String = uninitialized

    @BeanProperty
    @OptionTag(converter = classOf[LanguageConverter])
    var language: Language = uninitialized

    @BeanProperty
    var fileNameTemplate: String = uninitialized

    @BeanProperty
    var codeTemplate: String = uninitialized
  }

  class LanguageConverter extends Converter[Language] {
    override def fromString(value: String): Language = Language.fromCIString(CIString(value)).orNull
    override def toString(value: Language): String   = value.value
  }

  def getInstance(project: Project): HackerRankSettings =
    project.getService(classOf[HackerRankSettings])
}
