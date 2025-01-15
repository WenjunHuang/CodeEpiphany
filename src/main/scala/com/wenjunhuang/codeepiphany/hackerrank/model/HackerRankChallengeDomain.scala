package com.wenjunhuang.codeepiphany.hackerrank.model

import io.circe.derivation.ConfiguredDecoder

case class HackerRankChallengeDomain(
                                      id: Int,
                                      name: String,
                                      slug: String,
                                      contest: HackerRankContest,
                                      subDomains: List[HackerRankChallengeSubdomain]
)

case class HackerRankChallengeSubdomain(name: String, slug: String) derives ConfiguredDecoder
