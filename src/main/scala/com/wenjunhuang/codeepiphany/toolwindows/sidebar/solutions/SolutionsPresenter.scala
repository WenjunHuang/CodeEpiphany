package com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions

import cats.effect.{ IO, Resource }
import cats.effect.std.Queue
import cats.syntax.all.*
import fs2.Stream
import fs2.concurrent.SignallingRef
import javax.swing.JComponent
import org.typelevel.log4cats.LoggerFactory
import scala.concurrent.duration.*
import scala.jdk.OptionConverters.*

import com.intellij.openapi.fileEditor.{ FileEditorManagerEvent, FileEditorManagerListener }
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.ui.components.BorderLayoutPanel

import com.wenjunhuang.codeepiphany.database.Tables.CHALLENGE
import com.wenjunhuang.codeepiphany.leetcode.services.LeetCodeApi
import com.wenjunhuang.codeepiphany.model.newtypes.ChallengeId
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.{ AuthService, ChallengeRepository }
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientManager, HttpClientService }
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode.LeetCodeSolutionArticlesPresenter
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.walkaround.FileEditorManagerListenerBridge

class SolutionsPresenter(private val myProject: Project) extends Disposable {
  private implicit val httpClientManager: HttpClientManager[IO] =
    HttpClientService.getInstance(myProject).httpClientManager

  private val myLogger = LoggerFactory.getLogger[IO]
  @volatile
  private var myQueue: Option[Queue[IO, Option[(ChallengeId, CodeDojo)]]] = None

  private val myView = BorderLayoutPanel()

  createQueryPipeline()

  private def createQueryPipeline(): Unit = {
    Stream
      .eval((Queue.unbounded[IO, Option[(ChallengeId, CodeDojo)]], SignallingRef.of[IO, Boolean](false)).parTupled)
      .flatMap { case (queue, initSignal) =>
        Stream
          .resource(Resource.make(IO.delay {
            myQueue = Some(queue)
          })(_ => IO.delay { myQueue = None }))
          .flatMap { _ =>
            Stream
              .fromQueueNoneTerminated(queue)
              .evalMapAccumulate(initSignal) { case (lastSignal, context) =>
                lastSignal.set(true) *>
                  SignallingRef
                    .of[IO, Boolean](false)
                    .map((_, context))
              }
              .debounce(300.millis)
              .evalTap { (signal, context) =>
                Stream.eval {
                  (context match {
                    case (challengeId, CodeDojo.LeetCodeCN) =>
                      showLeetCodeSolutions(challengeId, CodeDojo.LeetCodeCN)
                    case (challengeId, CodeDojo.LeetCode) =>
                      showLeetCodeSolutions(challengeId, CodeDojo.LeetCode)
                    case _ =>
                      IO.unit
                  }).recoverWith { e => myLogger.error(e)("Failed to show LeetCode solutions") }
                }.interruptWhen(signal).compile.last
              }
          }
      }
      .compile
      .drain
      .unsafeRunAndForget()

    myProject.getMessageBus
      .connect(this)
      .subscribe(
        FileEditorManagerListener.FILE_EDITOR_MANAGER,
        new FileEditorManagerListenerBridge {
          override def selectionChanged(event: FileEditorManagerEvent): Unit = {
            Option(event.getNewFile) match {
              case Some(vf) =>
                val settings = ChallengeSettings.getInstance(myProject)
                settings.findChallengeId(vf) match
                  case Some(challenge) =>
                    myQueue
                      .foreach(_.offer(Some((ChallengeId(challenge.challengeId), challenge.dojo))).unsafeRunAndForget())
                  case None =>
              case None =>
            }
          }
        }
      )
  }

  private def showLeetCodeSolutions(
    challengeId: ChallengeId,
    codeDojo: CodeDojo.LeetCodeCN.type | CodeDojo.LeetCode.type
  ): IO[Unit] = {
    // Implement the logic to fetch and display LeetCode solutions
    if (
      AuthService
        .getInstance(myProject)
        .isLoggedIn(codeDojo)
    ) {
      ChallengeRepository
        .getInstance(myProject)
        .getDSLContextResource[IO]
        .use { dslContext =>
          IO.delay {
            dslContext
              .select(CHALLENGE.SLUG)
              .from(CHALLENGE)
              .where(CHALLENGE.ID.eq(challengeId.value))
              .fetchOptional()
              .toScala
              .map(_.value1())
              .getOrElse(throw new NoSuchElementException(s"No challenge found for ID: ${challengeId.value}"))
          }
        }
        .flatMap { questionSlug =>
          for
            solutionTags <- LeetCodeApi[IO](codeDojo).getSolutionTags(questionSlug)
            presenter <- IO.delay {
              val presenter = LeetCodeSolutionArticlesPresenter(
                myProject,
                LeetCodeSolutionArticlesPresenter.BootstrapParameters(questionSlug, solutionTags),
                codeDojo
              )
              myView.addToCenter(presenter.getViewComponent)
            }.evalOnEDTDefault()
          yield ()
        }
    } else {
      IO.unit
    }
  }

  def getView: JComponent = myView

  override def dispose(): Unit = myQueue.foreach(_.offer(None).unsafeRunAndForget())
}
