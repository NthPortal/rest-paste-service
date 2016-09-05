package io.github.nthportal.paste.core.conf

case class Conf(maxPasteSizeKB: Long = 2000, maxTotalDiskUsageKB: Long = 5000000L)

object Conf {
  object Static {
    val maxTitleLengthChars = 64
    val maxAuthorLengthChars = 64
    val maxDescriptionLengthChars = 200
  }
}
