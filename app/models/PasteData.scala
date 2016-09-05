package models

import io.github.nthportal.paste.core.IdManager.{readIdLength, revisionIdLength}
import io.github.nthportal.paste.core.conf.Conf.Static._
import slick.driver.SQLiteDriver.api._
import slick.lifted.Tag

class PasteData(tag: Tag) extends Table[PasteDatum](tag, "READ_DATA") {
  def readId = column[String]("READ_ID", O.PrimaryKey, O.Length(readIdLength, varying = false))
  def fileRevision = column[String]("FILE_REVISION_ID", O.Length(revisionIdLength, varying = false))
  def title = column[Option[String]]("TITLE", O.Default(None), O.Length(maxTitleLengthChars, varying = true))
  def author = column[Option[String]]("AUTHOR", O.Default(None), O.Length(maxAuthorLengthChars, varying = true))
  def description = column[Option[String]]("DESCRIPTION", O.Default(None), O.Length(maxDescriptionLengthChars, varying = true))
  def unixExpiration = column[Option[Long]]("EXPIRATION", O.Default(None))
  def editable = column[Boolean]("EDITABLE", O.Default(true))
  def deletable = column[Boolean]("DELETABLE", O.Default(true))
  def * = (readId, fileRevision, title, author, description, unixExpiration, editable, deletable) <>
    (PasteDatum.tupled, PasteDatum.unapply)
}

object PasteData extends TableQuery[PasteData](new PasteData(_)) {
  def withReadId(readId: String) = filter(_.readId === readId)
  def getWithReadId(readId: String) = withReadId(readId).result.headOption
  def ids = map(_.readId).result
}
