package com.wenjunhuang.codeepiphany.vfs

import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import scala.collection.mutable

import com.intellij.openapi.fileTypes.{FileTypeManager, FileTypes, LanguageFileType}
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.{VirtualFile, VirtualFileManager}
import com.intellij.vcs.editor.ComplexPathVirtualFileSystem

import com.wenjunhuang.codeepiphany.model.ChallengeRepository.ChallengeId
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.vfs.SolutionRemarkFileSystem.*

class SolutionRemarkFileSystem
    extends ComplexPathVirtualFileSystem[SolutionRemarkFilePath](SolutionRemarkFilePathSerializer) {
  private val myFileCache = mutable.Map[Long, VirtualFile]()

  override def getProtocol: String = SolutionRemarkFileSystem.PROTOCOL

  override def findOrCreateFile(project: Project, p: SolutionRemarkFilePath): VirtualFile = {
    myFileCache.getOrElseUpdate(p.solutionId, createSolutionRemarkFile(project, p))
  }

  private def createSolutionRemarkFile(
    project: Project,
    path: SolutionRemarkFileSystem.SolutionRemarkFilePath
  ): VirtualFile = {
    FileTypeManager.getInstance().getFileTypeByExtension("md") match
      case languageFileType: LanguageFileType =>
        SolutionRemarkFile(path, languageFileType)
      case _ => SolutionRemarkFile(path, FileTypes.PLAIN_TEXT)
  }
}

object SolutionRemarkFileSystem {
  final val PROTOCOL  = "solution-remark"
  final val SEPARATOR = "/"

  def getInstance(): SolutionRemarkFileSystem = {
    VirtualFileManager.getInstance().getFileSystem(PROTOCOL).asInstanceOf[SolutionRemarkFileSystem]
  }

  case class SolutionRemarkFilePath(solutionId: Long, challengeId: ChallengeId, codeDojo: CodeDojo, projectHash: String)
      extends ComplexPathVirtualFileSystem.ComplexPath {
    override def getProjectHash: String = projectHash

    override def getSessionId: String = solutionId.toString
  }

  object SolutionRemarkFilePath {
    def apply(
      solutionId: Long,
      challengeId: ChallengeId,
      codeDojo: CodeDojo,
      project: Project
    ): SolutionRemarkFilePath = {
      SolutionRemarkFilePath(solutionId, challengeId, codeDojo, project.getLocationHash)
    }
  }

  object SolutionRemarkFilePathSerializer
      extends ComplexPathVirtualFileSystem.ComplexPathSerializer[SolutionRemarkFilePath] {
    override def serialize(path: SolutionRemarkFilePath): String = {
      path.asJson.noSpaces
    }

    override def deserialize(path: String): SolutionRemarkFilePath = {
      decode[SolutionRemarkFilePath](path) match
        case Right(value) => value
        case Left(error) =>
          throw new IllegalArgumentException(s"Cannot deserialize $path to SolutionRemarkFilePath", error)
    }
  }
}
