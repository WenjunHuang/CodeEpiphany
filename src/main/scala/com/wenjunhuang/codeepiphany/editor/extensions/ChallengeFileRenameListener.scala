package com.wenjunhuang.codeepiphany.editor.extensions

import com.intellij.openapi.project.{Project, ProjectLocator}
import com.intellij.openapi.vfs.newvfs.events.{VFileDeleteEvent, VFileEvent, VFileMoveEvent, VFilePropertyChangeEvent}
import com.intellij.openapi.vfs.{AsyncFileListener, VirtualFile}
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings

import java.util
import scala.jdk.CollectionConverters.*

class ChallengeFileRenameListener extends AsyncFileListener {
  override def prepareChange(events: util.List[? <: VFileEvent]): AsyncFileListener.ChangeApplier = {
    val changes = events.asScala.collect {
      case propertyChange: VFilePropertyChangeEvent =>
        val oldName = propertyChange.getOldPath
        val newName = propertyChange.getNewPath
        if (oldName != newName) {
          val project = ProjectLocator.getInstance().guessProjectForFile(propertyChange.getFile)

          () => modifyCodeFilePath(project, oldName, newName)
        } else { () => () }

      case moveEvent: VFileMoveEvent if moveEvent.isValid =>
        val oldPath = moveEvent.getOldPath
        val newPath = moveEvent.getNewPath
        if (oldPath != newPath) {
          // If the file is moved, we need to update the path in the settings
          // and remove the old file reference.
          val project = ProjectLocator.getInstance().guessProjectForFile(moveEvent.getFile)

          () => modifyCodeFilePath(project, oldPath, newPath)
        } else { () => () }
      case deleteEvent: VFileDeleteEvent =>
        val oldPath = deleteEvent.getPath
        val project = ProjectLocator.getInstance().guessProjectForFile(deleteEvent.getFile)
        () => deleteCodeFile(project, deleteEvent.getFile)

    }.toList

    new AsyncFileListener.ChangeApplier() {
      override def afterVfsChange(): Unit = {
        changes.foreach(_.apply())
      }
    }
  }

  private def deleteCodeFile(project: Project, vf: VirtualFile): Unit = {
    if project != null && vf != null then
      val settings = ChallengeSettings.getInstance(project)
      settings.removeChallenge(vf)
  }

  private def modifyCodeFilePath(project: Project, oldPath: String, newPath: String): Unit = {
    if project != null then
      val settings = ChallengeSettings.getInstance(project)
      settings.modifyCodeFilePath(oldPath, newPath)
  }

}
