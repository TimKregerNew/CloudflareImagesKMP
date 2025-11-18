package com.kmpnetworking.cloudflare

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Android-specific tests for ImageData implementation
 * Uses Robolectric to test Android-specific code without a device
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AndroidImageDataTest {
    
    @Test
    fun testFromBitmap() = runTest {
        // Create a small test bitmap
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        
        val imageData = AndroidImageData.fromBitmap(
            bitmap = bitmap,
            format = Bitmap.CompressFormat.JPEG,
            quality = 90,
            fileName = "test.jpg"
        )
        
        assertEquals("image/jpeg", imageData.mimeType)
        assertEquals("test.jpg", imageData.fileName)
        
        val bytes = imageData.toByteArray()
        assertTrue(bytes.isNotEmpty(), "Should have image data")
    }
    
    @Test
    fun testFromBitmapPng() = runTest {
        val bitmap = Bitmap.createBitmap(5, 5, Bitmap.Config.ARGB_8888)
        
        val imageData = AndroidImageData.fromBitmap(
            bitmap = bitmap,
            format = Bitmap.CompressFormat.PNG,
            fileName = "test.png"
        )
        
        assertEquals("image/png", imageData.mimeType)
    }
    
    @Test
    fun testFromBytes() = runTest {
        val testBytes = ByteArray(100) { it.toByte() }
        
        val imageData = AndroidImageData.fromBytes(
            bytes = testBytes,
            mimeType = "image/jpeg",
            fileName = "test.jpg"
        )
        
        assertEquals("image/jpeg", imageData.mimeType)
        assertEquals("test.jpg", imageData.fileName)
        assertEquals(100L, imageData.sizeInBytes)
        
        val retrievedBytes = imageData.toByteArray()
        assertTrue(testBytes.contentEquals(retrievedBytes), "Bytes should match")
    }
    
    @Test
    fun testFromFile() {
        // Create a temporary test file
        val tempFile = File.createTempFile("test", ".jpg")
        tempFile.writeBytes(ByteArray(50))
        
        try {
            val imageData = AndroidImageData.fromFile(tempFile)
            
            assertEquals("image/jpeg", imageData.mimeType)
            assertEquals(tempFile.name, imageData.fileName)
            assertEquals(50L, imageData.sizeInBytes)
        } finally {
            tempFile.delete()
        }
    }
    
    @Test
    fun testFromPathWithDifferentExtensions() {
        val testCases = listOf(
            ".jpg" to "image/jpeg",
            ".jpeg" to "image/jpeg",
            ".png" to "image/png",
            ".gif" to "image/gif",
            ".webp" to "image/webp"
        )
        
        testCases.forEach { (extension, expectedMimeType) ->
            val tempFile = File.createTempFile("test", extension)
            tempFile.writeBytes(ByteArray(10))
            
            try {
                val imageData = AndroidImageData.fromPath(tempFile.absolutePath)
                assertNotNull(imageData, "Should create ImageData for $extension")
                assertEquals(expectedMimeType, imageData?.mimeType,
                    "MIME type should be correct for $extension")
            } finally {
                tempFile.delete()
            }
        }
    }
    
    @Test
    fun testImageDataFactory() = runTest {
        val bytes = ByteArray(25)
        
        val imageData = ImageDataFactory.fromBytes(
            bytes = bytes,
            mimeType = "image/png",
            fileName = "factory-test.png"
        )
        
        assertEquals("image/png", imageData.mimeType)
        assertEquals("factory-test.png", imageData.fileName)
        
        val retrievedBytes = imageData.toByteArray()
        assertEquals(25, retrievedBytes.size)
    }
}

