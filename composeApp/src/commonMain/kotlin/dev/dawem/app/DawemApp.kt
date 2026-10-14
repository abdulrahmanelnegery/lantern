package dev.dawem.app

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

enum class Section(val label: String, val icon: ImageVector) {
    Today("Today", Icons.Rounded.LightMode),
    Notes("Notes", Icons.Rounded.Description),
    Tasks("Tasks", Icons.Rounded.CheckCircle),
    Habits("Habits", Icons.Rounded.Loop),
}

/** Where the app is: one of the five sections (maybe filtered), or a screen opened from the side menu. */
class Nav {
    var section by mutableStateOf(Section.Today)
    /** A task list the Tasks section is filtered to; null shows all. */
    var taskList by mutableStateOf<String?>(null)
    /** A tag the Notes section is filtered to; null shows all. */
    var noteTag by mutableStateOf<String?>(null)
    var screen by mutableStateOf(Screen.Main)
    /** The habit whose page is open, when one is. */
    var habitId by mutableStateOf<String?>(null)

    fun go(s: Section, taskList: String? = null, noteTag: String? = null) {
        section = s; this.taskList = taskList; this.noteTag = noteTag; screen = Screen.Main; habitId = null
    }
}

enum class Screen { Main, Search, Journal, Insights, Settings, About }

private val WIDE = 840.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DawemApp(store: DawemStore, start: Nav = remember { Nav() }) {
    var splash by remember { mutableStateOf(true) }
    DawemTheme(store.settings) {
      Box {
        val nav = start
        val drawer = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        // Back goes to Today from any other section, then closes; it leaves a side screen, a filtered view or a habit's page first.
        PlatformBackHandler(nav.screen == Screen.Main && nav.section != Section.Today) { nav.go(Section.Today) }
        PlatformBackHandler(nav.screen == Screen.Main && nav.habitId != null) { nav.habitId = null }
        PlatformBackHandler(nav.screen != Screen.Main) { nav.screen = Screen.Main }
        PlatformBackHandler(nav.screen == Screen.Main && (nav.taskList != null || nav.noteTag != null)) { nav.taskList = null; nav.noteTag = null }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= WIDE
            val content: @Composable () -> Unit = {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(
                            title = { Text(title(nav), fontWeight = FontWeight.SemiBold) },
                            navigationIcon = { if (!wide) IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Rounded.Menu, "Menu") } },
                            actions = { IconButton(onClick = { nav.screen = Screen.Search }) { Icon(Icons.Rounded.Search, "Search") } },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                        )
                    },
                    bottomBar = {
                        if (!wide && nav.screen == Screen.Main) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            Section.entries.forEach { s ->
                                NavigationBarItem(selected = nav.section == s, onClick = { nav.go(s) }, icon = { Icon(s.icon, null) }, label = { Text(s.label) })
                            }
                        }
                    },
                ) { padding ->
                    // On a wide screen the content keeps a readable width instead of stretching edge to edge.
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
                        Box(Modifier.fillMaxHeight().widthIn(max = 880.dp)) {
                        when (nav.screen) {
                            Screen.Search -> SearchScreen(store, nav)
                            Screen.Journal -> JournalScreen(store)
                            Screen.Insights -> InsightsScreen(store, onOpenHabit = { id -> nav.go(Section.Habits); nav.habitId = id })
                            Screen.Settings -> SettingsScreen(store)
                            Screen.About -> AboutScreen()
                            Screen.Main -> when (nav.section) {
                                Section.Today -> TodayScreen(store)
                                Section.Notes -> NotesScreen(store, nav.noteTag)
                                Section.Tasks -> TasksScreen(store, nav.taskList)
                                Section.Habits -> HabitsScreen(store, nav.habitId, onOpen = { nav.habitId = it }, onClose = { nav.habitId = null })
                            }
                        }
                        }
                    }
                }
            }
            if (wide) {
                PermanentNavigationDrawer(drawerContent = { PermanentDrawerSheet(Modifier.width(300.dp)) { MenuContent(store, nav) {} } }) { content() }
            } else {
                ModalNavigationDrawer(drawerState = drawer, drawerContent = { ModalDrawerSheet { MenuContent(store, nav) { scope.launch { drawer.close() } } } }) { content() }
            }
        }
        androidx.compose.animation.AnimatedVisibility(splash, exit = androidx.compose.animation.fadeOut()) { Splash { splash = false } }
      }
    }
}

private fun title(nav: Nav): String = when (nav.screen) {
    Screen.Search -> "Search"
    Screen.Journal -> "Journal"
    Screen.Insights -> "Insights"
    Screen.Settings -> "Settings"
    Screen.About -> "About"
    Screen.Main -> when {
        nav.section == Section.Tasks && nav.taskList != null -> nav.taskList!!
        nav.section == Section.Notes && nav.noteTag != null -> "#${nav.noteTag}"
        else -> nav.section.label
    }
}

/** The side menu: the sections, your task lists and note tags, journal, search and settings. */
@Composable
private fun MenuContent(store: DawemStore, nav: Nav, close: () -> Unit) {
    var newList by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
        Column(Modifier.padding(horizontal = 28.dp, vertical = 24.dp)) {
            Text(Brand.NAME, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("by ${Brand.MAKER}", style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color(0xFFFF5A1F))
        }
        Section.entries.forEach { s ->
            Entry(s.icon, s.label, null, nav.screen == Screen.Main && nav.section == s && nav.taskList == null && nav.noteTag == null) { nav.go(s); close() }
        }
        Entry(Icons.Rounded.AutoStories, "Journal", null, nav.screen == Screen.Journal) { nav.screen = Screen.Journal; close() }
        Entry(Icons.Rounded.Insights, "Insights", null, nav.screen == Screen.Insights) { nav.screen = Screen.Insights; close() }

        val lists = store.taskLists()
        HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 10.dp))
        Label("Task lists")
        Entry(Icons.Rounded.List, "Inbox", store.tasks.count { !it.done && it.list.isEmpty() }, nav.screen == Screen.Main && nav.section == Section.Tasks && nav.taskList == "") { nav.go(Section.Tasks, taskList = ""); close() }
        lists.forEach { l ->
            Entry(Icons.Rounded.List, l, store.tasks.count { !it.done && it.list == l }, nav.screen == Screen.Main && nav.section == Section.Tasks && nav.taskList == l) { nav.go(Section.Tasks, taskList = l); close() }
        }
        Entry(Icons.Rounded.CreateNewFolder, "New list", null, false) { newList = true }

        val tags = store.noteTags()
        if (tags.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 10.dp))
            Label("Tags")
            tags.forEach { (tag, count) -> Entry(Icons.Rounded.Tag, tag, count, nav.screen == Screen.Main && nav.noteTag == tag) { nav.go(Section.Notes, noteTag = tag); close() } }
        }

        HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 10.dp))
        Entry(Icons.Rounded.Search, "Search", null, nav.screen == Screen.Search) { nav.screen = Screen.Search; close() }
        Entry(Icons.Rounded.Settings, "Settings", null, nav.screen == Screen.Settings) { nav.screen = Screen.Settings; close() }
        Entry(Icons.Rounded.Info, "About ${Brand.NAME}", null, nav.screen == Screen.About) { nav.screen = Screen.About; close() }
    }
    if (newList) NameDialog("New task list", "", "Create", onDismiss = { newList = false }) { name ->
        newList = false
        store.rememberTaskList(name.trim())
        nav.go(Section.Tasks, taskList = name.trim())
        close()
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp))
}

@Composable
private fun Entry(icon: ImageVector, label: String, count: Int?, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, null) },
        label = { Text(label, maxLines = 1) },
        badge = { if (count != null && count > 0) Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}
