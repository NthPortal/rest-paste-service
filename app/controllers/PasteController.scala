package controllers

import java.io.{File, PrintWriter}
import javax.inject.{Inject, Singleton}

import io.github.nthportal.paste.api.{Paste, PasteIds, PasteLifecycleInfo, PasteMetadata}
import io.github.nthportal.paste.core._
import models.{PasteData, PasteDatum, PasteWriteData}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future
import scala.io.Source
import scala.util.Try

@Singleton
class PasteController @Inject()(manager: Manager, idManager: IdManager) extends Controller {

  import PasteController._
  import manager.dbConfig.driver.api._

  private val db = manager.db
  private val pathConf = manager.pathConf

  def createPaste = Action.async(parse.json) {
    implicit request => {
      for (paste <- pasteFormat.reads(request.body)) yield createPasteCheckPreconditions(paste)
    } getOrElse Future.successful(BadRequest)
  }

  private def createPasteCheckPreconditions(paste: Paste): Future[Result] = {
    checkLifecyclePreconditions(paste) getOrElse doCreatePaste(paste)
  }

  private def doCreatePaste(paste: Paste): Future[Result] = {
    for {
      pair <- idManager.unusedRandomIdPair()
      revision = idManager.randomRevisionId
      _ = writeToFile(pathConf.pasteFile(revision), paste.body)
      datum = paste.toDatumWithIds(pair.readId, revision)
      _ <- db.run(PasteData += datum)
      deletable = paste.lifecycle.deletable.getOrElse(true)
      _ <- if (deletable) db.run(PasteWriteData += pair) else Future.successful(null)
    } yield {
      val writeId = if (deletable) Some(pair.writeId) else None
      Ok(idsWrites.writes(PasteIds(pair.readId, writeId)))
    }
  }

  def modifyPaste(writeId: String) = Action.async(parse.json) {
    implicit request => {
      for (paste <- pasteFormat.reads(request.body)) yield modifyPasteGetData(writeId, paste)
    } getOrElse Future.successful(BadRequest)
  }

  private def modifyPasteGetData(writeId: String, paste: Paste): Future[Result] = {
    for {
      datum <- dataForWriteId(writeId)
      res <- modifyPasteCheckPreconditions(datum, paste)
    } yield res
  } recover {
    case _ => NotFound
  }

  private def modifyPasteCheckPreconditions(datum: PasteDatum, paste: Paste): Future[Result] = {
    if (!datum.editable) Future.successful(Forbidden("Paste is not editable"))
    else checkLifecyclePreconditions(paste) getOrElse doModifyPaste(datum, paste)
  }

  private def doModifyPaste(oldDatum: PasteDatum, paste: Paste): Future[Result] = {
    val metadata = paste.metadata
    val lifecycle = paste.lifecycle
    val newDatum = oldDatum.copy(
      revisionId = idManager.randomRevisionId,
      title = metadata.title orElse oldDatum.title,
      author = metadata.author orElse oldDatum.author,
      description = metadata.description orElse oldDatum.description,
      deletable = lifecycle.deletable getOrElse oldDatum.deletable,
      editable = lifecycle.editable getOrElse oldDatum.editable,
      unixExpiration = lifecycle.expiresAfter orElse oldDatum.unixExpiration)

    writeToFile(pathConf.pasteFile(newDatum.revisionId), paste.body)
    for {
      _ <- db.run(PasteData.update(newDatum))
      _ <- manager.deletePasteFileLater(oldDatum.revisionId)
    } yield Created("Updated paste")
  }

  private def checkLifecyclePreconditions(paste: Paste): Option[Future[Result]] = {
    if (!paste.lifecycle.deletable.getOrElse(true) && paste.lifecycle.editable.getOrElse(true)) {
      Some(Future.successful(UnprocessableEntity("Cannot prevent deletion of paste if it is editable")))
    } else None
  }

  def pasteForReadId(readId: String) = Action.async({
    for {
      datum <- dataForReadId(readId)
    } yield getPasteFromFile(datum)
  } recover {
    case _ => NotFound
  })

  def pasteForWriteId(writeId: String) = Action.async({
    for {
      datum <- dataForWriteId(writeId)
    } yield getPasteFromFile(datum)
  } recover {
    case _ => NotFound
  })

  private def dataForWriteId(writeId: String): Future[PasteDatum] =
    for {
      writeOpt <- db.run(PasteWriteData.getWithWriteId(writeId))
      writeInfo <- futureFromOption(writeOpt)
      datum <- dataForReadId(writeInfo.readId)
    } yield datum

  private def dataForReadId(readId: String): Future[PasteDatum] =
    for {
      datumOpt <- db.run(PasteData.getWithReadId(readId))
      datum <- futureFromOption(datumOpt)
      nonExpired <- dataIfNotExpired(datum)
    } yield nonExpired

  private def dataIfNotExpired(datum: PasteDatum): Future[PasteDatum] = {
    for {
      expiration <- futureFromOption(datum.unixExpiration)
      if expiration < System.currentTimeMillis()
      _ <- deletePasteData(datum)
    } yield false
  } recover {
    case e: NoSuchElementException => true
  } map {
    case true => datum
    case false => throw new Exception("Expired paste")
  }

  private def getPasteFromFile(datum: PasteDatum): Result = {
    val body = Source.fromFile(pathConf.pasteFile(datum.revisionId)).mkString
    val paste = Paste.fromDatumWithBody(datum, body)
    Ok(Json.toJson(paste))
  }

  def deletePaste(writeId: String) = Action.async({
    for {
      datum <- dataForWriteId(writeId)
      res <- deletePasteCheckPreconditions(datum)
    } yield res
  } recover {
    case _ => NotFound
  })

  private def deletePasteCheckPreconditions(datum: PasteDatum): Future[Result] = {
    if (!datum.deletable) Future.successful(Forbidden("Paste is not deletable"))
    else doDeletePaste(datum)
  }

  private def doDeletePaste(datum: PasteDatum): Future[Result] = for (_ <- deletePasteData(datum)) yield NoContent

  private def deletePasteData(datum: PasteDatum): Future[Unit] =
    for {
      _ <- db.run(PasteData.withReadId(datum.readId).delete)
      _ <- manager.deletePasteFileLater(datum.revisionId)
    } yield Unit
}

object PasteController {
  implicit val metadataFormat = Json.format[PasteMetadata]
  implicit val lifecycleFormat = Json.format[PasteLifecycleInfo]
  implicit val pasteFormat = Json.format[Paste]
  implicit val idsWrites = Json.writes[PasteIds]

  private def writeToFile(file: File, content: String): Unit = {
    new PrintWriter(file) {
      write(content)
      close()
    }
  }

  private def futureFromOption[T](option: Option[T]): Future[T] = Future.fromTry(Try(option.get))
}
