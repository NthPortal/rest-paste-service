package io.github.nthportal.paste.core.conf

import java.io.{File, FileNotFoundException, IOException, PrintWriter}
import javax.inject.{Inject, Provider}

import com.fasterxml.jackson.core.JsonProcessingException
import play.api.Logger
import play.api.libs.json.Json
import resource._

import scala.io.Source
import scala.util.Try

class ConfProvider @Inject() private[core](pathConf: PathConf) extends Provider[Conf] {
  import ConfProvider._

  val config: Conf =
    Try {
      val confStr = Source.fromFile(pathConf.confFilePath).mkString
      confFormat.reads(Json.parse(confStr)).get
    } recover {
      case e: FileNotFoundException =>
        Logger.warn("Missing configuration file - will be automatically generated")
        throw e
      case e: JsonProcessingException =>
        Logger.error("Poorly formatted configuration file", e)
        throw e
      case e: NoSuchElementException =>
        Logger.error("Missing field in configuration file", e)
        throw e
      case e: IOException =>
        Logger.error("I/O Exception reading configuration file", e)
        throw e
      case t: Throwable =>
        Logger.error("Unknown error reading configuration file", t)
        throw t
    } getOrElse Conf()

  writeConfig(config, pathConf.confFilePath, overwrite = false)
  writeConfig(Conf(), pathConf.defaultConfFilePath, overwrite = true)

  override def get(): Conf = config
}

object ConfProvider {
  implicit private val limitsFormat = Json.format[Limits]
  implicit private val confFormat = Json.format[Conf]

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
