package id.neotica.removify.di

import id.neotica.removify.data.DcimExportRepositoryImpl
import id.neotica.removify.domain.repository.DcimExportRepository
import id.neotica.removify.ui.feature.export.DcimExportViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule: Module = module {
    singleOf(::DcimExportRepositoryImpl).bind(DcimExportRepository::class)
    viewModelOf(::DcimExportViewModel)
}
