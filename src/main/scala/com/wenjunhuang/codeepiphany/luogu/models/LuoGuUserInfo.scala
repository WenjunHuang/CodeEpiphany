package com.wenjunhuang.codeepiphany.luogu.models

import io.circe.derivation.ConfiguredDecoder

case class LuoGuUserInfo(uid:String,nickName: String, avatar: String) derives ConfiguredDecoder
