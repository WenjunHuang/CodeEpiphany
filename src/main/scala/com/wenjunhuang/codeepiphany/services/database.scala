package com.wenjunhuang.codeepiphany.services

import com.wenjunhuang.codeepiphany.database.Tables.SOLUTION
import com.wenjunhuang.codeepiphany.utils.IdGenerator
import org.jooq.DSLContext

import java.time.LocalDateTime
import scala.jdk.OptionConverters.*

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
