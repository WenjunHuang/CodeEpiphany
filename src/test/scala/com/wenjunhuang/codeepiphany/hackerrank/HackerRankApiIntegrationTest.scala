package com.wenjunhuang.codeepiphany.hackerrank

import cats.effect.IO
import cats.syntax.all.*
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.net.{ ProxyConfiguration, ProxySettings }
import com.wenjunhuang.codeepiphany.hackerrank.model.Contest.{ Master, ProjectEuler }
import com.wenjunhuang.codeepiphany.hackerrank.model.Contest
import com.wenjunhuang.codeepiphany.hackerrank.services.HackerRankApi
import com.wenjunhuang.codeepiphany.model.{ ApiError, CodeDojo, Language }
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientKeeper, HttpClientService }
import com.wenjunhuang.codeepiphany.utils.CookieUtil
import com.wenjunhuang.codeepiphany.utils.implicits.*
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat

import java.io.FileInputStream
import java.net.HttpCookie
import scala.io.Source

class HackerRankApiIntegrationTest extends BasePlatformTestCase {
  private var cookies: List[HttpCookie] = Nil

  private def setCookie(httpClientKeeper: HttpClientKeeper[IO]): IO[Unit] = {
    httpClientKeeper.updateCookiesForHost(CodeDojo.HackerRank.domain, cookies)
  }

  override def setUp(): Unit = {
    super.setUp()
    val proxy = ProxySettings.getInstance()
    proxy.setProxyConfiguration(ProxyConfiguration.proxy(ProxyConfiguration.ProxyProtocol.HTTP, "127.0.0.1", 9999, ""))

    val loginCookie = Source.fromInputStream(new FileInputStream(getBasePath + "/cookie")).getLines().mkString("\n")
    cookies = CookieUtil.parseCookies(loginCookie)
  }

  override def getBasePath: String = s"testResources/apiTestData/hackerrank"
  override def getTestDataPath     = s"${getBasePath}/${getTestName(false)}"

  def testSearchChallenges(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val hackerRankApi = HackerRankApi[IO]()
    hackerRankApi
      .searchChallenges(0, 10, Master, "algorithms")
      .map { challenges =>
        assertThat(challenges.size, not(0))
      }
      .unsafeRunSync()
    hackerRankApi
      .searchChallenges(0, 10, ProjectEuler, "projecteuler", Nil, Nil)
      .map(challenges => assertThat(challenges.size, not(0)))
      .unsafeRunSync()
  }

  def testCheckLogin(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val hackerRankApi = HackerRankApi[IO]()

    if hackerRankApi.checkLogin().unsafeRunSync() then println("Login success")
    else println("Login failed")
  }

  def testGetInitialData(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val hackerRankApi = HackerRankApi[IO]()
    (setCookie(httpClientKeeper.httpClientKeeper) *> hackerRankApi.getInitialData).attempt.unsafeRunSync() match {
      case Left(e) => throw e
      case Right((userInfo, challengeDomains)) =>
        assertThat(userInfo.username, allOf(notNullValue(), not("")))
        assertThat(userInfo.name, allOf(notNullValue(), not("")))
        assertThat(userInfo.avatar, allOf(notNullValue(), not("")))
        assertThat(challengeDomains.size, not(0))
        println(userInfo)
        println(challengeDomains)
    }
  }

  def testGetChallengeDetail(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val hackerRankApi = HackerRankApi[IO]()

    val result = hackerRankApi
      .getChallengeDetail("birthday-cake-candles", Master)
      .handleErrorWith {
        case ApiError.InvalidContent(e, message) =>
          IO.println(message)
        case e => IO.raiseError(e)
      }
      .unsafeRunSync()
    println(result)
  }

  def testGetChallengeContent(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val hackerRankApi = HackerRankApi[IO]()

    val result = hackerRankApi
      .getChallengeContent("birthday-cake-candles", Master)
      .handleErrorWith {
        case ApiError.InvalidContent(e, message) =>
          IO.println(message)
        case e => IO.raiseError(e)
      }
      .unsafeRunSync()
    println(result)
  }

  def testSearchMasterWithKeyword(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val hackerRankApi = HackerRankApi[IO]()

    val result = hackerRankApi
      .searchChallengesWithKeyword(Master, "sum")
      .flatMap(challenges =>
        challenges.map { case (contest, challenge) =>
          hackerRankApi.getChallengeDetail(challenge.challengeSlug, contest)
        }.parUnorderedSequence
      )
      .handleErrorWith {
        case ApiError.InvalidContent(e, message) =>
          IO.println(message) *> IO.delay(Nil)
        case e => IO.raiseError(e)
      }
      .unsafeRunSync()
    println(result)
  }

  def testSearchProjectEulerWithKeyword(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val hackerRankApi = HackerRankApi[IO]()

    val result = hackerRankApi
      .searchChallengesWithKeyword(ProjectEuler, "project")
      .flatMap(challenges =>
        challenges.map { case (contest, challenge) =>
          hackerRankApi.getChallengeDetail(challenge.challengeSlug, contest)
        }.parUnorderedSequence
      )
      .handleErrorWith {
        case ApiError.InvalidContent(e, message) =>
          IO.println(message) *> IO.delay(Nil)
        case e => IO.raiseError(e)
      }
      .unsafeRunSync()
    println(result)
  }

  def testRunCode(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val hackerRankApi = HackerRankApi[IO]()
    val code = Source.fromInputStream(new FileInputStream(getTestDataPath + "/Code.java")).getLines().mkString("\n")
    (setCookie(httpClientKeeper.httpClientKeeper) *>
      hackerRankApi
        .runAnswer("a-very-big-sum", Contest.Master, Language.Java, "15", code)
        .evalTap(response => IO.println(response))
        .compile
        .drain).handleErrorWith {
      case ApiError.InvalidContent(e, message) =>
        IO.println(message) *> IO.delay(Nil)
      case e => IO.raiseError(e)
    }.unsafeRunSync()

  }
}
