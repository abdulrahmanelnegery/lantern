package dev.lantern.app

import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.lantern.core.Vault
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var store: LanternStore

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        store = openStore()
        setContent { LanternApp(store) }
    }

    override fun onResume() {
        super.onResume()
        // Files may have changed under us (a sync, a copy from the laptop), or the day rolled over.
        if (::store.isInitialized) store.reload()
    }

    /** The vault is /Lantern on the phone when All files access is allowed, else a private folder. */
    private fun openStore(): LanternStore {
        val shared = Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()
        val dir = if (shared) File(Environment.getExternalStorageDirectory(), "Lantern") else File(filesDir, "Lantern")
        dir.mkdirs()
        return LanternStore(Vault(FileSystem.SYSTEM, dir.absolutePath.toPath()), dir.absolutePath)
    }
}
