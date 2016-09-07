package io.github.nthportal.paste.core.conf

import java.io.{File, IOException}
import javax.inject.Provider

import org.apache.commons.io.FileUtils
import play.api.Logger

class PathConfProvider(baseDir: String) extends Provider[PathConf] {
  override def get(): PathConf = PathConf(baseDir)

  /**
    * Ensure directories for service exist (based on [[baseDir]]).
    */
  PathConfProvider.ensureDirectoriesExistFor(get())
}

private object PathConfProvider {
  def ensureDirectoriesExistFor(pathConf: PathConf): Unit = {
    try {
      FileUtils.forceMkdir(new File(pathConf.dbDir))
      FileUtils.forceMkdir(new File(pathConf.pasteDir))
    } catch {
      case e: IOException =>
        Logger.error("Unable to create core directories", e)
        throw e
      case t: Throwable => throw t
    }
  }
}
