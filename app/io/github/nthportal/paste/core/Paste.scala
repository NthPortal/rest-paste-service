package io.github.nthportal.paste.core

import io.github.nthportal.paste.api.PasteLifecycleInfo

case class Paste(metadata: PasteMetadata, lifecycleInfo: PasteLifecycleInfo, body: String)
