package com.wenjunhuang.codeepiphany.hackerrank.services

import cats.effect.{ Async, Concurrent }
import cats.syntax.all.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.wenjunhuang.codeepiphany.hackerrank.HackerRankApi
import com.wenjunhuang.codeepiphany.hackerrank.model.Contest
import com.wenjunhuang.codeepiphany.model.{ Language, LanguageVersion }
import com.wenjunhuang.codeepiphany.services.editor.*
import com.wenjunhuang.codeepiphany.services.http.HttpClientKeeper
import com.wenjunhuang.codeepiphany.utils.implicits.*
import org.typelevel.log4cats.LoggerFactory

import java.io.File

object editor {
  def openChallenge[F[_]: Async: Concurrent: HttpClientKeeper: LoggerFactory](project: Project, challengeSlug: String, contest: Contest, language: Language, version: LanguageVersion): F[Unit] = {
    val api = HackerRankApi[F]()
    api
      .getChallengeContent(challengeSlug, contest)
      .map {
        case Some(content) =>
          content.codeTemplates.find { case ((lang, ver), _) => lang == language && ver == version }.map { case (_, template) =>
            s"""
               |/** begin header */
               |${template.header}
               |/** end header */
               |
               |/** begin template */
               |${template.template}
               |/** end template */
               |
               |/** begin tail */
               |${template.tail}
               |/** end tail */
               |""".stripMargin
          }
        case None => None
      }
      .flatMap {
        case None => Async[F].delay(Messages.showInfoMessage("Failed to open challenge", "Error")).evalOnUI()
        case Some(code) =>
          val dir      = File(project.getBasePath)
          val tempFile = File(dir, s"temp.${language.fileExt}")
          saveTextToFileAndRefresh(tempFile, code).flatMap(vf => openTextEditor(vf, project)).void
      }
  }

}
