package com.wenjunhuang.codeepiphany.vfs

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.{ FileTypeManager, LanguageFileType, PlainTextLanguage }
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.vfs.{ DeprecatedVirtualFileSystem, NonPhysicalFileSystem, VirtualFile, VirtualFileManager }
import com.intellij.vcs.editor.ComplexPathVirtualFileSystem
import com.intellij.vcs.editor.ComplexPathVirtualFileSystem.ComplexPathSerializer
import com.wenjunhuang.codeepiphany.model.{ ChallengeRepository, Language, LanguageVersion }
import com.wenjunhuang.codeepiphany.vfs.SubmissionCodeFileSystem.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import com.wenjunhuang.codeepiphany.database.Tables.*
import org.typelevel.ci.CIString

import scala.collection.mutable

class SubmissionCodeFileSystem
    extends ComplexPathVirtualFileSystem[SubmissionCodeFilePath](SubmissionCodeFilePathSerializer) {
  private val myFileCache          = mutable.Map[SubmissionCodeFilePath, VirtualFile]()
  override def getProtocol: String = PROTOCOL

  override def findOrCreateFile(project: Project, p: SubmissionCodeFilePath): VirtualFile = {
    myFileCache.getOrElseUpdate(p, createSubmissionCodeFile(project, p))
  }

  private def createSubmissionCodeFile(project: Project, pathId: SubmissionCodeFilePath): VirtualFile = {
    ChallengeRepository
      .getInstance(project)
      .getDSLContext
      .select(CHALLENGE_LANGUAGE.LANGUAGE, CHALLENGE_LANGUAGE.LANGUAGEVERSION)
      .from(SOLUTION_SUBMISSION)
      .innerJoin(CHALLENGE_LANGUAGE)
      .on(SOLUTION_SUBMISSION.CHALLENGELANGUAGEID.eq(CHALLENGE_LANGUAGE.ID))
      .where(SOLUTION_SUBMISSION.ID.eq(pathId.submissionId))
      .fetchOne() match
      case null => null
      case record =>
        Language.fromCIString(CIString(record.component1())) match
          case None => null
          case Some(lang) =>
            FileTypeManager.getInstance().getFileTypeByExtension(lang.fileExt) match
              case languageFileType: LanguageFileType =>
                SubmissionCodeFile(
                  pathId,
                  lang,
                  LanguageVersion.fromString(record.component2()),
                  languageFileType.getLanguage
                )
              case _ =>
                SubmissionCodeFile(
                  pathId,
                  lang,
                  LanguageVersion.fromString(record.component2()),
                  PlainTextLanguage.INSTANCE
                )
  }

}

object SubmissionCodeFileSystem {
  final val PROTOCOL  = "cesc"
  final val SEPARATOR = "/"

  def isValid(path: String): Boolean = path.contains(SEPARATOR)

  def getInstance(): SubmissionCodeFileSystem =
    VirtualFileManager.getInstance().getFileSystem(PROTOCOL).asInstanceOf[SubmissionCodeFileSystem]

  enum CodeType {
    case Local
    case Submission
  }

  case class SubmissionCodeFilePath(submissionId: Int, projectHash: String, codeType: CodeType)
      extends ComplexPathVirtualFileSystem.ComplexPath {
    override def getProjectHash: String = projectHash

    override def getSessionId: String = submissionId.toString
  }

  object SubmissionCodeFilePath {
    def apply(submissionId: Int, project: Project, codeType: CodeType): SubmissionCodeFilePath = {
      SubmissionCodeFilePath(submissionId, project.getLocationHash, codeType)
    }
  }

  object SubmissionCodeFilePathSerializer extends ComplexPathSerializer[SubmissionCodeFilePath] {
    override def deserialize(s: String): SubmissionCodeFilePath = {
      decode[SubmissionCodeFilePath](s) match {
        case Right(value) => value
        case Left(error)  => throw new IllegalArgumentException(s"Cannot deserialize $s: $error")
      }
    }

    override def serialize(p: SubmissionCodeFilePath): String = {
      p.asJson.noSpaces
    }
  }
}
