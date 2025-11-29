package integration

import cats.effect.IO
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.net.HttpConfigurable
import com.wenjunhuang.codeepiphany.leetcode.services.LeetCodeApi
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.CodeDojo.{LeetCode, LeetCodeCN}
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.utils.CookieUtil
import com.wenjunhuang.codeepiphany.utils.syntax.*
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat

import java.io.FileInputStream
import java.net.HttpCookie
import scala.io.Source

class LeetcodeApiIntegrationTest extends BasePlatformTestCase {

  private var cookies: List[HttpCookie]   = Nil
  private var cookiesCN: List[HttpCookie] = Nil

  private def setCookie(): IO[Unit] = {
    HttpClientManager.updateCookiesForHost(CodeDojo.LeetCode.domain, cookies) *>
      HttpClientManager.updateCookiesForHost(CodeDojo.LeetCodeCN.domain, cookiesCN)
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
    (HttpClientManager.clearCookiesForHost(CodeDojo.LeetCode.domain) *> HttpClientManager.clearCookiesForHost(
      CodeDojo.LeetCodeCN.domain
    )).unsafeRunSync()
    super.tearDown()
  }

  override def getBasePath: String = s"testResources/apiTestData/leetcode"
  override def getTestDataPath     = s"${getBasePath}/${getTestName(false)}"

  def testGetFavoriteList(): Unit = {
    val leetCodeApi = LeetCodeApi(CodeDojo.LeetCodeCN)
    leetCodeApi.getFavoriteList.map { favoriteList =>
      assertThat(favoriteList.size, not(0))
    }.unsafeRunSync()
  }

  def testTagTypeWithTags(): Unit = {
    val leetCodeApiCN = LeetCodeApi(CodeDojo.LeetCodeCN)
//
//    (setCookie(httpClientKeeper.httpClientKeeper) *>
//      leetCodeApiCN.getTagTypeWithTags.map { tagTypeWithTags =>
//        assertThat(tagTypeWithTags.size, not(0))
//      }).unsafeRunSync()

    val leetCodeApi = LeetCodeApi(CodeDojo.LeetCode)
    (setCookie() *>
      leetCodeApi.getTagTypeWithTags.map { tagTypeWithTags =>
        assertThat(tagTypeWithTags.size, not(0))
      }).unsafeRunSync()
  }

  def testSearchChallenges(): Unit = {
    val leetCodeCNApi = LeetCodeApi(CodeDojo.LeetCodeCN)
    leetCodeCNApi
      .searchChallenges(0, 50, None, None, None, None, Nil, None)
      .map { result =>
        assertThat(result.total, not(0))
      }
      .unsafeRunSync()
    val leetCodeApi = LeetCodeApi(CodeDojo.LeetCode)
    (setCookie()
      *>
        leetCodeApi
          .searchChallenges(0, 50, None, None, None, None, Nil, None)
          .map { result =>
            assertThat(result.total, not(0))
          })
      .unsafeRunSync()
  }

  def testCheckLogin(): Unit = {
    val leetCodeCNApi = LeetCodeApi(LeetCodeCN)
    val leetCodeApi   = LeetCodeApi(LeetCode)

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

    setCookie().unsafeRunSync()
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
    val leetCodeCNApi = LeetCodeApi(LeetCodeCN)
    val leetCodeApi   = LeetCodeApi(LeetCode)

    leetCodeCNApi.getUserInfo.map { userInfo =>
      assertThat(userInfo.userSlug, is(None))
    }.unsafeRunSync()
    leetCodeApi.getUserInfo.map { userInfo =>
      assertThat(userInfo.userSlug, is(None))
    }.unsafeRunSync()

    setCookie().unsafeRunSync()
    leetCodeCNApi.getUserInfo.map { userInfo =>
      assertThat(userInfo.userSlug, not(None))
    }.unsafeRunSync()
    leetCodeApi.getUserInfo.map { userInfo =>
      assertThat(userInfo.userSlug, not(None))
    }.unsafeRunSync()
  }

  def testGetQuestion(): Unit = {
    val leetCodeCNApi = LeetCodeApi(LeetCodeCN)
    val leetCodeApi   = LeetCodeApi(LeetCode)

    println(leetCodeCNApi.getQuestionData("median-of-two-sorted-arrays").unsafeRunSync())
  }

  def testGetCompanyTags(): Unit = {
//    val leetCodeCNApi = LeetCodeApi[IO](LeetCodeCN)
//    println(leetCodeCNApi.getCompanyTags.unsafeRunSync())

    val leetCodeApi = LeetCodeApi(LeetCode)
    println(leetCodeApi.getCompanyTags.unsafeRunSync())
  }

  def testGetQuestionCompanyTags(): Unit = {

    val leetCodeApi = LeetCodeApi(LeetCode)
    println(leetCodeApi.getQuestionCompanyTags.unsafeRunSync())
  }
  def testGetPositionTags(): Unit = {
//    val leetCodeCNApi = LeetCodeApi[IO](LeetCodeCN)
//    println(leetCodeCNApi.getPositionTags.unsafeRunSync())

    val leetCodeApi = LeetCodeApi(LeetCode)
    println(leetCodeApi.getPositionTags.unsafeRunSync())
  }

  def testGetCompanyQuestions(): Unit = {
    val leetCodeCNApi = LeetCodeApi(LeetCodeCN)
    println(
      (setCookie() *> leetCodeCNApi
        .searchCompanyChallenges(0, 20, Some("thirty-days"), List("bytedance"), Nil, None, None, Nil, None))
        .unsafeRunSync()
    )
//    val leetCodeApi = LeetCodeApi[IO](LeetCode)
//    println(
//      (setCookie(httpClientManager) *> leetCodeApi
//        .searchCompanyChallenges(0, 20, List("facebook"), Nil, None, None, None))
//        .unsafeRunSync()
//    )

  }

  def testGetSolutionTags(): Unit = {
    val leetCodeCNApi = LeetCodeApi(LeetCodeCN)
    println(
      (setCookie() *>
        leetCodeCNApi
          .getSolutionTags("two-sum"))
        .unsafeRunSync()
    )
    //    val leetCodeApi = LeetCodeApi[IO](LeetCode)
    //    println(
    //      (setCookie(httpClientManager) *> leetCodeApi
    //        .searchCompanyChallenges(0, 20, List("facebook"), Nil, None, None, None))
    //        .unsafeRunSync()
    //    )
  }
}
