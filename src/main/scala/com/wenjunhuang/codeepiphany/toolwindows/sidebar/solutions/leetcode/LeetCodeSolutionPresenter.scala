package com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode

import cats.effect.IO
import javax.swing.{JComponent, SwingConstants}
import scala.jdk.OptionConverters.*

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.AnimatedIcon
import com.intellij.util.ui.components.BorderLayoutPanel

import com.wenjunhuang.codeepiphany.database.Tables.CHALLENGE
import com.wenjunhuang.codeepiphany.leetcode.models.LeetCodeQuestionSolutionArticle
import com.wenjunhuang.codeepiphany.leetcode.services.LeetCodeApi
import com.wenjunhuang.codeepiphany.model.newtypes.ChallengeId
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.{AuthService, ChallengeRepository}
import com.wenjunhuang.codeepiphany.services.http.{HttpClientManager, HttpClientService}
import com.wenjunhuang.codeepiphany.utils.syntax.*

class LeetCodeSolutionPresenter(
  private val myChallengeId: ChallengeId,
  private val myProject: Project,
  private val myCodeDojo: CodeDojo.LeetCodeCN.type | CodeDojo.LeetCode.type
) { self =>
  private val myView =
    new BorderLayoutPanel(){
      override def removeNotify(): Unit = {
        super.removeNotify()
      }
    }
  myView.addToCenter(JBLabel("Loading...", AnimatedIcon.Default.INSTANCE, SwingConstants.CENTER))

  private implicit val httpClientManager: HttpClientManager[IO] =
    HttpClientService.getInstance(myProject).httpClientManager

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
        .getDSLContextResource[IO]
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
          }
        }
        .flatMap { questionSlug =>

          val api = LeetCodeApi[IO](myCodeDojo)
          for
            solutionTags <- api.getSolutionTags(questionSlug)
            userInfo     <- api.getUserInfo
            presenter <- IO.delay {
              val articleDetailPresenter = ArticleDetailPresenter(myProject, myCodeDojo)
              val articlesPresenter = LeetCodeSolutionArticlesPresenter(
                myProject,
                LeetCodeSolutionArticlesPresenter.BootstrapParameters(userInfo, questionSlug, solutionTags),
                {article=>
                  articleDetailPresenter.setArticle(article)
                },
                myCodeDojo
              )
              val splitter = Splitter(false, 0.3f)
              splitter.setShowDividerControls(true)
              splitter.setFirstComponent(articlesPresenter.getViewComponent)
              splitter.setSecondComponent(articleDetailPresenter.getView)
              myView.removeAll()
              myView.addToCenter(splitter)
            }.evalOnEDTDefault()
          yield ()
        }.unsafeRunAndForget()
    }
  }

  def getView:JComponent = myView
}

object LeetCodeSolutionPresenter {
  val EMPTY_FORM: JComponent = BorderLayoutPanel().addToCenter(
    new JBLabel("Please select a article to view the result.", SwingConstants.CENTER)
  )
}
