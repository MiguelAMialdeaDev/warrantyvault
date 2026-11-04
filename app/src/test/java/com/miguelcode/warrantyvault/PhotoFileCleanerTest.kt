package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.storage.PhotoFileCleanerImpl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PhotoFileCleanerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun setUp(): Triple<File, File, PhotoFileCleanerImpl> {
        val filesDir = tmp.newFolder("files")
        val photosDir = File(filesDir, "ticket_photos").apply { mkdirs() }
        return Triple(filesDir, photosDir, PhotoFileCleanerImpl(photosDir))
    }

    @Test
    fun `a ticket photo is deleted`() {
        val (_, photosDir, cleaner) = setUp()
        val photo = File(photosDir, "a.jpg").apply { writeText("x") }

        cleaner.delete(photo.absolutePath)

        assertFalse(photo.exists())
    }

    @Test
    fun `a file outside the photos folder is never deleted`() {
        val (filesDir, _, cleaner) = setUp()
        val database = File(filesDir, "warranty_vault_db").apply { writeText("x") }

        cleaner.delete(database.absolutePath)

        assertTrue(database.exists())
    }

    @Test
    fun `a path that climbs out of the photos folder is never deleted`() {
        val (filesDir, photosDir, cleaner) = setUp()
        val prefs = File(filesDir, "prefs.pb").apply { writeText("x") }

        cleaner.delete(File(photosDir, "../prefs.pb").path)

        assertTrue(prefs.exists())
    }

    @Test
    fun `the photos folder itself is never deleted`() {
        val (_, photosDir, cleaner) = setUp()

        cleaner.delete(photosDir.absolutePath)

        assertTrue(photosDir.exists())
    }

    @Test
    fun `blank paths are ignored`() {
        val (_, _, cleaner) = setUp()

        cleaner.delete(null)
        cleaner.delete("")
    }
}
