package id.neotica.removify.ui.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.neotica.removify.ui.components.NeoCard
import id.neotica.removify.ui.components.TransparentText40
import id.neotica.removify.utils.Constants

@Composable
fun SettingsView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        NeoCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Preferences will live here.",
                    color = TransparentText40,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "v${Constants.APP_VERSION}",
                    color = TransparentText40,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
