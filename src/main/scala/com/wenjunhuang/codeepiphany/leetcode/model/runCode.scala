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
      compileError: Option[String] = None,
      fullCompileError: Option[String],
      statusRuntime: Option[String],
      memory: Int,
      codeAnswer: List[String],
      codeOutput: List[String],
      stdOutputList: List[String],
      taskFinishTime: Long,
      taskName: String,
      statusMsg: String,
      fastSubmit: Boolean,
      totalCorrect: Option[Int] = none,
      totalTestcases: Option[Int],
      submissionId: String,
      runtimePercentile: Option[Double],
      statusMemory: String,
      memoryPercentile: Option[Double],
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
