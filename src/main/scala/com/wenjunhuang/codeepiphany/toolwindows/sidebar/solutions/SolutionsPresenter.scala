package com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions

import cats.effect.{ IO, Resource }
import cats.effect.std.Queue
import cats.syntax.all.*
import fs2.Stream
import fs2.concurrent.SignallingRef
import javax.swing.JComponent
import org.typelevel.log4cats.LoggerFactory
import scala.concurrent.duration.*

import com.intellij.openapi.fileEditor.{ FileEditorManagerEvent, FileEditorManagerListener }
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.components.BorderLayoutPanel

import com.wenjunhuang.codeepiphany.model.newtypes.ChallengeId
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientManager, HttpClientService }
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode.LeetCodeSolutionPresenter
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

  Disposer.register(myProject, this)

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
  ): IO[Unit] = IO.delay {
    val leetCodePresenter = LeetCodeSolutionPresenter(challengeId, myProject, codeDojo)
    myView.removeAll()
    myView.addToCenter(leetCodePresenter.getView)
    ()
  }.evalOnEDTDefault()

  def getView: JComponent = myView

  override def dispose(): Unit = {
    Option(myView.getParent).foreach(_.remove(myView))
    myQueue.foreach(_.offer(None).unsafeRunAndForget())
  }
}
