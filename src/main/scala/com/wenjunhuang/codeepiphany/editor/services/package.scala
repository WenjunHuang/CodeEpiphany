package com.wenjunhuang.codeepiphany.editor

import cats.syntax.all.*
import cats.effect.{ Async, Concurrent }
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.http.HttpClientKeeper
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings

package object services {
  def runCode[F[_]: Async: Concurrent: HttpClientKeeper](vf: VirtualFile, project: Project): F[Unit] = {
    val settings = ChallengeSettings.getInstance(project)
    settings.findChallengeId(vf) match
      case Some(item) =>
        item.dojo match
          case CodeDojo.HackerRank =>
            console.info[F](project, s"Start to run ${vf.getCanonicalPath}") *> hackerrank.runCode[F](vf, project, item)
          case _ => Async[F].unit
      case None => Async[F].unit
  }

}
