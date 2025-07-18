package com.wenjunhuang.codeepiphany.utils

import org.jetbrains.annotations.Nullable

import java.util.Optional
import scala.jdk.OptionConverters.*

object JavaUtils {
  def toOptional[T](value: Option[T]): Optional[T] = value.toJava
  def toOption[T](@Nullable value: T): Option[T]   = Option(value)
}
