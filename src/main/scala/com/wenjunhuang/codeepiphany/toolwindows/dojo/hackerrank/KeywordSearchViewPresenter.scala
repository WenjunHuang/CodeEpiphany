package com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank

import cats.effect.IO
import cats.effect.std.Queue
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.table.{JBTable, TableView}
import com.wenjunhuang.codeepiphany.hackerrank.model.{ChallengeDetail, Contest}
import com.wenjunhuang.codeepiphany.hackerrank.services.HackerRankApi
import com.wenjunhuang.codeepiphany.hackerrank.services.editor.openChallenge
import com.wenjunhuang.codeepiphany.services.http.{HttpClientKeeper, HttpClientService}
import com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.keys.CHALLENGE_PROVIDER_KEY
import com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.providers.ChallengeProvider
import com.wenjunhuang.codeepiphany.utils.implicits.*
import fs2.Stream
import fs2.concurrent.SignallingRef
import org.typelevel.ci.CIString
import org.typelevel.log4cats.{Logger, LoggerFactory, SelfAwareStructuredLogger}

import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

class KeywordSearchViewPresenter(private val myProject: Project) extends DocumentAdapter with Disposable {
  implicit private val myLogger: Logger[IO] = LoggerFactory[IO].getLogger

  implicit private val httpClientKeeper: HttpClientKeeper[IO] =
    HttpClientService.getInstance(myProject).httpClientKeeper
  private val myApi = HackerRankApi[IO]()

  private val myView: KeywordSearchView = KeywordSearchView(myProject, this)

  @volatile
  private var myQueue: Option[Queue[IO, Option[String]]] = None

  private val mySearchStream = for {
    queue          <- Queue.unbounded[IO, Option[String]]
    _              <- IO.delay { myQueue = Some(queue) }
    notInterrupted <- SignallingRef.of[IO, Boolean](false)
    _ <- Stream
      .fromQueueNoneTerminated(queue)
      .evalMapAccumulate(notInterrupted) { case (signal, keyword) =>
        for {
          _         <- signal.set(true)
          newSignal <- SignallingRef.of[IO, Boolean](false)
        } yield (newSignal, keyword)
      }
      .debounce(200.millis)
      .evalTap { case (signal, keyword) =>
        val masterChallenges = myApi.searchChallengesWithKeyword(Contest.Master, keyword).recoverWith(_ => IO.pure(Nil))
        val eulerChallenges =
          myApi.searchChallengesWithKeyword(Contest.ProjectEuler, keyword).recoverWith(_ => IO.pure(Nil))
        (Stream.evals(masterChallenges) ++ Stream.evals(eulerChallenges)).parEvalMapUnorderedUnbounded {
          case (contest, challenge) =>
            myApi.getChallengeDetail(challenge.challengeSlug, contest).attempt
        }.scan(Nil: List[ChallengeDetail]) {
          case (acc, Right(Some(challenge))) => acc :+ challenge
          case (acc, _)                      => acc
        }.evalTap { challenges =>
          IO.delay {
            updateChallenges(challenges)
          }.evalOnEDTAny()
        }.interruptWhen(signal)
          .attempt
          .evalMap {
            case Left(e) =>
              myLogger.warn(e)("Error while search challenges")
            case _ => IO.unit
          }
          .compile
          .drain
      }
      .onFinalize(myLogger.info("Search by keyword stream finalized"))
      .compile
      .drain
  } yield ()
  mySearchStream.unsafeRunAndForget()

  private val myChallengeProvider = new ChallengeProvider {
    override def openCurrentSelectedChallenge(): Unit =
      Option(myView.getTable.getSelectedObject) match
        case Some(selected) =>
          openChallenge[IO](myProject, selected.slug, Contest.fromCIString(CIString(selected.contestSlug)).get)
            .unsafeRunAndForget()
        case None => ()
  }

  Disposer.register(myProject, this)

  def uiDataSnapshot(dataSink: DataSink): Unit = {
    dataSink.set(CHALLENGE_PROVIDER_KEY, myChallengeProvider)
  }

  def getComponent: JComponent = myView

  private def updateChallenges(challenges: List[ChallengeDetail]): Unit =
    myView.getTableModel.setItems(challenges.asJava)

  override def textChanged(e: DocumentEvent): Unit = {
    val keyword = e.getDocument.getText(0, e.getDocument.getLength)
    if keyword.nonEmpty then myQueue.foreach(_.offer(Some(keyword)).unsafeRunAndForget())

  }

  override def dispose(): Unit =
    myQueue.foreach(_.offer(None).unsafeRunSync())
}
