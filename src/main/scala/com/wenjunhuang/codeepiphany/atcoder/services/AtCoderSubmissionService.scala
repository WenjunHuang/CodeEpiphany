package com.wenjunhuang.codeepiphany.atcoder.services

import cats.effect.IO
import cats.syntax.all.*
import fs2.Stream
import java.util.concurrent.CancellationException
import org.cef.browser.{ CefBrowser, CefFrame }
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest
import org.jooq.{ DSLContext, Record }
import org.typelevel.ci.CIString
import scala.jdk.OptionConverters.*

import com.intellij.openapi.application.{ ApplicationManager, ModalityState }
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.{ JBCefBrowserBase, JBCefCookie, JBCefJSQuery }

import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.atcoder.models.AtCoderSubmissionResponse
import com.wenjunhuang.codeepiphany.atcoder.settings.AtCoderSettingsConfigurable
import com.wenjunhuang.codeepiphany.database.Tables.{ ATCODER_CHALLENGE, CHALLENGE, CHALLENGE_LANGUAGE }
import com.wenjunhuang.codeepiphany.model.{ CodeDojo, Language, LanguageVersion, SubmissionResult }
import com.wenjunhuang.codeepiphany.model.CodeDojo.AtCoder
import com.wenjunhuang.codeepiphany.model.newtypes.SubmissionId
import com.wenjunhuang.codeepiphany.services.{
  console,
  BaseSubmissionService,
  ChallengeRepository,
  WebViewStyleProvider
}
import com.wenjunhuang.codeepiphany.services.console.MessageSeg
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import com.wenjunhuang.codeepiphany.utils.jcef.BaseJCefWebView
import com.wenjunhuang.codeepiphany.utils.syntax.*
import com.wenjunhuang.codeepiphany.vfs.WebPreviewVirtualFile

class AtCoderSubmissionService(project: Project) extends BaseSubmissionService(project, AtCoder) {
  override type SubmissionRequest  = Request
  override type SubmissionResponse = AtCoderSubmissionResponse

  override protected def prepareSubmissionRequest(item: ChallengeSettingsStateItem): IO[Request] =
    ChallengeRepository
      .getInstance(myProject)
      .getDSLContextResource
      .use { client => IO.delay(createRequest(item, client)) }

  override protected def updateSpecificSubmissionRecord(
    dsl: DSLContext,
    submissionId: SubmissionId,
    response: AtCoderSubmissionResponse
  ): SubmissionResponseInfo = SubmissionResponseInfo(response.result, response.message, response.submissionId)

  override protected def callApi(basicInfo: Request, processedCode: String): Stream[IO, AtCoderSubmissionResponse] =
    AtCoderApi.submitAnswer(
      basicInfo.contestId,
      basicInfo.problemId,
      basicInfo.languageId,
      processedCode,
      getCSRFAndCloudflare(basicInfo)
    )

  override protected def reportSubmitResult(
    basicInfo: SubmissionRequest,
    submissionId: SubmissionId,
    processedCode: String,
    lastResponseInfo: SubmissionResponseInfo,
    lastResponse: AtCoderSubmissionResponse
  ): IO[Unit] = {
    lastResponseInfo.result match
      case SubmissionResult.Success =>
        console.info(
          project,
          CodeDojo.AtCoder,
          MessageSeg.Hyperlink(
            PluginBundle.message("submissionResult.viewDetails"),
            { link =>
              AtCoderSubmissionService.showSubmissionDetails(project, lastResponse.submissionId, basicInfo.contestId)
            }
          ),
          "\n",
          PluginBundle.message("submission.passed") + "\n${lastResponse.message}"
        )
      case _ =>
        console.error(
          project,
          MessageSeg.Hyperlink(
            PluginBundle.message("submissionResult.viewDetails"),
            { link =>
              AtCoderSubmissionService.showSubmissionDetails(project, lastResponse.submissionId, basicInfo.contestId)
            }
          ),
          "\n",
          s"${lastResponseInfo.result.show}\n${lastResponse.message}"
        )
  }

  private def createRequest(item: ChallengeSettingsStateItem, client: DSLContext): Request = {
    client
      .select(
        CHALLENGE.DOJOID,
        CHALLENGE_LANGUAGE.LANGUAGE,
        CHALLENGE_LANGUAGE.LANGUAGEVERSION,
        ATCODER_CHALLENGE.CONTESTID
      )
      .from(CHALLENGE)
      .innerJoin(ATCODER_CHALLENGE)
      .on(CHALLENGE.ID.eq(ATCODER_CHALLENGE.ID))
      .innerJoin(CHALLENGE_LANGUAGE)
      .on(CHALLENGE.ID.eq(CHALLENGE_LANGUAGE.CHALLENGEID))
      .where(CHALLENGE.ID.eq(item.challengeId).and(CHALLENGE_LANGUAGE.ID.eq(item.challengeLanguageId)))
      .fetchOptional()
      .toScala
      .flatMap(parseRecord)
      .getOrElse(throw new IllegalStateException("Cannot find challenge data"))
  }

  private def parseRecord(record: Record): Option[Request] = {
    for {
      contestId <- Option(record.get(ATCODER_CHALLENGE.CONTESTID))
      problemId <- Option(record.get(CHALLENGE.DOJOID))
      language  <- Language.fromCIString(CIString(record.get(CHALLENGE_LANGUAGE.LANGUAGE)))
      langVer = LanguageVersion.fromString(record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION))
      programTypeId <- resolveLanguageId(language, langVer)
    } yield Request(contestId, problemId, language, langVer, programTypeId)
  }
  private def resolveLanguageId(language: Language, version: LanguageVersion): Option[String] =
    AtCoderSettingsConfigurable.ATCODER_LANGUAGES.get((language, version))

  private def getCSRFAndCloudflare(request: Request): IO[(String, String)] = {
    HttpClientManager
      .getCookiesForHost(CodeDojo.AtCoder.domain)
      .flatMap { cookies =>
        IO.async_[(String, String)] { cb =>
          val dialog           = DialogBuilder(myProject)
          val browserComponent = BaseJCefWebView.createDefaultBrowser()

          val jsQuery = JBCefJSQuery.create(browserComponent.asInstanceOf[JBCefBrowserBase])
          jsQuery.addHandler { (s: String) =>
            val (csrf, turnstile) = s.split(",") match {
              case Array(c, t) => (c, t)
              case _           => throw new IllegalArgumentException("Invalid CSRF and Turnstile response format")
            }
            ApplicationManager.getApplication.invokeLater(
              { () =>
                dialog.getDialogWrapper.close(0, true)
                cb(Right((csrf, turnstile)))
              },
              ModalityState.any()
            )
            JBCefJSQuery.Response(null)
          }

          browserComponent.getJBCefClient
            .addLoadHandler(
              new CefLoadHandlerAdapter {
                override def onLoadStart(
                  browser: CefBrowser,
                  frame: CefFrame,
                  transitionType: CefRequest.TransitionType
                ): Unit = {
                  if (frame.isMain) {
                    ApplicationManager.getApplication.invokeLater(
                      new Runnable {
                        override def run(): Unit = {
                          browserComponent.getComponent.setVisible(false)
                        }
                      },
                      ModalityState.any()
                    )
                  }
                }
                override def onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int): Unit = {
                  if (frame.isMain) {
                    // language=JavaScript
                    val js =
                      s"""
                         |document.documentElement.style = "overflow: hidden;";
                         |const node = document.querySelector("div.cf-challenge");
                         |if (node != null) {
                         |   node.parentElement.removeChild(node);
                         |   const mask = document.createElement("div");
                         |   mask.setAttribute("id","cf-turnstile-mask");
                         |   mask.style = "position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background-color: ${WebViewStyleProvider.DEFAULT.panelBackground
                          .webRgba(1.0)}; z-index: 9999;display: flex; align-items: center; justify-content: center;";
                         |   document.body.append(mask);
                         |   const csrf = document.querySelector("input[name='csrf_token']").value;
                         |   turnstile.render('#cf-turnstile-mask', {
                         |      sitekey: '0x4AAAAAAA6HJUmmLP7mLxx0',
                         |      theme: ${if (WebViewStyleProvider.DEFAULT.isDarkMode) "'dark'" else "'light'"},
                         |      callback: function(turnstile) {
                         |        const result = csrf + "," + turnstile;
                         |        console.log("CSRF and Turnstile result: " , result);
                         |        ${jsQuery.inject("result")}
                         |   }});
                         |}
                         |""".stripMargin
                    browser.executeJavaScript(js, browser.getURL, 0)
                    ApplicationManager.getApplication.invokeLater(
                      new Runnable {
                        override def run(): Unit = {
                          browserComponent.getComponent.setVisible(true)
                        }
                      },
                      ModalityState.any()
                    )
                  }
                }
              },
              browserComponent.getCefBrowser
            )

          cookies.foreach { cookie =>
            val jbcefCookie =
              JBCefCookie(cookie.getName, cookie.getValue, CodeDojo.AtCoder.domain.toString, "/", true, false)
            browserComponent.getJBCefCookieManager
              .setCookie(
                s"https://${CodeDojo.AtCoder.domain.toString}/contests/${request.contestId}/submit",
                jbcefCookie
              )
              .cancel(true)
          }

          browserComponent.getComponent.setBackground(WebViewStyleProvider.DEFAULT.panelBackground)
          dialog
            .centerPanel(browserComponent.getComponent)
          dialog.setTitle(PluginBundle.message("atcoder.cloudflare.turnstile"))
          dialog.addCancelAction()
          dialog.getDialogWrapper.setSize(400, 400)

          Disposer.register(browserComponent, jsQuery)
          Disposer.register(dialog, browserComponent)
          browserComponent.loadURL(s"https://${CodeDojo.AtCoder.domain.toString}/contests/${request.contestId}/submit")

          if !dialog.showAndGet() then
            cb(Left(CancellationException(PluginBundle.message("atcoder.cloudflare.cancelled"))))
        }
      }
      .evalOnEDTDefault()
  }

  case class Request(
    contestId: String,
    problemId: String,
    language: Language,
    langVer: LanguageVersion,
    languageId: String
  )
}
object AtCoderSubmissionService {
  def showSubmissionDetails(project: Project, submissionId: String, contestId: String): Unit = {
    HttpClientManager
      .getCookiesForHost(CodeDojo.AtCoder.domain)
      .flatMap { cookies =>
        IO.delay {
          val file = new WebPreviewVirtualFile(
            s"https://${CodeDojo.AtCoder.domain.toString}/contests/$contestId/submissions/$submissionId/",
            CodeDojo.AtCoder.domain.toString,
            cookies,
            submissionId
          )
          WebPreviewVirtualFile.openEditor(file, project)
        }.evalOnEDTWithWrite()
      }
      .unsafeRunAndForget()
  }
}
