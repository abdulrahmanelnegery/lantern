package dev.dawem.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** What the app is, who makes it, where its code lives and how it is licensed. */
@Composable
fun AboutScreen() {
    val links = LocalUriHandler.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(Brand.NAME, style = MaterialTheme.typography.headlineMedium)
        Text("داوم  ·  keep at it", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Version ${Brand.VERSION}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "Private notes, journal, tasks and habits in plain files you own. No account, no tracking, nothing leaves your device.",
            Modifier.padding(top = 8.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
        )
        Text("by ${Brand.MAKER}", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium, color = Color(0xFFFF5A1F))
        OutlinedButton(onClick = { links.openUri("https://github.com/zimamlabs") }, Modifier.fillMaxWidth()) { Text("Zimam on GitHub") }
        OutlinedButton(onClick = { links.openUri("https://github.com/zimamlabs/dawem") }, Modifier.fillMaxWidth()) { Text("Source code") }
        OutlinedButton(onClick = { links.openUri("https://github.com/zimamlabs/dawem/issues") }, Modifier.fillMaxWidth()) { Text("Report a problem") }
        Text(
            "© 2026 Zimam. Free software under the MIT licence.",
            Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
    }
}
