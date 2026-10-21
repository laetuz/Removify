package id.neotica.removify.ui.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.neotica.removify.ui.components.DarkPrimary
import id.neotica.removify.ui.components.DarkPrimaryCard

@Composable
fun AppNavigationBar(
    currentScreen: MainScreenType,
    onNavigate: (MainScreenType) -> Unit,
    modifier: Modifier = Modifier,
    items: List<NavItem> = navItems,
) {
    NavigationBar(
        containerColor = DarkPrimaryCard,
        tonalElevation = 0.dp,
        modifier = modifier,
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentScreen == item.type,
                onClick = { onNavigate(item.type) },
                icon = {
                    Text(text = item.indicator, style = MaterialTheme.typography.titleLarge)
                },
                label = {
                    Text(text = item.label, style = MaterialTheme.typography.labelSmall)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DarkPrimary,
                    unselectedIconColor = DarkPrimary.copy(alpha = 0.5f),
                    selectedTextColor = DarkPrimary,
                    unselectedTextColor = DarkPrimary.copy(alpha = 0.5f),
                    indicatorColor = DarkPrimary.copy(alpha = 0.15f),
                ),
            )
        }
    }
}
