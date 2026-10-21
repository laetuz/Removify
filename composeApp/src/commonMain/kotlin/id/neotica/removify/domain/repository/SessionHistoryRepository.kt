package id.neotica.removify.domain.repository

import id.neotica.removify.domain.model.ReviewSession

interface SessionHistoryRepository {
    suspend fun record(session: ReviewSession): Result<Unit>
    suspend fun recent(limit: Int = 50): Result<List<ReviewSession>>
}
