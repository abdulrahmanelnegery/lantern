package dev.dawem.journal

import dev.dawem.ui.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val moods = listOf("😞" to "Low", "😕" to "Meh", "😐" to "Okay", "🙂" to "Good", "😄" to "Great")

/** Mood and the journal text for one day, saved a moment after you stop typing. */
@Composable
fun JournalPanel(state: JournalState, date: kotlinx.datetime.LocalDate, modifier: Modifier = Modifier) {
    val entry = state.entry(date)
    var text by remember(date) { mutableStateOf(entry?.body.orEmpty()) }
    var mood by remember(date) { mutableStateOf(entry?.mood) }
    LaunchedEffect(text, mood) {
        if (text == entry?.body.orEmpty() && mood == entry?.mood) return@LaunchedEffect
        delay(700)
        state.save(JournalEntry(date, text, mood, entry?.tags.orEmpty()))
    }
    Panel(modifier) {
        Text("How are you?", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            moods.forEachIndexed { i, (face, label) ->
                val value = i + 1
                Column(
                    Modifier.clip(CircleShape).clickable { mood = if (mood == value) null else value }.padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(if (mood == value) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) { Text(face, style = MaterialTheme.typography.headlineSmall) }
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        OutlinedTextField(
            text, { text = it },
            placeholder = { Text("What happened? What are you thankful for?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 12,
        )
    }
}
