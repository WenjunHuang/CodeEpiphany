package com.wenjunhuang.codeepiphany.model

import org.jooq.{ Field, SortField }

enum OrderFilter {
  case Ascending, Descending

  def toJooqSortField[T](field: Field[T]): SortField[T] = this match
    case Ascending  => field.asc()
    case Descending => field.desc()
}
