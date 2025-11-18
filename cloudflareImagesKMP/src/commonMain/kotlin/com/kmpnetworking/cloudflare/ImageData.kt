package com.kmpnetworking.cloudflare

/**
 * Platform-agnostic interface for image data.
 * 
 * This allows the shared business logic to work with images
 * without knowing about platform-specific types (File, UIImage, etc.)
 * 
 * Platform-specific implementations:
 * - Android: AndroidImageData (wraps File, Uri, or byte array)
 * - iOS: IOSImageData (wraps UIImage or Data)
 */
interface ImageData {
    /**
     * The image data as a byte array
     */
    suspend fun toByteArray(): ByteArray
    
    /**
     * MIME type of the image (e.g., "image/jpeg", "image/png")
     */
    val mimeType: String
    
    /**
     * File name for the image
     */
    val fileName: String
    
    /**
     * Optional: Size of the image in bytes
     */
    val sizeInBytes: Long?
        get() = null
}

/**
 * Simple implementation of ImageData from raw bytes
 */
class ByteArrayImageData(
    private val bytes: ByteArray,
    override val mimeType: String = "image/jpeg",
    override val fileName: String = "image.jpg"
) : ImageData {
    override suspend fun toByteArray(): ByteArray = bytes
    override val sizeInBytes: Long = bytes.size.toLong()
}

/**
 * Create ImageData from bytes synchronously (for use in non-suspend contexts)
 */
fun createImageDataFromBytes(
    bytes: ByteArray,
    mimeType: String = "image/jpeg",
    fileName: String = "image.jpg"
): ImageData = ByteArrayImageData(bytes, mimeType, fileName)

/**
 * Factory functions to create ImageData from common sources
 */
expect object ImageDataFactory {
    /**
     * Create ImageData from a file path
     * 
     * @param filePath Path to the image file
     * @return ImageData instance or null if file doesn't exist
     */
    suspend fun fromFilePath(filePath: String): ImageData?
    
    /**
     * Create ImageData from raw bytes
     * 
     * @param bytes The image bytes
     * @param mimeType MIME type (default: image/jpeg)
     * @param fileName File name (default: image.jpg)
     * @return ImageData instance
     */
    fun fromBytes(
        bytes: ByteArray,
        mimeType: String = "image/jpeg",
        fileName: String = "image.jpg"
    ): ImageData
}

