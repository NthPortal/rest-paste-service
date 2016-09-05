package controllers

import java.io.{File, PrintWriter}
import javax.inject.{Inject, Singleton}

import io.github.nthportal.paste.api.{Paste, PasteIds, PasteLifecycleInfo, PasteMetadata}
import io.github.nthportal.paste.core._
import models.{PasteData, PasteDatum, PasteWriteData}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.util.Try

@Singleton
class PasteController @Inject()(manager: Manager, idManager: IdManager) extends Controller {
  import PasteController._
  import manager.dbConfig.driver.api._

  private val db = manager.db

  def createPaste = Action.async(parse.json) {
    implicit request => {
      for (paste <- pasteFormat.reads(request.body)) yield createNewPaste(paste)
    } getOrElse Future.successful(BadRequest)
  }

  def modifyPaste(writeId: String) = Action.async(parse.json) {
    implicit request => {
      for (datum <- dataForWriteId(writeId))
      yield modifyPasteGetData(datum, request)
    } recover {
      case _ => NotFound
    }
  }

  private def modifyPasteGetData(datum: PasteDatum, request: Request[JsValue]): Result = {
    for (paste <- pasteFormat.reads(request.body)) yield modifyPasteCheckPreconditions(datum, paste)
  } getOrElse BadRequest

  private def modifyPasteCheckPreconditions(datum: PasteDatum, paste: Paste): Result = {
    val lifecycle = paste.lifecycle
    if (!datum.editable) {
      Forbidden("Paste is not editable")
    } else if (lifecycle.expiresAfter.getOrElse(0L) > datum.unixExpiration.getOrElse(Long.MaxValue)) {
      Forbidden("Cannot make paste expire later")
    } else if (!lifecycle.deletable.getOrElse(true) && lifecycle.editable.getOrElse(true)) {
      UnprocessableEntity("Cannot prevent deletion of paste if it is still editable")
    } else {
      doModifyPaste(datum, paste)
    }
  }

  private def doModifyPaste(datum: PasteDatum, paste: Paste): Result = {
    val metadata = paste.metadata
    val lifecycle = paste.lifecycle
    val newDatum = datum.copy(
      title = metadata.title orElse datum.title,
      author = metadata.author orElse datum.author,
      description = metadata.description orElse datum.description,
      deletable = lifecycle.deletable getOrElse datum.deletable,
      editable = lifecycle.editable getOrElse datum.editable,
      unixExpiration = lifecycle.expiresAfter orElse datum.unixExpiration
    )
    // TODO: update database value
    ???
  }

  private def dataForWriteId(writeId: String): Future[PasteDatum] =
    for {
      writeOpt <- manager.db.run(PasteWriteData.withWriteId(writeId))
      writeInfo <- Future.fromTry(Try(writeOpt.get))
      datum <- dataForReadId(writeInfo.readId)
    } yield datum

  private def dataForReadId(readId: String): Future[PasteDatum] =
    // TODO: check expiration
    for {
      datumOpt <- manager.db.run(PasteData.withReadId(readId))
      datum <- Future.fromTry(Try(datumOpt.get))
    } yield datum

  def getPaste(readId: String) = Action.async({
    for {
      datum <- dataForReadId(readId)
    } yield getPasteFromFile(datum)
  } recover {
    case _ => NotFound
  })

  private def getPasteFromFile(datum: PasteDatum): Result = {
    val body = Source.fromFile(manager.pathConf.pasteDir + File.separator + datum.readId).mkString
    val paste = Paste.fromDatumWithBody(datum, body)
    Ok(Json.toJson(paste))
  }

  def deletePaste(writeId: String) = TODO

  private def createNewPaste(paste: Paste): Future[Result] = {
    for {
      pair <- idManager.unusedRandomIdPair
      _ = new PrintWriter(manager.pathConf.pasteDir + File.separator + pair.readId) {
        write(paste.body)
        close()
      }
      datum = paste.toDatum(pair.readId)
      _ <- db.run(PasteData.insertOrUpdate(datum))
      deletable = paste.lifecycle.deletable.getOrElse(true)
      _ <- if (deletable) db.run(PasteWriteData.insertOrUpdate(pair)) else Future.successful(null)
    } yield {
      val writeId = if (deletable) Some(pair.writeId) else None
      Ok(idsWrites.writes(PasteIds(pair.readId, writeId)))
    }
  }
}

object PasteController {
  implicit val metadataFormat = Json.format[PasteMetadata]
  implicit val lifecycleFormat = Json.format[PasteLifecycleInfo]
  implicit val pasteFormat = Json.format[Paste]
  implicit val idsWrites = Json.writes[PasteIds]
}
