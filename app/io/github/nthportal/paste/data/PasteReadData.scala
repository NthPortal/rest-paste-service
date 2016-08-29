package io.github.nthportal.paste.data

import slick.lifted.Tag
import slick.driver.SQLiteDriver.api._

class PasteReadData(tag: Tag) extends Table[PasteReadDatum](tag, "READ_DATA") {
  def readId = column[String]("READ_ID", O.PrimaryKey)
  def title = column[Option[String]]("TITLE", O.Default(None))
  def author = column[Option[String]]("AUTHOR", O.Default(None))
  def description = column[Option[String]]("DESCRIPTION", O.Default(None))
  def unixExpiration = column[Option[Long]]("EXPIRATION", O.Default(None))
  def * = (readId, title, author, description, unixExpiration) <> (PasteReadDatum.tupled, PasteReadDatum.unapply)
}

object PasteReadData extends TableQuery[PasteReadData](new PasteReadData(_)) {
  def withReadId(readId: String) = filter(_.readId === readId).result
  def ids = map(_.readId).result
}
