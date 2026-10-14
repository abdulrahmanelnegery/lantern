package dev.dawem.core

import kotlinx.serialization.Serializable

/** How the app looks; stored in the vault's `.lantern/settings.json` so it travels with the data. */
@Serializable
data class AppSettings(
    /** One of the palette ids the app knows; an unknown id falls back to the default. */
    val palette: String = "zimam",
    /** "system", "light" or "dark". */
    val mode: String = "system",
    /** The cards on the Today page, in order; an id not listed here is added at the end when the app knows it. */
    val homeOrder: List<String> = emptyList(),
    /** Cards hidden from the Today page. */
    val homeHidden: List<String> = emptyList(),
    /** The version whose "what's new" notes were closed last; empty when never. */
    val lastSeenVersion: String = "",
    /** True once the first-install tour was finished or skipped. */
    val tourDone: Boolean = false,
)
