package com.miguelcode.warrantyvault.storage

import com.miguelcode.warrantyvault.domain.storage.PhotoFileCleaner
import java.io.File

class PhotoFileCleanerImpl : PhotoFileCleaner {
    override fun delete(absolutePath: String?) {
        if (absolutePath.isNullOrBlank()) return
        runCatching { File(absolutePath).delete() }
    }
}
