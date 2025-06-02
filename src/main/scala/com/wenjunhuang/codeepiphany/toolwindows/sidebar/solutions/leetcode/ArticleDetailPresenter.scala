package com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode
import cats.effect.{IO, Resource}
import cats.effect.std.Queue
import fs2.Stream.*
import fs2.concurrent.SignallingRef
import org.typelevel.log4cats.LoggerFactory
import scala.concurrent.duration.*

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.leetcode.models.LeetCodeQuestionSolutionArticle
import com.wenjunhuang.codeepiphany.leetcode.services.LeetCodeApi
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.utils.syntax.*

class ArticleDetailPresenter(
  private val myProject: Project,
  private val myCodeDojo: CodeDojo.LeetCodeCN.type | CodeDojo.LeetCode.type
) extends Disposable {

  @volatile
  private var myQueue: Option[Queue[IO, Option[LeetCodeQuestionSolutionArticle]]] = None
  private val myLogger                                                            = LoggerFactory.getLogger[IO]

  createQueryPipeline()
  Disposer.register(myProject, this)

  private def createQueryPipeline(): Unit = {
    eval(
      (Queue.unbounded[IO, Option[LeetCodeQuestionSolutionArticle]], SignallingRef.of[IO, Boolean](false)).parTupled
    ).flatMap { case (queue, initSignal) =>
      resource(
        Resource.make(IO.delay {
          myQueue = Some(queue)
        })(_ =>
          IO.delay {
            myQueue = None
          }
        )
      ).flatMap { _ =>
        fromQueueNoneTerminated(queue)
          .evalMapAccumulate(initSignal) { case (lastSignal, context) =>
            lastSignal.set(true) *>
              SignallingRef
                .of[IO, Boolean](false)
                .map((_, context))
          }
          .debounce(300.millis)
          .evalTap { (signal, context) =>
            eval {
              LeetCodeApi(myCodeDojo, myProject)
                .getSolutionArticle(context.slug)
                .flatMap { article =>
                  IO.delay {
                    article.content
                  }.evalOnEDTDefault()
                }
                .recoverWith { e => myLogger.error(e)(s"Failed to show LeetCode solutions of ${context.slug}") }
            }.interruptWhen(signal).compile.last
          }
      }
    }.compile.drain.unsafeRunAndForget()
  }

  override def dispose(): Unit = {
    myQueue.foreach(_.offer(None).unsafeRunSync())
  }
}
