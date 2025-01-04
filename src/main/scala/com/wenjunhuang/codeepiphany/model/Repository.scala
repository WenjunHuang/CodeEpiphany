package com.wenjunhuang.codeepiphany.model

import cats.effect.Async
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.xmlb.annotations.OptionTag
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.settings.CodeEpiphanySettings
import com.wenjunhuang.codeepiphany.utils.XmlUtils.*
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jooq.{ DSLContext, SQLDialect }
import org.jooq.impl.DSL

import java.util as ju
import java.io.File
import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.compiletime.uninitialized
import cats.effect.IO
import com.wenjunhuang.codeepiphany.utils.implicits.*

@Service(Array(Level.PROJECT))
final class Repository(private val myProject: Project) extends Disposable {
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
  }

  private def getDatabaseFile: File = {
    val settings = CodeEpiphanySettings.getInstance(myProject).getState
    val folder = settings.databaseFolder match {
      case Some(folder) => File(folder)
      case None =>
        val path = File(myProject.getWorkspaceFile.getParent.findChild(Constants.PROJECT_NAME).getPath)
        FileUtil.createDirectory(path)
        val file = File(path, Constants.CHALLENGE_STORAGE_FILE)
        FileUtil.createIfDoesntExist(file)
        file
    }
    val file = File(folder, Constants.CHALLENGE_STORAGE_FILE)
    file
  }

  private def closeDataSource(wait: Boolean): Unit = {
    if wait then dataSource.foreach(_.close())
    else dataSource.foreach { ds => IO.delay(ds.close()).unsafeRunAndForget() }
    dataSource = None
  }

  def getDSLContext: DSLContext = DSL.using(dataSource.get, SQLDialect.SQLITE)

  def getDSLContextF[F[_]: Async]: F[DSLContext] = Async[F].delay(getDSLContext)

  override def dispose(): Unit = {
    closeDataSource(true)
  }
}

object Repository {
  def getInstance(project: Project): Repository = project.getService(classOf[Repository])

  class ChallengeStorageItem {
    @BeanProperty
    var id: String = uninitialized

    @BeanProperty
    var slug: String = uninitialized

    @BeanProperty
    var codeFilePath: String = uninitialized

    @BeanProperty
    var descriptionFilePath: String = uninitialized

    @(OptionTag @field)(converter = classOf[CodeDojoConverter])
    @BeanProperty
    var dojo: CodeDojo = uninitialized

    @BeanProperty
    var extras: ju.Map[String, String] = new ju.HashMap[String, String]()
  }

  class ChallengeStorageState {
    @BeanProperty
    var challenges: ju.Map[String, ChallengeStorageItem] = new ju.HashMap[String, ChallengeStorageItem]()
  }
}
