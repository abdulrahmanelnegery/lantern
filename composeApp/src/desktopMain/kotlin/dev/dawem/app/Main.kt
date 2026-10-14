package dev.dawem.app

import dev.dawem.ui.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.dawem.core.Vault
import dev.dawem.core.zipVault
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.sink
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {}

fun main() = application {
    val home = System.getProperty("user.home")
    // The app was called Lantern before Dawem: its vault folder is renamed once.
    File(home, "Lantern").let { old -> if (old.isDirectory && !File(home, "Dawem").exists()) old.renameTo(File(home, "Dawem")) }
    val dir = File(home, "Dawem").also { it.mkdirs() }
    val store = DawemStore(Vault(FileSystem.SYSTEM, dir.absolutePath.toPath()), dir.absolutePath)
    val actions = PlatformActions(exportVault = {
        val dialog = FileDialog(null as Frame?, "Save the vault as a zip", FileDialog.SAVE).apply { file = "Dawem-vault.zip"; isVisible = true }
        val name = dialog.file
        if (name != null) File(dialog.directory, name).sink().buffer().use { zipVault(FileSystem.SYSTEM, store.vault.root, it) }
    })
    val icon = androidx.compose.ui.res.painterResource("icon.png")
    Window(onCloseRequest = ::exitApplication, title = "Dawem", icon = icon, state = rememberWindowState(width = 1000.dp, height = 760.dp)) {
        CompositionLocalProvider(LocalPlatform provides actions) { DawemApp(store) }
    }
}
