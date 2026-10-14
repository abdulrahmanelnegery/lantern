package dev.thabit.app

import dev.thabit.ui.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.thabit.core.Vault
import dev.thabit.core.VaultLocation
import dev.thabit.core.zipVault
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.sink
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

fun main() = application {
    val home = System.getProperty("user.home")
    // The vault is ~/Zimam/Thabit. Older folders (~/Zimam/Dawem, ~/Dawem, and ~/Lantern from before the renames) are copied in and checked,
    // then left where they are as a backup; if a copy cannot be verified the old folder stays in use.
    val fs = FileSystem.SYSTEM
    val shared = File(home, "Zimam/Thabit").absolutePath.toPath()
    val olds = listOf("Zimam/Dawem", "Dawem", "Lantern").map { File(home, it).absolutePath.toPath() }
    val choice = VaultLocation.choose(fs, true, shared, olds, shared)
    val dir = File(choice.dir.toString()).also { it.mkdirs() }
    val store = ThabitStore(Vault(fs, dir.absolutePath.toPath()), dir.absolutePath)
    val actions = PlatformActions(removableOldFolders = { VaultLocation.removable(fs, choice.dir, shared, olds).map { it.toString() } }, removeOldFolder = { VaultLocation.remove(fs, it.toPath(), shared) }, migrationMessage = choice.message, exportVault = {
        val dialog = FileDialog(null as Frame?, "Save the vault as a zip", FileDialog.SAVE).apply { directory = File(dir, "exports").also { it.mkdirs() }.absolutePath; file = "Thabit-vault.zip"; isVisible = true }
        val name = dialog.file
        if (name != null) File(dialog.directory, name).sink().buffer().use { zipVault(FileSystem.SYSTEM, store.vault.root, it) }
    })
    val icon = androidx.compose.ui.res.painterResource("icon.png")
    Window(onCloseRequest = ::exitApplication, title = "Thabit", icon = icon, state = rememberWindowState(width = 1000.dp, height = 760.dp)) {
        CompositionLocalProvider(LocalPlatform provides actions) { ThabitApp(store) }
    }
}
