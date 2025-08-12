package com.wenjunhuang.codeepiphany.settings

import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.components.{PersistentStateComponent, Service, State, Storage}
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.xmlb.annotations.{OptionTag, XCollection}
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.model.newtypes.*
import com.wenjunhuang.codeepiphany.model.{CodeDojo, Constants, Language}
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.{ChallengeSettingsState, ChallengeSettingsStateItem, TestCase}
import com.wenjunhuang.codeepiphany.utils.ConfigConverters.{CodeDojoConverter, LanguageConverter}

import java.util as ju
import scala.annotation.meta.{beanGetter, beanSetter}
import scala.beans.BeanProperty
import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.*

/**
 * 因为这个对象可能会被多个线程修改，为了简单起见，这里使用了同步锁。
 */
@Service(Array(Level.PROJECT))
@State(name = Constants.CHALLENGE_SETTING, storages = Array(new Storage(Constants.CHALLENGE_SETTING_FILE)))
final class ChallengeSettings extends PersistentStateComponent[ChallengeSettingsState] {
  @volatile
  private var myState                           = ChallengeSettingsState()
  override def getState: ChallengeSettingsState = synchronized { myState }

  override def loadState(state: ChallengeSettingsState): Unit = synchronized {
    myState = state
  }

  def addChallenge(key: String, item: ChallengeSettingsStateItem): Unit = synchronized {
    val newItem = new ChallengeSettingsStateItem()
    newItem.challengeId = item.challengeId
    newItem.challengeLanguageId = item.challengeLanguageId
    newItem.solutionId = item.solutionId
    newItem.dojo = item.dojo
    newItem.language = item.language
    newItem.testCases = new ju.ArrayList[TestCase](item.testCases)
    newItem.defaultTestCases = new ju.ArrayList[TestCase](item.defaultTestCases)
    myState.challenges.put(key, newItem)
  }

  def removeChallenge(key: String): Unit = synchronized {
    myState.challenges.remove(key)
  }
  
  def removeChallenge(vf: VirtualFile): Unit = synchronized {
    myState.challenges.remove(vf.getCanonicalPath)
  }

  def addChallenge(vf: VirtualFile, item: ChallengeSettingsStateItem): Unit = synchronized {
    addChallenge(vf.getCanonicalPath, item)
  }

  def findChallengeId(filePath: String): Option[ChallengeSettingsStateItem] = synchronized {
    Option(myState.challenges.get(filePath)).map { item =>
      val result = new ChallengeSettingsStateItem()
      result.challengeId = item.challengeId
      result.challengeLanguageId = item.challengeLanguageId
      result.solutionId = item.solutionId
      result.dojo = item.dojo
      result.language = item.language
      result.defaultTestCases.addAll(item.defaultTestCases)
      result.testCases.addAll(item.testCases)
      result
    }
  }

  def updateChallengeTestCases(filePath: String, testCases: ju.List[TestCase]): Unit = synchronized {
    Option(myState.challenges.get(filePath)).foreach { item =>
      item.testCases.clear()
      item.testCases.addAll(testCases)
    }
  }
  def updateChallengeTestCases(vf: VirtualFile, testCases: ju.List[TestCase]): Unit = synchronized {
    Option(myState.challenges.get(vf.getCanonicalPath)).foreach { item =>
      item.testCases.clear()
      item.testCases.addAll(testCases)
    }
  }

  def setChallengeSolutionId(vf: VirtualFile, solutionId: SolutionId): Unit = synchronized {
    Option(myState.challenges.get(vf.getCanonicalPath)).foreach { item =>
      item.solutionId = solutionId.value
    }
  }

  def findChallengeId(vf: VirtualFile): Option[ChallengeSettingsStateItem] =
    findChallengeId(vf.getCanonicalPath)
    
  def modifyCodeFilePath(oldPath:String, newPath: String): Unit = synchronized {
    if myState.challenges.containsKey(oldPath) then
      val item = myState.challenges.remove(oldPath)
      myState.challenges.put(newPath, item)
  }
}

object ChallengeSettings {

  def getInstance(project: Project): ChallengeSettings = project.getService(classOf[ChallengeSettings])

  class ChallengeSettingsStateItem {
    @BeanProperty
    var challengeId: Long = uninitialized

    @BeanProperty
    var challengeLanguageId: Long = uninitialized

    @(OptionTag @beanGetter @beanSetter)(converter = classOf[CodeDojoConverter])
    @BeanProperty
    var dojo: CodeDojo = uninitialized

    @(OptionTag @beanGetter @beanSetter)(converter = classOf[LanguageConverter])
    @BeanProperty
    var language: Language = uninitialized

    @BeanProperty
    var solutionId: Long = uninitialized

    @(XCollection @beanGetter @beanSetter)(elementTypes = Array(classOf[TestCase]))
    @BeanProperty
    var defaultTestCases: ju.List[TestCase] = new ju.ArrayList[TestCase]()

    @(XCollection @beanGetter @beanSetter)(elementTypes = Array(classOf[TestCase]))
    @BeanProperty
    var testCases: ju.List[TestCase] = new ju.ArrayList[TestCase]()
  }

  class TestCase {
    @BeanProperty
    var input: String = uninitialized

    @BeanProperty
    var expectedOutput: String = uninitialized
  }

  object TestCase {
    def apply(input: String, expectedOutput: String): TestCase = {
      val r = new TestCase()
      r.input = input
      r.expectedOutput = expectedOutput
      r
    }

    extension (testCases: List[TestCase]) {
      def show: String = {
        if testCases.isEmpty then ""
        else
          testCases.zipWithIndex.map { (testCase, index) =>
            s"""
               |${PluginBundle.message("testcases.title", index + 1)}:
               |-----------------------------------------------------
               |input:
               |${testCase.input}
               |-----------------------------------------------------
               |expected output:
               |${testCase.expectedOutput}""".stripMargin
          }.mkString("\n")
      }
    }
  }

  object ChallengeSettingsStateItem {
    def apply(
      challengeId: ChallengeId,
      challengeLanguageId: ChallengeLanguageId,
      codeDojo: CodeDojo,
      language: Language,
      solutionId: SolutionId,
      testCases: List[TestCase],
      defaultTestCases: List[TestCase]
    ): ChallengeSettingsStateItem = {
      val r = new ChallengeSettingsStateItem()
      r.challengeId = challengeId.value
      r.challengeLanguageId = challengeLanguageId.value
      r.dojo = codeDojo
      r.language = language
      r.solutionId = solutionId.value
      r.testCases = new ju.ArrayList[TestCase](testCases.asJava)
      r.defaultTestCases = new ju.ArrayList[TestCase](defaultTestCases.asJava)
      r
    }
  }

  class ChallengeSettingsState {
    @BeanProperty
    var challenges: ju.Map[String, ChallengeSettingsStateItem] = new ju.HashMap[String, ChallengeSettingsStateItem]()

  }
}
