package com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank

import cats.effect.IO
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.project.Project
import com.intellij.ui.CardLayoutPanel
import com.wenjunhuang.codeepiphany.toolwindows.dojo.AbstractCodeDojoView
import com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.LoginLogoutProvider
import com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.keys.{ LOGIN_LOGOUT_KEY, SWITCHUI_PROVIDER_KEY }
import com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.providers.{ DojoUI, SwitchUIProvider }
import com.wenjunhuang.codeepiphany.hackerrank.HackerRankApi
import com.wenjunhuang.codeepiphany.hackerrank.services.auth.{ askForLogout, loadAuthenticationMayAskForLogin, AskForLoginResult }
import com.wenjunhuang.codeepiphany.model.{ messages, CodeDojo }
import com.wenjunhuang.codeepiphany.services.http.{HttpClientKeeper, HttpClientService}
import com.wenjunhuang.codeepiphany.utils.implicits.*

import javax.swing.JComponent

class HackerRankView(private val myProject: Project) extends CardLayoutPanel[DojoUI, DojoUI, JComponent] with AbstractCodeDojoView {

  implicit private val httpClientKeeper: HttpClientKeeper[IO] = HttpClientService.getInstance(myProject).httpClientKeeper
  private val myApi                                           = HackerRankApi[IO]()

  private val myUnauthenticatedView    = UnauthenticatedView()
  private val myQueryParamPresenter    = QueryParametersViewPresenter(myProject)
  private val myKeywordSearchPresenter = KeywordSearchViewPresenter(myProject)
  private var myCurrentUI              = DojoUI.Unauthenticated
  @volatile
  private var myIsLoggedIn = false

  select(myCurrentUI, false)

  private val myLoginLogoutProvider = new LoginLogoutProvider {
    override def login(): Unit = loadAuthenticationMayAskForLogin[IO](myProject, CodeDojo.HackerRank).flatMap {
      case AskForLoginResult.Done =>
        IO.delay {
          myProject.getMessageBus
            .syncPublisher(messages.LOGIN_LOGOUT_TOPIC)
            .login(CodeDojo.HackerRank)
          myIsLoggedIn = true
        } *> IO.delay {mySwitchUIProvider.switchTo(DojoUI.QueryParameters)}.evalOn(intellijUIContext)
      case _ => IO.unit
    }.unsafeRunAndForget()

    override def logout(): Unit = (askForLogout[IO](myProject, CodeDojo.HackerRank)
      *> IO.delay(myProject.getMessageBus.syncPublisher(messages.LOGIN_LOGOUT_TOPIC).logout(CodeDojo.HackerRank))
      *> IO.delay {
        myIsLoggedIn = false
        select(DojoUI.Unauthenticated, false)
      }.evalOn(intellijUIContext)).unsafeRunAndForget()

    override def isLoggedIn: Boolean = myIsLoggedIn
  }

  private val mySwitchUIProvider = new SwitchUIProvider {
    override def switchTo(ui: DojoUI): Unit =
      myCurrentUI = ui
      select(ui, false)

    override def getCurrentUI: DojoUI = myCurrentUI
  }

  override def prepare(key: DojoUI): DojoUI = key

  override def create(ui: DojoUI): JComponent = ui match {
    case DojoUI.Unauthenticated => myUnauthenticatedView
    case DojoUI.QueryParameters => myQueryParamPresenter.getComponent
    case DojoUI.SearchByKeyword => myKeywordSearchPresenter.getComponent
  }

  override def uiDataSnapshot(dataSink: DataSink): Unit =
    dataSink.set(LOGIN_LOGOUT_KEY, myLoginLogoutProvider)
    dataSink.set(SWITCHUI_PROVIDER_KEY, mySwitchUIProvider)
}
