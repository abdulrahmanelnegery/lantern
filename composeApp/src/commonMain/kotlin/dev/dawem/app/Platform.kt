package dev.dawem.app

import dev.dawem.ui.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/** The system back gesture on Android; Escape on the desktop. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

/** What only the platform can do; each app provides its own. */
class PlatformActions(
    /** Saves the vault as a zip the user chooses a place for. */
    val exportVault: () -> Unit = {},
    /** Opens the system screen that allows keeping the vault in a visible folder; null where that does not apply. */
    val allowVisibleFolder: (() -> Unit)? = null,
    /** True once the vault is in the visible folder (or the question does not apply). */
    val visibleFolderOn: Boolean = true,
)

val LocalPlatform = compositionLocalOf { PlatformActions() }
