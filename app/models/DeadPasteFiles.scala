package models

import io.github.nthportal.paste.core.IdManager.revisionIdLength
import slick.driver.SQLiteDriver.api._
import slick.lifted.Tag

class DeadPasteFiles(tag: Tag) extends Table[String](tag, "DEAD_PASTES") {
  def deadRevision = column[String]("DEAD_REVISION_ID", O.PrimaryKey, O.Length(revisionIdLength, varying = false))
  override def * = deadRevision
}

object DeadPasteFiles extends TableQuery[DeadPasteFiles](new DeadPasteFiles(_)) {
  def all = this.result
}
