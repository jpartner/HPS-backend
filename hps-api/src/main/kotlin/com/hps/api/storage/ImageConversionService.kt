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
        private const val MAX_DIMENSION = 2048
        private const val THUMBNAIL_SIZE = 400
    }

    fun isImage(contentType: String?): Boolean =
        contentType != null && IMAGE_TYPES.contains(contentType.lowercase())

    /**
     * Converts an image to WebP, resizing if larger than MAX_DIMENSION.
     * Also generates a thumbnail at THUMBNAIL_SIZE.
     * If conversion fails, returns original bytes unchanged with no thumbnail.
     */
    fun processImage(inputStream: InputStream, originalContentType: String): ProcessedImage {
        val bytes = inputStream.readBytes()

        return try {
            var image = ImmutableImage.loader().fromBytes(bytes)

            // Full size (capped at MAX_DIMENSION)
            if (image.width > MAX_DIMENSION || image.height > MAX_DIMENSION) {
                image = image.bound(MAX_DIMENSION, MAX_DIMENSION)
            }
            val fullOutput = ByteArrayOutputStream()
            image.forWriter(WebpWriter.DEFAULT).write(fullOutput)
            val fullBytes = fullOutput.toByteArray()

            // Thumbnail
            val thumb = image.bound(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
            val thumbOutput = ByteArrayOutputStream()
            thumb.forWriter(WebpWriter.DEFAULT).write(thumbOutput)
            val thumbBytes = thumbOutput.toByteArray()

            ProcessedImage(
                full = ConversionResult(
                    ByteArrayInputStream(fullBytes), "image/webp", "webp", fullBytes.size.toLong()
                ),
                thumbnail = ConversionResult(
                    ByteArrayInputStream(thumbBytes), "image/webp", "webp", thumbBytes.size.toLong()
                )
            )
        } catch (e: Exception) {
            log.warn("Failed to process image ({}), storing original: {}", originalContentType, e.message)
            ProcessedImage(
                full = ConversionResult(
                    ByteArrayInputStream(bytes), originalContentType,
                    originalContentType.substringAfter("/"), bytes.size.toLong()
                ),
                thumbnail = null
            )
        }
    }

    data class ConversionResult(
        val inputStream: InputStream,
        val contentType: String,
        val extension: String,
        val sizeBytes: Long
    )

    data class ProcessedImage(
        val full: ConversionResult,
        val thumbnail: ConversionResult?
    )
}
