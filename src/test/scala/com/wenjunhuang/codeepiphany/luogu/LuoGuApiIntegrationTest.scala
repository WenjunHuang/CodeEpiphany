package com.wenjunhuang.codeepiphany.luogu

import cats.effect.IO
import java.io.FileInputStream
import java.net.HttpCookie
import scala.io.Source

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.net.HttpConfigurable

import com.wenjunhuang.codeepiphany.luogu.services.LuoGuApi
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientManager, HttpClientService }
import com.wenjunhuang.codeepiphany.utils.CookieUtil
import com.wenjunhuang.codeepiphany.utils.implicits.*

class LuoGuApiIntegrationTest extends BasePlatformTestCase {
  private var cookies: List[HttpCookie] = Nil

  private def setCookie(httpClientKeeper: HttpClientManager[IO]): IO[Unit] = {
    httpClientKeeper.updateCookiesForHost(CodeDojo.LuoGu.domain, cookies)
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

  override def getBasePath: String = s"testResources/apiTestData/luogu"

  override def getTestDataPath = s"${getBasePath}/${getTestName(false)}"

  def testSearchChallenges(): Unit = {
    implicit val httpClientKeeper = HttpClientService.getInstance(getProject).httpClientManager
    val luoGuApi                  = LuoGuApi[IO]()
    println(
      (setCookie(httpClientKeeper) *>
        luoGuApi.searchChallenges(None, None, List.empty, None,1))
        .unsafeRunSync()
    )
    println(luoGuApi.searchChallenges(None, None, List.empty,None, 1).unsafeRunSync())
  }
}
