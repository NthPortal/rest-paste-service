package models

import io.github.nthportal.paste.core.IdManager.{readIdLength, writeIdLength}
import slick.driver.SQLiteDriver.api._
import slick.lifted.Tag

class PasteWriteData(tag: Tag) extends Table[IdPair](tag, "WRITE_DATA") {
  def writeId = column[String]("WRITE_ID", O.PrimaryKey, O.Length(writeIdLength, varying = false))
  def readId = column[String]("READ_ID", O.Length(readIdLength, varying = false))
  def readData = foreignKey("READ_DATA_FK", readId, PasteData)(_.readId, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
  def * = (readId, writeId) <> (IdPair.tupled, IdPair.unapply)
}

object PasteWriteData extends TableQuery[PasteWriteData](new PasteWriteData(_)) {
  def getWithWriteId(writeId: String) = filter(_.writeId === writeId).result.headOption
  def ids = map(_.writeId).result
}
