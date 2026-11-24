package id.neotica.removify.domain.model

data class DateFixPreview(
    val candidates: List<DateFixCandidate>,
    val skippedCount: Int,
    val skippedExamples: List<String>,
) {
    val matchedCount: Int get() = candidates.size
    val alreadyCount: Int get() = candidates.count { it.alreadyMatches }
    val toUpdateCount: Int get() = candidates.count { !it.alreadyMatches }
    val imageCount: Int get() = candidates.count { it.type == MediaType.IMAGE }
    val videoCount: Int get() = candidates.count { it.type == MediaType.VIDEO }
}
