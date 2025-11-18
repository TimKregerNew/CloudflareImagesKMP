# Cloudflare Images Setup Guide

Quick guide to get started with Cloudflare Images integration in your KMP app.

## Prerequisites

1. Cloudflare account
2. Cloudflare Images enabled (may require payment setup)
3. This KMP networking library integrated in your project

## Step 1: Get Cloudflare Credentials

### 1.1 Get your Account ID

1. Log in to [Cloudflare Dashboard](https://dash.cloudflare.com/)
2. Click on your profile (top right)
3. Or go to any domain → Look at the URL or right sidebar
4. Copy your **Account ID** (format: `abc123def456...`)

### 1.2 Create an API Token

1. Go to [API Tokens](https://dash.cloudflare.com/profile/api-tokens)
2. Click "Create Token"
3. Use "Create Custom Token"
4. Configure:
   - **Token name**: "Mobile App Image Upload"
   - **Permissions**: 
     - Account → Cloudflare Images → Edit
   - **Account Resources**: 
     - Include → Your account
   - **TTL**: Optional (set expiration if desired)
5. Click "Continue to summary"
6. Click "Create Token"
7. **Copy the token immediately** (shown only once!)

## Step 2: Configure Your App

### Android

#### Option 1: Environment Variables (Recommended for Development)

```bash
export CLOUDFLARE_ACCOUNT_ID="your-account-id"
export CLOUDFLARE_API_TOKEN="your-api-token"
```

In your code:

```kotlin
val uploader = CloudflareImageUploader.create(
    accountId = System.getenv("CLOUDFLARE_ACCOUNT_ID"),
    apiToken = System.getenv("CLOUDFLARE_API_TOKEN")
)
```

#### Option 2: local.properties (Not committed to Git)

Add to `local.properties`:

```properties
cloudflare.account.id=your-account-id
cloudflare.api.token=your-api-token
```

Access in code:

```kotlin
val properties = java.util.Properties()
val localPropertiesFile = File(rootDir, "local.properties")
if (localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
}

val accountId = properties.getProperty("cloudflare.account.id")
val apiToken = properties.getProperty("cloudflare.api.token")
```

#### Option 3: Build Config (Use with caution)

In `build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        buildConfigField("String", "CLOUDFLARE_ACCOUNT_ID", "\"${System.getenv("CLOUDFLARE_ACCOUNT_ID")}\"")
        buildConfigField("String", "CLOUDFLARE_API_TOKEN", "\"${System.getenv("CLOUDFLARE_API_TOKEN")}\"")
    }
}
```

### iOS

#### Option 1: Environment Variables

In Xcode:
1. Product → Scheme → Edit Scheme
2. Run → Arguments → Environment Variables
3. Add:
   - `CLOUDFLARE_ACCOUNT_ID`: your-account-id
   - `CLOUDFLARE_API_TOKEN`: your-api-token

In code:

```swift
let accountId = ProcessInfo.processInfo.environment["CLOUDFLARE_ACCOUNT_ID"] ?? ""
let apiToken = ProcessInfo.processInfo.environment["CLOUDFLARE_API_TOKEN"] ?? ""
```

#### Option 2: Config.plist (Not committed)

Create `Config.plist`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CloudflareAccountID</key>
    <string>your-account-id</string>
    <key>CloudflareAPIToken</key>
    <string>your-api-token</string>
</dict>
</plist>
```

Add to `.gitignore`:

```
Config.plist
```

Read in code:

```swift
if let path = Bundle.main.path(forResource: "Config", ofType: "plist"),
   let config = NSDictionary(contentsOfFile: path) {
    let accountId = config["CloudflareAccountID"] as? String ?? ""
    let apiToken = config["CloudflareAPIToken"] as? String ?? ""
}
```

## Step 3: Security Best Practices

### ⚠️ DO NOT hardcode credentials in your app!

**Bad:**
```kotlin
val accountId = "abc123..."  // ❌ Don't do this!
val apiToken = "your-token"  // ❌ Never commit tokens!
```

### ✅ Recommended: Backend Proxy Pattern

**For production apps, use a backend proxy:**

1. Mobile app authenticates with your backend
2. Backend validates user
3. Backend uploads to Cloudflare using server-side token
4. Returns image URL to mobile app

Benefits:
- API tokens never exposed to clients
- Better access control
- Can add custom validation
- Protect against abuse

Example:

```kotlin
// Mobile app calls your backend
val yourApi = NetworkClient.create(baseUrl = "https://yourapi.com")

suspend fun uploadImage(imageData: ImageData) {
    val result = yourApi.post<UploadRequest, UploadResponse>(
        path = "/api/images/upload",
        body = UploadRequest(
            imageBytes = imageData.toByteArray(),
            fileName = imageData.fileName
        ),
        headers = mapOf("Authorization" to "Bearer $userToken")
    )
}
```

Backend (Node.js example):

```javascript
app.post('/api/images/upload', authenticateUser, async (req, res) => {
  // Validate user can upload
  if (!canUserUpload(req.user)) {
    return res.status(403).json({ error: 'Upload not allowed' });
  }
  
  // Upload to Cloudflare using server-side credentials
  const formData = new FormData();
  formData.append('file', req.body.imageBytes);
  
  const response = await fetch(
    `https://api.cloudflare.com/client/v4/accounts/${ACCOUNT_ID}/images/v1`,
    {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${API_TOKEN}` },
      body: formData
    }
  );
  
  const data = await response.json();
  res.json({ imageUrl: data.result.variants[0] });
});
```

## Step 4: Test Your Setup

### Quick Test

```kotlin
suspend fun testCloudflareSetup() {
    val uploader = CloudflareImageUploader.create(
        accountId = "your-account-id",
        apiToken = "your-api-token"
    )
    
    try {
        // Test by getting usage stats
        val result = uploader.getUsageStats()
        
        result.onSuccess { stats ->
            println("✅ Cloudflare connection successful!")
            println("Used: ${stats.count.current} / ${stats.count.allowed}")
        }.onError { message, _ ->
            println("❌ Connection failed: $message")
        }
    } finally {
        uploader.close()
    }
}
```

### Test Image Upload

```kotlin
// Create a small test image
val testBytes = ByteArray(100)
val imageData = ImageDataFactory.fromBytes(testBytes)

val result = uploader.uploadImage(imageData)

result.onSuccess { response ->
    println("✅ Upload successful!")
    println("Image ID: ${response.id}")
    println("URL: ${response.publicUrl}")
}.onError { message, _ ->
    println("❌ Upload failed: $message")
}
```

## Step 5: Configure Variants (Optional)

Variants are different versions of your images (thumbnails, etc.)

1. Go to [Cloudflare Images Dashboard](https://dash.cloudflare.com/?to=/:account/images)
2. Click "Variants"
3. Create variants (e.g., "thumbnail", "large", "avatar")
4. Configure dimensions and fit mode

Access variants in your app:

```kotlin
result.onSuccess { response ->
    val publicUrl = response.publicUrl
    val thumbnailUrl = response.getVariantUrl("thumbnail")
    val avatarUrl = response.getVariantUrl("avatar")
}
```

## Step 6: Set Up Delivery Domains (Optional)

Use custom domains for image delivery:

1. Cloudflare Dashboard → Images → Delivery
2. Add your custom domain
3. Follow DNS configuration steps
4. Images will be served from your domain

## Troubleshooting

### Error: "Authentication error" / 401

- Token is incorrect or expired
- Token doesn't have Images:Write permission
- Check account ID is correct

**Fix**: Regenerate API token with correct permissions

### Error: "Account not found" / 404

- Account ID is incorrect
- Images not enabled for this account

**Fix**: Verify account ID and ensure Images is enabled

### Error: "Rate limit exceeded" / 429

- Too many requests in short time
- Cloudflare rate limiting active

**Fix**: Implement exponential backoff and retry logic

### Error: "Quota exceeded"

- Hit your plan's image limit
- Need to upgrade or delete old images

**Fix**: Check usage stats, upgrade plan, or clean up

### Upload times out

- Image too large
- Network issues
- Increase timeout

**Fix**:
```kotlin
val uploader = CloudflareImageUploader.create(
    accountId = accountId,
    apiToken = apiToken,
    timeoutMillis = 120000  // 2 minutes
)
```

## Next Steps

1. Read [Cloudflare README](shared/src/commonMain/kotlin/com/kmpnetworking/cloudflare/README.md) for full API documentation
2. Check example implementations:
   - Android: `androidApp/src/main/kotlin/com/kmpnetworking/android/CloudflareExampleActivity.kt`
   - iOS: `iosApp/iosApp/CloudflareExampleView.swift`
3. Explore [Cloudflare Images Documentation](https://developers.cloudflare.com/images/)

## Common Use Cases

- **User avatars**: Upload profile pictures
- **Photo galleries**: Store and serve user photos
- **Product images**: E-commerce product photos
- **Content moderation**: Upload for review before display
- **Thumbnail generation**: Automatic variants

## Cost Considerations

Cloudflare Images pricing (as of 2025):
- Storage: $5/month for up to 100,000 images
- Delivery: $1 per 100,000 images served

Check [current pricing](https://www.cloudflare.com/products/cloudflare-images/) for details.

## Support

- Cloudflare Documentation: https://developers.cloudflare.com/images/
- Cloudflare Community: https://community.cloudflare.com/
- Library Issues: Open an issue on GitHub

