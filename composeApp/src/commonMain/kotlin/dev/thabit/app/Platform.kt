package dev.thabit.app

import dev.thabit.ui.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/** What only the platform can do; each app provides its own. */
class PlatformActions(
    /** Saves the vault as a zip the user chooses a place for. */
    val exportVault: () -> Unit = {},
    /** Opens the system screen that allows keeping the vault in a visible folder; null where that does not apply. */
    val allowVisibleFolder: (() -> Unit)? = null,
    /** True once the vault is in the visible folder (or the question does not apply). */
    val visibleFolderOn: Boolean = true,
    /** Older vault folders that are fully copied into the Zimam folder and checked right now, so they can be removed without losing anything. */
    val removableOldFolders: () -> List<String> = { emptyList() },
    /** Removes one such folder, after checking again; true when it was removed. */
    val removeOldFolder: (String) -> Boolean = { false },
    /** Why the old folder is still in use, when the copy could not be verified. */
    val migrationMessage: String? = null,
)

val LocalPlatform = compositionLocalOf { PlatformActions() }
