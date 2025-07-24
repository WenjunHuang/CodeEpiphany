package com.wenjunhuang.codeepiphany.services
import cats.effect.{IO, Resource}
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.{FileDocumentManager, FileEditorManager, OpenFileDescriptor}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.{InputValidator, Messages}
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.{LocalFileSystem, VirtualFile}
import com.wenjunhuang.codeepiphany.utils.syntax.*

import java.io.{File, PrintWriter}

object file {
  def saveTextWithConflictResolution(file: File, content: String): IO[Option[File]] = {
    IO.delay {
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
    }.evalOnEDTAny().flatMap {
      case None          => IO.pure(None)
      case Some(newFile) => saveTextToFile(newFile, content).map(Some(_))
    }
  }

  private def saveTextToFile(file: File, content: String): IO[File] = {
    Resource.make(IO.blocking(PrintWriter(file)))(writer => IO.blocking(writer.close())).use { writer =>
      IO.blocking {
        writer.write(content)
        writer.flush()
        file
      }
    }
  }

  def refreshAndFindFileByIoFile(file: File): IO[VirtualFile] =
    IO.blocking {
      val vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
      if vf == null then throw new IllegalArgumentException(s"Cannot find file: ${file}")
      else vf
    }

  def openTextEditor(vf: VirtualFile, project: Project): IO[Editor] =
    IO.delay {
      val descriptor = OpenFileDescriptor(project, vf)
      FileEditorManager.getInstance(project).openTextEditor(descriptor, false)
    }.evalOnEDTWithWrite()

  def saveEditedFile(file: VirtualFile): IO[Either[Throwable, Unit]] =
    IO
      .delay(FileDocumentManager.getInstance().isFileModified(file))
      .flatMap { isModified =>
        if isModified then
          IO.delay {
            val fdm = FileDocumentManager.getInstance()
            fdm.saveDocument(fdm.getDocument(file))
          }.evalOnEDTWithWrite().attempt
        else IO.pure(Right(()))
      }

}
