package id.neotica.removify.ui.navigation

enum class MainScreenType { REVIEW, TRASH, SETTINGS }

data class NavItem(
    val type: MainScreenType,
    val label: String,
    val indicator: String,
)

val navItems = listOf(
    NavItem(MainScreenType.REVIEW, "Review", "\uD83D\uDDBC\uFE0F"),
    NavItem(MainScreenType.TRASH, "Trash", "\uD83D\uDDD1\uFE0F"),
    NavItem(MainScreenType.SETTINGS, "Settings", "\u2699\uFE0F"),
)
