package com.wenjunhuang.codeepiphany.utils

import cats.effect.Async
import java.io.File
import java.net.URI

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.fileTypes.ex.FileTypeChooser
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.io.URLUtil
import cats.syntax.all.*
import com.wenjunhuang.codeepiphany.utils.syntax.*

object BrowserUtils {
  def browseURI[F[_]: Async](uri: URI, project: Project): F[Unit] = {
    if uri.getScheme == URLUtil.FILE_PROTOCOL then
      Async[F].delay {
        val file = File(uri)
        if !file.exists() || file.isDirectory then throw new IllegalArgumentException(s"Invalid file: $file")
        file
      }.flatMap { file =>
        Async[F].delay {
          val vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
          FileEditorManager.getInstance(project).openFile(vf, false) match
            case null | Array() =>
              FileTypeChooser.associateFileType(vf.getName) match
                case null | FileTypes.UNKNOWN =>
                case _ =>
                  FileEditorManager.getInstance(project).openFile(vf, false)
            case _ =>
        }.void.evalOnEDTAny()
      }
    else
      Async[F].delay {
        BrowserUtil.browse(uri.toURL)
      }
  }

}
