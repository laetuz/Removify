package id.neotica.removify.ui.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.neotica.removify.ui.components.DarkPrimary
import id.neotica.removify.ui.components.DarkPrimaryCard

@Composable
fun AppNavigationRail(
    currentScreen: MainScreenType,
    onNavigate: (MainScreenType) -> Unit,
    modifier: Modifier = Modifier,
    items: List<NavItem> = navItems,
) {
    NavigationRail(
        containerColor = DarkPrimaryCard,
        modifier = modifier
            .fillMaxHeight()
            .width(80.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        items.forEach { item ->
            NavigationRailItem(
                selected = currentScreen == item.type,
                onClick = { onNavigate(item.type) },
                icon = {
                    Text(text = item.indicator, style = MaterialTheme.typography.titleLarge)
                },
                label = {
                    Text(text = item.label, style = MaterialTheme.typography.labelSmall)
                },
                colors = NavigationRailItemDefaults.colors(
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
