package integration

import cats.effect.IO
import cats.syntax.all.*
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.net.HttpConfigurable
import com.wenjunhuang.codeepiphany.hackerrank.models.HackerRankContest
import com.wenjunhuang.codeepiphany.hackerrank.models.HackerRankContest.{Master, ProjectEuler}
import com.wenjunhuang.codeepiphany.hackerrank.services.HackerRankApi
import com.wenjunhuang.codeepiphany.model.{ApiError, CodeDojo, Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.utils.CookieUtil
import com.wenjunhuang.codeepiphany.utils.syntax.*
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat

import java.io.FileInputStream
import java.net.HttpCookie
import scala.io.Source

class HackerRankApiIntegrationTest extends BasePlatformTestCase {
  private var cookies: List[HttpCookie] = Nil

  private def setCookie(): IO[Unit] = {
    HttpClientManager.updateCookiesForHost(CodeDojo.HackerRank.domain, cookies)
  }

  override def setUp(): Unit = {
    super.setUp()
    val config = HttpConfigurable.getInstance()
    config.USE_HTTP_PROXY = true
    config.PROXY_HOST = "127.0.0.1"
    config.PROXY_PORT = 9999

    val loginCookie = Source.fromInputStream(new FileInputStream(getBasePath + "/cookie")).getLines().mkString("\n")
    cookies = CookieUtil.parseCookies(loginCookie)
  }

  override def getBasePath: String = s"testResources/apiTestData/hackerrank"
  override def getTestDataPath     = s"${getBasePath}/${getTestName(false)}"

  def testSearchChallenges(): Unit = {
    val hackerRankApi = HackerRankApi
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
    val hackerRankApi = HackerRankApi

    if hackerRankApi.checkLogin().unsafeRunSync() then println("Login success")
    else println("Login failed")
  }

  def testGetInitialData(): Unit = {
    val hackerRankApi = HackerRankApi
    (setCookie() *> hackerRankApi.getInitialData).attempt.unsafeRunSync() match {
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
    val hackerRankApi = HackerRankApi

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
    val hackerRankApi = HackerRankApi

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
    val hackerRankApi = HackerRankApi

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
    val hackerRankApi = HackerRankApi

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
    val hackerRankApi = HackerRankApi
    val code = Source.fromInputStream(new FileInputStream(getTestDataPath + "/Code.java")).getLines().mkString("\n")
    (setCookie() *>
      hackerRankApi
        .runAnswer("a-very-big-sum", HackerRankContest.Master, Language.Java, LanguageVersion.fromString("15"), code)
        .evalTap(response => IO.println(response))
        .compile
        .drain).handleErrorWith {
      case ApiError.InvalidContent(e, message) =>
        IO.println(message) *> IO.delay(Nil)
      case e => IO.raiseError(e)
    }.unsafeRunSync()

  }
}
