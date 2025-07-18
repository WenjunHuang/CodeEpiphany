package com.wenjunhuang.codeepiphany.leetcode.models

import io.circe.derivation.ConfiguredEncoder
import io.circe.{Encoder, JsonObject}

final case class LeetCodeGraphQLRequest(operationName: String, query: String, variables: JsonObject)
    derives ConfiguredEncoder
