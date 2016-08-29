package io.github.nthportal.paste.data

case class PasteReadDatum(readId: String,
                          title: Option[String],
                          author: Option[String],
                          description: Option[String],
                          unixExpiration: Option[Long])

