package com.kmpnetworking.cloudflare

import com.kmpnetworking.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CloudflareImageUploaderTest {
    
    @Test
    fun testByteArrayImageDataCreation() {
        val bytes = ByteArray(100) { it.toByte() }
        val imageData = ByteArrayImageData(
            bytes = bytes,
            mimeType = "image/jpeg",
            fileName = "test.jpg"
        )
        
        assertEquals("image/jpeg", imageData.mimeType)
        assertEquals("test.jpg", imageData.fileName)
        assertEquals(100L, imageData.sizeInBytes)
    }
    
    @Test
    fun testImageDataFactoryFromBytes() {
        val bytes = ByteArray(50)
        val imageData = ImageDataFactory.fromBytes(
            bytes = bytes,
            mimeType = "image/png",
            fileName = "factory-test.png"
        )
        
        assertEquals("image/png", imageData.mimeType)
        assertEquals("factory-test.png", imageData.fileName)
    }
    
    @Test
    fun testCloudflareImageUploadResponseVariants() {
        val response = CloudflareImageUploadResponse(
            id = "test-123",
            filename = "test.jpg",
            uploaded = "2025-01-01T00:00:00.000Z",
            requireSignedURLs = false,
            variants = listOf(
                "https://imagedelivery.net/account/test-123/public",
                "https://imagedelivery.net/account/test-123/thumbnail",
                "https://imagedelivery.net/account/test-123/large"
            ),
            metadata = mapOf("user_id" to "12345")
        )
        
        assertEquals("test-123", response.id)
        assertEquals(3, response.variants.size)
        assertEquals("https://imagedelivery.net/account/test-123/public", response.publicUrl)
        assertEquals("https://imagedelivery.net/account/test-123/thumbnail", 
            response.getVariantUrl("thumbnail"))
        
        val variantMap = response.variantUrlMap
        assertEquals(3, variantMap.size)
        assertTrue(variantMap.containsKey("public"))
        assertTrue(variantMap.containsKey("thumbnail"))
        assertTrue(variantMap.containsKey("large"))
    }
    
    @Test
    fun testCloudflareImageListResponsePagination() {
        val response = CloudflareImageListResponse(
            images = listOf(),
            count = 20,
            page = 1,
            perPage = 20,
            totalCount = 50
        )
        
        assertTrue(response.hasMore)
        assertEquals(2, response.nextPage)
        
        val lastPageResponse = CloudflareImageListResponse(
            images = listOf(),
            count = 10,
            page = 3,
            perPage = 20,
            totalCount = 50
        )
        
        assertTrue(!lastPageResponse.hasMore)
        assertEquals(null, lastPageResponse.nextPage)
    }
    
    @Test
    fun testCloudflareUsageStats() {
        val stats = CloudflareUsageStats(
            count = CloudflareUsageCount(
                current = 5000,
                allowed = 10000
            )
        )
        
        assertEquals(50.0f, stats.count.percentageUsed)
        assertEquals(5000L, stats.count.remaining)
    }
    
    @Test
    fun testCloudflareUsageStatsAtLimit() {
        val stats = CloudflareUsageStats(
            count = CloudflareUsageCount(
                current = 10000,
                allowed = 10000
            )
        )
        
        assertEquals(100.0f, stats.count.percentageUsed)
        assertEquals(0L, stats.count.remaining)
    }
    
    @Test
    fun testCloudflareExceptionHandling() {
        val errors = listOf(
            CloudflareError(code = 1001, message = "Invalid API token"),
            CloudflareError(code = 1002, message = "Rate limit exceeded")
        )
        
        val exception = CloudflareException(errors)
        
        assertEquals("Invalid API token", exception.message)
        assertEquals(2, exception.allMessages.size)
        assertEquals(2, exception.errorCodes.size)
        assertTrue(exception.errorCodes.contains(1001))
        assertTrue(exception.errorCodes.contains(1002))
    }
    
    @Test
    fun testNetworkResultTransformations() {
        val successResult: NetworkResult<String> = NetworkResult.Success("test-image-id")
        
        val mapped = successResult.map { id -> 
            "https://example.com/images/$id" 
        }
        
        assertTrue(mapped.isSuccess)
        assertEquals("https://example.com/images/test-image-id", mapped.getOrNull())
    }
}

