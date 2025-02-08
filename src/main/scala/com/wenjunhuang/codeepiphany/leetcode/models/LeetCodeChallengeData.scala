package com.wenjunhuang.codeepiphany.leetcode.models

import io.circe.derivation.ConfiguredDecoder

case class LeetCodeChallengeData(
  questionId: String,
  frontendQuestionId: String,
  title: String,
  titleSlug: String,
  content: String,
  translatedTitle: Option[String] = None,
  translatedContent: Option[String] = None,
  isPaidOnly: Boolean,
  difficulty: String,
  likes: Int,
  dislikes: Int,
  isLiked: Option[Boolean] = None,
  similarQuestions: String,
  exampleTestcases: String,
  topicTags: List[LeetCodeQuestionTopicTag],
  codeSnippets: List[LeetCodeQuestionCodeSnippet],
  hints: List[String],
  status: Option[String] = None,
  testCase: String,
  metaData: String
) derives ConfiguredDecoder

case class LeetCodeQuestionSimilarQuestion(title: String, titleSlug: String, translatedTitle: Option[String] = None)
    derives ConfiguredDecoder
case class LeetCodeQuestionTopicTag(name: String, slug: String, translatedName: Option[String] = None)
    derives ConfiguredDecoder

case class LeetCodeQuestionCodeSnippet(lang: String, langSlug: String, code: String) derives ConfiguredDecoder

case class LeetCodeQuestionSolution(id: String, canSeeDetail: Boolean) derives ConfiguredDecoder

case class LeetCodeQuestionCodeMetaData(
  name: String,
  params: List[LeetCodeQuestionCodeParam],
  `return`: LeetCodeQuestionCodeReturn,
  manual: Boolean
) derives ConfiguredDecoder
case class LeetCodeQuestionCodeParam(name: String, `type`: String) derives ConfiguredDecoder
case class LeetCodeQuestionCodeReturn(`type`: String, size: String) derives ConfiguredDecoder
