package controllers

import java.io.PrintWriter
import javax.inject.{Inject, Singleton}

import io.github.nthportal.paste.api.{Paste, PasteIds, PasteLifecycleInfo, PasteMetadata}
import io.github.nthportal.paste.core._
import models.{PasteData, PasteDatum, PasteWriteData}
import org.apache.commons.io.FileUtils
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
  private val pathConf = manager.pathConf

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
      UnprocessableEntity("Cannot prevent deletion of paste if it is editable")
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
      writeInfo <- Future.fromTry(Try(writeOpt.get))
      datum <- dataForReadId(writeInfo.readId)
    } yield datum

  private def dataForReadId(readId: String): Future[PasteDatum] =
  // TODO: check expiration
    for {
      datumOpt <- db.run(PasteData.getWithReadId(readId))
      datum <- Future.fromTry(Try(datumOpt.get))
    } yield datum

  private def getPasteFromFile(datum: PasteDatum): Result = {
    val body = Source.fromFile(pathConf.pasteFile(datum.readId)).mkString
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
    else deletePasteData(datum)
  }

  private def deletePasteData(datum: PasteDatum): Future[Result] =
    for {
      _ <- db.run(PasteData.withReadId(datum.readId).delete)
      _ = FileUtils.forceDelete(pathConf.pasteFile(datum.readId))
    } yield NoContent

  private def createNewPaste(paste: Paste): Future[Result] = {
    for {
      pair <- idManager.unusedRandomIdPair
      _ = new PrintWriter(pathConf.pasteFile(pair.readId)) {
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
