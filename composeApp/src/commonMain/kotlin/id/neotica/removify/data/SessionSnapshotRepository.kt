package id.neotica.removify.data

import id.neotica.removify.domain.model.SessionSnapshot
import id.neotica.removify.platform.deleteAppFile
import id.neotica.removify.platform.readAppFile
import id.neotica.removify.platform.writeAppFile
import kotlinx.serialization.json.Json

class SessionSnapshotRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    suspend fun load(): SessionSnapshot? {
        val content = readAppFile(FILE_NAME) ?: return null
        return runCatching {
            json.decodeFromString(SessionSnapshot.serializer(), content)
        }.getOrNull()?.takeIf { it.version == CURRENT_VERSION }
    }

    suspend fun save(snapshot: SessionSnapshot): Result<Unit> =
        writeAppFile(FILE_NAME, json.encodeToString(SessionSnapshot.serializer(), snapshot))

    suspend fun clear(): Result<Unit> = deleteAppFile(FILE_NAME)

    private companion object {
        const val FILE_NAME = "session.json"
        const val CURRENT_VERSION = 1
    }
}
