package dev.thabit.core

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

/** The vault folder and the plain file handling every feature shares: reading, atomic writing, the clock and the JSON settings. */
class VaultFiles(
    val fs: FileSystem,
    val root: Path,
    private val clock: Clock = Clock.System,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) {
    val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    fun today(): LocalDate = clock.now().toLocalDateTime(zone).date

    fun stamp(): String = clock.now().toLocalDateTime(zone).toString().take(19)

    fun read(path: Path): String? = if (fs.exists(path)) fs.read(path) { readUtf8() } else null

    /** Names of the unreadable data files that were set aside as `.corrupt-` copies since the app opened. */
    val corruptCopies: MutableList<String> = mutableListOf()

    /**
     * Reads a JSON file. A missing file gives [default]. A file that exists but cannot be parsed also gives [default], but a
     * `<name>.corrupt-<stamp>` copy is kept first (once per distinct content), so the next save never destroys it silently.
     */
    fun <T> readJson(path: Path, serializer: KSerializer<T>, default: T): T {
        val text = read(path) ?: return default
        return try { json.decodeFromString(serializer, text) } catch (e: Exception) { keepCorruptCopy(path, text); default }
    }

    private fun keepCorruptCopy(path: Path, text: String) {
        val dir = path.parent ?: return
        val prefix = path.name + ".corrupt-"
        val already = fs.listOrNull(dir).orEmpty().any { it.name.startsWith(prefix) && read(it) == text }
        if (!already) {
            val copy = dir / (prefix + stamp().replace(':', '-'))
            write(copy, text)
            corruptCopies += copy.name
        }
    }

    /** Writes through a temporary file and renames it, so a crash never leaves half a file. */
    fun write(path: Path, text: String) {
        path.parent?.let { fs.createDirectories(it) }
        val tmp = path.parent!! / (path.name + ".tmp")
        fs.write(tmp) { writeUtf8(text) }
        fs.atomicMove(tmp, path)
    }

    fun dir(name: String): Path = root / name

    /** The vault's own folder for settings and the trash. */
    val trash: Path get() = root / ".lantern" / "trash"
}
