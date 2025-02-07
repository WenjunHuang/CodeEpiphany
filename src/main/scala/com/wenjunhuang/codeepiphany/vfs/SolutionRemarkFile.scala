package com.wenjunhuang.codeepiphany.vfs

import java.io.{ ByteArrayInputStream, InputStream, OutputStream }

import com.intellij.openapi.fileEditor.impl.FileDocumentManagerBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.{ VirtualFilePathWrapper, VirtualFileSystem }
import com.intellij.testFramework.LightVirtualFileBase
import com.intellij.util.LocalTimeCounter

import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.services.ChallengeRepository
import com.wenjunhuang.codeepiphany.utils.BufferedOutputStream
import com.wenjunhuang.codeepiphany.vfs.SolutionRemarkFileSystem.SolutionRemarkFilePath
import scala.jdk.OptionConverters.*

class SolutionRemarkFile(val myPath: SolutionRemarkFilePath, private val fileType: FileType)
    extends LightVirtualFileBase(myPath.getSessionId, fileType, LocalTimeCounter.currentTime())
    with VirtualFilePathWrapper {

  FileDocumentManagerBase.TRACK_NON_PHYSICAL.set(this, true)

  private val myFileSystem = SolutionRemarkFileSystem.getInstance()

  @volatile
  private var myContent: Option[String] = None

  override def getFileSystem: VirtualFileSystem = myFileSystem

  override def getPath: String = myFileSystem.getPath(myPath)

  override def getPresentablePath: String = {
    ProjectManagerEx.getInstanceEx.findOpenProjectByHash(myPath.projectHash) match
      case null => s"solutionId: ${myPath.solutionId}"
      case project =>
        ChallengeRepository
          .getInstance(project)
          .getDSLContext
          .select(SOLUTION.TITLE, CHALLENGE.TITLE)
          .from(SOLUTION)
          .innerJoin(CHALLENGE)
          .on(SOLUTION.CHALLENGEID.eq(CHALLENGE.ID))
          .where(SOLUTION.ID.eq(myPath.solutionId))
          .fetchOne() match
          case null => s"solutionId: ${myPath.solutionId}"
          case record =>
            s"${record.component2()} - ${record.component1()}"
  }

  override def enforcePresentableName(): Boolean = true

  override def getOutputStream(requestor: Any, newModificationStamp: Long, newTimeStamp: Long): OutputStream = {
    BufferedOutputStream { data =>
      ProjectManagerEx.getInstanceEx.findOpenProjectByHash(myPath.projectHash) match
        case null =>
        case project =>
          val newContent = String(data, java.nio.charset.StandardCharsets.UTF_8)
          ChallengeRepository
            .getInstance(project)
            .getDSLContext
            .update(SOLUTION)
            .set(SOLUTION.REMARK, newContent)
            .where(SOLUTION.ID.eq(myPath.solutionId))
            .execute()
          myContent = Some(newContent)
    }
  }

  override def contentsToByteArray(): Array[Byte] = getContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)

  override def getInputStream: InputStream = ByteArrayInputStream(contentsToByteArray())

  private def getContent: String = {
    myContent match
      case Some(content) => content
      case None =>
        val content = ProjectManagerEx.getInstanceEx.findOpenProjectByHash(myPath.projectHash) match
          case null => ""
          case project =>
            ChallengeRepository
              .getInstance(project)
              .getDSLContext
              .selectFrom(SOLUTION)
              .where(SOLUTION.ID.eq(myPath.solutionId))
              .fetchOptional()
              .toScala match
              case None => ""
              case Some(record) =>
                StringUtil.notNullize(record.getRemark)
        myContent = Some(content)
        content
  }
}
