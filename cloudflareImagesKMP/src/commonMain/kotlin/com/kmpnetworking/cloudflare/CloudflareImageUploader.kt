package com.kmpnetworking.cloudflare

import com.kmpnetworking.NetworkResult
import com.kmpnetworking.getHttpClientEngine
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.json.Json

/**
 * Cloudflare Images API client for uploading and managing images.
 * 
 * Business logic is centralized here, with platform-specific file handling
 * abstracted through the ImageData interface.
 * 
 * @param accountId Your Cloudflare account ID
 * @param apiToken Your Cloudflare API token with Images:Write permission
 * @param enableLogging Enable request/response logging
 * @param timeoutMillis Request timeout in milliseconds
 */
class CloudflareImageUploader(
    private val accountId: String,
    private val apiToken: String,
    private val enableLogging: Boolean = true,
    private val timeoutMillis: Long = 60000
) {
    
    private val baseUrl = "https://api.cloudflare.com/client/v4/accounts/$accountId/images/v1"
    
    private val httpClient = HttpClient(getHttpClientEngine()) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        
        if (enableLogging) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMillis
            connectTimeoutMillis = timeoutMillis
            socketTimeoutMillis = timeoutMillis
        }
        
        defaultRequest {
            header("Authorization", "Bearer $apiToken")
        }
    }
    
    /**
     * Upload an image to Cloudflare Images
     * 
     * @param imageData Platform-specific image data (see ImageData implementations)
     * @param id Optional custom ID for the image (auto-generated if not provided)
     * @param requireSignedURLs Whether the image requires signed URLs for access
     * @param metadata Optional metadata map (max 10 key-value pairs)
     * @param onProgress Optional progress callback (0.0 to 1.0)
     * @return NetworkResult containing the upload response
     */
    suspend fun uploadImage(
        imageData: ImageData,
        id: String? = null,
        requireSignedURLs: Boolean = false,
        metadata: Map<String, String>? = null,
        onProgress: ((Float) -> Unit)? = null
    ): NetworkResult<CloudflareImageUploadResponse> {
        return try {
            // Get image bytes (need to call suspend function)
            val imageBytes = imageData.toByteArray()
            
            println("Uploading image: ${imageData.fileName}, size: ${imageBytes.size} bytes, type: ${imageData.mimeType}")
            
            val response = httpClient.submitFormWithBinaryData(
                url = baseUrl,
                formData = formData {
                    // Add image file
                    append("file", imageBytes, Headers.build {
                        append(HttpHeaders.ContentType, imageData.mimeType)
                        append(HttpHeaders.ContentDisposition, "filename=\"${imageData.fileName}\"")
                    })
                    
                    // Add optional parameters
                    id?.let { 
                        println("Adding custom ID: $it")
                        append("id", it) 
                    }
                    append("requireSignedURLs", requireSignedURLs.toString())
                    
                    // Add metadata as JSON string (Cloudflare expects this format)
                    if (!metadata.isNullOrEmpty()) {
                        val metadataJson = metadata.entries.joinToString(",", "{", "}") { 
                            "\"${it.key}\":\"${it.value}\"" 
                        }
                        println("Adding metadata: $metadataJson")
                        append("metadata", metadataJson)
                    }
                }
            ) {
                onUpload { bytesSentTotal, contentLength ->
                    onProgress?.invoke(bytesSentTotal.toFloat() / contentLength.toFloat())
                }
            }
            
            if (response.status.isSuccess()) {
                val result: CloudflareApiResponse<CloudflareImage> = response.body()
                if (result.success) {
                    NetworkResult.Success(
                        CloudflareImageUploadResponse(
                            id = result.result.id,
                            filename = result.result.filename,
                            uploaded = result.result.uploaded,
                            requireSignedURLs = result.result.requireSignedURLs,
                            variants = result.result.variants,
                            metadata = result.result.meta
                        )
                    )
                } else {
                    NetworkResult.Error(
                        message = result.errors.firstOrNull()?.message ?: "Upload failed",
                        exception = CloudflareException(result.errors)
                    )
                }
            } else {
                NetworkResult.Error(
                    message = "Upload failed with status: ${response.status}",
                    exception = Exception(response.bodyAsText())
                )
            }
        } catch (e: Exception) {
            NetworkResult.Error(
                message = e.message ?: "Unknown error during upload",
                exception = e
            )
        }
    }
    
    /**
     * Upload an image from a URL
     * 
     * @param url URL of the image to upload
     * @param id Optional custom ID for the image
     * @param requireSignedURLs Whether the image requires signed URLs
     * @param metadata Optional metadata map
     * @return NetworkResult containing the upload response
     */
    suspend fun uploadImageFromUrl(
        url: String,
        id: String? = null,
        requireSignedURLs: Boolean = false,
        metadata: Map<String, String>? = null
    ): NetworkResult<CloudflareImageUploadResponse> {
        return try {
            val response = httpClient.submitFormWithBinaryData(
                url = baseUrl,
                formData = formData {
                    append("url", url)
                    id?.let { append("id", it) }
                    append("requireSignedURLs", requireSignedURLs.toString())
                    
                    // Add metadata as JSON string
                    if (!metadata.isNullOrEmpty()) {
                        val metadataJson = metadata.entries.joinToString(",", "{", "}") { 
                            "\"${it.key}\":\"${it.value}\"" 
                        }
                        append("metadata", metadataJson)
                    }
                }
            )
            
            if (response.status.isSuccess()) {
                val result: CloudflareApiResponse<CloudflareImage> = response.body()
                if (result.success) {
                    NetworkResult.Success(
                        CloudflareImageUploadResponse(
                            id = result.result.id,
                            filename = result.result.filename,
                            uploaded = result.result.uploaded,
                            requireSignedURLs = result.result.requireSignedURLs,
                            variants = result.result.variants,
                            metadata = result.result.meta
                        )
                    )
                } else {
                    NetworkResult.Error(
                        message = result.errors.firstOrNull()?.message ?: "Upload failed",
                        exception = CloudflareException(result.errors)
                    )
                }
            } else {
                NetworkResult.Error(
                    message = "Upload failed with status: ${response.status}",
                    exception = Exception(response.bodyAsText())
                )
            }
        } catch (e: Exception) {
            NetworkResult.Error(
                message = e.message ?: "Unknown error during upload",
                exception = e
            )
        }
    }
    
    /**
     * Get details about a specific image
     * 
     * @param imageId The ID of the image
     * @return NetworkResult containing image details
     */
    suspend fun getImageDetails(imageId: String): NetworkResult<CloudflareImageUploadResponse> {
        return try {
            val response = httpClient.get("$baseUrl/$imageId")
            
            if (response.status.isSuccess()) {
                val result: CloudflareApiResponse<CloudflareImage> = response.body()
                if (result.success) {
                    NetworkResult.Success(
                        CloudflareImageUploadResponse(
                            id = result.result.id,
                            filename = result.result.filename,
                            uploaded = result.result.uploaded,
                            requireSignedURLs = result.result.requireSignedURLs,
                            variants = result.result.variants,
                            metadata = result.result.meta
                        )
                    )
                } else {
                    NetworkResult.Error(
                        message = result.errors.firstOrNull()?.message ?: "Failed to get image details",
                        exception = CloudflareException(result.errors)
                    )
                }
            } else {
                NetworkResult.Error(
                    message = "Request failed with status: ${response.status}",
                    exception = Exception(response.bodyAsText())
                )
            }
        } catch (e: Exception) {
            NetworkResult.Error(
                message = e.message ?: "Unknown error",
                exception = e
            )
        }
    }
    
    /**
     * Delete an image
     * 
     * @param imageId The ID of the image to delete
     * @return NetworkResult indicating success or failure
     */
    suspend fun deleteImage(imageId: String): NetworkResult<Unit> {
        return try {
            val response = httpClient.delete("$baseUrl/$imageId")
            
            if (response.status.isSuccess()) {
                val result: CloudflareApiResponse<Unit> = response.body()
                if (result.success) {
                    NetworkResult.Success(Unit)
                } else {
                    NetworkResult.Error(
                        message = result.errors.firstOrNull()?.message ?: "Failed to delete image",
                        exception = CloudflareException(result.errors)
                    )
                }
            } else {
                NetworkResult.Error(
                    message = "Delete failed with status: ${response.status}",
                    exception = Exception(response.bodyAsText())
                )
            }
        } catch (e: Exception) {
            NetworkResult.Error(
                message = e.message ?: "Unknown error",
                exception = e
            )
        }
    }
    
    /**
     * List all images (paginated)
     * 
     * @param page Page number (default: 1)
     * @param perPage Results per page (max: 100, default: 20)
     * @return NetworkResult containing list of images
     */
    suspend fun listImages(
        page: Int = 1,
        perPage: Int = 20
    ): NetworkResult<CloudflareImageListResponse> {
        return try {
            println("Listing images: page=$page, perPage=$perPage")
            
            val response = httpClient.get(baseUrl) {
                parameter("page", page)
                parameter("per_page", perPage.coerceIn(1, 100))
            }
            
            println("List response status: ${response.status}")
            
            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                println("List response body: $responseBody")
                
                val result: CloudflareApiResponse<CloudflareImageList> = response.body()
                if (result.success) {
                    val listResponse = CloudflareImageListResponse(
                        images = result.result.images.map { img ->
                            CloudflareImageUploadResponse(
                                id = img.id,
                                filename = img.filename,
                                uploaded = img.uploaded,
                                requireSignedURLs = img.requireSignedURLs,
                                variants = img.variants,
                                metadata = img.meta
                            )
                        },
                        count = result.resultInfo?.count ?: result.result.images.size,
                        page = result.resultInfo?.page ?: page,
                        perPage = result.resultInfo?.perPage ?: perPage,
                        totalCount = result.resultInfo?.totalCount ?: result.result.images.size
                    )
                    println("Found ${listResponse.totalCount} total images, ${listResponse.images.size} on this page")
                    NetworkResult.Success(listResponse)
                } else {
                    val errorMsg = result.errors.firstOrNull()?.message ?: "Failed to list images"
                    println("List failed: $errorMsg")
                    NetworkResult.Error(
                        message = errorMsg,
                        exception = CloudflareException(result.errors)
                    )
                }
            } else {
                val errorBody = response.bodyAsText()
                println("List request failed with status: ${response.status}, body: $errorBody")
                NetworkResult.Error(
                    message = "Request failed with status: ${response.status}",
                    exception = Exception(errorBody)
                )
            }
        } catch (e: Exception) {
            println("List images exception: ${e.message}")
            e.printStackTrace()
            NetworkResult.Error(
                message = e.message ?: "Unknown error",
                exception = e
            )
        }
    }
    
    /**
     * Update image metadata or settings
     * 
     * @param imageId The ID of the image
     * @param requireSignedURLs Update signed URL requirement
     * @param metadata Update metadata (replaces existing)
     * @return NetworkResult containing updated image details
     */
    suspend fun updateImage(
        imageId: String,
        requireSignedURLs: Boolean? = null,
        metadata: Map<String, String>? = null
    ): NetworkResult<CloudflareImageUploadResponse> {
        return try {
            // Use request() with HttpMethod.Patch to work around Android HttpURLConnection limitations
            val response = httpClient.request("$baseUrl/$imageId") {
                method = HttpMethod.Patch
                contentType(ContentType.Application.Json)
                setBody(CloudflareUpdateImageRequest(
                    requireSignedURLs = requireSignedURLs,
                    metadata = metadata
                ))
            }
            
            if (response.status.isSuccess()) {
                val result: CloudflareApiResponse<CloudflareImage> = response.body()
                if (result.success) {
                    NetworkResult.Success(
                        CloudflareImageUploadResponse(
                            id = result.result.id,
                            filename = result.result.filename,
                            uploaded = result.result.uploaded,
                            requireSignedURLs = result.result.requireSignedURLs,
                            variants = result.result.variants,
                            metadata = result.result.meta
                        )
                    )
                } else {
                    NetworkResult.Error(
                        message = result.errors.firstOrNull()?.message ?: "Failed to update image",
                        exception = CloudflareException(result.errors)
                    )
                }
            } else {
                NetworkResult.Error(
                    message = "Update failed with status: ${response.status}",
                    exception = Exception(response.bodyAsText())
                )
            }
        } catch (e: Exception) {
            NetworkResult.Error(
                message = e.message ?: "Unknown error",
                exception = e
            )
        }
    }
    
    /**
     * Get usage statistics
     * 
     * @return NetworkResult containing usage information
     */
    suspend fun getUsageStats(): NetworkResult<CloudflareUsageStats> {
        return try {
            val response = httpClient.get("$baseUrl/stats")
            
            if (response.status.isSuccess()) {
                val result: CloudflareApiResponse<CloudflareUsageStats> = response.body()
                if (result.success) {
                    NetworkResult.Success(result.result)
                } else {
                    NetworkResult.Error(
                        message = result.errors.firstOrNull()?.message ?: "Failed to get stats",
                        exception = CloudflareException(result.errors)
                    )
                }
            } else {
                NetworkResult.Error(
                    message = "Request failed with status: ${response.status}",
                    exception = Exception(response.bodyAsText())
                )
            }
        } catch (e: Exception) {
            NetworkResult.Error(
                message = e.message ?: "Unknown error",
                exception = e
            )
        }
    }
    
    /**
     * Close the client and release resources
     */
    fun close() {
        httpClient.close()
    }
    
    companion object {
        /**
         * Create a CloudflareImageUploader instance
         */
        fun create(
            accountId: String,
            apiToken: String,
            enableLogging: Boolean = true,
            timeoutMillis: Long = 60000
        ): CloudflareImageUploader {
            return CloudflareImageUploader(accountId, apiToken, enableLogging, timeoutMillis)
        }
    }
}

