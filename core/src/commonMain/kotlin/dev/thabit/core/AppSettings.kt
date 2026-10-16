package dev.thabit.core

import kotlinx.serialization.Serializable

/** The colour themes the user can pick; the colours themselves are defined once in the ui module's Theme. */
object PaletteIds {
    const val DEFAULT = "zimam"
    val all = listOf(DEFAULT, "amber", "ocean", "forest", "rose", "slate")
}

/** The sections of the app in their default order: the pages of the pager, the bottom bar and the side menu. */
object Sections {
    const val HOME = "home"
    const val HABITS = "habits"
    const val TASKS = "tasks"
    const val INSIGHTS = "insights"
    val all = listOf(HOME, HABITS, TASKS, INSIGHTS)

    /** The sections to show: the user's [order] first (unknown names dropped), then any missing ones, minus [hidden]. Home is always kept. */
    fun visible(order: List<String>, hidden: Set<String>): List<String> {
        val ordered = order.filter { it in all }.distinct().let { it + (all - it.toSet()) }
        return ordered.filter { it == HOME || it !in hidden }
    }
}

/** How the app looks and behaves; stored in the vault's `.lantern/settings.json` so it travels with the data. */
@Serializable
data class AppSettings(
    /** One of [PaletteIds.all]; an unknown id falls back to the default. */
    val palette: String = PaletteIds.DEFAULT,
    /** "system", "light" or "dark". */
    val mode: String = "system",
    /** Text size step, 0 (small) to 3 (largest); 1 is normal. */
    val textSize: Int = 1,
    /** "comfortable" (default) or "compact": how much room rows and cards get. */
    val density: String = "comfortable",
    /** Skip the splash and the page animations. */
    val reduceMotion: Boolean = false,
    /** Sections in the order the user wants; empty means the default order. */
    val sectionOrder: List<String> = emptyList(),
    /** Sections hidden from the bottom bar and the side menu. Home cannot be hidden. */
    val hiddenSections: Set<String> = emptySet(),
    /** The section shown on opening: one of [Sections.all]. */
    val startSection: String = Sections.HOME,
    /** The cards on the Today page, in order; an id not listed here is added at the end when the app knows it. */
    val homeOrder: List<String> = emptyList(),
    /** Cards hidden from the Today page. */
    val homeHidden: List<String> = emptyList(),
    /** The version whose "what's new" notes were closed last; empty when never. */
    val lastSeenVersion: String = "",
    /** True once the first-install tour was finished or skipped. */
    val tourDone: Boolean = false,
    /** True once the one-time "Notes and journal moved to Athar" card was dismissed. */
    val atharNoticeDismissed: Boolean = false,
)

/** Brings a settings file (a hand edit, an older or newer version) back into the range the app understands. */
fun AppSettings.normalized(): AppSettings = copy(
    palette = palette.takeIf { it in PaletteIds.all } ?: PaletteIds.DEFAULT,
    mode = mode.takeIf { it in setOf("system", "light", "dark") } ?: "system",
    textSize = textSize.coerceIn(0, 3),
    density = density.takeIf { it in setOf("comfortable", "compact") } ?: "comfortable",
    sectionOrder = sectionOrder.filter { it in Sections.all }.distinct(),
    hiddenSections = hiddenSections.filter { it in Sections.all && it != Sections.HOME }.toSet(),
    startSection = startSection.takeIf { it in Sections.all && (it == Sections.HOME || it !in hiddenSections) } ?: Sections.HOME,
)
