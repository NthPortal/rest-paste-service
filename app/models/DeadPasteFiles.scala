package models

import java.util.UUID

import slick.driver.SQLiteDriver.api._
import slick.lifted.Tag

class DeadPasteFiles(tag: Tag) extends Table[UUID](tag, "DEAD_PASTES") {
  def deadRevision = column[UUID]("DEAD_REVISION", O.PrimaryKey)
  override def * = deadRevision
}

object DeadPasteFiles extends TableQuery[DeadPasteFiles](new DeadPasteFiles(_)) {
  def all = this.result
}
