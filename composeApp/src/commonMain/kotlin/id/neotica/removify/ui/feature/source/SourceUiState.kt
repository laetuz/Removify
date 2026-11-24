package id.neotica.removify.ui.feature.source

data class SourceUiState(
    val folderName: String? = null,
    val mediaCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val resumeAvailable: Boolean = false,
    val resumeDecided: Int = 0,
)
