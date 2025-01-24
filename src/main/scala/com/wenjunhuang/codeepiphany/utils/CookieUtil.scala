package com.wenjunhuang.codeepiphany.utils

import java.net.HttpCookie

object CookieUtil {
  def parseCookies(cookie: String): List[HttpCookie] =
    cookie
      .split(";")
      .map(_.trim)
      .collect {
        case cookie if cookie.contains("=") =>
          val Array(name, value) = cookie.trim.split("=", 2)
          HttpCookie(name, value)
      }.toList
    
  def encodeCookies(cookies:List[HttpCookie]):String =
    cookies.map(cookie => s"${cookie.getName}=${cookie.getValue}").mkString(";")
}
