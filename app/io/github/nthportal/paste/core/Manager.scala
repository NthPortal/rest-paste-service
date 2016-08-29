package io.github.nthportal.paste.core

import java.io.{FileNotFoundException, IOException}

import com.fasterxml.jackson.core.JsonProcessingException
import play.api.Logger
import play.api.libs.json.Json

import scala.io.Source
import scala.util.Try
import scala.util.control.NonFatal

object Manager {

  val config: Conf =
    Try {
      val confStr = Source.fromFile(Conf.confFilePath).mkString
      Json.reads[Conf].reads(Json.parse(confStr)).get
    } recover {
      case e: JsonProcessingException => Logger.error("Poorly formatted configuration file", e); throw e
      case e: FileNotFoundException => Logger.warn("Missing configuration file", e); throw e
      case e: IOException => Logger.error("Exception reading configuration file", e); throw e
      case NonFatal(e) => Logger.error("Unknown exception", e); throw e
      // Deliberately do not match fatal errors, so that it will crash because of a failed match
    } getOrElse Conf()
}
