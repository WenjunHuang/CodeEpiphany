package com.wenjunhuang.codeepiphany.atcoder.models

import io.circe.{ Decoder, HCursor, Json }
import io.circe.derivation.ConfiguredDecoder

case class AtCoderProblem(id: String, contestId: String,
                          problemIndex:String,
                          name:String,
                          title:String,
                          shortestSubmissionId:Long,
                          shortestContestId:String,
                          shortestUserId:String,
                          fastestSubmissionId:Long,
                          fastestContestId:String,
                          fastestUserId:String,
                          fastSubmissionId:Long,
                          fastContestId:String,
                          firstUserId:String,
                          solverCount:Int) derives ConfiguredDecoder
