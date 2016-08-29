package controllers

import javax.inject.{Inject, Singleton}

import controllers.PasteController._
import io.github.nthportal.paste.api.{PasteIds, PasteLifecycleInfo}
import io.github.nthportal.paste.core.{Manager, Paste, PasteMetadata}
import io.github.nthportal.paste.data.{PasteReadData, PasteReadDatum, PasteWriteData, PasteWriteDatum}
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class PasteController @Inject() extends Controller {
  def createPaste = Action(parse.multipartFormData) {
    implicit request => {
      for {
        file <- request.body.file(FormKeys.file)
        metadataSeq <- request.body.dataParts.get(FormKeys.metadata)
        lifecycleSeq <- request.body.dataParts.get(FormKeys.lifecycle)
        metadata = metadataReads.reads(Json.parse(metadataSeq.head))
        lifecycle = lifecycleReads.reads(Json.parse(lifecycleSeq.head))
      } yield Ok("Worked")
    } getOrElse BadRequest
  }

  def modifyPaste(writeId: String) = Action.async(parse.multipartFormData) {
    implicit request => {
      for {
        writeDatumSeq <- db.run(PasteWriteData.withWriteId(writeId))
        writeDatum <- writeDatumSeq.headOption
        readDatumSeq <- db.run(PasteReadData.withReadId(writeDatum.readId))
        readDatum <- readDatumSeq.headOption
      } yield modifyExistingPaste(writeDatum, readDatum, request)
    } recover {
      case _ => NotFound
    }
  }

  private def modifyExistingPaste(writeDatum: PasteWriteDatum,
                                  readDatum: PasteReadDatum,
                                  request: Request[MultipartFormData[Files.TemporaryFile]]): Result = {
    for {
      file <- request.body.file(FormKeys.file)
      metadataSeq <- request.body.dataParts.get(FormKeys.metadata)
      lifecycleSeq <- request.body.dataParts.get(FormKeys.lifecycle)
      metadata = metadataReads.reads(Json.parse(metadataSeq.head))
      lifecycle = lifecycleReads.reads(Json.parse(lifecycleSeq.head))
    } yield Ok("Updated")
  } getOrElse BadRequest

  def getPaste(readId: String) = TODO

  def deletePaste(writeId: String) = TODO

  private def createPaste(paste: Paste): Unit = {

  }
}

object PasteController {
  val metadataReads = Json.reads[PasteMetadata]
  val lifecycleReads = Json.reads[PasteLifecycleInfo]
  val idsWrites = Json.writes[PasteIds]

  val db = Manager.config.db

  object FormKeys {
    val file = "file"
    val metadata = "metadata"
    val lifecycle = "lifecycle"
  }

}
