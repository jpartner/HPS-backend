package com.hps.api.storage

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.webp.WebpWriter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

@Service
class ImageConversionService {

    private val log = LoggerFactory.getLogger(ImageConversionService::class.java)

    companion object {
        private val IMAGE_TYPES = setOf(
            "image/jpeg", "image/png", "image/webp", "image/gif",
            "image/heic", "image/heif"
        )
        // Max dimension to resize to (preserving aspect ratio)
        private const val MAX_DIMENSION = 2048
    }

    fun isImage(contentType: String?): Boolean =
        contentType != null && IMAGE_TYPES.contains(contentType.lowercase())

    /**
     * Converts an image to WebP format, resizing if larger than MAX_DIMENSION.
     * Returns the WebP bytes and "image/webp" content type.
     * If conversion fails, returns the original bytes unchanged.
     */
    fun convertToWebp(inputStream: InputStream, originalContentType: String): ConversionResult {
        // Already WebP — just resize if needed
        val bytes = inputStream.readBytes()

        return try {
            var image = ImmutableImage.loader().fromBytes(bytes)

            // Resize if too large
            if (image.width > MAX_DIMENSION || image.height > MAX_DIMENSION) {
                image = image.bound(MAX_DIMENSION, MAX_DIMENSION)
            }

            val output = ByteArrayOutputStream()
            image.forWriter(WebpWriter.DEFAULT).write(output)

            ConversionResult(
                inputStream = ByteArrayInputStream(output.toByteArray()),
                contentType = "image/webp",
                extension = "webp"
            )
        } catch (e: Exception) {
            log.warn("Failed to convert image to WebP ({}), storing original: {}", originalContentType, e.message)
            ConversionResult(
                inputStream = ByteArrayInputStream(bytes),
                contentType = originalContentType,
                extension = originalContentType.substringAfter("/")
            )
        }
    }

    data class ConversionResult(
        val inputStream: InputStream,
        val contentType: String,
        val extension: String
    )
}
