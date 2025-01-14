package com.wenjunhuang.codeepiphany.editor.extensions

import com.intellij.openapi.project.{Project, ProjectLocator}
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.newvfs.events.{VFileEvent, VFileMoveEvent, VFilePropertyChangeEvent}
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings

import java.util
import scala.jdk.CollectionConverters.*

class ChallengeFileRenameListener extends AsyncFileListener {
  override def prepareChange(events: util.List[? <: VFileEvent]): AsyncFileListener.ChangeApplier = {
    val changes = events.asScala.collect {
      case propertyChange: VFilePropertyChangeEvent if propertyChange.isRename =>
        val oldName = propertyChange.getOldPath
        val newName = propertyChange.getNewPath
        val project = ProjectLocator.getInstance().guessProjectForFile(propertyChange.getFile)

        () => modifyCodeFilePath(project, oldName, newName)

      case moveEvent: VFileMoveEvent if moveEvent.isValid =>
        val oldPath = moveEvent.getOldPath
        val newPath = moveEvent.getNewPath
        val project = ProjectLocator.getInstance().guessProjectForFile(moveEvent.getFile)

        () => modifyCodeFilePath(project, oldPath, newPath)
    }.toList

    new AsyncFileListener.ChangeApplier() {
      override def afterVfsChange(): Unit = {
        changes.foreach(_.apply())
      }
    }
  }

  private def modifyCodeFilePath(project: Project, oldPath: String, newPath: String): Unit = {
    if project != null then
      val settings = ChallengeSettings.getInstance(project)
      settings.findChallengeId(oldPath) match {
        case Some(challenge) =>
          settings.addChallenge(newPath, challenge)
          settings.removeChallenge(oldPath)
        case None =>
      }
  }

}
