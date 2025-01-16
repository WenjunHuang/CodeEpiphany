package com.wenjunhuang.codeepiphany.utils

import com.intellij.util.ui.ColumnInfo

import com.wenjunhuang.codeepiphany.model.OrderDirection
import com.wenjunhuang.codeepiphany.model.OrderDirection.*

abstract class OrderByColumnInfo[Item, Aspect](name: String) extends ColumnInfo[Item, Aspect](name) {
  def enableOrderBy: Boolean = false

  def getOrderFilter: Option[OrderDirection] = None

  def setOrderFilter(filter: Option[OrderDirection]): Unit = {}
}

object OrderByColumnInfo {
  def nextOrderFilter(filter: Option[OrderDirection]): Option[OrderDirection] = filter match
    case None             => Some(Ascending)
    case Some(Ascending)  => Some(Descending)
    case Some(Descending) => None
}
