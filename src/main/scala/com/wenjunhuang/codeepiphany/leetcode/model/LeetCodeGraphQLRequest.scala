package com.wenjunhuang.codeepiphany.leetcode.model

import io.circe.derivation.ConfiguredEncoder
import io.circe.{Encoder, Json, JsonObject}

final case class LeetCodeGraphQLRequest(operationName: String, query: String, variables: JsonObject)
    derives ConfiguredEncoder
