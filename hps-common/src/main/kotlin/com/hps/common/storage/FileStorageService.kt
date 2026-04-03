package com.hps.common.storage

import java.io.InputStream

interface FileStorageService {
    /**
     * Store a file and return its storage key (relative path).
     */
    fun store(folder: String, filename: String, contentType: String, inputStream: InputStream): String

    /**
     * Delete a file by its storage key.
     */
    fun delete(key: String)

    /**
     * Resolve a storage key to a publicly accessible URL.
     */
    fun resolveUrl(key: String): String
}
