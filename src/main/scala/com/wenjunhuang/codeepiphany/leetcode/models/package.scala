package com.wenjunhuang.codeepiphany.leetcode

import io.circe.derivation.Configuration
import org.typelevel.ci.CIString

import com.wenjunhuang.codeepiphany.model.*

package object models {
  given Configuration = Configuration.default.withDefaults

  extension (codeDojo: CodeDojo) {
    def leetCodeDifficulty(difficulty: ChallengeDifficulty): String =
      difficulty match
        case ChallengeDifficulty.Easy                  => "EASY"
        case ChallengeDifficulty.Medium                => "MEDIUM"
        case ChallengeDifficulty.Hard                  => "HARD"
        case ChallengeDifficulty.Advanced              => "HARD"
        case ChallengeDifficulty.Expert                => "HARD"
        case ChallengeDifficulty.CodeDojoDefined(_, _) => "HARD"

    def fromLeetCodeDifficulty(difficulty: String): ChallengeDifficulty =
      CIString(difficulty) match
        case d if d == CIString("easy")   => ChallengeDifficulty.Easy
        case m if m == CIString("medium") => ChallengeDifficulty.Medium
        case h if h == CIString("hard")   => ChallengeDifficulty.Hard

    def leetCodeStatus(status: ChallengeStatus): String = status match
      case ChallengeStatus.Unsolved => "NOT_STARTED"
      case ChallengeStatus.Solved   => "AC"
      case ChallengeStatus.Tried    => "TRIED"

    def leetCodeStatusForCompanySearch(status:ChallengeStatus):String = status match
      case ChallengeStatus.Unsolved => "TO_DO"
      case ChallengeStatus.Solved   => "SOLVED"
      case ChallengeStatus.Tried    => "ATTEMPTED"

    def fromLeetCodeStatus(status: String): ChallengeStatus = CIString(status) match
      case ns if ns == CIString("NOT_STARTED") || ns == CIString("TO_DO") => ChallengeStatus.Unsolved
      case ac if ac == CIString("AC") || ac == CIString("SOLVED")         => ChallengeStatus.Solved
      case t if t == CIString("TRIED") || t == CIString("NOTAC") || t == CIString("ATTEMPTED") => ChallengeStatus.Tried
      case _ => ChallengeStatus.Unsolved

    def leetCodeOrderDirection(direction: OrderDirection): String = direction match
      case OrderDirection.Ascending  => "ASCENDING"
      case OrderDirection.Descending => "DESCENDING"

    def fromLeetCodeOrderDirection(direction: String): OrderDirection = CIString(direction) match
      case a if a == CIString("ASCENDING")  => OrderDirection.Ascending
      case b if b == CIString("DESCENDING") => OrderDirection.Descending

    def leetCodeLanguage(language: Language, languageVersion: LanguageVersion): String =
      s"${language.value}${languageVersion.version}"

    def fromLeetCodeLanguage(language: String): Option[(Language, LanguageVersion)] = {
      val pattern = """^([a-zA-Z]*)(\d*)$""".r
      language match
        case pattern(lang, ver) =>
          Language.fromCIString(CIString(lang)).map { lang =>
            (lang, LanguageVersion.fromString(ver))
          }
        case _ => None
    }

    def fromLeetCodeRunResult(result: String, correctAnswer: Option[Boolean]): SubmissionResult = CIString(result) match
      case s if s.contains(CIString("Accepted")) =>
        if correctAnswer.contains(false) then SubmissionResult.Failure
        else SubmissionResult.Success
      case c if c.contains(CIString("Compile Error"))       => SubmissionResult.CompilationError
      case r if r.contains(CIString("Runtime Error"))       => SubmissionResult.RuntimeError
      case u if u.contains(CIString("Time Limit Exceeded")) => SubmissionResult.Timeout
      case e if e.contains(CIString("Wrong Answer"))        => SubmissionResult.Failure
      case _                                                => SubmissionResult.Unknown
  }
}
