package id.neotica.removify.domain.repository

import id.neotica.removify.domain.model.Settings

interface SettingsRepository {
    suspend fun load(): Settings
    suspend fun save(settings: Settings)
}
