package io.github.nthportal.paste.core

import java.io.{File, FileNotFoundException, IOException, PrintWriter}
import javax.inject.{Inject, Singleton}

import com.fasterxml.jackson.core.JsonProcessingException
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json
import slick.driver.JdbcProfile

import scala.io.Source
import scala.util.Try

@Singleton
class Manager @Inject()(dbConfigProvider: DatabaseConfigProvider) {
  implicit private val confFormat = Json.format[Conf]

  val config: Conf =
    Try {
      val confStr = Source.fromFile(Conf.confFilePath).mkString
      Json.fromJson[Conf](Json.parse(confStr)).get
    } recover {
      case e: FileNotFoundException => Logger.warn("Missing configuration file"); throw e
      case e: JsonProcessingException => Logger.error("Poorly formatted configuration file", e); throw e
      case e: IOException => Logger.error("I/O Exception reading configuration file", e); throw e
    } getOrElse Conf()

  writeConfigIfNotExists()

  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  private def writeConfigIfNotExists(): Unit = {
    val file = new File(Conf.confFilePath)
    if (!file.exists()) {
      try {
        new PrintWriter(Conf.confFilePath) {
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
