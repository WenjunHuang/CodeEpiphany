package com.wenjunhuang.codeepiphany.leetcode

import cats.effect.IO
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.net.{ ProxyConfiguration, ProxySettings }
import com.wenjunhuang.codeepiphany.leetcode.services.LeetCodeApi
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientKeeper, HttpClientService }
import com.wenjunhuang.codeepiphany.utils.CookieUtil
import com.wenjunhuang.codeepiphany.utils.implicits.*

import java.io.FileInputStream
import java.net.HttpCookie
import scala.io.Source
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat

class LeetcodeApiIntegrationTest extends BasePlatformTestCase {

  private var cookies: List[HttpCookie]   = Nil
  private var cookiesCN: List[HttpCookie] = Nil

  private def setCookie(httpClientKeeper: HttpClientKeeper[IO]): IO[Unit] = {
    httpClientKeeper.updateCookiesForHost(CodeDojo.LeetCode.domain, cookies) *>
      httpClientKeeper.updateCookiesForHost(CodeDojo.LeetCodeCN.domain, cookiesCN)
  }

  override def setUp(): Unit = {
    super.setUp()
    val proxy = ProxySettings.getInstance()
    proxy.setProxyConfiguration(ProxyConfiguration.proxy(ProxyConfiguration.ProxyProtocol.HTTP, "127.0.0.1", 9999, ""))

    val loginCookie = Source.fromInputStream(new FileInputStream(getBasePath + "/cookie")).getLines().mkString("\n")
    cookies = CookieUtil.parseCookies(loginCookie)

    val loginCookieCN =
      Source.fromInputStream(new FileInputStream(getBasePath + "/cookie_cn")).getLines().mkString("\n")
    cookiesCN = CookieUtil.parseCookies(loginCookieCN)
  }

  override def getBasePath: String = s"testResources/apiTestData/leetcode"
  override def getTestDataPath     = s"${getBasePath}/${getTestName(false)}"

  def testGetFavoriteList(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val leetCodeApi = LeetCodeApi[IO](CodeDojo.LeetCodeCN)
    leetCodeApi.getFavoriteList.map { favoriteList =>
      assertThat(favoriteList.size, not(0))
    }.unsafeRunSync()
  }

  def testTagTypeWithTags(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val leetCodeApiCN = LeetCodeApi[IO](CodeDojo.LeetCodeCN)
//
//    (setCookie(httpClientKeeper.httpClientKeeper) *>
//      leetCodeApiCN.getTagTypeWithTags.map { tagTypeWithTags =>
//        assertThat(tagTypeWithTags.size, not(0))
//      }).unsafeRunSync()

    val leetCodeApi = LeetCodeApi[IO](CodeDojo.LeetCode)
    (setCookie(httpClientKeeper.httpClientKeeper) *>
      leetCodeApi.getTagTypeWithTags.map { tagTypeWithTags =>
        assertThat(tagTypeWithTags.size, not(0))
      }).unsafeRunSync()
  }

  def testSearchChallenges(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val leetCodeCNApi = LeetCodeApi[IO](CodeDojo.LeetCodeCN)
    leetCodeCNApi
      .searchChallenges(0, 50)
      .map { result =>
        assertThat(result.total, not(0))
      }
      .unsafeRunSync()
    val leetCodeApi = LeetCodeApi[IO](CodeDojo.LeetCode)
    (setCookie(httpClientKeeper.httpClientKeeper)
      *>
        leetCodeApi
          .searchChallenges(0, 50)
          .map { result =>
            assertThat(result.total, not(0))
          })
      .unsafeRunSync()
  }
}
