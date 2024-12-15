package com.wenjunhuang.codeepiphany.hackerrank

import cats.effect.IO
import cats.syntax.all.*
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.net.{ProxyConfiguration, ProxySettings}
import com.wenjunhuang.codeepiphany.controllers.http.HttpClientService
import com.wenjunhuang.codeepiphany.hackerrank.model.ChallengeStatus.Unsolved
import com.wenjunhuang.codeepiphany.model.{ApiError, CodeDojo}
import com.wenjunhuang.codeepiphany.utils.CookieUtil
import com.wenjunhuang.codeepiphany.utils.implicits.*
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat

import java.io.FileInputStream
import scala.io.Source

class HackerRankApiIntegrationTest extends BasePlatformTestCase {
  override def setUp(): Unit = {
    super.setUp()
    val proxy = ProxySettings.getInstance()
    proxy.setProxyConfiguration(ProxyConfiguration.proxy(ProxyConfiguration.ProxyProtocol.HTTP, "127.0.0.1", 9999, ""))
  }

  override def getTestDataPath = s"testResources/apiTestData/hackerrank/${getTestName(false)}"

  def testSearchChallenges(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val hackerRankApi = HackerRankApi[IO]()
    hackerRankApi
      .searchChallenges(0, 10, None, Some("algorithms"))
      .map { challenges =>
        assertThat(challenges.size, not(0))
      }
      .unsafeRunSync()
    hackerRankApi
      .searchChallenges(0, 10, Some("projecteuler"), None, Nil, Nil)
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
    val content          = Source.fromInputStream(new FileInputStream(getTestDataPath + "/cookie")).getLines().mkString("\n")
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val hackerRankApi = HackerRankApi[IO]()
    (httpClientKeeper.httpClientKeeper.updateCookiesForHost(CodeDojo.HackerRank.domain, CookieUtil.parseCookies(content)) *> hackerRankApi.getInitialData).attempt.unsafeRunSync() match {
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

  def testGetChallengeDetail():Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val hackerRankApi = HackerRankApi[IO]()

    val result = hackerRankApi.getChallengeDetail("birthday-cake-candles", None)
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

    val result = hackerRankApi.getChallengeContent("birthday-cake-candles", None)
      .handleErrorWith{
        case ApiError.InvalidContent(e,message) => 
            IO.println(message)
        case e => IO.raiseError(e)
      }
      .unsafeRunSync()
    println(result)
  }
  
  def testSearchWithKeyword():Unit = {

    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val hackerRankApi = HackerRankApi[IO]()

    val result = hackerRankApi.searchChallengesWithKeyword(None,"sum")
      .flatMap(challenges =>
        challenges.map(challenge =>
          hackerRankApi.getChallengeDetail(challenge.challengeSlug, Some(challenge.contestSlug))
        ).parUnorderedSequence
      )
      .handleErrorWith{
        case ApiError.InvalidContent(e,message) =>
          IO.println(message) *> IO.delay(Nil)
        case e => IO.raiseError(e)
      }
      .unsafeRunSync()
    println(result)
  }
}
