package com.wenjunhuang.codeepiphany.codeforces

import io.circe.derivation.Configuration

package object models {
  given Configuration = Configuration.default.withDefaults
}
