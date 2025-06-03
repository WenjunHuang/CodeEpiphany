package com.wenjunhuang.codeepiphany.atcoder

import cats.effect.IO
import java.io.FileInputStream
import java.net.HttpCookie
import scala.io.Source

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.net.HttpConfigurable

import com.wenjunhuang.codeepiphany.atcoder.services.AtCoderApi
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.http.{HttpClientManager, HttpClientService}
import com.wenjunhuang.codeepiphany.utils.CookieUtil
import com.wenjunhuang.codeepiphany.utils.syntax.*

class AtCoderApiIntegrationTest extends BasePlatformTestCase {
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
  override def getBasePath: String = s"testResources/apiTestData/atcoder"

  override def getTestDataPath = s"${getBasePath}/${getTestName(false)}"
  def testGetAllContests(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    AtCoderApi[IO].getAllContests.map { contests =>
      assert(contests.nonEmpty)
    }.unsafeRunSync()
  }
}
