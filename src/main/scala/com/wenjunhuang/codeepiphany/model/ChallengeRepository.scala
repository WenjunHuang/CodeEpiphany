package com.wenjunhuang.codeepiphany.model

import cats.effect.kernel.Resource
import cats.effect.{Async, IO}
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.wenjunhuang.codeepiphany.settings.CodeEpiphanySettings
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.isDebug
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jooq.impl.DSL
import org.jooq.tools.JooqLogger
import org.jooq.{DSLContext, Log, SQLDialect}

import java.io.File

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
    val folder = settings.databaseFolder match
      case Some(folder) => File(folder)
      case None =>
        val projectIdeaFolder    = File(myProject.getWorkspaceFile.getParent.getPath)
        val projectSettingFolder = File(projectIdeaFolder, Constants.PROJECT_NAME)
        projectSettingFolder.mkdirs()
        projectSettingFolder

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

  opaque type ChallengeId = Int
  object ChallengeId {
    def apply(value: Int): ChallengeId = value
    extension (id: ChallengeId) {
      def value: Int = id
    }
  }

  opaque type ChallengeLanguageId = Int
  object ChallengeLanguageId {
    def apply(value: Int): ChallengeLanguageId = value
    extension (id: ChallengeLanguageId) {
      def value: Int = id
    }
  }

  opaque type SolutionId = Int
  object SolutionId {
    def apply(value: Int): SolutionId = value
    extension (id: SolutionId) {
      def value: Int = id
    }
  }

}
