package id.neotica.removify.domain.repository

import id.neotica.removify.domain.model.DateFixCandidate
import id.neotica.removify.domain.model.DateFixPreview
import id.neotica.removify.domain.model.DateFixProgress
import id.neotica.removify.domain.model.DateFixReport
import id.neotica.removify.domain.model.MediaSource

interface MediaDateFixRepository {
    suspend fun preview(source: MediaSource): Result<DateFixPreview>

    suspend fun apply(
        candidates: List<DateFixCandidate>,
        onProgress: (DateFixProgress) -> Unit,
        shouldCancel: () -> Boolean,
    ): Result<DateFixReport>
}
