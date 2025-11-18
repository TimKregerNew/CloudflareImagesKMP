# KMP Networking Library

A cross-platform networking library for Android and iOS using Kotlin Multiplatform (KMP) and Ktor.

## Features

- 🌐 Cross-platform HTTP client for Android and iOS
- 🔄 Support for GET, POST, PUT, DELETE requests
- 📦 Automatic JSON serialization/deserialization with kotlinx.serialization
- ✅ Type-safe result handling with `NetworkResult` sealed class
- 🔧 Configurable timeouts and logging
- 📱 Sample Android and iOS apps included

## Project Structure

```
kmp-test/
├── shared/                          # Shared KMP library
│   └── src/
│       ├── commonMain/              # Common code
│       │   └── kotlin/
│       │       └── com/kmpnetworking/
│       │           ├── NetworkClient.kt      # Main networking client
│       │           ├── NetworkResult.kt      # Result wrapper
│       │           └── models/               # Data models
│       ├── androidMain/             # Android-specific implementations
│       └── iosMain/                 # iOS-specific implementations
├── androidApp/                      # Android sample app
└── iosApp/                          # iOS sample app (Xcode project)
```

## Requirements

### General
- JDK 17 or later
- Gradle 8.4 or later

### For Android Development
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK with API level 24 (Android 7.0) or higher

### For iOS Development
- macOS with Xcode 14.0 or later
- CocoaPods (optional, for additional dependencies)

## Setup

### 1. Clone the Repository

```bash
cd /path/to/your/workspace
# Project already initialized in current directory
```

### 2. Build the Shared Module

```bash
./gradlew :shared:build
```

### 3. Run Android App

```bash
./gradlew :androidApp:installDebug
```

Or open the project in Android Studio and run the `androidApp` configuration.

### 4. Run iOS App

First, build the shared framework:

```bash
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

Then open the iOS project:

```bash
open iosApp/iosApp.xcodeproj
```

Build and run in Xcode (Cmd+R).

## Cloudflare Images Integration

The library includes full Cloudflare Images API support with all business logic in the shared module:

```kotlin
// Initialize uploader
val uploader = CloudflareImageUploader.create(
    accountId = "your-account-id",
    apiToken = "your-api-token"
)

// Android - Upload from File
val imageData = AndroidImageData.fromFile(file)
val result = uploader.uploadImage(imageData)

// iOS - Upload from UIImage
let imageData = IOSImageData.fromUIImage(image)
uploader.uploadImage(imageData: imageData) { result in }
```

**📖 See [Cloudflare README](documentation/CLOUDFLARE_README.md) for complete documentation**

Features:
- ✅ Upload images from files, URIs, Bitmaps (Android) or UIImage (iOS)
- ✅ Upload from URLs
- ✅ List, update, and delete images
- ✅ Track usage statistics
- ✅ Progress callbacks
- ✅ Platform-agnostic - all business logic shared!

## Using the Network Library

### Basic Usage

#### 1. Create a NetworkClient

```kotlin
val networkClient = NetworkClient.create(
    baseUrl = "https://api.example.com",
    enableLogging = true,
    timeoutMillis = 30000
)
```

#### 2. Make GET Requests

```kotlin
// Simple GET request
val result: NetworkResult<List<Post>> = networkClient.get(
    path = "/posts",
    headers = mapOf("Authorization" to "Bearer token"),
    parameters = mapOf("page" to "1")
)

// Handle the result
result.onSuccess { posts ->
    println("Fetched ${posts.size} posts")
}.onError { message, exception ->
    println("Error: $message")
}
```

#### 3. Make POST Requests

```kotlin
val newPost = CreatePostRequest(
    title = "Hello World",
    body = "This is a test post",
    userId = 1
)

val result: NetworkResult<Post> = networkClient.post(
    path = "/posts",
    body = newPost,
    headers = mapOf("Content-Type" to "application/json")
)
```

#### 4. Make PUT Requests

```kotlin
val updatedPost = Post(
    id = 1,
    title = "Updated Title",
    body = "Updated body",
    userId = 1
)

val result: NetworkResult<Post> = networkClient.put(
    path = "/posts/1",
    body = updatedPost
)
```

#### 5. Make DELETE Requests

```kotlin
val result: NetworkResult<Unit> = networkClient.delete(
    path = "/posts/1"
)
```

#### 6. Don't Forget to Close

```kotlin
// Always close the client when done
networkClient.close()
```

### Handling Results

The `NetworkResult` sealed class provides several utility methods:

```kotlin
// Check result type
if (result.isSuccess) { /* ... */ }
if (result.isError) { /* ... */ }

// Get data or null
val data: T? = result.getOrNull()

// Get data or default
val data: T = result.getOrDefault(defaultValue)

// Transform data
val mapped = result.map { data -> 
    data.copy(title = data.title.uppercase())
}

// Chain actions
result
    .onSuccess { data -> println("Success: $data") }
    .onError { message, exception -> println("Error: $message") }
```

### Creating Custom Data Models

Use `@Serializable` annotation for automatic JSON serialization:

```kotlin
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class User(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("email")
    val email: String
)
```

## Android Integration

In your Android app:

```kotlin
import com.kmpnetworking.NetworkClient
import com.kmpnetworking.NetworkResult
import kotlinx.coroutines.launch

class MyViewModel : ViewModel() {
    private val networkClient = NetworkClient.create(
        baseUrl = "https://api.example.com"
    )
    
    fun fetchData() {
        viewModelScope.launch {
            val result = networkClient.get<List<Data>>("/data")
            result.onSuccess { data ->
                // Update UI
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        networkClient.close()
    }
}
```

## iOS Integration

In your iOS app (Swift):

```swift
import shared

class NetworkViewModel: ObservableObject {
    private let networkClient: NetworkClient
    
    init() {
        networkClient = NetworkClient.Companion().create(
            baseUrl: "https://api.example.com",
            enableLogging: true,
            timeoutMillis: 30000
        )
    }
    
    deinit {
        networkClient.close()
    }
    
    func fetchData() {
        networkClient.get(
            path: "/data",
            headers: [:],
            parameters: [:]
        ) { result in
            if let success = result as? NetworkResult.Success<NSArray> {
                // Handle success
            } else if let error = result as? NetworkResult.Error {
                // Handle error
            }
        }
    }
}
```

## Configuration

### Gradle Versions

Update versions in `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "1.9.20"
ktor = "2.3.6"
agp = "8.2.0"
```

### Timeouts

Configure timeouts when creating the client:

```kotlin
val client = NetworkClient.create(
    baseUrl = "https://api.example.com",
    timeoutMillis = 60000  // 60 seconds
)
```

### Logging

Enable or disable logging:

```kotlin
val client = NetworkClient.create(
    baseUrl = "https://api.example.com",
    enableLogging = true  // Set to false in production
)
```

## Testing

### Unit Tests

Run unit tests for the shared module:

```bash
# All platforms
./gradlew :shared:allTests

# Android only
./gradlew :shared:testDebugUnitTest
```

Run Android app tests:

```bash
./gradlew :androidApp:test
```

### Integration Tests (Cloudflare)

The library includes integration tests that verify actual Cloudflare API functionality:

```bash
# Set credentials
export CLOUDFLARE_ACCOUNT_ID="your-account-id"
export CLOUDFLARE_API_TOKEN="your-api-token"

# Run integration tests
./scripts/run-integration-tests.sh

# Or use Gradle directly
./gradlew :shared:allTests
```

**Note**: Integration tests make real API calls and require valid Cloudflare credentials.

**📖 See [INTEGRATION_TESTS.md](documentation/INTEGRATION_TESTS.md) for complete testing documentation**

## Distribution

This library can be distributed for production use:

### Android - JFrog Artifactory

Publish to your JFrog Artifactory repository:

```bash
# Configure credentials (environment variables recommended)
export ARTIFACTORY_URL="https://your-company.jfrog.io/artifactory/libs-release-local"
export ARTIFACTORY_USERNAME="your-username"
export ARTIFACTORY_PASSWORD="your-api-key"

# Publish
./scripts/publish-android.sh 1.0.0
```

Then consume in your Android projects:

```kotlin
dependencies {
    implementation("com.kmpnetworking:shared:1.0.0")
}
```

### iOS - Swift Package Manager

Build and distribute the XCFramework:

```bash
# Build XCFramework
./scripts/build-xcframework.sh 1.0.0

# Upload to GitHub releases (the script will show the checksum)
gh release create v1.0.0 build/spm/shared.xcframework.zip

# Update Package.swift with the checksum
```

Then add to your iOS project via Xcode or Package.swift.

**📖 See [DISTRIBUTION.md](documentation/DISTRIBUTION.md) for detailed distribution instructions.**

## Building Sample Apps

### Android

```bash
./gradlew :androidApp:assembleRelease
```

The APK will be in `androidApp/build/outputs/apk/release/`

### iOS

1. Open the project in Xcode
2. Select "Any iOS Device" or a connected device
3. Product → Archive
4. Follow the Xcode archiving process

## Troubleshooting

### iOS Framework Not Found

If you get a framework error in iOS, run:

```bash
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

### Gradle Build Issues

Clean and rebuild:

```bash
./gradlew clean
./gradlew build
```

### Android Dependencies

If dependencies fail to resolve, sync Gradle files in Android Studio:
- File → Sync Project with Gradle Files

### iOS Build Issues

Clean the Xcode build:
- Product → Clean Build Folder (Shift+Cmd+K)

## API Reference

### NetworkClient Methods

| Method | Parameters | Returns | Description |
|--------|-----------|---------|-------------|
| `get<T>()` | path, headers, parameters | `NetworkResult<T>` | Perform GET request |
| `post<T, R>()` | path, body, headers | `NetworkResult<R>` | Perform POST request |
| `put<T, R>()` | path, body, headers | `NetworkResult<R>` | Perform PUT request |
| `delete<T>()` | path, headers | `NetworkResult<T>` | Perform DELETE request |
| `getRaw()` | path, headers | `NetworkResult<String>` | Get raw string response |
| `close()` | - | `Unit` | Close client and release resources |

### NetworkResult

Sealed class with two states:

- `Success<T>(data: T)` - Successful result with data
- `Error(message: String, exception: Exception?)` - Error result

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is open source and available under the MIT License.

## Resources

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Ktor Client Documentation](https://ktor.io/docs/client.html)
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)

## Support

For issues and questions, please open an issue on the GitHub repository.

