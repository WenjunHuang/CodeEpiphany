package com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode
import cats.effect.{Async, IO}
import cats.effect.std.Queue
import cats.syntax.all.*
import fs2.Stream
import java.awt.event.{MouseWheelEvent, MouseWheelListener}
import java.net.URI
import javax.swing.JComponent
import org.apache.commons.text.StringEscapeUtils
import org.typelevel.log4cats.LoggerFactory
import scala.concurrent.duration.*

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.leetcode.models.LeetCodeQuestionSolutionArticle
import com.wenjunhuang.codeepiphany.leetcode.services.LeetCodeApi
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.utils.{BrowserUtils, CancellableStream}
import com.wenjunhuang.codeepiphany.utils.syntax.*

class ArticleDetailPresenter(
  private val myProject: Project,
  private val myCodeDojo: CodeDojo.LeetCodeCN.type | CodeDojo.LeetCode.type
) extends Disposable {

  @volatile
  private var myQueryQueue: Option[Queue[IO, Option[LeetCodeQuestionSolutionArticle]]] = None
  private val myLogger                                                                 = LoggerFactory.getLogger[IO]
  private val myView = ArticleDetailView(this, myProject)

  createQueryPipeline()
  Disposer.register(myProject, this)
  Disposer.register(this, myView)

  def setArticle(article: LeetCodeQuestionSolutionArticle): Unit = {
    myQueryQueue.foreach(_.offer(Some(article)).unsafeRunSync())
  }

  private def createQueryPipeline(): Unit = {
    CancellableStream
      .setup[LeetCodeQuestionSolutionArticle, Unit](300.millis)(processQuery)
      .evalMap(queue =>
        IO.delay {
          myQueryQueue = Some(queue)
        }.evalOnEDTDefault()
      )
      .use(_ => IO.never)
      .unsafeRunAndForget()
  }

  private def processQuery(ctx: CancellableStream.StreamContext[LeetCodeQuestionSolutionArticle]): IO[Unit] = {
    val article = ctx.value
    myLogger.info(s"Processing article: ${article.slug}")
    Stream
      .eval(
        LeetCodeApi(myCodeDojo, myProject)
          .getSolutionArticle(article.slug)
          .flatMap { article =>
            IO.delay {
              val content = StringEscapeUtils.unescapeJava(article.content)
              myView.setArticleContent(Some((content, myCodeDojo)))
            }.evalOnEDTDefault()
          }
          .recoverWith { e => myLogger.error(e)(s"Failed to show LeetCode solutions of ${article.slug}") }
      )
      .interruptWhen(ctx.signal)
      .compile
      .drain
  }

  /** Handle user clicked a link in the description view browser
    */
  def userClickedLink[F[_]: Async](url: String): F[Unit] =
    Async[F].delay(URI.create(url)).flatMap(uri => BrowserUtils.browseURI(uri, myProject))

  override def dispose(): Unit = {
    myQueryQueue.foreach(_.offer(None).unsafeRunSync())
  }

  def getView: JComponent = myView
}
