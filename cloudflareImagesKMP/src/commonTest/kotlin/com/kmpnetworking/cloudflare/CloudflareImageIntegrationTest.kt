package com.kmpnetworking.cloudflare

import com.kmpnetworking.NetworkResult
import com.kmpnetworking.currentTimeMillis
import com.kmpnetworking.getEnvironmentVariable
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Integration tests for Cloudflare Images API
 * 
 * These tests actually call the Cloudflare API and require valid credentials.
 * They are skipped by default and only run when credentials are provided.
 * 
 * To run these tests:
 * 1. Set environment variables:
 *    export CLOUDFLARE_ACCOUNT_ID="your-account-id"
 *    export CLOUDFLARE_API_TOKEN="your-api-token"
 * 
 * 2. Run with Gradle:
 *    ./gradlew :shared:cleanTest :shared:test --tests "*Integration*"
 * 
 * Note: These tests will create and delete actual images in your Cloudflare account.
 */
@Suppress("unused")
class CloudflareImageIntegrationTest {
    
    private lateinit var uploader: CloudflareImageUploader
    private var testImageIds = mutableListOf<String>()
    
    // Check if credentials are available
    private val hasCredentials: Boolean
        get() = accountId.isNotEmpty() && apiToken.isNotEmpty()
    
    private val accountId: String
        get() = getEnvironmentVariable("CLOUDFLARE_ACCOUNT_ID") ?: ""
    
    private val apiToken: String
        get() = getEnvironmentVariable("CLOUDFLARE_API_TOKEN") ?: ""
    
    @BeforeTest
    fun setup() {
        if (hasCredentials) {
            uploader = CloudflareImageUploader.create(
                accountId = accountId,
                apiToken = apiToken,
                enableLogging = true,
                timeoutMillis = 60000
            )
        }
    }
    
    @AfterTest
    fun cleanup() = runTest {
        if (hasCredentials) {
            // Clean up any test images that were created
            testImageIds.forEach { imageId ->
                try {
                    uploader.deleteImage(imageId)
                    println("Cleaned up test image: $imageId")
                } catch (e: Exception) {
                    println("Failed to clean up image $imageId: ${e.message}")
                }
            }
            testImageIds.clear()
            uploader.close()
        }
    }
    
    @Test
    fun testUploadImage() = runTest {
        if (!hasCredentials) {
            println("⚠️  Skipping integration test - credentials not provided")
            return@runTest
        }
        
        println("🧪 Testing image upload to Cloudflare...")
        
        // Create a small test image (1x1 pixel red PNG)
        val testImageBytes = createTestPngImage()
        val imageData = ImageDataFactory.fromBytes(
            bytes = testImageBytes,
            mimeType = "image/png",
            fileName = "integration-test.png"
        )
        
        // Upload the image
        val result = uploader.uploadImage(
            imageData = imageData,
            metadata = mapOf(
                "test" to "true",
                "test_name" to "testUploadImage",
                "timestamp" to currentTimeMillis().toString()
            )
        )
        
        // Print error details if upload failed
        result.onError { message, exception ->
            println("❌ Upload failed: $message")
            exception?.printStackTrace()
        }
        
        // Verify the result
        assertTrue(result.isSuccess, "Upload should succeed")
        
        result.onSuccess { response ->
            println("✅ Upload successful!")
            println("   Image ID: ${response.id}")
            println("   Filename: ${response.filename}")
            println("   Variants: ${response.variants.size}")
            println("   Public URL: ${response.publicUrl}")
            
            assertNotNull(response.id, "Image ID should not be null")
            assertNotNull(response.filename, "Filename should not be null")
            assertTrue(response.variants.isNotEmpty(), "Should have at least one variant")
            
            // Track for cleanup
            testImageIds.add(response.id)
        }.onError { message, exception ->
            fail("Upload failed: $message", exception)
        }
    }
    
    @Test
    fun testUploadWithProgressTracking() = runTest {
        if (!hasCredentials) {
            println("⚠️  Skipping integration test - credentials not provided")
            return@runTest
        }
        
        println("🧪 Testing upload with progress tracking...")
        
        val progressUpdates = mutableListOf<Float>()
        val imageData = ImageDataFactory.fromBytes(
            bytes = createTestPngImage(),
            mimeType = "image/png",
            fileName = "progress-test.png"
        )
        
        val result = uploader.uploadImage(
            imageData = imageData,
            onProgress = { progress ->
                progressUpdates.add(progress)
                println("   Progress: ${(progress * 100).toInt()}%")
            }
        )
        
        // Print error details if upload failed
        result.onError { message, exception ->
            println("❌ Upload failed: $message")
            exception?.printStackTrace()
        }
        
        assertTrue(result.isSuccess, "Upload should succeed")
        assertTrue(progressUpdates.isNotEmpty(), "Should have progress updates")
        assertTrue(progressUpdates.last() > 0.5f, "Final progress should be substantial")
        
        result.onSuccess { response ->
            testImageIds.add(response.id)
            println("✅ Progress tracking test passed!")
        }
    }
    
    @Test
    fun testUploadImageFromUrl() = runTest {
        if (!hasCredentials) {
            println("⚠️  Skipping integration test - credentials not provided")
            return@runTest
        }
        
        println("🧪 Testing upload from URL...")
        
        // Use a reliable public test image URL
        // Using httpbin which provides test images
        val testImageUrl = "https://httpbin.org/image/png"
        
        val result = uploader.uploadImageFromUrl(
            url = testImageUrl,
            metadata = mapOf(
                "test" to "true",
                "source" to "url",
                "timestamp" to currentTimeMillis().toString()
            )
        )
        
        // Print error details if upload failed
        result.onError { message, exception ->
            println("❌ URL upload failed: $message")
            exception?.printStackTrace()
        }
        
        assertTrue(result.isSuccess, "URL upload should succeed")
        
        result.onSuccess { response ->
            println("✅ URL upload successful!")
            println("   Image ID: ${response.id}")
            testImageIds.add(response.id)
        }.onError { message, _ ->
            fail("URL upload failed: $message")
        }
    }
    
    @Test
    fun testListImages() = runTest {
        if (!hasCredentials) {
            println("⚠️  Skipping integration test - credentials not provided")
            return@runTest
        }
        
        println("🧪 Testing list images...")
        
        val result = uploader.listImages(page = 1, perPage = 10)
        
        assertTrue(result.isSuccess, "List should succeed")
        
        result.onSuccess { response ->
            println("✅ List images successful!")
            println("   Total images: ${response.totalCount}")
            println("   Current page: ${response.page}")
            println("   Images on page: ${response.images.size}")
            
            assertTrue(response.totalCount >= 0, "Should have valid count")
            assertTrue(response.images.size <= 10, "Should not exceed per_page limit")
        }.onError { message, _ ->
            fail("List images failed: $message")
        }
    }
    
    @Test
    fun testGetImageDetails() = runTest {
        if (!hasCredentials) {
            println("⚠️  Skipping integration test - credentials not provided")
            return@runTest
        }
        
        println("🧪 Testing get image details...")
        
        // First upload an image
        val imageData = ImageDataFactory.fromBytes(
            bytes = createTestPngImage(),
            mimeType = "image/png",
            fileName = "details-test.png"
        )
        
        val uploadResult = uploader.uploadImage(imageData)
        
        uploadResult.onSuccess { uploadResponse ->
            testImageIds.add(uploadResponse.id)
            
            // Now get its details
            val detailsResult = uploader.getImageDetails(uploadResponse.id)
            
            assertTrue(detailsResult.isSuccess, "Get details should succeed")
            
            detailsResult.onSuccess { details ->
                println("✅ Get details successful!")
                println("   ID: ${details.id}")
                println("   Filename: ${details.filename}")
                
                assertEquals(uploadResponse.id, details.id, "IDs should match")
                assertNotNull(details.uploaded, "Upload date should be present")
            }.onError { message, _ ->
                fail("Get details failed: $message")
            }
        }
    }
    
    @Test
    fun testUpdateImage() = runTest {
        if (!hasCredentials) {
            println("⚠️  Skipping integration test - credentials not provided")
            return@runTest
        }
        
        println("🧪 Testing update image metadata...")
        
        // Upload an image first
        val imageData = ImageDataFactory.fromBytes(
            bytes = createTestPngImage(),
            mimeType = "image/png",
            fileName = "update-test.png"
        )
        
        val uploadResult = uploader.uploadImage(imageData)
        
        uploadResult.onSuccess { uploadResponse ->
            testImageIds.add(uploadResponse.id)
            
            // Update its metadata
            val updateResult = uploader.updateImage(
                imageId = uploadResponse.id,
                metadata = mapOf(
                    "updated" to "true",
                    "version" to "2"
                )
            )
            
            // Print error details if update failed
            updateResult.onError { message, exception ->
                println("❌ Update failed: $message")
                exception?.printStackTrace()
            }
            
            assertTrue(updateResult.isSuccess, "Update should succeed")
            
            updateResult.onSuccess { updated ->
                println("✅ Update successful!")
                println("   Updated metadata: ${updated.metadata}")
                
                assertEquals("true", updated.metadata?.get("updated"), "Metadata should be updated")
            }.onError { message, _ ->
                fail("Update failed: $message")
            }
        }
    }
    
    @Test
    fun testDeleteImage() = runTest {
        if (!hasCredentials) {
            println("⚠️  Skipping integration test - credentials not provided")
            return@runTest
        }
        
        println("🧪 Testing delete image...")
        
        // Upload an image
        val imageData = ImageDataFactory.fromBytes(
            bytes = createTestPngImage(),
            mimeType = "image/png",
            fileName = "delete-test.png"
        )
        
        val uploadResult = uploader.uploadImage(imageData)
        
        uploadResult.onSuccess { uploadResponse ->
            val imageId = uploadResponse.id
            
            // Delete it
            val deleteResult = uploader.deleteImage(imageId)
            
            assertTrue(deleteResult.isSuccess, "Delete should succeed")
            println("✅ Delete successful!")
            
            // Verify it's gone
            val detailsResult = uploader.getImageDetails(imageId)
            assertTrue(detailsResult.isError, "Image should not be found after deletion")
            
            // Remove from cleanup list since we already deleted it
            testImageIds.remove(imageId)
        }
    }
    
    @Test
    fun testGetUsageStats() = runTest {
        if (!hasCredentials) {
            println("⚠️  Skipping integration test - credentials not provided")
            return@runTest
        }
        
        println("🧪 Testing get usage stats...")
        
        val result = uploader.getUsageStats()
        
        assertTrue(result.isSuccess, "Get stats should succeed")
        
        result.onSuccess { stats ->
            println("✅ Stats retrieved successfully!")
            println("   Current: ${stats.count.current}")
            println("   Allowed: ${stats.count.allowed}")
            println("   Usage: ${stats.count.percentageUsed}%")
            println("   Remaining: ${stats.count.remaining}")
            
            assertTrue(stats.count.current >= 0, "Current count should be non-negative")
            assertTrue(stats.count.allowed > 0, "Allowed should be positive")
            assertTrue(stats.count.percentageUsed >= 0f, "Percentage should be non-negative")
        }.onError { message, _ ->
            fail("Get stats failed: $message")
        }
    }
    
    @Test
    fun testUploadWithCustomId() = runTest {
        if (!hasCredentials) {
            println("⚠️  Skipping integration test - credentials not provided")
            return@runTest
        }
        
        println("🧪 Testing upload with custom ID...")
        
        val customId = "test-custom-id-${currentTimeMillis()}"
        val imageData = ImageDataFactory.fromBytes(
            bytes = createTestPngImage(),
            mimeType = "image/png",
            fileName = "custom-id-test.png"
        )
        
        val result = uploader.uploadImage(
            imageData = imageData,
            id = customId
        )
        
        // Print error details if upload failed
        result.onError { message, exception ->
            println("❌ Custom ID upload failed: $message")
            exception?.printStackTrace()
        }
        
        assertTrue(result.isSuccess, "Upload with custom ID should succeed")
        
        result.onSuccess { response ->
            println("✅ Custom ID upload successful!")
            println("   Custom ID: ${response.id}")
            
            assertEquals(customId, response.id, "Should use custom ID")
            testImageIds.add(response.id)
        }.onError { message, _ ->
            fail("Custom ID upload failed: $message")
        }
    }
    
    @Test
    fun testInvalidCredentials() = runTest {
        println("🧪 Testing invalid credentials handling...")
        
        val invalidUploader = CloudflareImageUploader.create(
            accountId = "invalid-account-id",
            apiToken = "invalid-token",
            enableLogging = false
        )
        
        val imageData = ImageDataFactory.fromBytes(
            bytes = createTestPngImage(),
            mimeType = "image/png",
            fileName = "test.png"
        )
        
        val result = invalidUploader.uploadImage(imageData)
        
        assertTrue(result.isError, "Should fail with invalid credentials")
        
        result.onError { message, exception ->
            println("✅ Correctly handled invalid credentials")
            println("   Error: $message")
            assertNotNull(message, "Should have error message")
        }
        
        invalidUploader.close()
    }
    
    // Helper function to create a minimal 1x1 PNG image
    // Using a base64-encoded valid 1x1 red PNG image
    private fun createTestPngImage(): ByteArray {
        // Base64-encoded valid 1x1 red PNG (RGBA)
        // This is a properly formatted PNG that Cloudflare can decode
        val base64Png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        // Use Java Base64 decoder (works on JVM for Android unit tests)
        return java.util.Base64.getDecoder().decode(base64Png)
    }
    
}

