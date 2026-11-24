package id.neotica.removify.di

import id.neotica.removify.data.local.JvmMediaDateFixRepository
import id.neotica.removify.domain.repository.MediaDateFixRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule: Module = module {
    singleOf(::JvmMediaDateFixRepository).bind(MediaDateFixRepository::class)
}
