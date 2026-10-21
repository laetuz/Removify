package id.neotica.removify.platform

import id.neotica.removify.domain.model.MediaItem

expect suspend fun openVideo(item: MediaItem): Boolean
