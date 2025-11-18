package com.kmpnetworking.cloudflare

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

/**
 * Android implementation of ImageData
 */
class AndroidImageData private constructor(
    private val dataProvider: suspend () -> ByteArray,
    override val mimeType: String,
    override val fileName: String,
    override val sizeInBytes: Long?
) : ImageData {
    override suspend fun toByteArray(): ByteArray = dataProvider()
    
    companion object {
        /**
         * Create from a File
         */
        fun fromFile(file: File): AndroidImageData {
            val mimeType = when (file.extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                else -> "application/octet-stream"
            }
            
            return AndroidImageData(
                dataProvider = { file.readBytes() },
                mimeType = mimeType,
                fileName = file.name,
                sizeInBytes = file.length()
            )
        }
        
        /**
         * Create from a file path
         */
        fun fromPath(path: String): AndroidImageData? {
            val file = File(path)
            return if (file.exists()) fromFile(file) else null
        }
        
        /**
         * Create from a Uri (requires Context)
         */
        fun fromUri(context: Context, uri: Uri): AndroidImageData? {
            return try {
                val contentResolver = context.contentResolver
                val fileName = uri.lastPathSegment ?: "image.jpg"
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                
                AndroidImageData(
                    dataProvider = {
                        contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: throw IllegalStateException("Cannot read from URI")
                    },
                    mimeType = mimeType,
                    fileName = fileName,
                    sizeInBytes = null
                )
            } catch (e: Exception) {
                null
            }
        }
        
        /**
         * Create from a Bitmap
         */
        fun fromBitmap(
            bitmap: Bitmap,
            format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
            quality: Int = 90,
            fileName: String = "image.jpg"
        ): AndroidImageData {
            val mimeType = when (format) {
                Bitmap.CompressFormat.JPEG -> "image/jpeg"
                Bitmap.CompressFormat.PNG -> "image/png"
                Bitmap.CompressFormat.WEBP, Bitmap.CompressFormat.WEBP_LOSSY, 
                Bitmap.CompressFormat.WEBP_LOSSLESS -> "image/webp"
            }
            
            return AndroidImageData(
                dataProvider = {
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(format, quality, outputStream)
                    outputStream.toByteArray()
                },
                mimeType = mimeType,
                fileName = fileName,
                sizeInBytes = null
            )
        }
        
        /**
         * Create from byte array
         */
        fun fromBytes(
            bytes: ByteArray,
            mimeType: String = "image/jpeg",
            fileName: String = "image.jpg"
        ): AndroidImageData {
            return AndroidImageData(
                dataProvider = { bytes },
                mimeType = mimeType,
                fileName = fileName,
                sizeInBytes = bytes.size.toLong()
            )
        }
    }
}

/**
 * Android implementation of ImageDataFactory
 */
actual object ImageDataFactory {
    actual suspend fun fromFilePath(filePath: String): ImageData? {
        return AndroidImageData.fromPath(filePath)
    }
    
    actual fun fromBytes(
        bytes: ByteArray,
        mimeType: String,
        fileName: String
    ): ImageData {
        return AndroidImageData.fromBytes(bytes, mimeType, fileName)
    }
}

