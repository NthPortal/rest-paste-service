package models

import slick.driver.SQLiteDriver.api._
import slick.lifted.Tag

class PasteWriteData(tag: Tag) extends Table[PasteWriteInfo](tag, "WRITE_DATA") {
  def writeId = column[String]("WRITE_ID", O.PrimaryKey)
  def readId = column[String]("READ_ID")
  def readData = foreignKey("READ_DATA_FK", readId, PasteData)(_.readId, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
  def * = (writeId, readId) <> (PasteWriteInfo.tupled, PasteWriteInfo.unapply)
}

object PasteWriteData extends TableQuery[PasteWriteData](new PasteWriteData(_)) {
  def withWriteId(writeId: String) = filter(_.writeId === writeId).result.headOption
  def ids = map(_.writeId).result
}
