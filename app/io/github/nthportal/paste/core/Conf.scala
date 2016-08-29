package io.github.nthportal.paste.core

import java.io.{File, IOException}

import org.apache.commons.io.FileUtils
import play.api.Logger
import slick.jdbc.JdbcBackend

case class Conf(rootDir: String = Conf.defaultRootDir,
                maxPasteSizeKB: Long = 2000,
                maxTotalDiskUsageKB: Long = 5000000L) {
  val dbDir = rootDir + File.separator + "data"
  val dbFile = dbDir + File.separator + "paste.db"
  val pasteDir = rootDir + File.separator + "pastes"
  val db = JdbcBackend.Database.forURL("jdbc:sqlite:" + dbFile)

  def createDirectories(): Boolean = {
    try {
      FileUtils.forceMkdir(new File(dbDir))
      FileUtils.forceMkdir(new File(pasteDir))
      true
    } catch {
      case e: IOException =>
        Logger.error("Unable to create core directories", e)
        false
      case t: Throwable => throw t
    }
  }
}

object Conf {
  val defaultRootDir = sys.props.getOrElse("user.home", ".") + File.separator + ".paste"
  val confFilePath = defaultRootDir + File.separator + "conf.json"
}
