package com.wenjunhuang.codeepiphany.luogu

import io.circe.derivation.Configuration

package object models {
  given Configuration = Configuration.default.withDefaults
}
