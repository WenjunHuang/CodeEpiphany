package com.wenjunhuang.codeepiphany.model

import io.circe.{ Decoder, Encoder }
import monix.newtypes.NewtypeWrapped
import monix.newtypes.integrations.DerivedCirceCodec

object newtypes {

  type ChallengeId = ChallengeId.Type

  object ChallengeId extends NewtypeWrapped[Long] with DerivedCirceCodec

  type CodeDojoChallengeId = CodeDojoChallengeId.Type
  object CodeDojoChallengeId extends NewtypeWrapped[String]

  type ChallengeLanguageId = ChallengeLanguageId.Type
  object ChallengeLanguageId extends NewtypeWrapped[Long]

  type SolutionId = SolutionId.Type
  object SolutionId extends NewtypeWrapped[Long]

  type SubmissionId = SubmissionId.Type
  object SubmissionId extends NewtypeWrapped[Long]

  type FileNameTemplate = FileNameTemplate.Type
  object FileNameTemplate extends NewtypeWrapped[String]

  type CodeTemplate = CodeTemplate.Type
  object CodeTemplate extends NewtypeWrapped[String]
}
