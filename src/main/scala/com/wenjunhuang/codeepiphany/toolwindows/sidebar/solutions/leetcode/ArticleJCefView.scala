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
          GET_IFRAME_URL -> ""
        )
      }
    )
  }

  override protected def onUserClickedLink(url: String): Unit = {
    presenter.userClickedLink[IO](url).unsafeRunAndForget()
  }

  override protected def getIndexPath: String = "leetCodeSolutionArticle/index.html"

  override protected def getTemplateVariables: Map[String, String] = {
    Map(
      ARTICLE_CONTENT ->
        Base64.getEncoder.encodeToString(
          myContent.map(_._1).getOrElse("No article selected 🌟").getBytes(StandardCharsets.UTF_8)
        ),
      GET_IFRAME_URL -> ""
    )
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
  private val GET_IFRAME_URL  = "{{getIframeUrl}}"
}
