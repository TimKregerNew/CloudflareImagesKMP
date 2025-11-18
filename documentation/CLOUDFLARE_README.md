# Cloudflare Images Integration

This module provides seamless integration with Cloudflare Images API for uploading and managing images across Android and iOS platforms.

## Features

- ✅ Upload images to Cloudflare Images
- ✅ Upload from URLs
- ✅ Manage image metadata
- ✅ List, update, and delete images
- ✅ Track usage statistics
- ✅ Progress tracking for uploads
- ✅ Platform-agnostic image handling
- ✅ All business logic in shared code

## Setup

### Get Cloudflare Credentials

1. Log in to [Cloudflare Dashboard](https://dash.cloudflare.com/)
2. Go to Images
3. Get your Account ID and API Token
   - Account ID: Found in the URL or Images page
   - API Token: Create one with "Images:Write" permission

### Initialize the Uploader

```kotlin
val uploader = CloudflareImageUploader.create(
    accountId = "your-account-id",
    apiToken = "your-api-token",
    enableLogging = true
)
```

## Usage Examples

### Android

#### Upload from File

```kotlin
import com.kmpnetworking.cloudflare.CloudflareImageUploader
import com.kmpnetworking.cloudflare.AndroidImageData
import java.io.File

suspend fun uploadImage(file: File) {
    val uploader = CloudflareImageUploader.create(
        accountId = "your-account-id",
        apiToken = "your-api-token"
    )
    
    val imageData = AndroidImageData.fromFile(file)
    
    val result = uploader.uploadImage(
        imageData = imageData,
        metadata = mapOf(
            "user_id" to "12345",
            "upload_source" to "android_app"
        ),
        onProgress = { progress ->
            println("Upload progress: ${(progress * 100).toInt()}%")
        }
    )
    
    result.onSuccess { response ->
        println("Image uploaded! ID: ${response.id}")
        println("Public URL: ${response.publicUrl}")
    }.onError { message, _ ->
        println("Upload failed: $message")
    }
    
    uploader.close()
}
```

#### Upload from Uri (Camera/Gallery)

```kotlin
import android.content.Context
import android.net.Uri

suspend fun uploadFromGallery(context: Context, uri: Uri) {
    val imageData = AndroidImageData.fromUri(context, uri)
    
    if (imageData != null) {
        val result = uploader.uploadImage(imageData)
        // Handle result
    }
}
```

#### Upload from Bitmap

```kotlin
import android.graphics.Bitmap

suspend fun uploadBitmap(bitmap: Bitmap) {
    val imageData = AndroidImageData.fromBitmap(
        bitmap = bitmap,
        format = Bitmap.CompressFormat.JPEG,
        quality = 90
    )
    
    val result = uploader.uploadImage(imageData)
    // Handle result
}
```

### iOS

#### Upload from UIImage

```swift
import shared

func uploadImage(image: UIImage) async {
    let uploader = CloudflareImageUploader.Companion().create(
        accountId: "your-account-id",
        apiToken: "your-api-token",
        enableLogging: true,
        timeoutMillis: 60000
    )
    
    if let imageData = IOSImageData.Companion().fromUIImage(
        image: image,
        compressionQuality: 0.9,
        format: .jpeg,
        fileName: "photo.jpg"
    ) {
        uploader.uploadImage(
            imageData: imageData,
            id: nil,
            requireSignedURLs: false,
            metadata: ["user_id": "12345"],
            onProgress: { progress in
                print("Upload progress: \(progress.floatValue * 100)%")
            }
        ) { result in
            if let success = result as? NetworkResult.Success<CloudflareImageUploadResponse> {
                let response = success.data as! CloudflareImageUploadResponse
                print("Uploaded! URL: \(response.publicUrl ?? "")")
            } else if let error = result as? NetworkResult.Error {
                print("Error: \(error.message)")
            }
        }
    }
    
    uploader.close()
}
```

#### Upload from File Path

```swift
func uploadFromFile(path: String) async {
    if let imageData = IOSImageData.Companion().fromPath(path: path) {
        // Upload using imageData
    }
}
```

### Common Operations (Same on Both Platforms)

#### Upload from URL

```kotlin
suspend fun uploadFromUrl() {
    val result = uploader.uploadImageFromUrl(
        url = "https://example.com/image.jpg",
        id = "custom-id-123",
        metadata = mapOf("source" to "web")
    )
}
```

#### List Images

```kotlin
suspend fun listAllImages() {
    val result = uploader.listImages(page = 1, perPage = 50)
    
    result.onSuccess { response ->
        println("Found ${response.totalCount} images")
        response.images.forEach { image ->
            println("${image.id}: ${image.filename}")
        }
        
        if (response.hasMore) {
            println("More images available on page ${response.nextPage}")
        }
    }
}
```

#### Get Image Details

```kotlin
suspend fun getImageInfo(imageId: String) {
    val result = uploader.getImageDetails(imageId)
    
    result.onSuccess { image ->
        println("Filename: ${image.filename}")
        println("Uploaded: ${image.uploaded}")
        println("Variants: ${image.variantUrlMap}")
    }
}
```

#### Update Image

```kotlin
suspend fun updateImageMetadata(imageId: String) {
    val result = uploader.updateImage(
        imageId = imageId,
        requireSignedURLs = true,
        metadata = mapOf(
            "updated_at" to "2025-01-01",
            "status" to "published"
        )
    )
}
```

#### Delete Image

```kotlin
suspend fun deleteImage(imageId: String) {
    val result = uploader.deleteImage(imageId)
    
    result.onSuccess {
        println("Image deleted successfully")
    }
}
```

#### Get Usage Statistics

```kotlin
suspend fun checkQuota() {
    val result = uploader.getUsageStats()
    
    result.onSuccess { stats ->
        println("Used: ${stats.count.current} / ${stats.count.allowed}")
        println("Percentage: ${stats.count.percentageUsed}%")
        println("Remaining: ${stats.count.remaining}")
    }
}
```

## Working with Variants

Cloudflare Images creates multiple variants of your uploaded images:

```kotlin
result.onSuccess { response ->
    // Get specific variant URL
    val thumbnailUrl = response.getVariantUrl("thumbnail")
    val publicUrl = response.publicUrl
    
    // Get all variant URLs
    val allVariants = response.variantUrlMap
    allVariants.forEach { (name, url) ->
        println("$name: $url")
    }
}
```

## Error Handling

```kotlin
uploader.uploadImage(imageData).onError { message, exception ->
    when (exception) {
        is CloudflareException -> {
            // Cloudflare-specific errors
            exception.allMessages.forEach { println(it) }
            exception.errorCodes.forEach { println("Error code: $it") }
        }
        else -> {
            // Network or other errors
            println("Error: $message")
        }
    }
}
```

## Best Practices

### 1. Reuse the Uploader Instance

```kotlin
class ImageRepository {
    private val uploader = CloudflareImageUploader.create(
        accountId = BuildConfig.CLOUDFLARE_ACCOUNT_ID,
        apiToken = BuildConfig.CLOUDFLARE_API_TOKEN
    )
    
    suspend fun upload(imageData: ImageData) = uploader.uploadImage(imageData)
    
    fun cleanup() = uploader.close()
}
```

### 2. Compress Images Before Upload

```kotlin
// Android
val compressedImageData = AndroidImageData.fromBitmap(
    bitmap = originalBitmap,
    format = Bitmap.CompressFormat.JPEG,
    quality = 80  // Balance quality and size
)

// iOS
let compressedImageData = IOSImageData.fromUIImage(
    image: originalImage,
    compressionQuality: 0.8
)
```

### 3. Use Custom IDs for Easy Reference

```kotlin
val result = uploader.uploadImage(
    imageData = imageData,
    id = "user-${userId}-avatar-${timestamp}"
)
```

### 4. Add Metadata for Searching

```kotlin
uploader.uploadImage(
    imageData = imageData,
    metadata = mapOf(
        "user_id" to userId,
        "content_type" to "avatar",
        "uploaded_from" to "mobile_app"
    )
)
```

### 5. Handle Progress for Better UX

```kotlin
uploader.uploadImage(
    imageData = imageData,
    onProgress = { progress ->
        updateUI(progress)
    }
)
```

### 6. Always Close When Done

```kotlin
try {
    // Use uploader
} finally {
    uploader.close()
}
```

## Security Considerations

### API Token Storage

**Never hardcode tokens in your app!**

```kotlin
// ❌ Bad
val apiToken = "your-token-here"

// ✅ Good - Use environment variables or secure storage
val apiToken = System.getenv("CLOUDFLARE_API_TOKEN")

// ✅ Better - Proxy through your backend
// Your backend handles authentication and generates temporary tokens
```

### Signed URLs

For sensitive images, use signed URLs:

```kotlin
uploader.uploadImage(
    imageData = imageData,
    requireSignedURLs = true  // Images require signed URLs
)
```

### Backend Proxy Pattern (Recommended)

Instead of exposing Cloudflare credentials in mobile apps:

1. Create an endpoint in your backend
2. Backend validates user authentication
3. Backend uploads to Cloudflare
4. Returns image URL to client

```kotlin
// Mobile app - upload to your backend
suspend fun uploadThroughBackend(imageData: ImageData) {
    val yourApi = NetworkClient.create(baseUrl = "https://yourapi.com")
    
    // Your backend handles Cloudflare upload
    val result = yourApi.post<ImageUploadRequest, ImageUploadResponse>(
        path = "/api/images/upload",
        body = ImageUploadRequest(imageBytes = imageData.toByteArray())
    )
}
```

## Troubleshooting

### Upload Fails with 401 Unauthorized
- Verify API token has "Images:Write" permission
- Check token hasn't expired

### Upload Times Out
- Image too large - compress before upload
- Increase timeout: `timeoutMillis = 120000`

### Out of Memory (Large Images)
- Compress images before upload
- Use streaming if available

### iOS Build Issues
- Ensure `@OptIn(ExperimentalForeignApi::class)` is present
- Update Kotlin version if needed

## API Reference

See the full API documentation in the source files:
- `CloudflareImageUploader.kt` - Main uploader class
- `ImageData.kt` - Platform-agnostic image interface
- `CloudflareModels.kt` - Response models

## Rate Limits

Cloudflare Images has rate limits. Handle them gracefully:

```kotlin
result.onError { message, exception ->
    if (exception is CloudflareException) {
        val rateLimitError = exception.errorCodes.contains(429)
        if (rateLimitError) {
            // Implement retry with backoff
        }
    }
}
```

## More Information

- [Cloudflare Images Documentation](https://developers.cloudflare.com/images/)
- [Cloudflare Images API](https://developers.cloudflare.com/api/operations/cloudflare-images-upload-an-image-via-url)

