package dev.dawem.app

import dev.dawem.ui.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** One box that searches notes, journal entries and tasks. */
@Composable
fun SearchScreen(store: DawemStore, nav: Nav) {
    var query by remember { mutableStateOf("") }
    val hits = remember(query, store.notes, store.tasks, store.todayEntry) { store.vault.search(query) }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            query, { query = it }, singleLine = true, placeholder = { Text("Search notes, journal and tasks") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        )
        if (query.isBlank()) EmptyHint("Search everything", "Type a word to look through your notes, journal and tasks at once.")
        else if (hits.isEmpty()) EmptyHint("Nothing found", "Try another word.")
        LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(hits) { h ->
                Panel(onClick = {
                    when (h.kind) {
                        "note" -> nav.go(Section.Notes)
                        "task" -> nav.go(Section.Tasks)
                        else -> nav.screen = Screen.Journal
                    }
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            when (h.kind) { "note" -> Icons.Rounded.Description; "task" -> Icons.Rounded.CheckCircle; else -> Icons.Rounded.AutoStories },
                            null, tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(h.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(h.snippet, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
