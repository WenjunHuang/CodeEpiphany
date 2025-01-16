package com.wenjunhuang.codeepiphany.leetcode.model

import io.circe.{Encoder, JsonObject}
import io.circe.derivation.ConfiguredEncoder

final case class LeetCodeGraphQLRequest(operationName: String, query: String, variables: JsonObject)
    derives ConfiguredEncoder
