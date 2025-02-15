package com.wenjunhuang.codeepiphany.luogu.models

import io.circe.derivation.ConfiguredDecoder

case class LuoGuUserInfo(nickName: String, avatar: String) derives ConfiguredDecoder
