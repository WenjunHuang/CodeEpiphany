package com.wenjunhuang.codeepiphany.hackerrank

import cats.effect.IO
import cats.syntax.all.*
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.wenjunhuang.codeepiphany.controllers.http.{HttpClientKeeper, HttpClientService}
import com.wenjunhuang.codeepiphany.hackerrank.model.ChallengeSkill.Intermediate
import com.wenjunhuang.codeepiphany.hackerrank.model.ChallengeStatus.Unsolved
import com.wenjunhuang.codeepiphany.utils.implicits.*
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat

class ApiTest extends BasePlatformTestCase {
  def testApi(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
//    val httpClientKeeper = HttpClientKeeper[IO]()
    val hackerRankApi = HackerRankApi[IO]()

    (
      hackerRankApi
        .searchChallenges(0, 10, None, Some("algorithms")),
      hackerRankApi.searchChallenges(0, 10, Some("projecteuler"), None, List(Unsolved), Nil)
    ).mapN { case (challenges1, challenges2) =>
      assertThat(challenges1.size, not(0))
      assertThat(challenges2.size, not(0))
    }.unsafeRunSync()
  }

  def testCheckLogin(): Unit = {
    val httpClientKeeper = HttpClientService.getInstance(getProject)
    import httpClientKeeper.*
    val hackerRankApi = HackerRankApi[IO]()

    if hackerRankApi.checkLogin().unsafeRunSync() then println("Login success")
    else println("Login failed")
  }
}
