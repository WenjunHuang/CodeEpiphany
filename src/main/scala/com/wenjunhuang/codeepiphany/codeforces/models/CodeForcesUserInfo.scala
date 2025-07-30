package com.wenjunhuang.codeepiphany.codeforces.models

import io.circe.derivation.ConfiguredDecoder

case class CodeForcesUserInfo(nickName: String, avatar: String) derives ConfiguredDecoder
