package com.wenjunhuang.codeepiphany.model

import cats.effect.{Async, IO}
import cats.effect.kernel.Resource
import com.zaxxer.hikari.HikariDataSource
import java.io.File
import org.flywaydb.core.Flyway
import org.jooq.{DSLContext, Log, SQLDialect}
import org.jooq.impl.DSL
import org.jooq.tools.JooqLogger

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.{PathMacroManager, Service}
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil

import com.wenjunhuang.codeepiphany.settings.CodeEpiphanySettings
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.isDebug
import io.circe.*

@Service(Array(Level.PROJECT))
final class ChallengeRepository(private val myProject: Project) extends Disposable {
  Disposer.register(myProject, this)

  myProject.getMessageBus
    .connect(this)
    .subscribe(
      CodeEpiphanySettings.DATABASE_FOLDER_TOPIC,
      _ => {
        closeDataSource(false)
        createDataSource()
      }
    )

  @volatile
  private var dataSource: Option[HikariDataSource] = None

  createDataSource()

  private def createDataSource(): Unit = {
    val ds     = HikariDataSource()
    val dbFile = getDatabaseFile
    ds.setDriverClassName("org.sqlite.JDBC")
    ds.setJdbcUrl(s"jdbc:sqlite:${dbFile.getCanonicalPath}")
    val flyway = Flyway
      .configure(getClass.getClassLoader)
      .dataSource(ds)
      .locations("classpath:db/migration")
      .load()
    flyway.migrate()
    dataSource = Some(ds)

    if isDebug then JooqLogger.globalThreshold(Log.Level.DEBUG)
  }

  private def getDatabaseFile: File = {
    val settings = CodeEpiphanySettings.getInstance(myProject).getState
    val folder = settings.getDatabaseFolder(myProject)
    val file = File(folder, Constants.CHALLENGE_STORAGE_FILE)
    FileUtil.createIfDoesntExist(file)
    file
  }

  private def closeDataSource(wait: Boolean): Unit = {
    if wait then dataSource.foreach(_.close())
    else dataSource.foreach { ds => IO.delay(ds.close()).unsafeRunAndForget() }
    dataSource = None
  }

  def getDSLContext: DSLContext = DSL.using(dataSource.get, SQLDialect.SQLITE)

  def getDSLContextResource[F[_]: Async]: Resource[F, DSLContext] =
    Resource.make(Async[F].delay(getDSLContext))(dsl => Async[F].pure(()))

  override def dispose(): Unit = {
    closeDataSource(true)
  }
}

object ChallengeRepository {
  def getInstance(project: Project): ChallengeRepository = project.getService(classOf[ChallengeRepository])

  opaque type ChallengeId = Long

  object ChallengeId {
    implicit val decoder: Decoder[ChallengeId] = Decoder.decodeLong.map(ChallengeId.apply)
    implicit val encoder: Encoder[ChallengeId] = Encoder.encodeLong.contramap(_.value)
    
    def apply(value: Long): ChallengeId        = value
    extension (id: ChallengeId) {
      def value: Long = id
    }
  }

  opaque type ChallengeLanguageId = Long
  object ChallengeLanguageId {
    def apply(value: Long): ChallengeLanguageId = value
    extension (id: ChallengeLanguageId) {
      def value: Long = id
    }
  }

  opaque type SolutionId = Long
  object SolutionId {
    def apply(value: Long): SolutionId = value
    extension (id: SolutionId) {
      def value: Long = id
    }
  }

  opaque type SubmissionId = Long
  object SubmissionId {
    def apply(value: Long): SolutionId = value
    extension (id: SolutionId) {
      def value: Long = id
    }
  }
}
