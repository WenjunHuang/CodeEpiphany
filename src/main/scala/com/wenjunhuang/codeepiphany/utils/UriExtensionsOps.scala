package com.wenjunhuang.codeepiphany.utils

import org.http4s.Uri

private trait UriExtensionsOps {
  extension (uri: Uri) {
    def addIf(cond: Boolean)(f: Uri => Uri): Uri = if (cond) f(uri) else uri

    def addOpt[A](opt: Option[A])(f: (Uri, A) => Uri): Uri = opt.fold(uri)(a => f(uri, a))
  }

}
