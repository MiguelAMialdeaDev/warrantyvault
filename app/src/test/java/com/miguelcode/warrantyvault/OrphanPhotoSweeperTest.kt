package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.storage.OrphanPhotoSweeper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class OrphanPhotoSweeperTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val now = 10L * 24 * 60 * 60 * 1000
    private val old = now - 3L * 24 * 60 * 60 * 1000
    private val recent = now - 60L * 60 * 1000

    private fun photo(dir: File, name: String, modified: Long) =
        File(dir, name).apply { writeText("jpg"); setLastModified(modified) }

    @Test
    fun `an old photo that no warranty uses is deleted`() {
        val dir = tmp.newFolder("ticket_photos")
        val orphan = photo(dir, "orphan.jpg", old)

        val deleted = OrphanPhotoSweeper(dir, now = { now }).sweep(emptySet())

        assertEquals(1, deleted)
        assertFalse(orphan.exists())
    }

    @Test
    fun `a photo used by a warranty is kept however old it is`() {
        val dir = tmp.newFolder("ticket_photos")
        val used = photo(dir, "used.jpg", old)

        OrphanPhotoSweeper(dir, now = { now }).sweep(setOf(used.absolutePath))

        assertTrue(used.exists())
    }

    @Test
    fun `a recent unsaved photo is kept so a restored draft does not lose it`() {
        val dir = tmp.newFolder("ticket_photos")
        val draft = photo(dir, "draft.jpg", recent)

        OrphanPhotoSweeper(dir, now = { now }).sweep(emptySet())

        assertTrue(draft.exists())
    }

    @Test
    fun `a missing photos folder is not an error`() {
        val dir = File(tmp.root, "never_created")

        assertEquals(0, OrphanPhotoSweeper(dir, now = { now }).sweep(emptySet()))
    }

    @Test
    fun `a used photo stored under another path prefix is kept`() {
        val dir = tmp.newFolder("ticket_photos")
        val used = photo(dir, "3f2a.jpg", old)

        OrphanPhotoSweeper(dir, now = { now })
            .sweep(setOf("/data/data/com.miguelcode.warrantyvault/files/ticket_photos/3f2a.jpg"))

        assertTrue(used.exists())
    }
}
