package dev.lantern.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.lantern.core.Vault
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {}

fun main() = application {
    val dir = File(System.getProperty("user.home"), "Lantern").also { it.mkdirs() }
    val store = LanternStore(Vault(FileSystem.SYSTEM, dir.absolutePath.toPath()), dir.absolutePath)
    val icon = androidx.compose.ui.res.painterResource("icon.png")
    Window(onCloseRequest = ::exitApplication, title = "Lantern", icon = icon, state = rememberWindowState(width = 460.dp, height = 820.dp)) {
        LanternApp(store)
    }
}
