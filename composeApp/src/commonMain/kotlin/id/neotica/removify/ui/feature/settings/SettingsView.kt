package id.neotica.removify.ui.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import id.neotica.removify.ui.components.NeoCard
import id.neotica.removify.ui.components.TransparentText40
import id.neotica.removify.ui.feature.export.DcimExportCard
import id.neotica.removify.utils.Constants

@Composable
fun SettingsView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )

        DcimExportCard()

        NeoCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Removify v${Constants.APP_VERSION}",
                    color = TransparentText40,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
