package com.wenjunhuang.codeepiphany.toolwindows.sidebar.description

import cats.effect.IO
import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.WebViewStyleProvider
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.description.DescriptionJCefView.*
import com.wenjunhuang.codeepiphany.utils.jcef.BaseJCefWebView
import com.wenjunhuang.codeepiphany.utils.syntax.*

import java.nio.charset.StandardCharsets

class DescriptionJCefView(
  private val presenter: ChallengeDescriptionPresenter,
  styleProvider: WebViewStyleProvider,
  myProject: Project
) extends BaseJCefWebView(styleProvider, myProject, "webview") {

  // Set up HTTP server with description-specific resources
  override protected def setupHttpServer(): Unit = {
    myHttpServer.addCustomResponse(
      "/intellijStyle.css",
      { () =>
        ChallengeDescriptionStyle.getStyle(styleProvider, myContent.map(_._2)).getBytes(StandardCharsets.UTF_8)
      },
      "text/css"
    )
    myHttpServer.addTemplateResponse(
      "/challengeDescription/index.html",
      "challengeDescription/index.html",
      "text/html",
      () =>
        Map(
          DESCRIPTION_CONTENT -> myContent.map(_._1).getOrElse("No challenge selected 🌟"),
          EXTRA_HEADER        -> myContent.map(_._2).map(CodoDojoHeaders.getHeader).getOrElse("")
        )
    )
  }

  override protected def onUserClickedLink(url: String): Unit = {
    presenter.userClickedLink[IO](url).unsafeRunAndForget()
  }

  override protected def getIndexPath: String = "challengeDescription/index.html"

  @volatile
  var myContent: Option[(String, CodeDojo)] = None
  def setDescription(content: Option[(String, CodeDojo)]): Unit = {
    myContent = content
    reload()
  }
}

object DescriptionJCefView {
  private val DESCRIPTION_CONTENT = "{{descriptionContent}}"
  private val EXTRA_HEADER        = "{{extraHeader}}"
}
