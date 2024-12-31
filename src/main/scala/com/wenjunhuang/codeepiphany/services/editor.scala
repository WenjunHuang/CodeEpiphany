package com.wenjunhuang.codeepiphany.services
import cats.effect.{Resource, Sync}
import cats.effect.kernel.Async
import cats.effect.syntax.all.*
import cats.syntax.all.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.{FileDocumentManager, FileEditorManager, OpenFileDescriptor}
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.{LocalFileSystem, VirtualFile}
import com.wenjunhuang.codeepiphany.utils.implicits.*
import org.typelevel.log4cats.LoggerFactory

import java.io.{File, PrintWriter}

object editor {
  def saveTextToFileAndRefresh[F[_]: Sync](file: File, content: String): F[VirtualFile] =
    Resource.make(Sync[F].blocking(PrintWriter(file)))(writer => Sync[F].blocking(writer.close())).use { writer =>
      Sync[F].blocking {
        writer.write(content)
        writer.flush()
      }
    } *> Sync[F].blocking(LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file))

  def openTextEditor[F[_]: Async](vf: VirtualFile, project: Project): F[Editor] =
    Async[F].delay {
      val descriptor = OpenFileDescriptor(project, vf)
      FileEditorManager.getInstance(project).openTextEditor(descriptor, false)
    }.evalOnUI()

  def saveEditedFile[F[_]: Async: LoggerFactory](file: VirtualFile): F[Either[Throwable, Unit]] =
    Async[F]
      .delay(FileDocumentManager.getInstance().isFileModified(file))
      .flatMap { isModified =>
        if isModified then
          Async[F].delay {
            val fdm = FileDocumentManager.getInstance()
            fdm.saveDocument(fdm.getDocument(file))
          }.evalOn(intellijWriteThreadContext).attempt
        else Async[F].pure(Right(()))
      }

}
