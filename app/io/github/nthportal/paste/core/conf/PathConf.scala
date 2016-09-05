package io.github.nthportal.paste.core.conf

import java.io.File
import javax.inject.Inject

import play.api.Configuration

class PathConf @Inject() (configuration: Configuration) {
  val baseDir = configuration.getString("paste.internal.fs.base").get
  val confFilePath = baseDir + File.separator + "conf.json"
  val defaultConfFilePath = baseDir + File.separator + "conf.json.default"
  val pasteDir = baseDir + File.separator + "pastes"
  val dbDir = baseDir + File.separator + "data"

  def pasteFile(revisionId: String): File = new File(pasteDir + File.separator + revisionId)
}
