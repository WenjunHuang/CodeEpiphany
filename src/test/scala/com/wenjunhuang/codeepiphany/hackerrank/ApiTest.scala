package com.wenjunhuang.codeepiphany.hackerrank

import cats.effect.IO
import cats.syntax.all.*
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.net.{ ProxyConfiguration, ProxySettings }
import com.wenjunhuang.codeepiphany.controllers.http.HttpClientService
import com.wenjunhuang.codeepiphany.hackerrank.model.ChallengeStatus.Unsolved
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.utils.CookieUtil
import com.wenjunhuang.codeepiphany.utils.implicits.*
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat

import java.io.FileInputStream
import scala.io.Source

class ApiTest extends BasePlatformTestCase {
  override def setUp(): Unit = {
    super.setUp()
    val proxy = ProxySettings.getInstance()
    proxy.setProxyConfiguration(ProxyConfiguration.proxy(ProxyConfiguration.ProxyProtocol.HTTP, "127.0.0.1", 9999, ""))
  }

  override def getTestDataPath = s"apiTestData/hackerrank/${getTestName(false)}"

  def testSearchChallenges(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
//    val httpClientKeeper = HttpClientKeeper[IO]()
    val hackerRankApi = HackerRankApi[IO]()

    (
      hackerRankApi
        .searchChallenges(0, 10, None, Some("algorithms")),
      hackerRankApi.searchChallenges(0, 10, Some("projecteuler"), None, List(Unsolved), Nil)
    ).mapN { case (challenges1, challenges2) =>
      assertThat(challenges1.size, not(0))
      assertThat(challenges2.size, not(0))
    }.unsafeRunSync()
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
    (httpClientKeeper.httpClientKeeper.updateCookiesForHost(CodeDojo.HackerRank.domain, CookieUtil.parseCookies(content)) *> hackerRankApi.getInitialData()).attempt.unsafeRunSync() match {
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
}
