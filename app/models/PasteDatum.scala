package models

import java.util.UUID

case class PasteDatum(readId: String,
                      revision: UUID,
                      title: Option[String],
                      author: Option[String],
                      description: Option[String],
                      unixExpiration: Option[Long],
                      editable: Boolean,
                      deletable: Boolean)

