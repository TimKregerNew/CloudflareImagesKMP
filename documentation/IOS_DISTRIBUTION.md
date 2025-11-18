# iOS Distribution Guide

This guide explains how to distribute the CloudflareImagesKMP library for iOS via Swift Package Manager.

## Overview

The library is distributed as an XCFramework that supports:
- iOS Device (arm64)
- iOS Simulator (arm64) - Apple Silicon Macs
- iOS Simulator (x64) - Intel Macs

## Prerequisites

- macOS with Xcode installed
- Swift Package Manager (included with Xcode)
- GitHub repository with Releases enabled

## Building the XCFramework

### Step 1: Build the XCFramework

Run the build script:

```bash
./scripts/build-xcframework.sh 1.0.0
```

This will:
1. Build frameworks for all iOS architectures
2. Create the XCFramework
3. Zip it for distribution
4. Calculate the checksum

**Output:**
- XCFramework zip: `build/spm/cloudflareImagesKMP.xcframework.zip`
- Checksum: (displayed in terminal)

### Step 2: Upload to GitHub Releases

1. Go to your GitHub repository: https://github.com/TimKregerNew/CloudflareImagesKMP/releases/new
2. Create a new release:
   - **Tag**: `v1.0.0` (must start with `v`)
   - **Title**: `Version 1.0.0`
   - **Description**: Release notes
   - **Attach**: Upload `build/spm/cloudflareImagesKMP.xcframework.zip`
3. Click "Publish release"

### Step 3: Update Package.swift

The repository includes a `Package.swift` file that needs to be updated with:
- The version number in the URL
- The checksum from the build

**Option 1: Use the helper script**

```bash
./scripts/update-package-swift.sh 1.0.0 <checksum>
```

**Option 2: Manual update**

Edit `Package.swift`:

```swift
.binaryTarget(
    name: "cloudflareImagesKMP",
    url: "https://github.com/TimKregerNew/CloudflareImagesKMP/releases/download/1.0.0/cloudflareImagesKMP.xcframework.zip",
    checksum: "abc123def456..." // Replace with actual checksum
)
```

### Step 4: Commit and Tag

```bash
# Commit the Package.swift update
git add Package.swift
git commit -m "Update Package.swift for version 1.0.0"
git push origin main

# Create and push the release tag (if not done via GitHub UI)
git tag v1.0.0
git push origin v1.0.0
```

## Using the Package

### In Xcode

1. Open your Xcode project
2. File → Add Package Dependencies
3. Enter the repository URL: `https://github.com/TimKregerNew/CloudflareImagesKMP.git`
4. Select the version (e.g., `1.0.0`)
5. Click "Add Package"
6. Select the `CloudflareImagesKMP` product
7. Click "Add Package"

### In Package.swift (for Swift Package projects)

Add to your `Package.swift`:

```swift
dependencies: [
    .package(url: "https://github.com/TimKregerNew/CloudflareImagesKMP.git", from: "1.0.0")
],
targets: [
    .target(
        name: "YourTarget",
        dependencies: [
            .product(name: "CloudflareImagesKMP", package: "CloudflareImagesKMP")
        ]
    )
]
```

### Using in Swift Code

```swift
import CloudflareImagesKMP

// Create the uploader
let uploader = CloudflareImageUploader.Companion().create(
    accountId: "your-account-id",
    apiToken: "your-api-token",
    enableLogging: true,
    timeoutMillis: 60000
)

// Use the uploader
Task {
    let result = try await uploader.uploadImage(
        imageData: imageData,
        id: nil,
        requireSignedURLs: false,
        metadata: ["source": "ios_app"]
    )
    
    if result.isSuccess {
        let response = result.getOrNull()
        print("Uploaded: \(response?.publicUrl ?? "")")
    }
}
```

## Versioning

Follow [Semantic Versioning](https://semver.org/):
- **MAJOR**: Breaking API changes
- **MINOR**: New features (backwards compatible)
- **PATCH**: Bug fixes (backwards compatible)

When releasing a new version:
1. Update version in `gradle.properties` (if using)
2. Build new XCFramework
3. Upload to GitHub Releases with new tag
4. Update `Package.swift` with new version and checksum
5. Commit and push

## Troubleshooting

### "No such module 'CloudflareImagesKMP'"

- Ensure the package is added to your target's dependencies
- Clean build folder: Product → Clean Build Folder (⇧⌘K)
- Restart Xcode

### "Checksum mismatch"

- Verify the checksum in `Package.swift` matches the uploaded zip file
- Recalculate: `swift package compute-checksum <path-to-zip>`

### "Framework not found"

- Ensure you're using the correct version tag
- Check that the GitHub Release exists and the zip file is attached
- Verify the URL in `Package.swift` is correct

## CI/CD Integration

You can automate the release process with GitHub Actions. See `.github/workflows/publish.yml` for an example workflow.

