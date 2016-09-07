package io.github.nthportal.paste.core.conf

import java.io.File

case class PathConf private[core](baseDir: String) {
  val confFilePath = baseDir + File.separator + "conf.json"
  val defaultConfFilePath = baseDir + File.separator + "conf.json.default"
  val pasteDir = baseDir + File.separator + "pastes"
  val dbDir = baseDir + File.separator + "data"

  def pasteFile(revisionId: String): File = new File(pasteDir + File.separator + revisionId)
}
