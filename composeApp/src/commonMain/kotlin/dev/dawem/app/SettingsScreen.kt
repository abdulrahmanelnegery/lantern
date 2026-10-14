package dev.dawem.app

import dev.dawem.ui.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun SettingsScreen(store: DawemStore) {
    val platform = LocalPlatform.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        Header("Appearance")
        Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (id, label) ->
                FilterChip(store.settings.mode == id, { store.saveSettings(store.settings.copy(mode = id)) }, { Text(label) })
            }
        }
        Row(Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Palettes.forEach { p ->
                val on = store.settings.palette == p.id
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { store.saveSettings(store.settings.copy(palette = p.id)) }) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF000000 or p.swatch))
                            .then(if (on) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) { if (on) Icon(Icons.Rounded.Check, null, tint = Color.Black.copy(alpha = 0.7f)) }
                    Text(p.label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Header("Today page")
        val order = todayOrder(store.settings)
        order.forEachIndexed { i, id ->
            val label = TodayCards.first { it.first == id }.second
            val hidden = id in store.settings.homeHidden
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Switch(!hidden, { on -> store.saveSettings(store.settings.copy(homeOrder = order, homeHidden = if (on) store.settings.homeHidden - id else store.settings.homeHidden + id)) })
                Text(label, Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
                androidx.compose.material3.IconButton(enabled = i > 0, onClick = { store.saveSettings(store.settings.copy(homeOrder = order.toMutableList().also { it[i] = it[i - 1]; it[i - 1] = id })) }) { Icon(Icons.Rounded.KeyboardArrowUp, "Move up") }
                androidx.compose.material3.IconButton(enabled = i < order.lastIndex, onClick = { store.saveSettings(store.settings.copy(homeOrder = order.toMutableList().also { it[i] = it[i + 1]; it[i + 1] = id })) }) { Icon(Icons.Rounded.KeyboardArrowDown, "Move down") }
            }
        }
        Header("Your vault")
        Text(store.location, Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodyLarge)
        Text(
            "${store.notes.size} notes, ${store.tasks.size} tasks, ${store.habits.count { !it.archived }} habits. " +
                "Everything is plain files in this folder, so any editor, Obsidian or a sync tool can read it.",
            Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (platform.allowVisibleFolder != null && !platform.visibleFolderOn) {
            Action(Icons.Rounded.FolderSpecial, "Keep the vault in a visible folder", "Allow access to all files, then restart Dawem. The vault moves to /Dawem.") { platform.allowVisibleFolder.invoke() }
        }
        Header("Move your data")
        Action(Icons.Rounded.IosShare, "Export the vault as a zip", "Save a copy to share with your laptop or keep as a backup.") { platform.exportVault() }
        Header("About")
        Text("${Brand.NAME} ${Brand.VERSION}  ·  by ${Brand.MAKER}", Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodyLarge)
        Text("Private notes, journal, tasks and habits. MIT licence.", Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
