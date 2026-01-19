package com.wenjunhuang.codeepiphany.toolwindows.sidebar.description

import cats.effect.kernel.Resource.ExitCase
import cats.effect.std.Queue
import cats.effect.{ Async, IO }
import cats.syntax.all.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.{ FileEditorManagerEvent, FileEditorManagerListener }
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.wenjunhuang.codeepiphany.database.Tables.CHALLENGE
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.newtypes.ChallengeId
import com.wenjunhuang.codeepiphany.services.ChallengeRepository
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.settings.dojo.BaseCodeDojoSettings
import com.wenjunhuang.codeepiphany.utils.BrowserUtils
import com.wenjunhuang.codeepiphany.utils.syntax.*
import com.wenjunhuang.codeepiphany.utils.walkaround.FileEditorManagerListenerBridge
import fs2.Stream
import org.typelevel.log4cats.LoggerFactory

import java.net.URI
import scala.concurrent.duration.*
import scala.jdk.OptionConverters.*

class ChallengeDescriptionPresenter(private val myProject: Project) extends Disposable {
  private val logger            = Logger.getInstance(getClass)
  private val myLogger          = LoggerFactory.getLogger[IO]
  private val myDescriptionView = ChallengeDescriptionView(this, myProject)

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
                  setChallenge(Some((ChallengeId(challenge.challengeId), challenge.dojo)))
                case None =>
                  setChallenge(None)
            case None =>
              setChallenge(None)
          }
        }
      }
    )

  @volatile
  private var myQueue: Option[Queue[IO, Option[(ChallengeId, CodeDojo)]]] = None

  private val myCancelToken = (for {
    queue <- Queue.unbounded[IO, Option[(ChallengeId, CodeDojo)]]
    _     <- IO.delay { myQueue = Option(queue) }
    _ <- Stream
      .fromQueueUnterminated(queue)
      .debounce(200.millis)
      .evalTap {
        case Some((challengeId, dojo)) =>
          val repository = ChallengeRepository.getInstance(myProject)
          repository.getDSLContextResource.use { dsl =>
            IO.blocking {
              dsl
                .selectFrom(CHALLENGE)
                .where(CHALLENGE.ID.eq(challengeId.value))
                .fetchOptional()
                .toScala
                .map { _.getDescription }
            }.flatMap { record =>
              IO.delay {
                record.map { description =>
                  val customCSS = BaseCodeDojoSettings.getInstance(myProject, dojo).getState.descriptionCSS
                  myDescriptionView.setDescription(Some(description, customCSS, dojo))
                }
              }.evalOnEDTDefault()
            }.void
          }.handleErrorWith { e =>
            myLogger.warn(e)("Error while fetching challenge description")
          }
        case None =>
          IO.delay { myDescriptionView.setDescription(None) }
            .evalOnEDTDefault()
            .handleErrorWith { e =>
              myLogger.warn(e)("Error while fetching challenge description")
            }
      }
      .onFinalizeCase {
        case ExitCase.Canceled =>
          myLogger.debug("Description presenter stream canceled")
        case _ => IO.unit
      }
      .compile
      .drain
  } yield ()).unsafeRunCancelable()

  Disposer.register(myProject, this)

  /** Handle user clicked a link in the description view browser
    */
  def userClickedLink[F[_]: Async](url: String): F[Unit] =
    Async[F].delay(URI.create(url)).flatMap(uri => BrowserUtils.browseURI(uri, myProject))

  private def setChallenge(challenge: Option[(ChallengeId, CodeDojo)]): Unit = {
    if myQueue.isEmpty then logger.info("Queue is not initialized")
    else
      logger.info("Queue is initialized")
      myQueue.foreach(_.offer(challenge).unsafeRunAndForget())
  }

  def getView: ChallengeDescriptionView = myDescriptionView

  override def dispose(): Unit = {
    logger.debug("DescriptionPresenter disposed")
    myCancelToken()
  }
}

object ChallengeDescriptionPresenter {}
