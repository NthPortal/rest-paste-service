package modules

import java.io.{File, IOException}

import com.google.inject.AbstractModule
import io.github.nthportal.paste.core.Conf
import org.apache.commons.io.FileUtils
import play.api.Logger

/**
  * Ensure directories for service are created before the application is loaded.
  */
class Module extends AbstractModule {
  override def configure(): Unit = {
    ensureDirectoriesExist()
  }

  private def ensureDirectoriesExist(): Boolean = {
    try {
      FileUtils.forceMkdir(new File(Conf.dbDir))
      FileUtils.forceMkdir(new File(Conf.pasteDir))
      true
    } catch {
      case e: IOException =>
        Logger.error("Unable to create core directories", e)
        false
      case t: Throwable => throw t
    }
  }
}
