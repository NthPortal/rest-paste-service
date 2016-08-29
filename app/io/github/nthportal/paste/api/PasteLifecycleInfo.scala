package io.github.nthportal.paste.api

case class PasteLifecycleInfo(editable: Option[Boolean] = Some(true),
                              deletable: Option[Boolean] = Some(true),
                              expiresAfter: Option[Long] = None)
