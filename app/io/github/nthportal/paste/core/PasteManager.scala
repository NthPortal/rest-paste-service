package io.github.nthportal.paste.core

import javax.inject.{Inject, Singleton}

import models.DeadPasteFiles
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class PasteManager @Inject() private[core](dbConfig: DatabaseConfig[JdbcProfile]) {
  import dbConfig.driver.api._

  val db = dbConfig.db

  def deletePasteFileLater(revisionId: String): Future[Unit] = {
    for {
      _ <- db.run(DeadPasteFiles += revisionId)
    // TODO: put it somewhere else as well? (and like, actually delete it)
    } yield Unit
  }
}
