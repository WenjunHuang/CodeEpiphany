package com.wenjunhuang.codeepiphany.hackerrank

import cats.effect.IO
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.wenjunhuang.codeepiphany.hackerrank.model.QuestionSkill.Intermediate
import com.wenjunhuang.codeepiphany.hackerrank.model.QuestionStatus.{Solved, Unsolved}
import com.wenjunhuang.codeepiphany.http.HttpClientKeeper
import com.wenjunhuang.codeepiphany.runtime.intellijIORuntime
import cats.syntax.all.*
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat

class ApiTest extends BasePlatformTestCase {
  def testApi(): Unit = {
    val httpClientKeeper = HttpClientKeeper[IO]()
    val hackerRankApi    = HackerRankApi[IO](httpClientKeeper)

    (
      hackerRankApi
        .getAlgorithmsChallenges(0, 10),
      hackerRankApi.getAlgorithmsChallenges(0, 10, Nil, List(Intermediate))
    ).mapN { case (challenges1, challenges2) =>
      assertThat(challenges1.size, not(0))
      assertThat(challenges2.size, not(0))
      
    }.unsafeRunSync()
  }
}
