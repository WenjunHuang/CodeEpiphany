package com.wenjunhuang.codeepiphany.hackerrank.models

import io.circe.derivation.{ConfiguredCodec, ConfiguredDecoder}

case class HackerRankChallengeDomain(id: Int,
                                      name: String,
                                      slug: String,
                                      contest: HackerRankContest,
                                      subDomains: List[HackerRankChallengeSubdomain]) derives ConfiguredCodec

case class HackerRankChallengeSubdomain(name: String, slug: String) derives ConfiguredCodec
