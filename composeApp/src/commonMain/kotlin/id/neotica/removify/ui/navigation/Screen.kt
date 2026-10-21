package id.neotica.removify.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {
    @Serializable data object Source : Screen
    @Serializable data object Setup : Screen
    @Serializable data object Deck : Screen
    @Serializable data object Summary : Screen
    @Serializable data object Trash : Screen
    @Serializable data object Settings : Screen
}

fun Screen.toMainScreenType(): MainScreenType? = when (this) {
    Screen.Source -> MainScreenType.REVIEW
    Screen.Trash -> MainScreenType.TRASH
    Screen.Settings -> MainScreenType.SETTINGS
    else -> null
}

fun MainScreenType.toScreen(): Screen = when (this) {
    MainScreenType.REVIEW -> Screen.Source
    MainScreenType.TRASH -> Screen.Trash
    MainScreenType.SETTINGS -> Screen.Settings
}
