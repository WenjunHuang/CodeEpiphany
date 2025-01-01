package com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank

import cats.effect.IO
import cats.effect.std.Queue
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.table.JBTable
import com.wenjunhuang.codeepiphany.hackerrank.model.{ChallengeDetail, Contest}
import com.wenjunhuang.codeepiphany.hackerrank.services.HackerRankApi
import com.wenjunhuang.codeepiphany.services.http.{HttpClientKeeper, HttpClientService}
import com.wenjunhuang.codeepiphany.utils.implicits.*
import fs2.Stream
import fs2.concurrent.SignallingRef
import org.typelevel.log4cats.LoggerFactory

import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

class KeywordSearchViewPresenter(private val myProject: Project) extends DocumentAdapter with Disposable {
  private val myLogger = LoggerFactory[IO].getLogger

  implicit private val httpClientKeeper: HttpClientKeeper[IO] = HttpClientService.getInstance(myProject).httpClientKeeper
  private val myApi                                           = HackerRankApi[IO]()

  private val myView                 = KeywordSearchView(myProject, this)
  private val myChallengesTableModel = ChallengesTableModel()
  private val myChallengesTable      = myChallengesTableModel.createTableView(uiDataSnapshot)

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
        val eulerChallenges  = myApi.searchChallengesWithKeyword(Contest.ProjectEuler, keyword).recoverWith(_ => IO.pure(Nil))
        (Stream.evals(masterChallenges) ++ Stream.evals(eulerChallenges)).parEvalMapUnorderedUnbounded { case (contest, challenge) =>
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

  Disposer.register(myProject, this)

  def getTableView: JBTable = myChallengesTable

  def uiDataSnapshot(dataSink: DataSink): Unit = {}

  def getComponent: JComponent = myView

  def updateChallenges(challenges: List[ChallengeDetail]): Unit =
    myChallengesTableModel.setItems(challenges.asJava)

  override def textChanged(e: DocumentEvent): Unit = {
    val keyword = e.getDocument.getText(0, e.getDocument.getLength)
    if keyword.nonEmpty then myQueue.foreach(_.offer(Some(keyword)).unsafeRunAndForget())

  }

  override def dispose(): Unit =
    myQueue.foreach(_.offer(None).unsafeRunSync())
}
