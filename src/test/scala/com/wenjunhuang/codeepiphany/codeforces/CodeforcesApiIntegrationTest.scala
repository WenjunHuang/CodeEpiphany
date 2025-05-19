package com.wenjunhuang.codeepiphany.codeforces

import cats.effect.IO
import java.io.FileInputStream
import java.net.HttpCookie
import junit.framework.TestCase.fail
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import scala.io.Source

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.net.HttpConfigurable

import com.wenjunhuang.codeepiphany.codeforces.services.CodeForcesApi
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.http.{HttpClientManager, HttpClientService}
import com.wenjunhuang.codeepiphany.utils.CookieUtil
import com.wenjunhuang.codeepiphany.utils.implicits.*

class CodeforcesApiIntegrationTest extends BasePlatformTestCase {
  private var cookies: List[HttpCookie] = Nil

  private def setCookie(httpClientKeeper: HttpClientManager[IO]): IO[Unit] = {
    httpClientKeeper.updateCookiesForHost(CodeDojo.CodeForces.domain, cookies)
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

  override def getBasePath: String = s"testResources/apiTestData/codeforces"

  override def getTestDataPath = s"${getBasePath}/${getTestName(false)}"

  def testGetProblemSets(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val api = CodeForcesApi[IO]
    api.getAllProblemSets.flatMap { problems =>
      IO.delay {
        assertThat(problems.size, not(0))
        println(problems.size)
      }
    }.unsafeRunSync()
  }

  def testCheckLogin(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val api = CodeForcesApi[IO]
    (setCookie(httpClientKeeper.httpClientManager)
      *>
        api
          .checkLogin()).flatMap { result =>
      IO.delay {
        assertThat(result, is(true))
      }
    }
      .unsafeRunSync()
  }

  def testGetTags(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val api = CodeForcesApi[IO]
    (setCookie(httpClientKeeper.httpClientManager)
      *>
      api
        .getProblemTags).flatMap { result =>
        IO.delay {
          assertThat(result.size, not(0))
        }
      }
      .unsafeRunSync()
  }

  def testGetChallengeData(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val api = CodeForcesApi[IO]
    (setCookie(httpClientKeeper.httpClientManager)
      *>
      api
        .getChallengeData(None,2063,"F2")).flatMap { result =>
        IO.delay {
          result match
            case None => fail("No challenge data found")
            case Some(r) =>
              println(r)
              assertThat(r.description,notNullValue())
        }
      }
      .unsafeRunSync()
  }
}
