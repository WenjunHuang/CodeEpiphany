package com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode

import cats.effect.IO
import cats.syntax.all.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.Disposer
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.components.BorderLayoutPanel
import com.wenjunhuang.codeepiphany.database.Tables.CHALLENGE
import com.wenjunhuang.codeepiphany.leetcode.services.LeetCodeApi
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.newtypes.ChallengeId
import com.wenjunhuang.codeepiphany.services.{AuthService, ChallengeRepository}
import com.wenjunhuang.codeepiphany.utils.syntax.*
import com.wenjunhuang.codeepiphany.utils.ui.UnauthenticatedView

import javax.swing.{JComponent, SwingConstants}
import scala.jdk.OptionConverters.*

class LeetCodeSolutionPresenter(
  private val myChallengeId: ChallengeId,
  private val myProject: Project,
  private val myCodeDojo: CodeDojo.LeetCodeCN.type | CodeDojo.LeetCode.type
) extends Disposable {
  private val myView                                       = new BorderLayoutPanel()
  private var myArticleDetailPresenter: Option[Disposable] = None
  private var myArticlesPresenter: Option[Disposable]      = None

  initialize()

  private def initialize(): Unit = {
    // Implement the logic to fetch and display LeetCode solutions
    if (
      AuthService
        .getInstance(myProject)
        .isLoggedIn(myCodeDojo)
    ) {
      ChallengeRepository
        .getInstance(myProject)
        .getDSLContextResource
        .use { dslContext =>
          IO.delay {
            dslContext
              .select(CHALLENGE.SLUG)
              .from(CHALLENGE)
              .where(CHALLENGE.ID.eq(myChallengeId.value))
              .fetchOptional()
              .toScala
              .map(_.value1())
              .getOrElse(throw new NoSuchElementException(s"No challenge found for ID: ${myChallengeId.value}"))
          }.handleErrorWith { error =>
            IO.delay {
              myView.addToCenter(
                new JBLabel(
                  s"Error fetching challenge slug: ${error.getMessage}",
                  AnimatedIcon.Default.INSTANCE,
                  SwingConstants.CENTER
                )
              )
            }.evalOnEDTDefault() *> IO.raiseError(error)
          }
        }
        .flatMap { questionSlug =>
          val api = LeetCodeApi(myCodeDojo)
          for
            _ <- IO.delay {
              myView.removeAll()
              myView.addToCenter(
                new JBLabel(s"Loading solutions ...", AnimatedIcon.Default.INSTANCE, SwingConstants.CENTER)
              )
            }.evalOnEDTDefault()
            solutionTags <- api.getSolutionTags(questionSlug)
            userInfo     <- api.getUserInfo
            _ <- IO.delay {
              myView.removeAll()
              myArticleDetailPresenter.foreach(Disposer.dispose)
              myArticlesPresenter.foreach(Disposer.dispose)

              val articleDetailPresenter = ArticleDetailPresenter(myProject, myCodeDojo)
              val articlesPresenter = SolutionArticlesPresenter(
                myProject,
                SolutionArticlesPresenter.BootstrapParameters(userInfo, questionSlug, solutionTags),
                { article =>
                  articleDetailPresenter.setArticle(article)
                },
                myCodeDojo
              )

              val splitter = new Splitter(false, 0.3f)
              splitter.setShowDividerControls(true)
              splitter.setFirstComponent(articlesPresenter.getViewComponent)
              splitter.setSecondComponent(articleDetailPresenter.getView)
              myView.addToCenter(splitter)

              myArticleDetailPresenter = articleDetailPresenter.some
              myArticlesPresenter = articlesPresenter.some
            }.evalOnEDTDefault()
          yield ()
        }
        .unsafeRunAndForget()
    } else {
      myView.addToCenter(new UnauthenticatedView(myCodeDojo))
    }
  }

  def getView: JComponent = myView

  override def dispose(): Unit = {
    myArticlesPresenter.foreach(Disposer.dispose)
    myArticleDetailPresenter.foreach(Disposer.dispose)
  }
}

object LeetCodeSolutionPresenter {
  val EMPTY_FORM: JComponent =
    BorderLayoutPanel().addToCenter(new JBLabel("Please select a article to view the result.", SwingConstants.CENTER))
}
