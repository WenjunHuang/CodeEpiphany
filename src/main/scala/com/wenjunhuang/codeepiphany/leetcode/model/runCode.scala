package com.wenjunhuang.codeepiphany.leetcode.model

import cats.syntax.all.*
import io.circe.derivation.{Configuration, ConfiguredDecoder, ConfiguredEncoder}
import io.circe.Decoder

object runCode {
  // this api use snake_case for member names :}
  given Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults

  case class LeetCodeRunRequest(lang: String, dataInput: String, questionId: String, typedCode: String)
      derives ConfiguredEncoder

  case class LeetCodeRunResponse(interpretId: String, testCase: String, interpretExpectedId: String)
      derives ConfiguredDecoder

  enum LeetCodeRunResult {
    case Started(state: String)
    case Success(
      state: String,
      statusCode: Int,
      runSuccess: Boolean,
      correctAnswer: Option[Boolean] = None,
      compareResult: Option[String] = None,
      compileError: Option[String] = None,
      fullCompileError: Option[String],
      runtimeError: Option[String] = None,
      fullRuntimeError: Option[String] = None,
      statusRuntime: Option[String] = None,
      memory: Option[Int] = None,
      codeAnswer: List[String] = Nil,
      expectedCodeAnswer: List[String] = Nil,
      codeOutput: List[String] = Nil,
      expectedCodeOutput: List[String] = Nil,
      stdOutputList: List[String] = Nil,
      expectedStdOutputList: List[String] = Nil,
      taskFinishTime: Long,
      taskName: String,
      statusMsg: String,
      fastSubmit: Boolean,
      totalCorrect: Option[Int] = none,
      totalTestcases: Option[Int] = None,
      submissionId: String,
      runtimePercentile: Option[Double] = None,
      statusMemory: String,
      memoryPercentile: Option[Double] = None,
      prettyLang: String
    )
  }

  object LeetCodeRunResult {
    private implicit val successDecoder: Decoder[Success] = ConfiguredDecoder.derived[Success]
    private implicit val startedDecoder: Decoder[Started] = ConfiguredDecoder.derived[Started]
    implicit val decoder: Decoder[LeetCodeRunResult] = Decoder.instance { cursor =>
      for {
        discriminator <- cursor.downField("state").as[String]
        result <- discriminator match {
          case "SUCCESS" => cursor.as[Success].widen
          case "STARTED" => cursor.as[Started].widen
          case _         => Left(io.circe.DecodingFailure(s"Unknown state: $discriminator", cursor.history))
        }
      } yield result
    }
  }
}
