package models

case class PasteDatum(readId: String,
                      title: Option[String],
                      author: Option[String],
                      description: Option[String],
                      unixExpiration: Option[Long],
                      editable: Boolean,
                      deletable: Boolean)

