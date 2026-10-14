package dev.thabit.journal

import dev.thabit.ui.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import dev.thabit.journal.JournalEntry

private val faces = listOf("😞", "😕", "😐", "🙂", "😄")

/** Every day you wrote about, newest first; tap one to read or change it. */
@Composable
fun JournalScreen(state: JournalState, today: kotlinx.datetime.LocalDate) {
    var open by remember { mutableStateOf<LocalDate?>(null) }
    // The list is read again whenever today's entry changes.
    val dates = state.dates()
    PlatformBackHandler(open != null) { open = null }
    val day = open
    if (day != null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(prettyDate(day) + " " + day.year, style = MaterialTheme.typography.titleLarge)
            JournalPanel(state, day, Modifier.padding(top = 12.dp))
        }
        return
    }
    if (dates.isEmpty()) EmptyHint("No entries yet", "Write a few lines on the Today screen and they will show up here.")
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(dates, key = { it.toString() }) { d ->
            val e: JournalEntry? = state.entry(d)
            Panel(onClick = { open = d }) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(e?.mood?.let { faces[(it - 1).coerceIn(0, 4)] } ?: "·", style = MaterialTheme.typography.titleLarge)
                    Text(prettyDate(d) + " " + d.year, style = MaterialTheme.typography.titleSmall)
                }
                val text = e?.body.orEmpty()
                if (text.isNotBlank()) Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
