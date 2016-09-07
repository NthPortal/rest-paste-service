package io.github.nthportal.paste.core.conf

case class Conf private[core](maxTotalPasteBodyDiskUsageKB: Long = 5000000L,
                              perIPLimits: Limits = Limits(),
                              authenticatedLimits: Limits = Limits()) {
  val static = Conf.Static
}

object Conf {
  object Static {
    val maxTitleLengthChars = 64
    val maxAuthorLengthChars = 64
    val maxDescriptionLengthChars = 200
  }
}
