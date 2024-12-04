package com.wenjunhuang.codeepiphany.model

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.{PersistentStateComponent, State, Storage}
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.{MapAnnotation, Transient}
import com.wenjunhuang.codeepiphany.model.QuestionStorage.{QuestionItem, StorageState}
import org.jetbrains.annotations.NotNull

import java.util as ju
import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.*

@State(name = Constants.ProjectName, storages = Array(new Storage(value = Constants.QuestionStorageFile)))
class QuestionStorage(private val project: Project) extends PersistentStateComponent[StorageState] with Disposable {
  private var questions = Map[String, QuestionItem]()

  override def getState: StorageState =
    StorageState(Some(questions))

  override def loadState(newState: StorageState): Unit =
    questions = newState.questions.asScala.mapValuesInPlace { (_, v) =>
      v.dojo = CodeDojo.optionValueOf(v.dojoName).orNull
      v
    }.filterInPlace((_, v) => v.dojo != null).toMap

  def findQuestionItemByFilePath(filePath: String): Option[QuestionItem] =
    questions.values.find(_.codeFilePath == filePath)

  def addQuestionItem(item: QuestionItem): Unit =
    questions += item.codeFilePath -> item

  def removeQuestionItem(filePath: String): Unit =
    questions -= filePath


  override def dispose(): Unit = {
    
  }
}

object QuestionStorage {
  def getInstance(project: Project): QuestionStorage = project.getService(classOf[QuestionStorage])

  class QuestionItem {
    var dojoId: String              = uninitialized
    var slug: String                = uninitialized
    var dojoName: String            = uninitialized
    var codeFilePath: String        = uninitialized
    var descriptionFilePath: String = uninitialized

    @Transient
    var dojo: CodeDojo = uninitialized
  }

  class StorageState(q: Option[Map[String, QuestionItem]]) {
    def this() = this(None)

    @NotNull
    @MapAnnotation
    var questions: ju.Map[String, QuestionItem] = q.map(_.asJava).orNull
  }
}
