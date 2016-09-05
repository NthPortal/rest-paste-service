package io.github.nthportal.paste.core

import java.io.{File, FileNotFoundException, IOException, PrintWriter}
import javax.inject.{Inject, Singleton}

import com.fasterxml.jackson.core.JsonProcessingException
import io.github.nthportal.paste.core.conf.{Conf, PathConf}
import models.DeadPasteFiles
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import slick.driver.JdbcProfile

import scala.concurrent.Future
import scala.io.Source
import scala.util.Try

@Singleton
class Manager @Inject()(val pathConf: PathConf, dbConfigProvider: DatabaseConfigProvider) {
  implicit private val confFormat = Json.format[Conf]
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  val config: Conf =
    Try {
      val confStr = Source.fromFile(pathConf.confFilePath).mkString
      Json.fromJson[Conf](Json.parse(confStr)).get
    } recover {
      case e: FileNotFoundException => Logger.warn("Missing configuration file"); throw e
      case e: JsonProcessingException => Logger.error("Poorly formatted configuration file", e); throw e
      case e: IOException => Logger.error("I/O Exception reading configuration file", e); throw e
      case t: Throwable => Logger.error("Unknown error reading configuration file", t); throw t
    } getOrElse Conf()

  writeConfigIfNotExists()

  def deletePasteFileLater(revisionId: String): Future[Unit] = {
    for {
      _ <- db.run(DeadPasteFiles += revisionId)
    // TODO: put it somewhere else as well? (and like, actually delete it)
    } yield Unit
  }

  private def writeConfigIfNotExists(): Unit = {
    val file = new File(pathConf.confFilePath)
    if (!file.exists()) {
      try {
        new PrintWriter(pathConf.confFilePath) {
          Json.format[Conf]
          write(Json.prettyPrint(Json.toJson(config)))
          close()
        }
      } catch {
        case e: IOException => Logger.error("I/O Exception writing configuration file", e)
      }
    }
  }
}
