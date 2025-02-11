package com.wenjunhuang.codeepiphany.settings

import java.io.File
import scala.annotation.meta.field

import com.intellij.openapi.components.*
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.Attribute

import com.wenjunhuang.codeepiphany.model.Constants
import com.wenjunhuang.codeepiphany.settings.CodeEpiphanySettings.CodeEpiphanySettingsState

@Service(Array(Level.PROJECT))
@State(name = Constants.SETTING, storages = Array(new Storage(Constants.SETTING_FILE)))
final class CodeEpiphanySettings(private val myProject: Project)
    extends PersistentStateComponent[CodeEpiphanySettingsState]() {
  private var state = CodeEpiphanySettingsState()

  override def getState: CodeEpiphanySettingsState = state

  override def loadState(newState: CodeEpiphanySettingsState): Unit =
    state = newState

}

object CodeEpiphanySettings {
  val TOPIC =
    new Topic[CodeEpiphanySettingsChangedListener](
      classOf[CodeEpiphanySettingsChangedListener],
      Topic.BroadcastDirection.TO_CHILDREN
    )

  trait CodeEpiphanySettingsChangedListener {
    def changed(): Unit
  }

  def getInstance(project: Project): CodeEpiphanySettings = project.getService(classOf[CodeEpiphanySettings])

  class CodeEpiphanySettingsState {
    @(Attribute @field)
    var databaseFolder: String = s"$$PROJECT_DIR$$/.idea/${Constants.PROJECT_NAME}"

    def getDatabaseFolder(project: Project): String = {
      PathMacroManager.getInstance(project).expandPath(databaseFolder)
    }

    def setDatabaseFolder(folder: String, project: Project): Unit = {
      databaseFolder = PathMacroManager.getInstance(project).collapsePath(folder)
    }
  }

  trait DatabaseFolderNotifier {
    def databaseFolderChanged(databaseFolder: File): Unit
  }

  @Topic.ProjectLevel
  val DATABASE_FOLDER_TOPIC: Topic[DatabaseFolderNotifier] = Topic(classOf[DatabaseFolderNotifier])
}
