package com.wenjunhuang.codeepiphany.luogu.models

import org.http4s.Uri

import com.wenjunhuang.codeepiphany.model.OrderDirection
import com.wenjunhuang.codeepiphany.model.OrderDirection.Ascending

enum LuoGuSearchOrderBy {
  case PID
  case Title
  case Difficulty

  def createOrderBy(uri: Uri, direction: OrderDirection): Uri = this match {
    case PID        => orderDirection(uri.withQueryParam("orderBy", "pid"), direction)
    case Title      => orderDirection(uri.withQueryParam("orderBy", "name"), direction)
    case Difficulty => orderDirection(uri.withQueryParam("orderBy", "difficulty"), direction)
  }

  def orderDirection(uri: Uri, direction: OrderDirection): Uri = direction match
    case Ascending                 => uri.withQueryParam("order", "asc")
    case OrderDirection.Descending => uri.withQueryParam("order", "desc")
}
