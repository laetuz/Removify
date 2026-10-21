package id.neotica.removify.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.neotica.removify.ui.components.DarkBackground
import id.neotica.removify.ui.components.DarkPrimary
import id.neotica.removify.ui.navigation.AppNavigationBar
import id.neotica.removify.ui.navigation.AppNavigationRail
import id.neotica.removify.ui.navigation.MainScreenType
import id.neotica.removify.utils.Constants

@Composable
fun MainView(
    current: MainScreenType,
    onNavigate: (MainScreenType) -> Unit,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = maxWidth < 600.dp

        if (isCompact) {
            Scaffold(
                containerColor = DarkBackground,
                topBar = { MainTopBar() },
                bottomBar = {
                    AppNavigationBar(currentScreen = current, onNavigate = onNavigate)
                },
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                        .padding(paddingValues),
                ) {
                    content()
                }
            }
        } else {
            Scaffold(
                containerColor = DarkBackground,
                topBar = { MainTopBar() },
            ) { paddingValues ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                        .padding(paddingValues),
                ) {
                    AppNavigationRail(currentScreen = current, onNavigate = onNavigate)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 1.dp),
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar() {
    Column {
        TopAppBar(
            title = {
                Text(text = Constants.APP_NAME, color = DarkPrimary)
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground),
        )
        HorizontalDivider(thickness = 2.dp, color = DarkPrimary)
    }
}
