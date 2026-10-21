package id.neotica.removify.di

import id.neotica.removify.data.MediaRepositoryImpl
import id.neotica.removify.data.TrashRepositoryImpl
import id.neotica.removify.domain.repository.MediaRepository
import id.neotica.removify.domain.repository.TrashRepository
import id.neotica.removify.session.SessionStore
import id.neotica.removify.ui.feature.deck.SwipeDeckViewModel
import id.neotica.removify.ui.feature.source.SourceViewModel
import id.neotica.removify.ui.feature.trash.TrashViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    singleOf(::MediaRepositoryImpl).bind(MediaRepository::class)
    singleOf(::TrashRepositoryImpl).bind(TrashRepository::class)
    singleOf(::SessionStore)
    viewModelOf(::SourceViewModel)
    viewModelOf(::SwipeDeckViewModel)
    viewModelOf(::TrashViewModel)
}

val appModules = arrayOf(appModule, platformModule)

fun initializeKoin() = startKoin { modules(*appModules) }
