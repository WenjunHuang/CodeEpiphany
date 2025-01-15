package com.wenjunhuang.codeepiphany.vfs

import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.vfs.{ VirtualFilePathWrapper, VirtualFileSystem }
import com.intellij.testFramework.LightVirtualFileBase
import com.intellij.util.{ IncorrectOperationException, LocalTimeCounter }
import com.wenjunhuang.codeepiphany.database.Tables.SOLUTION_SUBMISSION
import com.wenjunhuang.codeepiphany.model.{ ChallengeRepository, Language, LanguageVersion }
import com.wenjunhuang.codeepiphany.vfs.SubmissionCodeFileSystem.SubmissionCodeFilePath

import java.io.{ ByteArrayInputStream, InputStream, OutputStream }
import java.nio.charset.StandardCharsets

class SubmissionCodeFile(
  private val myPath: SubmissionCodeFilePath,
  private val myLanguage: Language,
  private val myLanguageVersion: LanguageVersion,
  private val myIdeaLanguage: com.intellij.lang.Language
) extends LightVirtualFileBase(
      myPath.getSessionId,
      SubmissionCodeFileType(myLanguage, myLanguageVersion, myIdeaLanguage),
      LocalTimeCounter.currentTime()
    )
    with VirtualFilePathWrapper {
  private val myFileSystem              = SubmissionCodeFileSystem.getInstance()
  private var myContent: Option[String] = None

  override def getPresentablePath: String = myPath.getSessionId

  override def enforcePresentableName(): Boolean = true

  override def getPresentableName: String = myPath.getSessionId

  override def getFileSystem: VirtualFileSystem = myFileSystem

  override def getPath: String = myFileSystem.getPath(myPath)

  override def getOutputStream(requestor: Any, newModificationStamp: Long, newTimeStamp: Long): OutputStream =
    throw new IncorrectOperationException("File is not writable: " + this)

  override def contentsToByteArray(): Array[Byte] = getContent.getBytes(StandardCharsets.UTF_8)

  override def getInputStream: InputStream = ByteArrayInputStream(contentsToByteArray())

  override def isWritable: Boolean = false

  private def getContent: String = {
    myContent match
      case None =>
        val content = ProjectManagerEx.getInstanceEx.findOpenProjectByHash(myPath.projectHash) match
          case null => ""
          case project =>
            val dsl = ChallengeRepository.getInstance(project).getDSLContext
            dsl.selectFrom(SOLUTION_SUBMISSION).where(SOLUTION_SUBMISSION.ID.eq(myPath.submissionId)).fetchOne() match
              case null => ""
              case record =>
                myPath.codeType match
                  case SubmissionCodeFileSystem.CodeType.Local      => record.getLocalcode
                  case SubmissionCodeFileSystem.CodeType.Submission => record.getSubmitcode
        myContent = Some(content)
        content
      case Some(content) => content
  }
}
