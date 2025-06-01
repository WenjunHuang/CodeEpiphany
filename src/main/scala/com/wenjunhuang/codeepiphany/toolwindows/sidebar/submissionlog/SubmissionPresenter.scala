package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog

import cats.effect.{IO, Resource}
import cats.effect.std.Queue
import cats.syntax.all.*
import fs2.concurrent.SignallingRef
import fs2.Stream
import javax.swing.event.ListSelectionListener
import javax.swing.JComponent
import org.typelevel.ci.CIString
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.hackerrank.models.HackerRankContest
import com.wenjunhuang.codeepiphany.model.{CodeDojo, Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.model.newtypes.SubmissionId
import com.wenjunhuang.codeepiphany.model.CodeDojo.*
import com.wenjunhuang.codeepiphany.services.ChallengeRepository
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionLogPresenter.SubmissionType
import com.wenjunhuang.codeepiphany.utils.implicits.*

class SubmissionPresenter(private val myProject: Project) extends Disposable {
  private val mySubmissionLogPresenter = new SubmissionLogPresenter(myProject)
  private val myView                   = SubmissionView(mySubmissionLogPresenter.getViewComponent)
  private val myLogger                 = Logger.getInstance(getClass)
  @volatile
  private var mySelectedSubmissionQueue: Option[Queue[IO, Option[SubmissionLogEntry]]] = None

  private val mySelectedSubmissionCanceller =
    Stream
      .eval((Queue.unbounded[IO, Option[SubmissionLogEntry]], SignallingRef.of[IO, Boolean](false)).parTupled)
      .flatMap { case (queue, initSignal) =>
        Stream
          .resource(
            Resource
              .make(IO.delay { mySelectedSubmissionQueue = Some(queue) }) { _ =>
                IO.delay { mySelectedSubmissionQueue = None }
              }
          )
          .flatMap { _ =>
            Stream
              .fromQueueUnterminated(queue)
              // when a new query is received, we need to cancel the ongoing query to dump the old results
              .evalMapAccumulate(initSignal) { case (signal, state) =>
                for {
                  _         <- signal.set(true)
                  newSignal <- SignallingRef.of[IO, Boolean](false)
                } yield (newSignal, state)
              }
              .debounce(200.millis)
              .evalTap { (signal, state) =>
                state match
                  case Some(selected) =>
                    Stream
                      .eval(fetchSubmission(SubmissionId(selected.id)))
                      .evalTap {
                        case Some(submission) =>
                          IO.delay {
                            myView.setDetail(submission)
                          }.evalOnEDTAny()
                        case None =>
                          IO.delay {
                            myView.setDetailEmpty()
                          }.evalOnEDTAny()
                      }
                      .interruptWhen(signal)
                      .attempt
                      .compile
                      .drain
                  case None =>
                    IO.delay {
                      myView.setDetailEmpty()
                    }.evalOnEDTAny()
              }
          }
      }
      .compile
      .drain
      .unsafeRunCancelable()

  mySubmissionLogPresenter.getQueryResultTableSelectionModel.addListSelectionListener { e =>
    if !e.getValueIsAdjusting then
      mySubmissionLogPresenter.getQueryResultTableSelectionModel.getMinSelectionIndex match
        case selectedIndex if selectedIndex >= 0 =>
          val submission = mySubmissionLogPresenter.getQueryResultTableModel.getItem(selectedIndex)
          mySelectedSubmissionQueue.foreach(_.offer(Some(submission)).unsafeRunAndForget())
        case _ =>
  }

  private def fetchSubmission(submissionId: SubmissionId): IO[Option[SubmissionType]] = {
    ChallengeRepository
      .getInstance(myProject)
      .getDSLContextResource[IO]
      .use { dsl =>
        IO.delay {
          dsl
            .select((SOLUTION_SUBMISSION.fields() ++ CHALLENGE_LANGUAGE.fields() ++ CHALLENGE.fields())*)
            .from(SOLUTION_SUBMISSION)
            .innerJoin(CHALLENGE_LANGUAGE)
            .on(SOLUTION_SUBMISSION.CHALLENGELANGUAGEID.eq(CHALLENGE_LANGUAGE.ID))
            .innerJoin(CHALLENGE)
            .on(CHALLENGE_LANGUAGE.CHALLENGEID.eq(CHALLENGE.ID))
            .where(SOLUTION_SUBMISSION.ID.eq(submissionId.value))
            .fetchOptional()
            .toScala
            .flatMap { record =>
              val language        = Language.fromCIString(CIString(record.get(CHALLENGE_LANGUAGE.LANGUAGE)))
              val languageVersion = LanguageVersion.fromString(record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION))
              val codeDojo        = CodeDojo.fromCIString(CIString(record.get(CHALLENGE.DOJO)))
              (language, codeDojo).mapN { (lang, dojo) =>
                val submissionRecord = record.into(SOLUTION_SUBMISSION)
                dojo match {
                  case dojo @ (LeetCode | LeetCodeCN) =>
                    dsl
                      .selectFrom(LEETCODE_SUBMISSION)
                      .where(LEETCODE_SUBMISSION.ID.eq(submissionId.value))
                      .fetchOptional()
                      .toScala
                      .map { leetcodeSubmission =>
                        dojo match {
                          case LeetCode =>
                            SubmissionType.LeetCodeSubmission(
                              lang,
                              languageVersion,
                              record.get(CHALLENGE.SLUG),
                              submissionRecord,
                              leetcodeSubmission
                            )
                          case LeetCodeCN =>
                            SubmissionType.LeetCodeCNSubmission(
                              lang,
                              languageVersion,
                              record.get(CHALLENGE.SLUG),
                              submissionRecord,
                              leetcodeSubmission
                            )
                        }
                      }
                  case HackerRank =>
                    val hackerCases = dsl
                      .selectFrom(HACKERRANK_SUBMISSION_CASE)
                      .where(HACKERRANK_SUBMISSION_CASE.SUBMISSIONID.eq(submissionId.value))
                      .fetch()
                      .asScala
                      .toList
                    val contest = dsl
                      .select(HACKERRANK_CHALLENGE.CONTEST)
                      .from(HACKERRANK_CHALLENGE)
                      .where(HACKERRANK_CHALLENGE.ID.eq(record.get(CHALLENGE.ID)))
                      .fetchOne()
                      .component1()
                    val contestSlug = HackerRankContest.fromCIString(CIString(contest)).flatMap {
                      case HackerRankContest.Master           => None
                      case p @ HackerRankContest.ProjectEuler => Some(p.slug)
                    }

                    Some(
                      SubmissionType.HackerRankSubmission(
                        lang,
                        languageVersion,
                        record.get(CHALLENGE.SLUG),
                        contestSlug,
                        submissionRecord,
                        hackerCases
                      )
                    )
                  case CodeForces =>
                    dsl
                      .selectFrom(CODEFORCES_CHALLENGE)
                      .where(CODEFORCES_CHALLENGE.ID.eq(record.get(CHALLENGE.ID)))
                      .fetchOptional()
                      .toScala
                      .map { codeForcesChallenge =>
                        SubmissionType.CodeForcesSubmission(
                          lang,
                          languageVersion,
                          submissionRecord,
                          codeForcesChallenge.getContestid,
                          Option(codeForcesChallenge.getProblemsetname)
                        )
                      }
                  case AtCoder =>
                    dsl
                      .selectFrom(ATCODER_CHALLENGE)
                      .where(ATCODER_CHALLENGE.ID.eq(record.get(CHALLENGE.ID)))
                      .fetchOptional()
                      .toScala
                      .map { atCoderChallenge =>
                        SubmissionType.AtCoderSubmission(
                          lang,
                          languageVersion,
                          submissionRecord,
                          atCoderChallenge.getContestid,
                          record.get(CHALLENGE.DOJOID)
                        )
                      }
                  case LuoGu =>
                    SubmissionType.LuoGuSubmission(lang, languageVersion, submissionRecord).some
                }
              }.flatten
            }
        }
      }
  }

  def getView: JComponent = myView.getComponent

  override def dispose(): Unit = {
    mySelectedSubmissionCanceller()
  }
}
