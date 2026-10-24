package id.neotica.removify.domain.repository

import id.neotica.removify.domain.model.DcimCopyResult
import id.neotica.removify.domain.model.DcimExportProgress
import id.neotica.removify.domain.model.DcimItem

interface DcimExportRepository {
    suspend fun listDcim(): Result<List<DcimItem>>

    suspend fun copyToTree(
        items: List<DcimItem>,
        treeUri: String,
        shouldCancel: () -> Boolean,
        onProgress: (DcimExportProgress) -> Unit,
    ): Result<DcimCopyResult>
}
