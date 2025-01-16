package com.wenjunhuang.codeepiphany.utils

import java.util as ju
import org.cef.network.CefRequest
import org.http4s.Headers
import scala.jdk.CollectionConverters.*

private trait CefExtensionsOps {
  extension (cefRequest: CefRequest) {
    def headers: Headers = {
      val jMap = ju.HashMap[String, String]()
      cefRequest.getHeaderMap(jMap)
      jMap.asScala.foldLeft(Headers.empty) { case (headers, (k, v)) => headers.put(k -> v) }
    }
  }
}
