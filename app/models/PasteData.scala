package models

import slick.driver.SQLiteDriver.api._
import slick.lifted.Tag

class PasteData(tag: Tag) extends Table[PasteDatum](tag, "READ_DATA") {
  def readId = column[String]("READ_ID", O.PrimaryKey)
  def title = column[Option[String]]("TITLE", O.Default(None))
  def author = column[Option[String]]("AUTHOR", O.Default(None))
  def description = column[Option[String]]("DESCRIPTION", O.Default(None))
  def unixExpiration = column[Option[Long]]("EXPIRATION", O.Default(None))
  def editable = column[Boolean]("EDITABLE", O.Default(true))
  def deletable = column[Boolean]("DELETABLE", O.Default(true))
  def * = (readId, title, author, description, unixExpiration, editable, deletable) <> (PasteDatum.tupled, PasteDatum.unapply)
}

object PasteData extends TableQuery[PasteData](new PasteData(_)) {
  def withReadId(readId: String) = filter(_.readId === readId)
  def getWithReadId(readId: String) = withReadId(readId).result.headOption
  def ids = map(_.readId).result
}
