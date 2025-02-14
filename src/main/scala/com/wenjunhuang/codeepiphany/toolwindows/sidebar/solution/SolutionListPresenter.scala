package com.wenjunhuang.codeepiphany.toolwindows.sidebar.solution

import cats.effect.IO
import cats.effect.std.Queue
import fs2.Stream
import java.time.LocalDateTime
import javax.swing.{JComponent, JTree}
import javax.swing.tree.DefaultMutableTreeNode
import org.jooq.impl.DSL
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

import com.intellij.openapi.fileEditor.{FileEditorManager, FileEditorManagerEvent, FileEditorManagerListener}
import com.intellij.openapi.project.Project
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.treeStructure.treetable.{ListTreeTableModelOnColumns, TreeColumnInfo}
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.util.ui.ColumnInfo

import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.newtypes.ChallengeId
import com.wenjunhuang.codeepiphany.services.ChallengeRepository
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.IdGenerator
import com.wenjunhuang.codeepiphany.utils.walkaround.FileEditorManagerListenerBridge
import com.wenjunhuang.codeepiphany.vfs.{SolutionRemarkFile, SolutionRemarkFileSystem}

class SolutionListPresenter(val myProject: Project) extends Disposable {
  private val myLogger = LoggerFactory[IO].getLogger
  private val myColumns = Array(
    new TreeColumnInfo("Title"),
    new ColumnInfo[DefaultMutableTreeNode, Int]("Submissions") {
      override def valueOf(item: DefaultMutableTreeNode): Int = item.getUserObject match
        case SolutionEntry.SolutionNode(_, _, submissions, _)  => submissions
        case SolutionEntry.LanguageNode(_, _, submissionCount) => submissionCount
    }
  )

  val myTreeRender: ColoredTreeCellRenderer = new ColoredTreeCellRenderer {
    override def customizeCellRenderer(
      tree: JTree,
      value: AnyRef,
      selected: Boolean,
      expanded: Boolean,
      leaf: Boolean,
      row: Int,
      hasFocus: Boolean
    ): Unit = {
      value match
        case node: DefaultMutableTreeNode =>
          node.getUserObject match {
            case SolutionEntry.SolutionNode(_, title, _, _) =>
              append(title)
            case SolutionEntry.LanguageNode(language, version, _) =>
              setIcon(language.icon)
              append(s"${language.show} ${version.version}")
            case _ =>
          }
        case _ =>
    }
  }

  myProject.getMessageBus
    .connect(this)
    .subscribe(
      FileEditorManagerListener.FILE_EDITOR_MANAGER,
      new FileEditorManagerListenerBridge {
        override def selectionChanged(event: FileEditorManagerEvent): Unit = {
          Option(event.getNewFile) match {
            case Some(vf: SolutionRemarkFile) if !mySelectedChallenge.exists(_._1 == vf.myPath.challengeId) =>
              setChallenge(Some((vf.myPath.challengeId, vf.myPath.codeDojo)))
            case Some(vf) =>
              val settings = ChallengeSettings.getInstance(myProject)
              settings.findChallengeId(vf) match
                case Some(challenge) if !mySelectedChallenge.exists(_._1.value == challenge.challengeId) =>
                  setChallenge(Some((ChallengeId(challenge.challengeId), challenge.dojo)))
                case _ =>
            case _ =>
          }
        }
      }
    )

  @volatile
  private var mySelectedChallenge: Option[(ChallengeId, CodeDojo)] = None
  private val myRootNode                                           = DefaultMutableTreeNode("Solutions")

  val myTreeModel: ListTreeTableModelOnColumns =
    ListTreeTableModelOnColumns(myRootNode, myColumns)

  private val myView = SolutionListView(this)

  @volatile
  private var myQueue: Option[Queue[IO, Option[(ChallengeId, CodeDojo)]]] = None

  private val myCancelToken = (for {
    queue <- Queue.unbounded[IO, Option[(ChallengeId, CodeDojo)]]
    _     <- IO.delay { myQueue = Option(queue) }
    _ <- Stream
      .fromQueueUnterminated(queue)
      .debounce(200.millis)
      .evalMap {
        case Some((challengeId, codeDojo)) =>
          ChallengeRepository.getInstance(myProject).getDSLContextResource[IO].use { dsl =>
            IO.blocking {
              val (title, difficulty) = dsl
                .selectFrom(CHALLENGE)
                .where(CHALLENGE.ID.eq(challengeId.value).and(CHALLENGE.DOJO.eq(codeDojo.value)))
                .fetchOptional()
                .toScala
                .map { record =>
                  (record.getTitle, ChallengeDifficulty.fromCIString(CIString(record.getDifficulty)))
                }
                .getOrElse(("Unknown", None))
              val solutions = dsl
                .select(
                  DSL.count().`as`("submissions") +:
                    (SOLUTION_SUBMISSION.fields() ++ SOLUTION.fields() ++ CHALLENGE_LANGUAGE.fields() ++ CHALLENGE
                      .fields()) *
                )
                .from(SOLUTION)
                .leftJoin(SOLUTION_SUBMISSION)
                .on(SOLUTION_SUBMISSION.SOLUTIONID.eq(SOLUTION.ID))
                .leftJoin(CHALLENGE_LANGUAGE)
                .on(SOLUTION_SUBMISSION.CHALLENGELANGUAGEID.eq(CHALLENGE_LANGUAGE.ID))
                .innerJoin(CHALLENGE)
                .on(SOLUTION.CHALLENGEID.eq(CHALLENGE.ID))
                .where(CHALLENGE.ID.eq(challengeId.value).and(CHALLENGE.DOJO.eq(codeDojo.value)))
                .groupBy(SOLUTION.ID, CHALLENGE_LANGUAGE.LANGUAGE, CHALLENGE_LANGUAGE.LANGUAGEVERSION)
                .fetch()
                .asScala
                .groupBy { record =>
                  (
                    record.get(SOLUTION.ID),
                    record.get(SOLUTION.TITLE),
                    record.get(SOLUTION.CREATEDATETIME),
                    record.get(SOLUTION.ISDEFAULT)
                  )
                }
                .toList
                .sortBy { case ((_, _, createdAt, _), _) => createdAt }
                .map { case ((solutionId, solutionTitle, _, isDefault), records) =>
                  val parent           = DefaultMutableTreeNode()
                  var totalSubmissions = 0
                  records.foreach { record =>
                    Option(record.get(CHALLENGE_LANGUAGE.LANGUAGE)) match
                      case Some(lang) =>
                        Language.fromCIString(CIString(lang)).foreach { lang =>
                          val submissions = record.get("submissions", classOf[Integer]).intValue()
                          val child = DefaultMutableTreeNode(
                            SolutionEntry.LanguageNode(
                              lang,
                              LanguageVersion.fromString(record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION)),
                              submissions
                            )
                          )
                          totalSubmissions += submissions
                          parent.add(child)
                        }
                      case None =>
                  }
                  parent.setUserObject(
                    SolutionEntry.SolutionNode(solutionId, solutionTitle, totalSubmissions, isDefault == 1)
                  )
                  parent
                }
              Option((codeDojo, title, difficulty, solutions))
            }
          }
        case None =>
          IO.delay { None }
      }
      .evalTap {
        case Some((codeDojo, title, difficulty, solutions)) =>
          IO.delay {
            myView.setChallengeName(title)
            myView.setDifficulty(difficulty.map(_.showAsHtml).getOrElse(""))
            myView.setCodeDojo(codeDojo)
            myRootNode.removeAllChildren()
            solutions.foreach(myRootNode.add)
            myTreeModel.nodeStructureChanged(myRootNode)
          }.evalOnEDTAny()
        case None =>
          IO.delay {
            myView.setChallengeName("No challenge selected")
            myRootNode.removeAllChildren()
            myTreeModel.nodeStructureChanged(myRootNode)
          }.evalOnEDTAny()
      }
      .onFinalize(myLogger.info("Solution worker is finalized"))
      .compile
      .drain
  } yield ()).unsafeRunCancelable()

  Disposer.register(myProject, this)

  def isSolutionTitleAvailable(title: String): Boolean = {
    if StringUtil.isEmpty(title) then false
    else
      mySelectedChallenge match {
        case Some((challengeId, _)) =>
          val dsl = ChallengeRepository.getInstance(myProject).getDSLContext
          dsl
            .selectCount()
            .from(SOLUTION)
            .where(SOLUTION.CHALLENGEID.eq(challengeId.value).and(SOLUTION.TITLE.eq(title)))
            .fetchOne()
            .value1() == 0
        case None =>
          false
      }
  }

  def modifySolutionTitle(solutionId: Long, treeNode: DefaultMutableTreeNode, title: String): Unit = {
    (ChallengeRepository.getInstance(myProject).getDSLContextResource[IO].use { dsl =>
      IO.blocking {
        dsl
          .update(SOLUTION)
          .set(SOLUTION.TITLE, title)
          .where(SOLUTION.ID.eq(solutionId))
          .execute()
      }
    } *> IO.delay {
      treeNode.getUserObject match {
        case solution: SolutionEntry.SolutionNode =>
          treeNode.setUserObject(solution.copy(title = title))
          myTreeModel.nodeChanged(treeNode)
        case _ =>
      }
    }.evalOnEDTDefault()).unsafeRunAndForget()
  }

  def getView: JComponent = myView.getComponent

  override def dispose(): Unit = {
    myCancelToken()
  }

  def addNewSolution(title: String): Unit = {
    mySelectedChallenge match
      case Some((challengeId, _)) =>
        ChallengeRepository
          .getInstance(myProject)
          .getDSLContextResource[IO]
          .use { dsl =>
            IO.blocking {
              val record = dsl
                .newRecord(SOLUTION)
                .setId(IdGenerator.nextId())
                .setTitle(title)
                .setCreatedatetime(LocalDateTime.now())
                .setChallengeid(challengeId.value)
              record.store()
              record.getId
            }.flatMap { solutionId =>
              IO.delay {
                myRootNode.add(
                  DefaultMutableTreeNode(SolutionEntry.SolutionNode(solutionId, title, 0, isDefault = false))
                )
                myTreeModel.nodeStructureChanged(myRootNode)
              }.evalOnEDTDefault()
            }
          }
          .unsafeRunAndForget()
      case None =>
  }

  def requery(): Unit = {
    myQueue.foreach(_.offer(mySelectedChallenge).unsafeRunAndForget())
  }

  def openSolutionRemarkEditor(solutionId: Long): Unit = {
    mySelectedChallenge match
      case Some((challengeId, codeDojo)) =>
        val solutionRemarkFilePath =
          SolutionRemarkFileSystem.SolutionRemarkFilePath(solutionId, challengeId, codeDojo, myProject.getLocationHash)
        val solutionFile = SolutionRemarkFileSystem.getInstance().findOrCreateFile(myProject, solutionRemarkFilePath)
        if solutionFile != null then FileEditorManager.getInstance(myProject).openFile(solutionFile)
      case _ =>
  }

  private def setChallenge(challenge: Option[(ChallengeId, CodeDojo)]): Unit = {
    mySelectedChallenge = challenge
    requery()
  }

}
