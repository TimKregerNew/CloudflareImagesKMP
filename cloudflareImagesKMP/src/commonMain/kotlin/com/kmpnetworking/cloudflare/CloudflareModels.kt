package com.kmpnetworking.cloudflare

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Standard Cloudflare API response wrapper
 */
@Serializable
internal data class CloudflareApiResponse<T>(
    @SerialName("success")
    val success: Boolean,
    @SerialName("errors")
    val errors: List<CloudflareError> = emptyList(),
    @SerialName("messages")
    val messages: List<String> = emptyList(),
    @SerialName("result")
    val result: T,
    @SerialName("result_info")
    val resultInfo: CloudflareResultInfo? = null
)

/**
 * Cloudflare error structure
 */
@Serializable
data class CloudflareError(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String
)

/**
 * Pagination information
 */
@Serializable
internal data class CloudflareResultInfo(
    @SerialName("count")
    val count: Int,
    @SerialName("page")
    val page: Int,
    @SerialName("per_page")
    val perPage: Int,
    @SerialName("total_count")
    val totalCount: Int
)

/**
 * Internal Cloudflare image structure
 */
@Serializable
internal data class CloudflareImage(
    @SerialName("id")
    val id: String,
    @SerialName("filename")
    val filename: String,
    @SerialName("uploaded")
    val uploaded: String,
    @SerialName("requireSignedURLs")
    val requireSignedURLs: Boolean,
    @SerialName("variants")
    val variants: List<String>,
    @SerialName("meta")
    val meta: Map<String, String>? = null
)

/**
 * Internal image list structure
 */
@Serializable
internal data class CloudflareImageList(
    @SerialName("images")
    val images: List<CloudflareImage>
)

/**
 * Update image request
 */
@Serializable
internal data class CloudflareUpdateImageRequest(
    @SerialName("requireSignedURLs")
    val requireSignedURLs: Boolean? = null,
    @SerialName("metadata")
    val metadata: Map<String, String>? = null
)

/**
 * Public response model for image upload
 */
data class CloudflareImageUploadResponse(
    val id: String,
    val filename: String,
    val uploaded: String,
    val requireSignedURLs: Boolean,
    val variants: List<String>,
    val metadata: Map<String, String>? = null
) {
    /**
     * Get the public URL for a specific variant
     * 
     * @param variantName Name of the variant (e.g., "public", "thumbnail")
     * @return URL string or null if variant doesn't exist
     */
    fun getVariantUrl(variantName: String): String? {
        return variants.firstOrNull { it.contains("/$variantName") }
    }
    
    /**
     * Get the public URL (if available)
     */
    val publicUrl: String?
        get() = getVariantUrl("public")
    
    /**
     * Get all variant URLs as a map
     */
    val variantUrlMap: Map<String, String>
        get() = variants.mapNotNull { url ->
            val variantName = url.substringAfterLast("/")
            variantName to url
        }.toMap()
}

/**
 * Response for listing images
 */
data class CloudflareImageListResponse(
    val images: List<CloudflareImageUploadResponse>,
    val count: Int,
    val page: Int,
    val perPage: Int,
    val totalCount: Int
) {
    /**
     * Check if there are more pages
     */
    val hasMore: Boolean
        get() = (page * perPage) < totalCount
    
    /**
     * Get next page number
     */
    val nextPage: Int?
        get() = if (hasMore) page + 1 else null
}

/**
 * Usage statistics
 */
@Serializable
data class CloudflareUsageStats(
    @SerialName("count")
    val count: CloudflareUsageCount
)

@Serializable
data class CloudflareUsageCount(
    @SerialName("current")
    val current: Long,
    @SerialName("allowed")
    val allowed: Long
) {
    /**
     * Percentage of quota used
     */
    val percentageUsed: Float
        get() = if (allowed > 0) (current.toFloat() / allowed.toFloat()) * 100f else 0f
    
    /**
     * Remaining quota
     */
    val remaining: Long
        get() = (allowed - current).coerceAtLeast(0)
}

/**
 * Exception for Cloudflare API errors
 */
class CloudflareException(
    val errors: List<CloudflareError>
) : Exception(errors.firstOrNull()?.message ?: "Cloudflare API error") {
    
    /**
     * Get all error messages
     */
    val allMessages: List<String>
        get() = errors.map { it.message }
    
    /**
     * Get all error codes
     */
    val errorCodes: List<Int>
        get() = errors.map { it.code }
    
    override fun toString(): String {
        return "CloudflareException(errors=${errors.joinToString { "${it.code}: ${it.message}" }})"
    }
}

