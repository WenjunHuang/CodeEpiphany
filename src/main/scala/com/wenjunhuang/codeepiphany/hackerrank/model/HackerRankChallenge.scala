package com.wenjunhuang.codeepiphany.hackerrank.model

import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion}
import io.circe.{Decoder, HCursor, Json}
import io.circe.derivation.ConfiguredDecoder
import monocle.Lens
import monocle.macros.GenLens
import org.typelevel.ci.CIString

case class ChallengeDetail(
  id: Int,
  slug: String,
  name: String,
  bookmarked: Option[Boolean],
  solved: Option[Boolean],
  attempted: Option[Boolean],
  contestSlug: String,
  userScore: Double,
  preview: Option[String],
  bodyHtml: Option[String],
  difficulty: Double,
  difficultyName: String,
  solvedScore: Double,
  skill: Option[String],
  successRatio: Double,
  totalCount: Int,
  solvedCount: Int,
  maxScore: Int
) derives ConfiguredDecoder

case class LanguageTemplate(header: String, template: String, tail: String)

case class ChallengeContent(detail: ChallengeDetail, codeTemplates: Map[(Language, LanguageVersion), LanguageTemplate])

object ChallengeContent {
  implicit val decoder: Decoder[ChallengeContent] = (c: HCursor) =>
    for {
      detail <- c.as[ChallengeDetail]
      codes  <- c.as[Map[String, Json]]
    } yield {
      var templates    = Map.empty[(Language, LanguageVersion), LanguageTemplate]
      val templateLens = GenLens[LanguageTemplate](_.template)
      val headerLens   = GenLens[LanguageTemplate](_.header)
      val tailLens     = GenLens[LanguageTemplate](_.tail)

      def updateTemplates(langStr: String, verStr: String, value: String, lens: Lens[LanguageTemplate, String]): Unit =
        Language.fromCIString(CIString(langStr)).foreach { lang =>
          val ver =
            if verStr.isEmpty then LanguageVersion.AnyVersion
            else LanguageVersion.SpecificVersion(verStr)
          templates = templates.updatedWith((lang, ver)) {
            case Some(t) => Some(lens.modify(_ => value)(t))
            case None    => Some(lens.modify(_ => value)(LanguageTemplate("", "", "")))
          }
        }

      codes.foreach { case (prop, json) =>
        val templatePattern     = """^([a-zA-Z]*)(\d*)_template$""".r
        val templateHeadPattern = """^([a-zA-Z]*)(\d*)_template_head$""".r
        val templateTailPattern = """^([a-zA-Z]*)(\d*)_template_tail$""".r
        val value               = json.as[String].getOrElse("")

        prop match {
          case templatePattern(lang, ver) =>
            updateTemplates(lang, ver, value, templateLens)
          case templateHeadPattern(lang, ver) =>
            updateTemplates(lang, ver, value, headerLens)
          case templateTailPattern(lang, ver) =>
            updateTemplates(lang, ver, value, tailLens)
          case _ => ()
        }
      }
      ChallengeContent(detail, templates)
    }
}
