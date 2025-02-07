package com.wenjunhuang.codeepiphany.services

import cats.effect.{Async, IO}
import cats.effect.kernel.Resource
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import java.io.File
import org.flywaydb.core.Flyway
import org.jooq.{DSLContext, Log, SQLDialect}
import org.jooq.impl.DSL
import org.jooq.tools.JooqLogger

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil

import com.wenjunhuang.codeepiphany.model.Constants
import com.wenjunhuang.codeepiphany.settings.CodeEpiphanySettings
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.isDebug

@Service(Array(Level.PROJECT))
final class ChallengeRepository(private val myProject: Project) extends Disposable {
  Disposer.register(myProject, this)

  myProject.getMessageBus
    .connect(this)
    .subscribe(
      CodeEpiphanySettings.DATABASE_FOLDER_TOPIC,
      _ => {
        closeDataSource(false)
        myDataSource = None
      }
    )

  @volatile
  private var myDataSource: Option[HikariDataSource] = None


  private def createDataSource(): HikariDataSource = synchronized {
    myDataSource.getOrElse {
      val config = HikariConfig()
      val dbFile = getDatabaseFile
      config.setDriverClassName("org.sqlite.JDBC")
      config.setJdbcUrl(s"jdbc:sqlite:${dbFile.getCanonicalPath}")
      config.setPoolName("CodeEpiphanyHikariPool")
      config.setRegisterMbeans(true)

      val ds = HikariDataSource(config)
      val flyway = Flyway
        .configure(getClass.getClassLoader)
        .dataSource(ds)
        .locations("classpath:db/migration")
        .load()
      flyway.migrate()
      myDataSource = Some(ds)

      if isDebug then JooqLogger.globalThreshold(Log.Level.DEBUG)
      ds
    }
  }

  private def getDatabaseFile: File = {
    val settings = CodeEpiphanySettings.getInstance(myProject).getState
    val folder = settings.getDatabaseFolder(myProject)
    val file = File(folder, Constants.CHALLENGE_STORAGE_FILE)
    FileUtil.createIfDoesntExist(file)
    file
  }

  private def closeDataSource(wait: Boolean): Unit = {
    if wait then myDataSource.foreach(_.close())
    else myDataSource.foreach { ds => IO.delay(ds.close()).unsafeRunAndForget() }
    myDataSource = None
  }

  def getDSLContext: DSLContext = DSL.using(createDataSource(), SQLDialect.SQLITE)

  def getDSLContextResource[F[_]: Async]: Resource[F, DSLContext] =
    Resource.make(Async[F].delay(getDSLContext))(dsl => Async[F].pure(()))

  override def dispose(): Unit = {
    closeDataSource(true)
  }
}

object ChallengeRepository {
  def getInstance(project: Project): ChallengeRepository = project.getService(classOf[ChallengeRepository])

}
