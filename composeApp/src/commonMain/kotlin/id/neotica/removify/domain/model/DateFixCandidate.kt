package id.neotica.removify.domain.model

data class DateFixCandidate(
    val id: String,
    val name: String,
    val type: MediaType,
    val targetEpochMs: Long,
    val currentEpochMs: Long?,
    val alreadyMatches: Boolean,
)
