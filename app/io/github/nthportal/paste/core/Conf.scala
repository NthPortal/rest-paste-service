package io.github.nthportal.paste.core

import java.io.File

case class Conf(maxPasteSizeKB: Long = 2000, maxTotalDiskUsageKB: Long = 5000000L)

object Conf {
  val rootDir = sys.props.getOrElse("user.home", ".") + File.separator + ".paste"
  val confFilePath = rootDir + File.separator + "conf.json"
  val pasteDir = rootDir + File.separator + "pastes"
  val dbDir = rootDir + File.separator + "data"
}
