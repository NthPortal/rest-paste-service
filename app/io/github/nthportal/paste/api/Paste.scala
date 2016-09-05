package io.github.nthportal.paste.api

import java.util.UUID

import models.PasteDatum

case class Paste(metadata: PasteMetadata, lifecycle: PasteLifecycleInfo, body: String) {
  def toDatumWithIds(readId: String, revision: UUID) = {
    PasteDatum(
      readId = readId,
      revision = revision,
      title = metadata.title,
      author = metadata.author,
      description = metadata.description,
      unixExpiration = lifecycle.expiresAfter,
      editable = lifecycle.editable.getOrElse(false),
      deletable = lifecycle.deletable.getOrElse(true))
  }
}

object Paste {
  def fromDatumWithBody(datum: PasteDatum, body: String) = Paste(
    metadata = PasteMetadata(
      title = datum.title,
      author = datum.author,
      description = datum.description),
    lifecycle = PasteLifecycleInfo(
      editable = Some(datum.editable),
      deletable = Some(datum.deletable),
      expiresAfter = datum.unixExpiration),
    body = body)
}
