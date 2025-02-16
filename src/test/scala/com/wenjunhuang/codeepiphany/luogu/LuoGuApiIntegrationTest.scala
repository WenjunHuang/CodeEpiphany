package com.wenjunhuang.codeepiphany.luogu

import cats.effect.IO
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import io.circe.optics.*
import io.circe.parser.*
import java.io.FileInputStream
import java.net.{HttpCookie, URLDecoder}
import org.jsoup.Jsoup
import scala.io.Source

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.net.HttpConfigurable

import com.wenjunhuang.codeepiphany.luogu.services.LuoGuApi
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.http.{HttpClientManager, HttpClientService}
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
        luoGuApi.searchChallenges(None, None, List.empty, None, 1))
        .unsafeRunSync()
    )
    println(luoGuApi.searchChallenges(None, None, List.empty, None, 1).unsafeRunSync())
  }

  def testTransformToHtml(): Unit = {
    val chineseContent =
      Source.fromInputStream(new FileInputStream(getBasePath + "/chinesecontent.json")).getLines().mkString("\n")
    val japaneseContent =
      Source.fromInputStream(new FileInputStream(getBasePath + "/japanesecontent.json")).getLines().mkString("\n")

    def markdownToHtml(markdown: String): String = {
      val options  = new MutableDataSet()
      val parser   = Parser.builder(options).build()
      val renderer = HtmlRenderer.builder(options).build()

      val document = parser.parse(markdown)
      renderer.render(document)
    }

    parse(japaneseContent).map { json =>
      val title =
        s"# ${JsonPath.root.data.problem.pid.string.getOption(json).getOrElse("")} ${JsonPath.root.data.problem.title.string.getOption(json).getOrElse("")}"
      val description =
        s"## 题目描述\n\n${JsonPath.root.data.problem.content.description.string.getOption(json).getOrElse("")}"
      val formatI = s"## 输入格式\n\n${JsonPath.root.data.problem.content.formatI.string.getOption(json).getOrElse("")}"
      val formatO = s"## 输出格式\n\n${JsonPath.root.data.problem.content.formatO.string.getOption(json).getOrElse("")}"
      val hint    = s"## 说明/提示\n\n${JsonPath.root.data.problem.content.hint.string.getOption(json).getOrElse("")}"
      val translation = JsonPath.root.data.problem.translation.string.getOption(json).getOrElse("") match {
        case "" => ""
        case t  => s"## 题意翻译\n\n$t"
      }
      val limits = JsonPath.root.data.problem.limits.time.arr
        .getOption(json)
        .map(_.toList)
        .zip(JsonPath.root.data.problem.limits.memory.arr.getOption(json).map(_.toList))
        .map(it => it._1.zip(it._2))
        .getOrElse(List.empty) match {
        case (time, memory) :: _ =>
          s"## 时间限制\n\n${time.asNumber.map(_.toDouble / 1000).getOrElse(0)}s\n\n## 内存限制\n\n${memory.asNumber.map(_.toDouble / 1024).getOrElse(0)}MB"
        case _ => ""
      }

      val samples =
        JsonPath.root.data.problem.samples.arr
          .getOption(json)
          .map(_.toList)
          .getOrElse(List.empty)
          .zipWithIndex
          .map { case (sample, index) =>
            sample.as[List[String]].getOrElse(List.empty) match
              case input :: output :: Nil =>
                s"## 样例${index + 1}\n\n#### 输入\n\n```\n$input\n```\n\n#### 输出\n\n```\n$output\n```"
              case _ => ""
          }
          .mkString("\n")
      val markdown = s"$title\n$limits\n$description\n$formatI\n$formatO\n$translation\n$samples\n$hint"
      println(markdownToHtml(markdown))

    }

  }

  def testParseACResponse(): Unit = {
    val acHtml =
      Source.fromInputStream(new FileInputStream(getBasePath + "/ACResponse.html")).getLines().mkString("\n")
    val regex = """(?s:.*decodeURIComponent\("(.*)"\).*)""".r
    acHtml match
      case regex(encoded) =>
        println(URLDecoder.decode(encoded, "UTF-8"))
  }
}
