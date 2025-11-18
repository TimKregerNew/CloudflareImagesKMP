# Distribution Guide

This guide explains how to distribute the KMP Networking library for Android (via JFrog Artifactory) and iOS (via Swift Package Manager).

## Table of Contents

- [Android Distribution (JFrog Artifactory)](#android-distribution-jfrog-artifactory)
- [iOS Distribution (Swift Package Manager)](#ios-distribution-swift-package-manager)
- [Versioning](#versioning)
- [CI/CD Integration](#cicd-integration)

---

## Android Distribution (JFrog Artifactory)

### Prerequisites

1. JFrog Artifactory account and repository
2. Artifactory credentials (username and password/API key)

### Configuration

#### Option 1: Environment Variables (Recommended for CI/CD)

```bash
export ARTIFACTORY_URL="https://your-company.jfrog.io/artifactory/libs-release-local"
export ARTIFACTORY_USERNAME="your-username"
export ARTIFACTORY_PASSWORD="your-api-key"
```

#### Option 2: gradle.properties

Edit `gradle.properties`:

```properties
artifactory.url=https://your-company.jfrog.io/artifactory/libs-release-local
artifactory.username=your-username
artifactory.password=your-api-key
```

#### Option 3: local.properties (Not committed to Git)

Create `local.properties`:

```properties
artifactory.url=https://your-company.jfrog.io/artifactory/libs-release-local
artifactory.username=your-username
artifactory.password=your-api-key
```

### Publishing to JFrog Artifactory

```bash
# Set the version
export VERSION=1.0.0

# Build and publish
./gradlew :shared:assembleRelease :shared:publishReleasePublicationToArtifactoryRepository -Plibrary.version=$VERSION
```

Or with a single command:

```bash
./gradlew :shared:publish -Plibrary.version=1.0.0
```

### Verify Upload

1. Log in to your JFrog Artifactory dashboard
2. Navigate to your repository
3. Look for: `com/kmpnetworking/shared/1.0.0/`

You should see:
- `shared-1.0.0.aar` - The Android library
- `shared-1.0.0-sources.jar` - Source code
- `shared-1.0.0-javadoc.jar` - Documentation
- `shared-1.0.0.pom` - Maven metadata

### Using the Published Library

In your Android app's `build.gradle.kts`:

```kotlin
repositories {
    maven {
        url = uri("https://your-company.jfrog.io/artifactory/libs-release-local")
        credentials {
            username = project.findProperty("artifactory.username")?.toString()
            password = project.findProperty("artifactory.password")?.toString()
        }
    }
}

dependencies {
    implementation("com.kmpnetworking:shared:1.0.0")
}
```

---

## iOS Distribution (Swift Package Manager)

### Prerequisites

1. GitHub repository (or other Git hosting)
2. Xcode 12.0 or later
3. macOS for building

### Step-by-Step Process

#### 1. Build the XCFramework

Use the provided script:

```bash
./scripts/build-xcframework.sh 1.0.0
```

This will:
- Build frameworks for all iOS architectures (arm64, x86_64, simulator arm64)
- Create an XCFramework
- Zip it for distribution
- Calculate the checksum

**Output:**
```
✅ XCFramework built successfully!

📍 Location: build/spm/shared.xcframework.zip
📏 Size: 2.5M
🔐 Checksum: abc123def456...
```

#### 2. Create a GitHub Release

```bash
# Tag the release
git tag 1.0.0
git push origin 1.0.0

# Create a GitHub release and upload the zip file
# You can do this via GitHub UI or using gh CLI:
gh release create 1.0.0 \
  build/spm/shared.xcframework.zip \
  --title "Release 1.0.0" \
  --notes "Release notes here"
```

#### 3. Update Package.swift

Update `Package.swift` with the URL and checksum:

```swift
// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "KMPNetworking",
    platforms: [
        .iOS(.v14)
    ],
    products: [
        .library(
            name: "KMPNetworking",
            targets: ["shared"]
        ),
    ],
    targets: [
        .binaryTarget(
            name: "shared",
            url: "https://github.com/yourusername/kmp-networking/releases/download/1.0.0/shared.xcframework.zip",
            checksum: "abc123def456..."  // Use the checksum from step 1
        )
    ]
)
```

#### 4. Commit and Push

```bash
git add Package.swift
git commit -m "Update Package.swift for version 1.0.0"
git push origin main
```

### Using the Swift Package

#### In Xcode

1. File → Add Package Dependencies
2. Enter your repository URL: `https://github.com/yourusername/kmp-networking`
3. Select version `1.0.0`
4. Click "Add Package"

#### In Package.swift

```swift
dependencies: [
    .package(url: "https://github.com/yourusername/kmp-networking", from: "1.0.0")
],
targets: [
    .target(
        name: "YourTarget",
        dependencies: [
            .product(name: "KMPNetworking", package: "kmp-networking")
        ]
    )
]
```

### Using in Your iOS App

```swift
import shared

let client = NetworkClient.Companion().create(
    baseUrl: "https://api.example.com",
    enableLogging: true,
    timeoutMillis: 30000
)
```

---

## Versioning

### Semantic Versioning

Follow [Semantic Versioning](https://semver.org/):

- **MAJOR** version: Incompatible API changes
- **MINOR** version: Add functionality (backwards-compatible)
- **PATCH** version: Bug fixes (backwards-compatible)

### Updating Version

Update the version in `gradle.properties`:

```properties
library.version=1.1.0
```

---

## CI/CD Integration

### GitHub Actions Example

Create `.github/workflows/publish.yml`:

```yaml
name: Publish Library

on:
  push:
    tags:
      - 'v*'

jobs:
  publish-android:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Publish to Artifactory
        env:
          ARTIFACTORY_URL: ${{ secrets.ARTIFACTORY_URL }}
          ARTIFACTORY_USERNAME: ${{ secrets.ARTIFACTORY_USERNAME }}
          ARTIFACTORY_PASSWORD: ${{ secrets.ARTIFACTORY_PASSWORD }}
        run: |
          VERSION=${GITHUB_REF#refs/tags/v}
          ./gradlew :shared:publish -Plibrary.version=$VERSION
  
  publish-ios:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build XCFramework
        run: |
          VERSION=${GITHUB_REF#refs/tags/v}
          ./scripts/build-xcframework.sh $VERSION
      
      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          files: build/spm/shared.xcframework.zip
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      
      - name: Update Package.swift
        run: |
          VERSION=${GITHUB_REF#refs/tags/v}
          CHECKSUM=$(swift package compute-checksum build/spm/shared.xcframework.zip)
          # Update Package.swift with new version and checksum
          # This step would need a script to update the file
```

### GitLab CI Example

Create `.gitlab-ci.yml`:

```yaml
stages:
  - build
  - publish

variables:
  VERSION: $CI_COMMIT_TAG

publish-android:
  stage: publish
  image: openjdk:17-jdk
  only:
    - tags
  script:
    - ./gradlew :shared:publish -Plibrary.version=$VERSION
  variables:
    ARTIFACTORY_URL: $ARTIFACTORY_URL
    ARTIFACTORY_USERNAME: $ARTIFACTORY_USERNAME
    ARTIFACTORY_PASSWORD: $ARTIFACTORY_PASSWORD

publish-ios:
  stage: publish
  tags:
    - macos
  only:
    - tags
  script:
    - ./scripts/build-xcframework.sh $VERSION
    # Upload to releases or artifact storage
```

---

## Quick Reference

### Android (JFrog)

```bash
# Publish
./gradlew :shared:publish -Plibrary.version=1.0.0

# Consume
implementation("com.kmpnetworking:shared:1.0.0")
```

### iOS (SPM)

```bash
# Build
./scripts/build-xcframework.sh 1.0.0

# Create release
gh release create 1.0.0 build/spm/shared.xcframework.zip

# Update Package.swift with checksum
```

---

## Troubleshooting

### Android Issues

**Authentication Failed**
- Verify your Artifactory credentials
- Check if API key has write permissions
- Ensure repository exists

**Build Failed**
- Run `./gradlew clean`
- Check Java version (requires JDK 11+)
- Verify `shared/publish.gradle.kts` is present

### iOS Issues

**XCFramework Creation Failed**
- Ensure you're on macOS
- Install Xcode Command Line Tools: `xcode-select --install`
- Check if all architectures built successfully

**Checksum Mismatch**
- Re-download the zip file
- Recalculate: `swift package compute-checksum shared.xcframework.zip`
- Ensure the correct file is uploaded to GitHub

**Swift Package Not Found**
- Verify Package.swift is in repository root
- Check URL in Package.swift matches release URL
- Ensure tag exists: `git tag -l`

---

## Support

For issues and questions about distribution:
- Check existing GitHub issues
- Create a new issue with distribution logs
- Include platform (Android/iOS) and version

For JFrog Artifactory issues:
- Contact your Artifactory administrator
- Check [JFrog documentation](https://www.jfrog.com/confluence/display/JFROG/JFrog+Artifactory)

For Swift Package Manager issues:
- Check [Apple's SPM documentation](https://developer.apple.com/documentation/swift_packages)
- Verify Xcode version compatibility


