package dev.lantern.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

private enum class Section(val label: String, val icon: ImageVector) {
    Today("Today", Icons.Rounded.LightMode),
    Notes("Notes", Icons.Rounded.Description),
    Tasks("Tasks", Icons.Rounded.CheckCircle),
    Habits("Habits", Icons.Rounded.Loop),
    Money("Money", Icons.Rounded.Savings),
}

@Composable
fun LanternApp(store: LanternStore) {
    LanternTheme {
        var section by remember { mutableStateOf(Section.Today) }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Section.entries.forEach { s ->
                        NavigationBarItem(
                            selected = section == s,
                            onClick = { section = s },
                            icon = { Icon(s.icon, null) },
                            label = { Text(s.label) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (section) {
                    Section.Today -> TodayScreen(store)
                    Section.Notes -> NotesScreen(store)
                    Section.Tasks -> TasksScreen(store)
                    Section.Habits -> HabitsScreen(store)
                    Section.Money -> MoneyScreen(store)
                }
            }
        }
    }
}
