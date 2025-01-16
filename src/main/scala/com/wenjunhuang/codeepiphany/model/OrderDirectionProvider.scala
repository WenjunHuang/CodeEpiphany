package com.wenjunhuang.codeepiphany.model

trait OrderDirectionProvider[T] {
  def getDirectionOf(v: T): Option[OrderDirection]
  def setDirectionOf(v: T, direction: Option[OrderDirection]): Unit
}
