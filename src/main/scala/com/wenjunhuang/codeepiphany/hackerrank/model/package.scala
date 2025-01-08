package com.wenjunhuang.codeepiphany.hackerrank

import cats.syntax.all.*
import cats.Show
import com.intellij.openapi.util.text.StringUtil
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.model.{ CodeDojo, Constants, Language, LanguageVersion }
import com.wenjunhuang.codeepiphany.utils.Colors
import io.circe.{ Decoder, HCursor, Json }
import io.circe.derivation.{ Configuration, ConfiguredDecoder }
import monocle.Lens
import monocle.macros.GenLens
import org.typelevel.ci.CIString

import scala.annotation.unused
import scala.beans.BeanProperty

package object model {
  enum ChallengeStatus(val value: String) {
    case Solved   extends ChallengeStatus("solved")
    case Unsolved extends ChallengeStatus("unsolved")
  }

  object ChallengeStatus {
    implicit val showInstance: Show[ChallengeStatus] = Show.show[ChallengeStatus](skill =>
      PluginBundle.message(s"hackerrank.model.question.status.${StringUtil.decapitalize(skill.toString)}")
    )
  }

  enum ChallengeSkill(val value: String) {
    case Intermediate extends ChallengeSkill("Problem Solving (Intermediate)")
    case Advanced     extends ChallengeSkill("Problem Solving (Advanced)")
    case Basic        extends ChallengeSkill("Problem Solving (Basic)")
  }

  object ChallengeSkill {
    implicit val showInstance: Show[ChallengeSkill] = Show.show[ChallengeSkill](skill =>
      PluginBundle.message(s"hackerrank.model.question.skill.${StringUtil.decapitalize(skill.toString)}")
    )

    def fromCIString(str: CIString): Option[ChallengeSkill] =
      if str == CIString(Intermediate.value) then Some(Intermediate)
      else if str == CIString(Advanced.value) then Some(Advanced)
      else if str == CIString(Basic.value) then Some(Basic)
      else None
  }

  private given hackerRankConfig: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults

  case class UserInfo(username: String, name: String, avatar: String) derives ConfiguredDecoder
  final val EMPTY_USERINFO: UserInfo = UserInfo("", "", "")

  enum Contest(val slug: String) {
    case Master       extends Contest("master")
    case ProjectEuler extends Contest("projecteuler")
  }

  object Contest {
    def fromCIString(cis: CIString): Option[Contest] =
      if cis == CIString(Master.slug) then Some(Master)
      else if cis == CIString(ProjectEuler.slug) then Some(ProjectEuler)
      else None
  }

  case class ChallengeDomain(
    id: Int,
    name: String,
    slug: String,
    contest: Contest,
    subDomains: List[ChallengeSubdomain]
  )

  // constant for project euler contest
  // project euler contest seems to be a special case, so we need to define it here
  final val PROJECT_EULER_DOMAIN: ChallengeDomain =
    ChallengeDomain(99, "Project Euler", "projecteuler", Contest.ProjectEuler, List.empty)

  case class ChallengeSubdomain(name: String, slug: String) derives ConfiguredDecoder

  case class ChallengeSearchByKeyWord(
    contestName: String,
    contestSlug: String,
    challengeId: Int,
    challengeName: String,
    challengeSlug: String,
    name: String
  ) derives ConfiguredDecoder

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

  case class ChallengeContent(
    detail: ChallengeDetail,
    codeTemplates: Map[(Language, LanguageVersion), LanguageTemplate]
  )

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

        def updateTemplates(
          langStr: String,
          verStr: String,
          value: String,
          lens: Lens[LanguageTemplate, String]
        ): Unit =
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

  case class RunCodeResponse(
    id: Int,
    status: Int, // 0: progressing; 1: done
    challengeId: Int,
    stdinUrl: List[String] = Nil,
    expectedOutputUrl: List[String] = Nil,
    stdin: List[String] = Nil,
    stdout: List[String] = Nil,
    stderr: List[String] = Nil,
    signal: List[Int] = Nil,
    testcaseMessage: List[String] = Nil,
    testcaseStatus: List[Int] = Nil,
    memory: List[Int] = Nil,
    time: List[Double] = Nil,
    timeLimit: Option[Int] = None,
    result: Option[Int] = None,
    expectedOutput: List[String] = Nil,
    compileCommand: Option[String] = None,
    compilemessage: Option[String] = None,
    errorCode: Option[Int] = None
  ) derives ConfiguredDecoder

}
