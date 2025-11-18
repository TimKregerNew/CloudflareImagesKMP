# Quick Start - Library Distribution

Fast track guide to distribute your KMP Networking library.

## 🤖 Android (JFrog Artifactory)

### One-Time Setup

1. Get your JFrog credentials
2. Set environment variables:

```bash
export ARTIFACTORY_URL="https://your-company.jfrog.io/artifactory/libs-release-local"
export ARTIFACTORY_USERNAME="your-username"
export ARTIFACTORY_PASSWORD="your-api-key"
```

### Publish

```bash
./scripts/publish-android.sh 1.0.0
```

### Consume

```kotlin
// In your app's build.gradle.kts
repositories {
    maven {
        url = uri("https://your-company.jfrog.io/artifactory/libs-release-local")
        credentials {
            username = "your-username"
            password = "your-api-key"
        }
    }
}

dependencies {
    implementation("com.kmpnetworking:shared:1.0.0")
}
```

---

## 🍎 iOS (Swift Package Manager)

### Build XCFramework

```bash
./scripts/build-xcframework.sh 1.0.0
```

Output shows:
- Location of zip file
- **Checksum** (copy this!)

### Create GitHub Release

```bash
# Tag and push
git tag v1.0.0
git push origin v1.0.0

# Create release with the zip file
gh release create v1.0.0 \
  build/spm/shared.xcframework.zip \
  --title "Release 1.0.0" \
  --notes "Initial release"
```

Or upload via GitHub web UI: Releases → Draft new release

### Update Package.swift

```swift
.binaryTarget(
    name: "shared",
    url: "https://github.com/YOURUSER/YOURREPO/releases/download/v1.0.0/shared.xcframework.zip",
    checksum: "PASTE_CHECKSUM_HERE"
)
```

Commit and push:

```bash
git add Package.swift
git commit -m "Update Package.swift for v1.0.0"
git push
```

### Consume

In Xcode:
1. File → Add Package Dependencies
2. Paste your GitHub URL
3. Select version 1.0.0

---

## 🤝 Both Platforms at Once

### Using GitHub Actions (Automated)

Set up secrets in GitHub Settings → Secrets:
- `ARTIFACTORY_URL`
- `ARTIFACTORY_USERNAME`
- `ARTIFACTORY_PASSWORD`

Then just push a tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The workflow in `.github/workflows/publish.yml` will:
- ✅ Publish Android to JFrog
- ✅ Build iOS XCFramework
- ✅ Create GitHub release with checksum

---

## 📋 Checklist

### Before First Release

- [ ] Update `gradle.properties` with correct version
- [ ] Update `Package.swift` repository URL
- [ ] Configure JFrog credentials
- [ ] Test build locally: `./gradlew :shared:build`
- [ ] Test Android publish (to test repo)
- [ ] Test iOS build: `./scripts/build-xcframework.sh 1.0.0`

### For Each Release

- [ ] Update version number in `gradle.properties`
- [ ] Update CHANGELOG (if you have one)
- [ ] Run tests: `./gradlew :shared:test`
- [ ] Commit version bump
- [ ] Tag release: `git tag v1.0.0`
- [ ] Push: `git push origin v1.0.0`
- [ ] Wait for CI/CD or run manually:
  - [ ] Android: `./scripts/publish-android.sh 1.0.0`
  - [ ] iOS: `./scripts/build-xcframework.sh 1.0.0`
  - [ ] Create GitHub release
  - [ ] Update `Package.swift` with checksum
  - [ ] Push `Package.swift` update

### After Release

- [ ] Test consuming library in a real project
- [ ] Verify on JFrog dashboard
- [ ] Verify GitHub release is visible
- [ ] Update documentation if needed

---

## 🆘 Quick Troubleshooting

### Android publish fails with 401 Unauthorized
→ Check credentials, verify API key has write permissions

### iOS build fails
→ Ensure on macOS with Xcode installed: `xcode-select --install`

### Swift Package checksum mismatch
→ Recalculate: `swift package compute-checksum build/spm/shared.xcframework.zip`

### Version already exists
→ Increment version number, JFrog doesn't allow overwriting

---

## 📚 More Info

- Full details: [DISTRIBUTION.md](DISTRIBUTION.md)
- Usage examples: [USAGE_EXAMPLES.md](USAGE_EXAMPLES.md)
- Main docs: [README.md](README.md)


