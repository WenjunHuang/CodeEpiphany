package com.wenjunhuang.codeepiphany.utils
import cats.Show
import io.circe.{Decoder, Encoder}

import com.wenjunhuang.codeepiphany.PluginBundle

case class Pagination(currentPage: Int = 1, pageSize: PageSize = PageSize.Twenty, totalSize: Int = 0) {
  require(currentPage > 0, "Current page must be greater than 0")
 
  def resetToFirstPage: Pagination = this.copy(currentPage = 1)

  def totalPages: Int =
    math.max(1, math.ceil(totalSize.toDouble / pageSize.value).toInt)
}

enum PageSize(val value: Int) {
  case Twenty     extends PageSize(20)
  case Fifty      extends PageSize(50)
  case OneHundred extends PageSize(100)
}

object PageSize {
  implicit val circeEncoder: Encoder[PageSize] = Encoder.encodeInt.contramap[PageSize](_.value)
  implicit val circeDecoder: Decoder[PageSize] = Decoder.decodeInt.emap(value => fromInt(value).toRight("Unknown page size value"))
  
  implicit val showInstance: Show[PageSize] = Show.show[PageSize] {
    case Twenty     => PluginBundle.message("pagesize.20")
    case Fifty      => PluginBundle.message("pagesize.50")
    case OneHundred => PluginBundle.message("pagesize.100")
  }

  def fromInt(value: Int): Option[PageSize] =
    value match
      case Twenty.value     => Some(Twenty)
      case Fifty.value      => Some(Fifty)
      case OneHundred.value => Some(OneHundred)
      case _                => None
}
