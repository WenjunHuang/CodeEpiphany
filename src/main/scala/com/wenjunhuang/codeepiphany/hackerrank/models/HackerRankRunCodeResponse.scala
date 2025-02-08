package com.wenjunhuang.codeepiphany.hackerrank.models

import io.circe.derivation.ConfiguredDecoder

case class HackerRankRunCodeResponse(
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

case class HackerRankSubmissionResponse(
  id: Long,
  challengeId: Long,
  status: String, // "Processing", "Wrong Answer","Accepted","Compilation error"
  score: String,
  scoreProcessed: Int, // 3 means fully processed and no more query needed
  solved: Int,
  partial: Int,
  compileStatus: Option[Int],
  compileMessage: Option[String],
  testcaseStatus: List[Int],
  testcaseMessage: List[String],
  codecheckerSignal: List[Int],
  codecheckerTime: List[BigDecimal],
  stderr: Option[String]
) derives ConfiguredDecoder
