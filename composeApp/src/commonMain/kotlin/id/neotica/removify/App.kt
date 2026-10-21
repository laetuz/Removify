package id.neotica.removify

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import id.neotica.removify.ui.MainView
import id.neotica.removify.ui.components.RemovifyTheme
import id.neotica.removify.ui.feature.deck.SwipeDeckView
import id.neotica.removify.ui.feature.settings.SettingsView
import id.neotica.removify.ui.feature.setup.SessionSetupView
import id.neotica.removify.ui.feature.source.SourceView
import id.neotica.removify.ui.feature.summary.SessionSummaryView
import id.neotica.removify.ui.feature.trash.TrashView
import id.neotica.removify.ui.navigation.MainScreenType
import id.neotica.removify.ui.navigation.Screen
import id.neotica.removify.ui.navigation.toMainScreenType
import id.neotica.removify.ui.navigation.toScreen

@Composable
fun App() {
    RemovifyTheme {
        val backStack = remember { NavBackStack<Screen>(Screen.Source) }

        NavDisplay(
            backStack = backStack,
            entryProvider = { screen ->
                NavEntry(key = screen) { key ->
                    when (key) {
                        Screen.Setup -> SessionSetupView(
                            onBack = { backStack.removeLastOrNull() },
                            onStart = { backStack.add(Screen.Deck) },
                        )

                        Screen.Deck -> SwipeDeckView(
                            onFinish = {
                                backStack.clear()
                                backStack.add(Screen.Summary)
                            },
                        )

                        Screen.Summary -> SessionSummaryView(
                            onNewSession = {
                                backStack.clear()
                                backStack.add(Screen.Source)
                            },
                            onOpenTrash = {
                                backStack.clear()
                                backStack.add(Screen.Trash)
                            },
                        )

                        else -> MainView(
                            current = key.toMainScreenType() ?: MainScreenType.REVIEW,
                            onNavigate = { type ->
                                backStack.clear()
                                backStack.add(type.toScreen())
                            },
                        ) {
                            when (key) {
                                Screen.Source -> SourceView(
                                    onContinue = { backStack.add(Screen.Setup) },
                                )

                                Screen.Trash -> TrashView()
                                else -> SettingsView()
                            }
                        }
                    }
                }
            },
        )
    }
}
