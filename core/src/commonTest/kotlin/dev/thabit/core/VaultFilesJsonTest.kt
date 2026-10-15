package dev.thabit.core

import kotlinx.datetime.TimeZone
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VaultFilesJsonTest {
    private val fs = FakeFileSystem()
    private val files = VaultFiles(fs, "/v".toPath(), kotlinx.datetime.Clock.System, TimeZone.UTC)
    private val path = "/v/habits/habits.json".toPath()
    private val ints = ListSerializer(Int.serializer())

    private fun copies() = fs.listOrNull("/v/habits".toPath()).orEmpty().map { it.name }.filter { it.startsWith("habits.json.corrupt-") }

    @Test fun aMissingFileIsTheDefaultAndLeavesNoCopy() {
        assertEquals(listOf(9), files.readJson(path, ints, listOf(9)))
        assertTrue(copies().isEmpty())
    }

    @Test fun aGoodFileIsDecoded() {
        files.write(path, "[1,2]")
        assertEquals(listOf(1, 2), files.readJson(path, ints, emptyList()))
        assertTrue(copies().isEmpty() && files.corruptCopies.isEmpty())
    }

    @Test fun aBrokenFileGivesTheDefaultButIsKeptAsACorruptCopy() {
        files.write(path, "[1, oops")
        assertEquals(emptyList(), files.readJson(path, ints, emptyList()))
        assertEquals(1, copies().size)
        assertEquals("[1, oops", files.read("/v/habits/${copies().single()}".toPath()))
        assertEquals(copies(), files.corruptCopies)
    }

    @Test fun readingTheSameBrokenFileAgainDoesNotPileUpCopies() {
        files.write(path, "[1, oops")
        repeat(3) { files.readJson(path, ints, emptyList()) }
        assertEquals(1, copies().size)
    }

    @Test fun savingAfterABrokenReadKeepsTheOriginalBytes() {
        files.write(path, "[1, oops")
        files.readJson(path, ints, emptyList())
        files.write(path, "[7]")
        assertEquals("[1, oops", files.read("/v/habits/${copies().single()}".toPath()))
        assertEquals("[7]", files.read(path))
    }
}
