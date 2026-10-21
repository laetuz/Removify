package id.neotica.removify.ui.feature.source

data class SourceUiState(
    val folderPath: String? = null,
    val mediaCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
