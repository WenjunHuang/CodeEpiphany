package com.wenjunhuang.codeepiphany.services
import cats.effect.{Resource, Sync}
import cats.effect.kernel.Async
import cats.syntax.all.*
import java.io.{File, PrintWriter}
import org.typelevel.log4cats.LoggerFactory

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.{FileDocumentManager, FileEditorManager, OpenFileDescriptor}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.{InputValidator, Messages}
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.{LocalFileSystem, VirtualFile}

import com.wenjunhuang.codeepiphany.utils.implicits.*

object file {
  def saveTextWithConflictResolution[F[_]: Async](file: File, content: String): F[Option[File]] = {
    Async[F].delay {
      if file.exists() then
        // show a rename dialog
        val extWithDot = FileUtilRt.getExtension(file.getName) match
          case "" => ""
          case e  => s".$e"

        val nameWithoutExt = FileUtilRt.getNameWithoutExtension(file.getName)
        Option(
          Messages.showInputDialog(
            "File already exists. Please enter a new name:",
            "File Already Exists",
            Messages.getQuestionIcon,
            nameWithoutExt,
            new InputValidator {
              override def checkInput(inputString: String): Boolean =
                !File(file.getParentFile, s"${inputString}${extWithDot}").exists()

              override def canClose(inputString: String): Boolean = true
            }
          )
        ).map { newName => File(file.getParentFile, s"${newName}${extWithDot}") }
      else Some(file)
    }.evalOnEDTAny()
     .flatMap {
      case None          => Async[F].pure(None)
      case Some(newFile) => saveTextToFile(newFile, content).map(Some(_))
    }
  }

  def saveTextToFile[F[_]: Sync](file: File, content: String): F[File] = {
    Resource.make(Sync[F].blocking(PrintWriter(file)))(writer => Sync[F].blocking(writer.close())).use { writer =>
      Sync[F].blocking {
        writer.write(content)
        writer.flush()
        file
      }
    }
  }

  def refreshAndFindFileByIoFile[F[_]: Sync](file: File): F[VirtualFile] =
    Sync[F].blocking {
      val vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
      if vf == null then throw new IllegalArgumentException(s"Cannot find file: ${file}")
      else vf
    }

  def openTextEditor[F[_]: Async](vf: VirtualFile, project: Project): F[Editor] =
    Async[F].delay {
      val descriptor = OpenFileDescriptor(project, vf)
      FileEditorManager.getInstance(project).openTextEditor(descriptor, false)
    }.evalOnEDTAny()

  def saveEditedFile[F[_]: Async: LoggerFactory](file: VirtualFile): F[Either[Throwable, Unit]] =
    Async[F]
      .delay(FileDocumentManager.getInstance().isFileModified(file))
      .flatMap { isModified =>
        if isModified then
          Async[F].delay {
            val fdm = FileDocumentManager.getInstance()
            fdm.saveDocument(fdm.getDocument(file))
          }.evalOnEDTWithWrite().attempt
        else Async[F].pure(Right(()))
      }

}
