package modules

import java.io.{File, IOException}

import com.google.inject.AbstractModule
import io.github.nthportal.paste.core.conf.PathConf
import org.apache.commons.io.FileUtils
import play.api.{Configuration, Environment, Logger}

/**
  * Ensure directories for service are created before the application is loaded.
  */
class Module(env: Environment, cfg: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    ensureDirectoriesExist()
  }

  private def ensureDirectoriesExist(): Unit = {
    val pathConf = new PathConf(cfg)
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
