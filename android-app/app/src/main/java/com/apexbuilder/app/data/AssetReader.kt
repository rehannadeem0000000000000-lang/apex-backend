package com.apexbuilder.app.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.apexbuilder.app.model.FileAsset
import java.io.ByteArrayOutputStream
import java.io.IOException

class AssetReader(context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    fun displayName(uri: Uri): String {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) cursor.getString(nameIndex)?.takeIf(String::isNotBlank)?.let { return it }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank) ?: "selected-file"
    }

    @Throws(IOException::class)
    fun read(uri: Uri, fallbackName: String): FileAsset {
        val input = resolver.openInputStream(uri) ?: throw IOException("Could not open the selected file.")
        val output = ByteArrayOutputStream()
        input.use { stream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                total += count
                if (total > MAX_FILE_SIZE_BYTES) {
                    throw IOException("Files must be 25 MB or smaller.")
                }
                output.write(buffer, 0, count)
            }
        }
        val bytes = output.toByteArray()
        if (bytes.isEmpty()) throw IOException("The selected file is empty.")
        return FileAsset(
            fileName = sanitizeFileName(displayName(uri).ifBlank { fallbackName }),
            bytes = bytes,
        )
    }

    private fun sanitizeFileName(name: String): String {
        val baseName = name.substringAfterLast('/').substringAfterLast('\\')
        return baseName
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .take(120)
            .takeUnless { it.isBlank() || it == "." || it == ".." }
            ?: "upload.bin"
    }

    private companion object {
        const val MAX_FILE_SIZE_BYTES = 25L * 1024L * 1024L
    }
}