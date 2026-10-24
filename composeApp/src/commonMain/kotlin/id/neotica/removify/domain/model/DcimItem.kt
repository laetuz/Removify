package id.neotica.removify.domain.model

data class DcimItem(
    val id: Long,
    val contentUri: String,
    val name: String,
    val relativePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateTakenEpochMs: Long,
    val type: MediaType,
)
