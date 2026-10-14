package dev.dawem.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import dev.dawem.core.Vault
import dev.dawem.core.zipVault
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.sink
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var store: DawemStore

    private val exportZip = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { writeZip(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        store = openStore()
        val actions = PlatformActions(
            exportVault = { exportZip.launch("Dawem-vault.zip") },
            allowVisibleFolder = if (Build.VERSION.SDK_INT >= 30) ({ openAllFilesAccess() }) else null,
            visibleFolderOn = Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager(),
        )
        setContent { CompositionLocalProvider(LocalPlatform provides actions) { DawemApp(store) } }
    }

    override fun onResume() {
        super.onResume()
        // Files may have changed under us (a sync, a copy from the laptop), or the day rolled over.
        if (::store.isInitialized) store.reload()
    }

    private fun writeZip(uri: Uri) {
        runCatching {
            contentResolver.openOutputStream(uri)?.sink()?.buffer()?.use { zipVault(FileSystem.SYSTEM, store.vault.root, it) }
        }
    }

    private fun openAllFilesAccess() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))
        runCatching { startActivity(intent) }.onFailure { startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
    }

    /**
     * The vault is /Dawem on the phone when All files access is allowed, else a private folder.
     * The first start after allowing it brings the private vault across.
     */
    private fun openStore(): DawemStore {
        // The app was called Lantern before Dawem: its vault folders move across once, by renaming, so nothing is copied twice.
        fun moved(old: File, new: File): File { if (!new.exists() && old.isDirectory) old.renameTo(new); return new }
        val private = moved(File(filesDir, "Lantern"), File(filesDir, "Dawem"))
        val shared = Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()
        val dir = if (shared) moved(File(Environment.getExternalStorageDirectory(), "Lantern"), File(Environment.getExternalStorageDirectory(), "Dawem")) else private
        if (shared && !File(dir, "notes").exists() && private.exists()) runCatching { private.copyRecursively(dir, overwrite = false) }
        dir.mkdirs()
        return DawemStore(Vault(FileSystem.SYSTEM, dir.absolutePath.toPath()), dir.absolutePath)
    }
}
