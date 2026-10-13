package dev.lantern.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(store: LanternStore) {
    val platform = LocalPlatform.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        Header("Your vault")
        Text(store.location, Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodyLarge)
        Text(
            "${store.notes.size} notes, ${store.tasks.size} tasks, ${store.habits.count { !it.archived }} habits, ${store.txns.size} transactions. " +
                "Everything is plain files in this folder, so any editor, Obsidian or a sync tool can read it.",
            Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (platform.allowVisibleFolder != null && !platform.visibleFolderOn) {
            Action(Icons.Rounded.FolderSpecial, "Keep the vault in a visible folder", "Allow access to all files, then restart Lantern. The vault moves to /Lantern.") { platform.allowVisibleFolder.invoke() }
        }
        Header("Move your data")
        Action(Icons.Rounded.IosShare, "Export the vault as a zip", "Save a copy to share with your laptop or keep as a backup.") { platform.exportVault() }
        Header("About")
        Text("Lantern 0.2.0", Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodyLarge)
        Text("Private notes, journal, tasks, habits and money. MIT licence.", Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 6.dp))
}

@Composable
private fun Action(icon: ImageVector, label: String, note: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.padding(start = 20.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
