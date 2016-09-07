package io.github.nthportal.paste.core.conf

case class Limits private[core](maxPasteSizeKB: Option[Long] = Some(2000),
                                maxTotalPasteDiskUsageKB: Option[Long] = None,
                                maxTotalPasteCount: Option[Int] = None)
