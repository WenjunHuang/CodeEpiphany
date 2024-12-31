package com.wenjunhuang.codeepiphany.utils

trait KotlinCompOps {
  implicit val any2KtUnit: Conversion[Any, kotlin.Unit] = _ => kotlin.Unit.INSTANCE
}
