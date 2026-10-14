package dev.thabit.app

import dev.thabit.ui.*
import dev.thabit.core.ReleaseNotes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val REPO = "https://github.com/zimamlabs/thabit"

/** The mark on a glow, what the app is, the tour and the notes, a privacy card and the links. */
@Composable
fun AboutScreen(store: ThabitStore) {
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Glow(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))) {
            Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BrandMark(Modifier.size(112.dp))
                Text(Brand.NAME, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text("ثابت", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text("Notes, journal, tasks and habits, kept in plain files", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text("Version ${Brand.VERSION}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Thabit (\"steady\") is one private place for your notes, a daily journal, tasks and habits. Everything is plain files in a folder you own, so it works offline, opens in any editor and moves between your phone and laptop by copying the folder.", style = MaterialTheme.typography.bodyLarge)
        Panel {
            Text("Show the tour again", style = MaterialTheme.typography.titleSmall)
            Text("A quick walk through what Thabit does and where your data lives.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { store.tourOpen = true }) { Text("Start the tour") }
        }
        Panel {
            Text("What's new in ${Brand.VERSION}", style = MaterialTheme.typography.titleSmall)
            ReleaseNotes.forVersion(Brand.VERSION).forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium) }
        }
        Panel {
            Text("Private by design", style = MaterialTheme.typography.titleSmall)
            Text("No account, no tracking. Your data stays in ${store.location}.", style = MaterialTheme.typography.bodyMedium)
        }
        Panel {
            Text("Links", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { uri.openUri(REPO) }) { Text("Repository") }
                TextButton(onClick = { uri.openUri("$REPO/releases") }) { Text("Releases") }
                TextButton(onClick = { uri.openUri("$REPO/issues") }) { Text("Issues") }
                TextButton(onClick = { uri.openUri("$REPO/blob/main/LICENSE") }) { Text("Licence") }
            }
        }
        Text("by Zimam Labs  ·  MIT licence", Modifier.fillMaxWidth().padding(vertical = 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
