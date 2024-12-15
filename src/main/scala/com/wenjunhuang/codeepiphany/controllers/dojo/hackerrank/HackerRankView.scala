package com.wenjunhuang.codeepiphany.controllers.dojo.hackerrank

import cats.effect.IO
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.project.Project
import com.intellij.ui.CardLayoutPanel
import com.wenjunhuang.codeepiphany.controllers.dojo.AbstractCodeDojoView
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.LoginLogoutProvider
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.keys.LOGIN_LOGOUT_KEY
import com.wenjunhuang.codeepiphany.controllers.http.{ HttpClientKeeper, HttpClientService }
import com.wenjunhuang.codeepiphany.hackerrank.HackerRankApi
import com.wenjunhuang.codeepiphany.hackerrank.services.auth.{ askForLogout, loadAuthenticationMayAskForLogin, AskForLoginResult }
import com.wenjunhuang.codeepiphany.model.{ messages, CodeDojo }
import com.wenjunhuang.codeepiphany.utils.implicits.*

import javax.swing.JComponent

enum HackerRankViewType {
  case Unauthenticated
  case QueryParameters
  case SearchByKeyword
}

class HackerRankView(private val myProject: Project) extends CardLayoutPanel[HackerRankViewType, HackerRankViewType, JComponent] with AbstractCodeDojoView {

  implicit private val httpClientKeeper: HttpClientKeeper[IO] = HttpClientService.getInstance(myProject).httpClientKeeper
  private val myApi                                           = HackerRankApi[IO]()

  private val myUnauthenticatedView = UnauthenticatedView()
  private val myQueryParamPresenter = QueryParametersViewPresenter(myProject)
  private val myKeywordSearchView   = KeywordSearchView(myProject)

  @volatile
  private var myIsLoggedIn = false

  private val myLoginLogoutProvider = new LoginLogoutProvider {
    override def login(): Unit = loadAuthenticationMayAskForLogin[IO](myProject, CodeDojo.HackerRank).flatMap {
      case AskForLoginResult.Done =>
        IO.delay {
          myProject.getMessageBus
            .syncPublisher(messages.LOGIN_LOGOUT_TOPIC)
            .login(CodeDojo.HackerRank)
          myIsLoggedIn = true
        } *> IO.delay(select(HackerRankViewType.QueryParameters, false)).evalOn(intellijUIContext)
      case _ => IO.unit
    }.unsafeRunAndForget()

    override def logout(): Unit = (askForLogout[IO](myProject, CodeDojo.HackerRank)
      *> IO.delay(myProject.getMessageBus.syncPublisher(messages.LOGIN_LOGOUT_TOPIC).logout(CodeDojo.HackerRank))
      *> IO.delay {
        myIsLoggedIn = false
        select(HackerRankViewType.Unauthenticated, false)
      }.evalOn(intellijUIContext)).unsafeRunAndForget()

    override def isLoggedIn: Boolean = myIsLoggedIn
  }

  select(HackerRankViewType.Unauthenticated, false)

  override def prepare(key: HackerRankViewType): HackerRankViewType = key

  override def create(ui: HackerRankViewType): JComponent = ui match {
    case HackerRankViewType.Unauthenticated => myUnauthenticatedView
    case HackerRankViewType.QueryParameters => myQueryParamPresenter.getComponent
    case HackerRankViewType.SearchByKeyword => myKeywordSearchView
  }

  override def uiDataSnapshot(dataSink: DataSink): Unit =
    dataSink.set(LOGIN_LOGOUT_KEY, myLoginLogoutProvider)
}
