package com.wenjunhuang.codeepiphany.leetcode

import cats.effect.IO

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.net.HttpConfigurable

import com.wenjunhuang.codeepiphany.leetcode.services.LeetCodeApi
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.CodeDojo.{ LeetCode, LeetCodeCN }
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientManager, HttpClientService }
import com.wenjunhuang.codeepiphany.utils.CookieUtil
import com.wenjunhuang.codeepiphany.utils.implicits.*
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import java.io.FileInputStream
import java.net.HttpCookie
import scala.io.Source

class LeetcodeApiIntegrationTest extends BasePlatformTestCase {

  private var cookies: List[HttpCookie]   = Nil
  private var cookiesCN: List[HttpCookie] = Nil

  private def setCookie(httpClientKeeper: HttpClientManager[IO]): IO[Unit] = {
    httpClientKeeper.updateCookiesForHost(CodeDojo.LeetCode.domain, cookies) *>
      httpClientKeeper.updateCookiesForHost(CodeDojo.LeetCodeCN.domain, cookiesCN)
  }

  override def setUp(): Unit = {
    super.setUp()
    val config = HttpConfigurable.getInstance()
    config.USE_HTTP_PROXY = true
    config.PROXY_HOST = "127.0.0.1"
    config.PROXY_PORT = 9999

    val loginCookie = Source.fromInputStream(new FileInputStream(getBasePath + "/cookie")).getLines().mkString("\n")
    cookies = CookieUtil.parseCookies(loginCookie)

    val loginCookieCN =
      Source.fromInputStream(new FileInputStream(getBasePath + "/cookie_cn")).getLines().mkString("\n")
    cookiesCN = CookieUtil.parseCookies(loginCookieCN)
  }

  override def tearDown(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject).httpClientManager
    (httpClientKeeper.clearCookiesForHost(CodeDojo.LeetCode.domain) *> httpClientKeeper.clearCookiesForHost(
      CodeDojo.LeetCodeCN.domain
    )).unsafeRunSync()
    super.tearDown()
  }

  override def getBasePath: String = s"testResources/apiTestData/leetcode"
  override def getTestDataPath     = s"${getBasePath}/${getTestName(false)}"

  def testGetFavoriteList(): Unit = {
    val httpClientService = HttpClientService.getInstance(getProject)
    import httpClientService.*
    val leetCodeApi = LeetCodeApi[IO](CodeDojo.LeetCodeCN)
    leetCodeApi.getFavoriteList.map { favoriteList =>
      assertThat(favoriteList.size, not(0))
    }.unsafeRunSync()
  }

  def testTagTypeWithTags(): Unit = {
    val httpClientService = HttpClientService.getInstance(getProject)
    import httpClientService.*
    val leetCodeApiCN = LeetCodeApi[IO](CodeDojo.LeetCodeCN)
//
//    (setCookie(httpClientKeeper.httpClientKeeper) *>
//      leetCodeApiCN.getTagTypeWithTags.map { tagTypeWithTags =>
//        assertThat(tagTypeWithTags.size, not(0))
//      }).unsafeRunSync()

    val leetCodeApi = LeetCodeApi[IO](CodeDojo.LeetCode)
    (setCookie(httpClientManager) *>
      leetCodeApi.getTagTypeWithTags.map { tagTypeWithTags =>
        assertThat(tagTypeWithTags.size, not(0))
      }).unsafeRunSync()
  }

  def testSearchChallenges(): Unit = {
    val httpClientService = HttpClientService.getInstance(getProject)
    import httpClientService.*
    val leetCodeCNApi = LeetCodeApi[IO](CodeDojo.LeetCodeCN)
    leetCodeCNApi
      .searchChallenges(0, 50, None, None, None, None, Nil, None)
      .map { result =>
        assertThat(result.total, not(0))
      }
      .unsafeRunSync()
    val leetCodeApi = LeetCodeApi[IO](CodeDojo.LeetCode)
    (setCookie(httpClientManager)
      *>
        leetCodeApi
          .searchChallenges(0, 50, None, None, None, None, Nil, None)
          .map { result =>
            assertThat(result.total, not(0))
          })
      .unsafeRunSync()
  }

  def testCheckLogin(): Unit = {
    val httpClientService = HttpClientService.getInstance(getProject)
    import httpClientService.*
    val leetCodeCNApi = LeetCodeApi[IO](LeetCodeCN)
    val leetCodeApi   = LeetCodeApi[IO](LeetCode)

    leetCodeCNApi
      .checkLogin()
      .map { result =>
        assertThat(result, is(false))
      }
      .unsafeRunSync()

    leetCodeApi
      .checkLogin()
      .map { result =>
        assertThat(result, is(false))
      }
      .unsafeRunSync()

    setCookie(httpClientManager).unsafeRunSync()
    leetCodeCNApi
      .checkLogin()
      .map { result =>
        assertThat(result, is(true))
      }
      .unsafeRunSync()

    leetCodeApi
      .checkLogin()
      .map { result =>
        assertThat(result, is(true))
      }
      .unsafeRunSync()
  }

  def testGetUserInfo(): Unit = {
    val httpClientService = HttpClientService.getInstance(getProject)
    import httpClientService.*
    val leetCodeCNApi = LeetCodeApi[IO](LeetCodeCN)
    val leetCodeApi   = LeetCodeApi[IO](LeetCode)

    leetCodeCNApi.getUserInfo.map { userInfo =>
      assertThat(userInfo.userSlug, is(None))
    }.unsafeRunSync()
    leetCodeApi.getUserInfo.map { userInfo =>
      assertThat(userInfo.userSlug, is(None))
    }.unsafeRunSync()

    setCookie(httpClientManager).unsafeRunSync()
    leetCodeCNApi.getUserInfo.map { userInfo =>
      assertThat(userInfo.userSlug, not(None))
    }.unsafeRunSync()
    leetCodeApi.getUserInfo.map { userInfo =>
      assertThat(userInfo.userSlug, not(None))
    }.unsafeRunSync()
  }

  def testGetQuestion(): Unit = {
    val httpClientService = HttpClientService.getInstance(getProject)
    import httpClientService.*
    val leetCodeCNApi = LeetCodeApi[IO](LeetCodeCN)
    val leetCodeApi   = LeetCodeApi[IO](LeetCode)

    println(leetCodeCNApi.getQuestionData("median-of-two-sorted-arrays").unsafeRunSync())
  }

  def testGetCompanyTags(): Unit = {
    val httpClientService = HttpClientService.getInstance(getProject)
    import httpClientService.*
//    val leetCodeCNApi = LeetCodeApi[IO](LeetCodeCN)
//    println(leetCodeCNApi.getCompanyTags.unsafeRunSync())

    val leetCodeApi = LeetCodeApi[IO](LeetCode)
    println(leetCodeApi.getCompanyTags.unsafeRunSync())
  }

  def testGetQuestionCompanyTags(): Unit = {
    val httpClientService = HttpClientService.getInstance(getProject)
    import httpClientService.*
//        val leetCodeCNApi = LeetCodeApi[IO](LeetCodeCN)
//        println(leetCodeCNApi.getQuestionCompanyTags.unsafeRunSync())

    val leetCodeApi = LeetCodeApi[IO](LeetCode)
    println(leetCodeApi.getQuestionCompanyTags.unsafeRunSync())
  }
  def testGetPositionTags(): Unit = {
    val httpClientService = HttpClientService.getInstance(getProject)
    import httpClientService.*
//    val leetCodeCNApi = LeetCodeApi[IO](LeetCodeCN)
//    println(leetCodeCNApi.getPositionTags.unsafeRunSync())

    val leetCodeApi = LeetCodeApi[IO](LeetCode)
    println(leetCodeApi.getPositionTags.unsafeRunSync())
  }

  def testGetCompanyQUestions(): Unit = {
    val httpClientService = HttpClientService.getInstance(getProject)
    import httpClientService.*
//    val leetCodeCNApi = LeetCodeApi[IO](LeetCodeCN)
//    println(
//      (setCookie(httpClientManager) *> leetCodeCNApi.searchCompanyChallenges(0, 20, List("bytedance"), Nil, None))
//        .unsafeRunSync()
//    )
    val leetCodeApi = LeetCodeApi[IO](LeetCode)
    println(
      (setCookie(httpClientManager) *> leetCodeApi.searchCompanyChallenges(0, 20, List("facebook"), Nil, None))
        .unsafeRunSync()
    )

  }
}
