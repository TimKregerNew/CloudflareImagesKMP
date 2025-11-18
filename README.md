# Cloudflare Images KMP

A Kotlin Multiplatform (KMP) library for Cloudflare Images API, providing cross-platform image upload and management for Android and iOS.

## Features

- ☁️ Full Cloudflare Images API integration
- 📤 Upload images from files, URIs, Bitmaps (Android) or UIImage (iOS)
- 📋 List, update, and delete images
- 📊 Track usage statistics
- 📱 Sample Android and iOS apps included
- 🔧 All business logic in shared code

## Project Structure

```
kmp-test/
├── cloudflareImagesKMP/             # Shared KMP library
│   └── src/
│       ├── commonMain/              # Common code
│       │   └── kotlin/
│       │       └── com/kmpnetworking/
│       │           └── cloudflare/  # Cloudflare Images API
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

### 2. Build the Library Module

```bash
./gradlew :cloudflareImagesKMP:build
```

### 3. Run Android App

```bash
./gradlew :androidApp:installDebug
```

Or open the project in Android Studio and run the `androidApp` configuration.

### 4. Run iOS App

First, build the framework:

```bash
./gradlew :cloudflareImagesKMP:embedAndSignAppleFrameworkForXcode
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


## Testing

### Unit Tests

Run unit tests for the library:

```bash
# All platforms
./gradlew :cloudflareImagesKMP:allTests

# Android only
./gradlew :cloudflareImagesKMP:testDebugUnitTest
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
./testScripts/run-integration-tests.sh

# Or use Gradle directly
./gradlew :cloudflareImagesKMP:allTests
```

**Note**: Integration tests make real API calls and require valid Cloudflare credentials.

**📖 See [INTEGRATION_TESTS.md](documentation/INTEGRATION_TESTS.md) for complete testing documentation**


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
./gradlew :cloudflareImagesKMP:embedAndSignAppleFrameworkForXcode
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

