package com.wenjunhuang.codeepiphany.leetcode.model

import cats.syntax.all.*
import io.circe.derivation.{ Configuration, ConfiguredDecoder, ConfiguredEncoder }
import io.circe.Decoder

object submitAnswer {
  // this api use snake_case for member names :}
  given Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults

  case class LeetCodeSubmitAnswerRequest(lang: String, questionId: String, typedCode: String) derives ConfiguredEncoder

  case class LeetCodeSubmitAnswerResponse(submissionId: String) derives ConfiguredDecoder

  enum LeetCodeSubmitAnswerResult {
    case Started(state: String)
    case Success(
      statusCode: Int,
      lang: String,
      runSuccess: Boolean,
      compileError:Option[String]=None,
      fullCompileError: Option[String]=None,
      statusRuntime: String,
      memory: Int,
      displayRuntime: Option[String] = None,
      questionId: String,
      elaspedTime: Option[Long] = None,
      compareResult: String,
      codeOutput: String,
      stdOutput: String,
      lastTestcase: String,
      expectedOutput: String,
      taskFinishTime: Long,
      taskName: String,
      finished: Boolean,
      statusMsg: String,
      state: String,
      fastSubmit: Boolean,
      totalCorrect: Option[Int] = None,
      totalTestcases: Option[Int] = None,
      submissionId: String,
      runtimePercentile: Option[Double],
      statusMemory: String,
      memoryPercentile: Option[Double],
      prettyLang: String,
      inputFormatted: Option[String] = None,
      input: Option[String]= None
    )
  }

  object LeetCodeSubmitAnswerResult {
    private implicit val successDecoder: Decoder[Success] = ConfiguredDecoder.derived[Success]
    private implicit val startedDecoder: Decoder[Started] = ConfiguredDecoder.derived[Started]
    implicit val decoder: Decoder[LeetCodeSubmitAnswerResult] = Decoder.instance { cursor =>
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
