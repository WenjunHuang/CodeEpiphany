package com.wenjunhuang.codeepiphany.atcoder.models

import io.circe.derivation.ConfiguredDecoder

case class AtCoderUserInfo(nickName: String, avatar: String) derives ConfiguredDecoder
