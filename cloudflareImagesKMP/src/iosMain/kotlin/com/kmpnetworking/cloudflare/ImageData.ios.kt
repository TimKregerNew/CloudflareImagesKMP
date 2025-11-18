package com.kmpnetworking.cloudflare

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.UIKit.*
import platform.posix.memcpy

/**
 * iOS implementation of ImageData
 */
class IOSImageData private constructor(
    private val dataProvider: suspend () -> ByteArray,
    override val mimeType: String,
    override val fileName: String,
    override val sizeInBytes: Long?
) : ImageData {
    override suspend fun toByteArray(): ByteArray = dataProvider()
    
    companion object {
        /**
         * Create from UIImage
         */
        fun fromUIImage(
            image: UIImage,
            compressionQuality: Double = 0.9,
            format: ImageFormat = ImageFormat.JPEG,
            fileName: String = "image.jpg"
        ): IOSImageData? {
            val data = when (format) {
                ImageFormat.JPEG -> UIImageJPEGRepresentation(image, compressionQuality)
                ImageFormat.PNG -> UIImagePNGRepresentation(image)
            } ?: return null
            
            val mimeType = when (format) {
                ImageFormat.JPEG -> "image/jpeg"
                ImageFormat.PNG -> "image/png"
            }
            
            return IOSImageData(
                dataProvider = { data.toByteArray() },
                mimeType = mimeType,
                fileName = fileName,
                sizeInBytes = data.length.toLong()
            )
        }
        
        /**
         * Create from NSData
         */
        fun fromNSData(
            data: NSData?,
            mimeType: String = "image/jpeg",
            fileName: String = "image.jpg"
        ): IOSImageData? {
            if (data == null) return null
            return IOSImageData(
                dataProvider = { data.toByteArray() },
                mimeType = mimeType,
                fileName = fileName,
                sizeInBytes = data.length.toLong()
            )
        }
        
        /**
         * Create from file path
         */
        fun fromPath(path: String): IOSImageData? {
            val fileManager = NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(path)) return null
            
            val data = NSData.dataWithContentsOfFile(path) ?: return null
            val url = NSURL.fileURLWithPath(path)
            val fileName = url.lastPathComponent ?: "image.jpg"
            
            val mimeType = when (fileName.substringAfterLast('.', "").lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                else -> "application/octet-stream"
            }
            
            return IOSImageData(
                dataProvider = { data.toByteArray() },
                mimeType = mimeType,
                fileName = fileName,
                sizeInBytes = data.length.toLong()
            )
        }
        
        /**
         * Create from byte array
         */
        fun fromBytes(
            bytes: ByteArray,
            mimeType: String = "image/jpeg",
            fileName: String = "image.jpg"
        ): IOSImageData {
            return IOSImageData(
                dataProvider = { bytes },
                mimeType = mimeType,
                fileName = fileName,
                sizeInBytes = bytes.size.toLong()
            )
        }
    }
    
    enum class ImageFormat {
        JPEG,
        PNG
    }
}

/**
 * Extension to convert NSData to ByteArray
 */
@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    val bytes = ByteArray(length)
    
    if (length > 0) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, length.toULong())
        }
    }
    
    return bytes
}

/**
 * iOS implementation of ImageDataFactory
 */
actual object ImageDataFactory {
    actual suspend fun fromFilePath(filePath: String): ImageData? {
        return IOSImageData.fromPath(filePath)
    }
    
    actual fun fromBytes(
        bytes: ByteArray,
        mimeType: String,
        fileName: String
    ): ImageData {
        return IOSImageData.fromBytes(bytes, mimeType, fileName)
    }
}

