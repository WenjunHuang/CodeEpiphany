package com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode

import cats.effect.IO

import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.WebViewStyleProvider
import com.wenjunhuang.codeepiphany.utils.jcef.BaseJCefWebView
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode.ArticleJCefView.*
import com.wenjunhuang.codeepiphany.utils.syntax.*
import java.nio.charset.StandardCharsets
import java.util.Base64
import org.cef.browser.CefFrame
import org.cef.network.CefRequest
import org.http4s.{ Headers, MediaType }
import org.http4s.headers.{ `Content-Length`, `Content-Type`, Accept, Referer }
import org.typelevel.ci.CIString

import com.wenjunhuang.codeepiphany.leetcode.services.LeetCodeApi

class ArticleJCefView(
  private val presenter: ArticleDetailPresenter,
  styleProvider: WebViewStyleProvider,
  myProject: Project
) extends BaseJCefWebView(styleProvider, myProject, "webview") {

  // Set up HTTP server with article-specific resources
  override protected def setupHttpServer(): Unit = {
    myHttpServer.addCustomResponse(
      "/intellijStyle.css",
      { () =>
        styleProvider.baseStyle.getBytes(StandardCharsets.UTF_8)
      },
      "text/css"
    )
    myHttpServer.addTemplateResponse(
      "/leetCodeSolutionArticle/index.html",
      "leetCodeSolutionArticle/index.html",
      "text/html",
      { () =>
        Map(
          ARTICLE_CONTENT ->
            Base64.getEncoder.encodeToString(
              myContent.map(_._1).getOrElse("No article selected 🌟").getBytes(StandardCharsets.UTF_8)
            ),
          CODEDOJO ->
            Base64.getEncoder.encodeToString(myContent.map(_._2.value).getOrElse("").getBytes(StandardCharsets.UTF_8)),
          GET_IFRAME_URL -> ""
        )
      }
    )
  }

  override protected def onUserClickedLink(url: String): Unit = {
    presenter.userClickedLink[IO](url).unsafeRunAndForget()
  }

  override protected def getIndexPath: String = "leetCodeSolutionArticle/index.html"

  override protected def requestFilter(frame: CefFrame, req: CefRequest): Boolean = {
    if (!frame.isMain && CIString(req.getURL).contains(CodeDojo.LeetCode.domain)) {
      // leetcode 的iframe，需要自己处理
      false
    } else {
      true
    }
  }

  override protected def createHeaders(request: CefRequest): IO[Headers] = {
    val url = CIString(request.getURL)
    if (url.contains(CodeDojo.LeetCode.domain)) {
      // leetcode 的iframe，需要自己处理
      val base = Headers(
        "Referer" -> "https://leetcode.com",
        "Accept" -> "text/html,application/xhtml+xml,application/xml;q=1.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
        "Accept-Language" -> "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7,de;q=0.6"
      )
      if (url.contains(CIString("graphql"))) {
        LeetCodeApi(CodeDojo.LeetCode, myProject).getCSRFToken.map { csrfToken =>
          base ++ Headers("X-CSRFToken" -> csrfToken, `Content-Type`(MediaType.application.json))
        }
      } else {
        IO.pure(base)
      }
    } else {
      super.createHeaders(request)
    }
  }
  @volatile
  var myContent: Option[(String, CodeDojo)] = None

  def setArticleContent(content: Option[(String, CodeDojo)]): Unit = {
    myContent = content
    reload()
  }
}

object ArticleJCefView {
  private val ARTICLE_CONTENT = "{{articleContent}}"
  private val CODEDOJO        = "{{codeDojo}}"
  private val GET_IFRAME_URL  = "{{getIframeUrl}}"
}
