package com.wenjunhuang.codeepiphany.codeforces.services

import cats.effect.IO
import cats.syntax.all.*
import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.codeforces.models.CodeForcesSubmissionResponse
import com.wenjunhuang.codeepiphany.codeforces.settings.CodeForcesSettingsConfigurable
import com.wenjunhuang.codeepiphany.database.Tables.{ CHALLENGE, CHALLENGE_LANGUAGE, CODEFORCES_CHALLENGE }
import com.wenjunhuang.codeepiphany.model.CodeDojo.CodeForces
import com.wenjunhuang.codeepiphany.model.newtypes.SubmissionId
import com.wenjunhuang.codeepiphany.model.{ CodeDojo, Language, LanguageVersion, SubmissionResult }
import com.wenjunhuang.codeepiphany.services.{
  console,
  BaseSubmissionService,
  ChallengeRepository,
  WebViewStyleProvider
}
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import org.jooq.{ DSLContext, Record }
import org.typelevel.ci.CIString

import scala.jdk.OptionConverters.*
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.openapi.application.{ ApplicationManager, ModalityState }
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.{ JBCefBrowserBase, JBCefCookie, JBCefJSQuery }
import com.wenjunhuang.codeepiphany.services.console.MessageSeg
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.utils.extensions.webRgba
import com.wenjunhuang.codeepiphany.utils.jcef.BaseJCefWebView
import com.wenjunhuang.codeepiphany.vfs.WebPreviewVirtualFile
import com.wenjunhuang.codeepiphany.utils.syntax.*
import org.cef.browser.{ CefBrowser, CefFrame }
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest

import java.util.concurrent.CancellationException

class CodeForcesSubmissionService(project: Project) extends BaseSubmissionService(project, CodeForces) {
  override type SubmissionRequest  = CFRequest
  override type SubmissionResponse = CodeForcesSubmissionResponse

  override protected def prepareSubmissionRequest(item: ChallengeSettingsStateItem): IO[CFRequest] =
    ChallengeRepository
      .getInstance(myProject)
      .getDSLContextResource
      .use { client => IO.delay(createCFRequest(item, client)) }

  override protected def updateSpecificSubmissionRecord(
    dsl: DSLContext,
    submissionId: SubmissionId,
    response: CodeForcesSubmissionResponse
  ): SubmissionResponseInfo = SubmissionResponseInfo(response.result, response.message, response.submissionId.toString)

  override protected def callApi(
    basicInfo: CFRequest,
    processedCode: String
  ): fs2.Stream[IO, CodeForcesSubmissionResponse] =
    CodeForcesApi.submitAnswer(
      basicInfo.contestId,
      basicInfo.index,
      basicInfo.problemSetName,
      basicInfo.programTypeId,
      processedCode,
      getCSRFAndCloudflare(basicInfo)
    )

  override protected def reportSubmitResult(
    basicInfo: SubmissionRequest,
    submissionId: SubmissionId,
    processedCode: String,
    lastResponseInfo: SubmissionResponseInfo,
    lastResponse: CodeForcesSubmissionResponse
  ): IO[Unit] = {
    lastResponseInfo.result match
      case SubmissionResult.Success =>
        console.info(
          project,
          CodeForces,
          MessageSeg.Hyperlink(
            PluginBundle.message("submissionResult.viewDetails"),
            { link =>
              CodeForcesSubmissionService.showSubmissionDetails(
                project,
                basicInfo.problemSetName,
                basicInfo.contestId.toString,
                lastResponse.submissionId.toString
              )
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
              CodeForcesSubmissionService.showSubmissionDetails(
                project,
                basicInfo.problemSetName,
                basicInfo.contestId.toString,
                lastResponse.submissionId.toString
              )
            }
          ),
          "\n",
          s"${lastResponseInfo.result.show}\n${lastResponse.message}"
        )
  }

  private def createCFRequest(item: ChallengeSettingsStateItem, client: DSLContext): CFRequest = {
    client
      .select(
        CHALLENGE.DOJOID,
        CODEFORCES_CHALLENGE.CONTESTID,
        CODEFORCES_CHALLENGE.INDEX,
        CODEFORCES_CHALLENGE.PROBLEMSETNAME,
        CHALLENGE_LANGUAGE.LANGUAGE,
        CHALLENGE_LANGUAGE.LANGUAGEVERSION
      )
      .from(CHALLENGE)
      .innerJoin(CODEFORCES_CHALLENGE)
      .on(CHALLENGE.ID.eq(CODEFORCES_CHALLENGE.ID))
      .innerJoin(CHALLENGE_LANGUAGE)
      .on(CHALLENGE.ID.eq(CHALLENGE_LANGUAGE.CHALLENGEID))
      .where(CHALLENGE.ID.eq(item.challengeId).and(CHALLENGE_LANGUAGE.ID.eq(item.challengeLanguageId)))
      .fetchOptional()
      .toScala
      .flatMap(parseCodeForcesRecord)
      .getOrElse(throw new IllegalStateException("Cannot find challenge data"))
  }

  private def parseCodeForcesRecord(record: Record): Option[CFRequest] = {
    for {
      contestId <- Option(record.get(CODEFORCES_CHALLENGE.CONTESTID)).map(_.toLong)
      index     <- Option(record.get(CODEFORCES_CHALLENGE.INDEX))
      problemset = Option(record.get(CODEFORCES_CHALLENGE.PROBLEMSETNAME))
      language <- Language.fromCIString(CIString(record.get(CHALLENGE_LANGUAGE.LANGUAGE)))
      langVer = LanguageVersion.fromString(record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION))
      programTypeId <- resolveProgramType(language, langVer)
    } yield CFRequest(contestId, index, problemset, language, langVer, programTypeId)
  }
  private def resolveProgramType(language: Language, version: LanguageVersion): Option[String] =
    CodeForcesSettingsConfigurable.CODEFORCES_LANGUAGES.get((language, version))

  private def getCSRFAndCloudflare(request: CFRequest): IO[(String, String,String,String)] = {
    HttpClientManager
      .getCookiesForHost(CodeDojo.CodeForces.domain)
      .flatMap { cookies =>
        IO.async_[(String, String, String, String)] { cb =>
          val dialog           = DialogBuilder(myProject)
          val browserComponent = BaseJCefWebView.createDefaultBrowser()

          val jsQuery = JBCefJSQuery.create(browserComponent.asInstanceOf[JBCefBrowserBase])
          jsQuery.addHandler { (s: String) =>
            val (csrf, turnstile, ftaa, bfaa) = s.split(",") match {
              case Array(c, t, f, b) => (c, t, f, b)
              case _                 => throw new IllegalArgumentException("Invalid CSRF and Turnstile response format")
            }
            ApplicationManager.getApplication.invokeLater(
              { () =>
                dialog.getDialogWrapper.close(0, true)
                cb(Right((csrf, turnstile, ftaa, bfaa)))
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
                         |const node = document.querySelector("div.turnstile-container");
                         |if (node != null) {
                         |   node.parentElement.removeChild(node);
                         |   const mask = document.createElement("div");
                         |   mask.setAttribute("id","cf-turnstile-mask");
                         |   mask.style = "position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background-color: ${WebViewStyleProvider.DEFAULT.panelBackground
                          .webRgba(1.0)}; z-index: 9999;display: flex; align-items: center; justify-content: center;";
                         |   document.body.append(mask);
                         |   const csrf = document.querySelector("input[name='csrf_token']").value;
                         |   const ftaa = window._ftaa;
                         |   const bfaa = window._bfaa;
                         |   turnstile.render('#cf-turnstile-mask', {
                         |      sitekey: '0x4AAAAAACDz1ltMYNJCAQZS',
                         |      theme: ${if (WebViewStyleProvider.DEFAULT.isDarkMode) "'dark'" else "'light'"},
                         |      callback: function(turnstile) {
                         |        const result = csrf + "," + turnstile + "," + ftaa + "," + bfaa;
                         |        console.log("CSRF,Turnstile,FTAA,BFAA result: " , result);
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
              JBCefCookie(cookie.getName, cookie.getValue, CodeDojo.CodeForces.domain.toString, "/", true, false)
            browserComponent.getJBCefCookieManager
              .setCookie(s"https://${CodeDojo.CodeForces.domain.toString}/problemset/submit", jbcefCookie)
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
          browserComponent.loadURL(s"https://${CodeDojo.CodeForces.domain.toString}/problemset/submit")

          if !dialog.showAndGet() then
            cb(Left(CancellationException(PluginBundle.message("atcoder.cloudflare.cancelled"))))
        }
      }
      .evalOnEDTDefault()
  }

  case class CFRequest(
    contestId: Long,
    index: String,
    problemSetName: Option[String],
    language: Language,
    langVer: LanguageVersion,
    programTypeId: String
  )
}
object CodeForcesSubmissionService {
  def showSubmissionDetails(
    project: Project,
    problemSetName: Option[String],
    contestId: String,
    submissionId: String
  ): Unit = {
    val link = problemSetName
      .map((name: String) =>
        "https://codeforces.com/problemsets/" + name + "/submission/" + contestId + "/" + submissionId
      )
      .getOrElse("https://codeforces.com/problemset/submission/" + contestId + "/" + submissionId)
    HttpClientManager
      .getCookiesForHost(CodeForces.domain)
      .flatMap { cookies =>
        IO.delay {
          val file = new WebPreviewVirtualFile(link, CodeForces.domain.toString, cookies, submissionId)
          WebPreviewVirtualFile.openEditor(file, project)
        }.evalOnEDTWithWrite()
      }
      .unsafeRunAndForget()
  }
}
