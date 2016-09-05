package io.github.nthportal.paste.core

import java.io.{File, FileNotFoundException, IOException, PrintWriter}
import javax.inject.{Inject, Singleton}

import com.fasterxml.jackson.core.JsonProcessingException
import io.github.nthportal.paste.core.conf.{Conf, Limits, PathConf}
import models.DeadPasteFiles
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import resource._
import slick.driver.JdbcProfile

import scala.concurrent.Future
import scala.io.Source
import scala.util.Try

@Singleton
class Manager @Inject()(val pathConf: PathConf, dbConfigProvider: DatabaseConfigProvider) {

  import Manager._

  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  val config: Conf =
    Try {
      val confStr = Source.fromFile(pathConf.confFilePath).mkString
      confFormat.reads(Json.parse(confStr)).get
    } recover {
      case e: FileNotFoundException => Logger.warn("Missing configuration file"); throw e
      case e: JsonProcessingException => Logger.error("Poorly formatted configuration file", e); throw e
      case e: IOException => Logger.error("I/O Exception reading configuration file", e); throw e
      case t: Throwable => Logger.error("Unknown error reading configuration file", t); throw t
    } getOrElse Conf()

  writeConfig(config, pathConf.confFilePath, overwrite = false)
  writeConfig(Conf(), pathConf.defaultConfFilePath, overwrite = true)

  def deletePasteFileLater(revisionId: String): Future[Unit] = {
    for {
      _ <- db.run(DeadPasteFiles += revisionId)
    // TODO: put it somewhere else as well? (and like, actually delete it)
    } yield Unit
  }

  private def writeConfig(conf: Conf, path: String, overwrite: Boolean = false): Unit = {
    val file = new File(path)
    if (overwrite || !file.exists()) {
      try {
        for (writer <- managed(new PrintWriter(file))) writer.write(Json.prettyPrint(Json.toJson(conf)))
      } catch {
        case e: IOException => Logger.error("I/O exception writing configuration file at " + path, e)
      }
    }
  }
}

object Manager {
  implicit private val limitsFormat = Json.format[Limits]
  implicit private val confFormat = Json.format[Conf]
}
