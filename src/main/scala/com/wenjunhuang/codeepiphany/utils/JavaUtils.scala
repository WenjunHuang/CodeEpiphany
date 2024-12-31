package com.wenjunhuang.codeepiphany.utils

import java.util.Optional
import scala.jdk.OptionConverters.*

object JavaUtils {
  def toOptional[T](value:Option[T]):Optional[T] = value.toJava
}
