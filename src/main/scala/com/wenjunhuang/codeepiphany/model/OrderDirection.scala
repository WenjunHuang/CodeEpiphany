package com.wenjunhuang.codeepiphany.model

import org.jooq.{Field, SortField}

enum OrderDirection {
  case Ascending
  case Descending

  def toJooqSortField[T](field: Field[T]): SortField[T] = this match
    case Ascending  => field.asc()
    case Descending => field.desc()
}
