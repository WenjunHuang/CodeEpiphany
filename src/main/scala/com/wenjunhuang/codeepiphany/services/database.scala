package com.wenjunhuang.codeepiphany.services

import java.time.LocalDateTime
import org.jooq.DSLContext
import scala.jdk.OptionConverters.*

import com.wenjunhuang.codeepiphany.database.Tables.SOLUTION
import com.wenjunhuang.codeepiphany.utils.IdGenerator

object database {

  def getOrCreateDefaultSolution(dsl: DSLContext, challengeId: Long): Long = {
    dsl
      .selectFrom(SOLUTION)
      .where(SOLUTION.CHALLENGEID.eq(challengeId).and(SOLUTION.ISDEFAULT.eq(1)))
      .fetchOptional()
      .toScala
      .getOrElse {
        val newRecord = dsl.newRecord(SOLUTION)
        newRecord
          .setId(IdGenerator.nextId())
          .setChallengeid(challengeId)
          .setTitle("Default")
          .setIsdefault(1)
          .setCreatedatetime(LocalDateTime.now())
        newRecord.store()
        newRecord
      }
      .getId
  }
}
