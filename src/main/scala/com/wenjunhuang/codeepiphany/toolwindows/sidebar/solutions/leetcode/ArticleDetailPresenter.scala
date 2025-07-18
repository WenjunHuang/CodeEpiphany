package com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode
import cats.effect.std.Queue
import cats.effect.{Async, IO}
import cats.syntax.all.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.wenjunhuang.codeepiphany.leetcode.models.LeetCodeQuestionSolutionArticle
import com.wenjunhuang.codeepiphany.leetcode.services.LeetCodeApi
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.syntax.*
import com.wenjunhuang.codeepiphany.utils.{BrowserUtils, CancellableStream}
import fs2.Stream
import org.apache.commons.text.translate.{AggregateTranslator, LookupTranslator, OctalUnescaper, UnicodeUnescaper}
import org.typelevel.log4cats.LoggerFactory

import java.net.URI
import java.util.Collections
import javax.swing.JComponent
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

class ArticleDetailPresenter(
  private val myProject: Project,
  private val myCodeDojo: CodeDojo.LeetCodeCN.type | CodeDojo.LeetCode.type
) extends Disposable {

  @volatile
  private var myQueryQueue: Option[Queue[IO, Option[LeetCodeQuestionSolutionArticle]]] = None
  private val myLogger                                                                 = LoggerFactory.getLogger[IO]
  private val myView = ArticleDetailView(this, myProject)
  
  Disposer.register(myProject, this)
  Disposer.register(this, myView)
  
  createQueryPipeline()

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
    val articleItem = ctx.value
    myLogger.info(s"Processing article: ${articleItem.slug}")
    Stream
      .eval(
        LeetCodeApi(myCodeDojo)
          .getSolutionArticle(articleItem.slug)
          .flatMap { article =>
            IO.delay {
              val content =
                if (myCodeDojo == CodeDojo.LeetCode) ArticleDetailPresenter.UNESCAPE_LEETCODE.translate(article.content)
                else article.content
              myView.setArticleContent(Some((content, myCodeDojo)))
            }.evalOnEDTDefault()
          }
          .evalAsBackgroundProgress(myProject, s"Opening ${myCodeDojo.show} solution article")
          .recoverWith { e =>
            myLogger.error(e)(s"Failed to show ${myCodeDojo.show} solutions of ${articleItem.title}")
          }
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

object ArticleDetailPresenter {
  private val UNESCAPE_LEETCODE = new AggregateTranslator(
    new OctalUnescaper(), // .between('\1', '\377'),
    new UnicodeUnescaper(),
    new LookupTranslator(
      Collections.unmodifiableMap(Map("\\n" -> "\n", "\\\\" -> "\\", "\\\"" -> "\"", "\\'" -> "'").asJava)
    )
  )
}
