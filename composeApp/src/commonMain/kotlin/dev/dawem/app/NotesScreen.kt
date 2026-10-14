package dev.dawem.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.dawem.core.Note
import kotlinx.coroutines.delay

@Composable
fun NotesScreen(store: DawemStore, tag: String? = null) {
    var openId by remember { mutableStateOf<String?>(null) }
    val open = store.notes.firstOrNull { it.id == openId }
    if (openId != null) {
        NoteEditor(store, open ?: Note(openId!!, "", "", "", ""), onClose = { openId = null })
        return
    }
    var query by remember { mutableStateOf("") }
    val shown = store.notes.filter { (tag == null || tag in it.tags) && (query.isBlank() || it.title.contains(query, true) || it.body.contains(query, true) || it.tags.any { t -> t.contains(query, true) }) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = { FloatingActionButton(onClick = { openId = "" }) { Icon(Icons.Rounded.Add, "New note") } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                query, { query = it }, singleLine = true, placeholder = { Text("Search notes") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            )
            if (shown.isEmpty()) EmptyHint(if (store.notes.isEmpty()) "No notes yet" else "Nothing found", "Tap + to write one. Notes are Markdown files in your vault.")
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(shown, key = { it.id }) { n ->
                    Panel(onClick = { openId = n.id }) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(n.title.ifBlank { "Untitled" }, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (n.pinned) Icon(Icons.Rounded.PushPin, "Pinned", tint = MaterialTheme.colorScheme.primary)
                        }
                        val preview = n.body.lineSequence().filter { it.isNotBlank() && !it.startsWith("#") }.take(3).joinToString(" ")
                        if (preview.isNotBlank()) Text(preview, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        if (n.tags.isNotEmpty()) Text(n.tags.joinToString("  ") { "#$it" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditor(store: DawemStore, start: Note, onClose: () -> Unit) {
    var note by remember(start.id) { mutableStateOf(start.copy(id = if (start.id.isEmpty() || store.notes.none { it.id == start.id }) "" else start.id)) }
    var tags by remember(start.id) { mutableStateOf(start.tags.joinToString(" ")) }
    // A note you open is read first; a new one starts in edit mode. The pencil switches.
    var reading by remember(start.id) { mutableStateOf(start.id.isNotEmpty() && store.notes.any { it.id == start.id }) }
    // Typing is saved shortly after it stops; a note that is still empty is never written.
    LaunchedEffect(note.title, note.body, note.pinned, tags) {
        if (note.title.isBlank() && note.body.isBlank()) return@LaunchedEffect
        delay(600)
        val saved = store.saveNote(note.copy(tags = tags.split(' ', ',').map { it.trim().trimStart('#') }.filter { it.isNotEmpty() }))
        if (note.id.isEmpty()) note = note.copy(id = saved.id, created = saved.created)
    }
    PlatformBackHandler(true, onClose)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (note.title.isBlank()) "New note" else note.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { reading = !reading }) { Icon(if (reading) Icons.Rounded.Edit else Icons.Rounded.Visibility, if (reading) "Edit" else "Read") }
                    IconButton(onClick = { note = note.copy(pinned = !note.pinned) }) { Icon(Icons.Rounded.PushPin, "Pin", tint = if (note.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton(onClick = { if (note.id.isNotEmpty()) store.trashNote(note.id); onClose() }) { Icon(Icons.Rounded.Delete, "Delete") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (reading) Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(note.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (tags.isNotBlank()) Text(tags.split(' ', ',').filter { it.isNotBlank() }.joinToString("  ") { "#" + it.trimStart('#') }, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            MarkdownText(note.body, Modifier.padding(bottom = 32.dp))
        } else Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(note.title, { note = note.copy(title = it) }, singleLine = true, placeholder = { Text("Title") }, modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.titleLarge)
            OutlinedTextField(tags, { tags = it }, singleLine = true, placeholder = { Text("Tags, separated by spaces") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(note.body, { note = note.copy(body = it) }, placeholder = { Text("Write in Markdown…") }, modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 12.dp))
        }
    }
}
