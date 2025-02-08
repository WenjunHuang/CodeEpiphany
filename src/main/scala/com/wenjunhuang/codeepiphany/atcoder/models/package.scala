package com.wenjunhuang.codeepiphany.atcoder
import io.circe.derivation.Configuration

package object models {
  given Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults
  
}
