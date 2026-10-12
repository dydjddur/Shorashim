package com.shorashim.core.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ImageUtils {
    private const val TAG = "ImageUtils"
    private val memoryCache = object : LruCache<String, ImageBitmap>(120) {}

    /**
     * Reads an image Uri from ContentResolver, corrects EXIF orientation,
     * downsamples and resizes to max [maxDimension] px, and compresses to JPEG [quality]%.
     * Returns a data URI string: "data:image/jpeg;base64,..."
     */
    fun uriToCompressedBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 400,
        quality: Int = 80,
    ): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null

            // Read EXIF orientation
            val orientation = try {
                val exif = android.media.ExifInterface(ByteArrayInputStream(bytes))
                exif.getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                )
            } catch (e: Exception) {
                android.media.ExifInterface.ORIENTATION_NORMAL
            }

            // Decode dimensions
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
            val origW = boundsOptions.outWidth
            val origH = boundsOptions.outHeight
            if (origW <= 0 || origH <= 0) return null

            var sampleSize = 1
            while ((origW / sampleSize) > maxDimension * 2 || (origH / sampleSize) > maxDimension * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                ?: return null

            // Handle EXIF orientation (especially critical on Samsung devices)
            val matrix = Matrix()
            when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }
            val rotated = if (!matrix.isIdentity) {
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            } else {
                decoded
            }

            // Scale to maxDimension
            val maxEdge = maxOf(rotated.width, rotated.height)
            val finalBitmap = if (maxEdge > maxDimension) {
                val scale = maxDimension.toFloat() / maxEdge
                val targetW = (rotated.width * scale).toInt().coerceAtLeast(1)
                val targetH = (rotated.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(rotated, targetW, targetH, true)
            } else {
                rotated
            }

            val baos = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            val compressedBytes = baos.toByteArray()
            val base64Str = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64Str"
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to compress image Uri: $uri", e)
            null
        }
    }

    /**
     * Decodes a photo string (Base64 data URI, raw Base64, or local file path) into ImageBitmap.
     * Uses in-memory LRU cache for 0ms retrieval on recurring UI passes.
     */
    fun decodeToBitmap(photoPath: String?): ImageBitmap? {
        if (photoPath.isNullOrBlank()) return null

        memoryCache.get(photoPath)?.let { return it }

        return try {
            val bitmap = when {
                photoPath.startsWith("data:image") -> {
                    val base64 = photoPath.substringAfter("base64,")
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                photoPath.startsWith("http://") || photoPath.startsWith("https://") -> {
                    null
                }
                photoPath.startsWith("file://") || photoPath.startsWith("/") -> {
                    val cleanPath = photoPath.removePrefix("file://")
                    BitmapFactory.decodeFile(cleanPath)
                }
                else -> {
                    try {
                        val bytes = Base64.decode(photoPath, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (_: Exception) {
                        null
                    }
                }
            }

            val imgBitmap = bitmap?.asImageBitmap()
            if (imgBitmap != null) {
                memoryCache.put(photoPath, imgBitmap)
            }
            imgBitmap
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to decode photo string", e)
            null
        }
    }

    /**
     * Downloads and caches remote HTTP/HTTPS images in memory.
     */
    fun loadHttpBitmap(urlStr: String): ImageBitmap? {
        memoryCache.get(urlStr)?.let { return it }
        return try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.doInput = true
            connection.connect()
            val stream = connection.inputStream
            val bmp = BitmapFactory.decodeStream(stream)
            stream.close()
            val imgBitmap = bmp?.asImageBitmap()
            if (imgBitmap != null) {
                memoryCache.put(urlStr, imgBitmap)
            }
            imgBitmap
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to load HTTP image $urlStr: ${e.message}")
            null
        }
    }
}
