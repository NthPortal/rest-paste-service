package io.github.nthportal.paste.data

import slick.lifted.Tag
import slick.driver.SQLiteDriver.api._

class PasteWriteData(tag: Tag) extends Table[PasteWriteDatum](tag, "WRITE_DATA") {
  def writeId = column[String]("WRITE_ID", O.PrimaryKey)
  def readId = column[String]("READ_ID")
  def readData = foreignKey("READ_DATA_FK", readId, PasteReadData)(_.readId, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
  def editable = column[Boolean]("EDITABLE", O.Default(true))
  def deletable = column[Boolean]("DELETABLE", O.Default(true))
  def * = (writeId, readId, editable, deletable) <> (PasteWriteDatum.tupled, PasteWriteDatum.unapply)
}

object PasteWriteData extends TableQuery[PasteWriteData](new PasteWriteData(_)) {
  def withWriteId(writeId: String) = filter(_.writeId === writeId).result
  def ids = map(_.writeId).result
}
