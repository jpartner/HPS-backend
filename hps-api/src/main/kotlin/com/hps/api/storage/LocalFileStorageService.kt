package com.hps.api.storage

import com.hps.common.storage.FileStorageService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

@Service
class LocalFileStorageService(
    @Value("\${hps.storage.local.base-dir:./uploads}") private val baseDir: String,
    @Value("\${hps.storage.local.base-url:http://localhost:8080/files}") private val baseUrl: String
) : FileStorageService {

    override fun store(folder: String, filename: String, contentType: String, inputStream: InputStream): String {
        val extension = filename.substringAfterLast('.', "")
        val uniqueName = "${UUID.randomUUID()}${if (extension.isNotEmpty()) ".$extension" else ""}"
        val key = "$folder/$uniqueName"

        val targetDir = Path.of(baseDir, folder)
        Files.createDirectories(targetDir)

        val targetPath = targetDir.resolve(uniqueName)
        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)

        return key
    }

    override fun delete(key: String) {
        val path = Path.of(baseDir, key)
        Files.deleteIfExists(path)
    }

    override fun resolveUrl(key: String): String {
        return "$baseUrl/$key"
    }
}
