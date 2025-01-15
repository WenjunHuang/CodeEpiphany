package com.wenjunhuang.codeepiphany.hackerrank.model

import io.circe.derivation.ConfiguredDecoder

case class ChallengeDomain(id: Int, name: String, slug: String, contest: Contest, subDomains: List[ChallengeSubdomain])

case class ChallengeSubdomain(name: String, slug: String)derives ConfiguredDecoder
