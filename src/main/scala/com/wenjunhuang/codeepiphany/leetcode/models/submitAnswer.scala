package com.wenjunhuang.codeepiphany.leetcode.models

import cats.syntax.all.*
import io.circe.Decoder
import io.circe.derivation.{Configuration, ConfiguredDecoder, ConfiguredEncoder}

object submitAnswer {
  // this api use snake_case for member names :}
  given Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults

  case class LeetCodeSubmitAnswerRequest(lang: String,
                                         questionId: String,
                                         typedCode: String) derives ConfiguredEncoder

  case class LeetCodeSubmitAnswerResponse(submissionId: Int) derives ConfiguredDecoder

  enum LeetCodeSubmitAnswerResult {
    case Started(
      @transient
      leetCodeSubmissionId: String = "",
      state: String
    )
    case Pending(
      @transient
      leetCodeSubmissionId: String = "",
      state: String
    )
    case Success(
      statusCode: Int,
      lang: String,
      runSuccess: Boolean,
      compileError: Option[String] = None,
      fullCompileError: Option[String] = None,
      runtimeError: Option[String] = None,
      fullRuntimeError: Option[String] = None,
      statusRuntime: String,
      memory: Int,
      displayRuntime: Option[String] = None,
      questionId: String,
      elaspedTime: Option[Long] = None,
      compareResult: Option[String] = None,
      codeOutput: Option[String] = None,
      stdOutput: Option[String] = None,
      lastTestcase: Option[String] = None,
      expectedOutput: Option[String] = None,
      taskFinishTime: Long,
      taskName: String,
      finished: Boolean,
      statusMsg: String,
      state: String,
      fastSubmit: Option[Boolean] = None,
      totalCorrect: Option[Int] = None,
      totalTestcases: Option[Int] = None,
      submissionId: String,
      runtimePercentile: Option[Float],
      statusMemory: String,
      memoryPercentile: Option[Float],
      prettyLang: String,
      inputFormatted: Option[String] = None,
      input: Option[String] = None
    )
  }

  object LeetCodeSubmitAnswerResult {
    private implicit val successDecoder: Decoder[Success] = ConfiguredDecoder.derived[Success]
    private implicit val startedDecoder: Decoder[Started] = ConfiguredDecoder.derived[Started]
    private implicit val pendingDecoder: Decoder[Pending] = ConfiguredDecoder.derived[Pending]
    implicit val decoder: Decoder[LeetCodeSubmitAnswerResult] = Decoder.instance { cursor =>
      for {
        discriminator <- cursor.downField("state").as[String]
        result <- discriminator match {
          case "SUCCESS" => cursor.as[Success].widen
          case "STARTED" => cursor.as[Started].widen
          case "PENDING" => cursor.as[Pending].widen
          case _         => Left(io.circe.DecodingFailure(s"Unknown state: $discriminator", cursor.history))
        }
      } yield result
    }
  }
}
