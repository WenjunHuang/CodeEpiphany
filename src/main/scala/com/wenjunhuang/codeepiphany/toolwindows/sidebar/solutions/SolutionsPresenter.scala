package com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions

import cats.effect.std.Queue
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.{FileEditorManagerEvent, FileEditorManagerListener}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.components.BorderLayoutPanel
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.newtypes.ChallengeId
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode.LeetCodeSolutionPresenter
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.luogu.LuoGuSolutionPresenter
import com.wenjunhuang.codeepiphany.utils.syntax.*
import com.wenjunhuang.codeepiphany.utils.walkaround.FileEditorManagerListenerBridge
import fs2.Stream
import fs2.concurrent.SignallingRef
import org.typelevel.log4cats.LoggerFactory

import javax.swing.JComponent
import scala.concurrent.duration.*

class SolutionsPresenter(private val myProject: Project) extends Disposable {

  private val myLogger = LoggerFactory.getLogger[IO]
  private val myView   = BorderLayoutPanel()

  // 当前活跃的presenter及其disposable
  private var myCurrentPresenter: Option[SolutionPresenterInfo] = None

  @volatile
  private var myQueue: Option[Queue[IO, Option[(ChallengeId, CodeDojo)]]] = None

  createQueryPipeline()
  Disposer.register(myProject, this)

  // 封装presenter信息的内部类
  private case class SolutionPresenterInfo(presenter: Disposable, view: JComponent, dojo: CodeDojo)

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
                    case (challengeId, dojo) => showSolutions(challengeId, dojo)
                  }).recoverWith { e => myLogger.error(e)("Failed to show solutions") }
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

  /** 统一的解决方案展示方法，根据CodeDojo类型创建对应的presenter
    */
  private def showSolutions(challengeId: ChallengeId, dojo: CodeDojo): IO[Unit] = {
    // 清理当前presenter并创建新的
    IO.delay {
      cleanupCurrentPresenter()
    }.evalOnEDTAny() *> createPresenter(challengeId, dojo)
  }

  /** 清理当前presenter
    */
  private def cleanupCurrentPresenter(): Unit = {
    myCurrentPresenter.foreach { presenterInfo =>
      Disposer.dispose(presenterInfo.presenter)
    }
    myCurrentPresenter = None
    myView.removeAll()
  }

  /** 根据CodeDojo类型创建对应的presenter
    */
  private def createPresenter(challengeId: ChallengeId, dojo: CodeDojo): IO[Unit] = IO.delay {
    val presenterInfo = dojo match {
      case CodeDojo.LeetCodeCN =>
        val presenter = LeetCodeSolutionPresenter(challengeId, myProject, CodeDojo.LeetCodeCN)
        SolutionPresenterInfo(presenter, presenter.getView, dojo)

      case CodeDojo.LeetCode =>
        val presenter = LeetCodeSolutionPresenter(challengeId, myProject, CodeDojo.LeetCode)
        SolutionPresenterInfo(presenter, presenter.getView, dojo)

      case CodeDojo.LuoGu =>
        val presenter = LuoGuSolutionPresenter(challengeId, myProject)
        SolutionPresenterInfo(presenter, presenter.getView, dojo)

      // 为后续添加其他presenter预留位置
      case CodeDojo.AtCoder =>
        // TODO: 添加AtCoderSolutionPresenter
        createEmptyPresenter(dojo)

      case CodeDojo.CodeForces =>
        // TODO: 添加CodeForcesSolutionPresenter
        createEmptyPresenter(dojo)

      case CodeDojo.HackerRank =>
        // TODO: 添加HackerRankSolutionPresenter
        createEmptyPresenter(dojo)
    }

    myView.addToCenter(presenterInfo.view)
    myCurrentPresenter = Some(presenterInfo)
  }.evalOnEDTDefault()

  /** 创建空的presenter（占位符，用于未实现的dojo类型）
    */
  private def createEmptyPresenter(dojo: CodeDojo): SolutionPresenterInfo = {
    import com.intellij.ui.components.JBLabel

    import javax.swing.SwingConstants

    val emptyView = new JBLabel(s"Solutions for ${dojo.show} are not yet implemented", SwingConstants.CENTER)

    val emptyPresenter = new Disposable {
      override def dispose(): Unit = () // 空实现
    }

    SolutionPresenterInfo(emptyPresenter, emptyView, dojo)
  }

  def getView: JComponent = myView

  override def dispose(): Unit = {
    cleanupCurrentPresenter()
    myQueue.foreach(_.offer(None).unsafeRunAndForget())
  }
}
