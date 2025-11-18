# Integration Tests Guide

This document explains how to run the integration tests for the Cloudflare Images functionality.

## What Are Integration Tests?

Integration tests verify that the library works correctly with the actual Cloudflare API. Unlike unit tests that test isolated components, integration tests:

- ✅ Make real HTTP requests to Cloudflare
- ✅ Upload actual images
- ✅ Verify API responses
- ✅ Test error handling with real scenarios
- ✅ Clean up after themselves

## Prerequisites

Before running integration tests, you need:

1. **Cloudflare Account** with Images enabled
2. **Account ID** and **API Token** with Images:Write permission
3. **Active internet connection**

⚠️ **Warning**: Integration tests will create and delete actual images in your Cloudflare account. While they clean up after themselves, there's a small chance test images could remain if tests are interrupted.

## Setup Credentials

### Option 1: Environment Variables (Recommended)

```bash
export CLOUDFLARE_ACCOUNT_ID="your-account-id-here"
export CLOUDFLARE_API_TOKEN="your-api-token-here"
```

### Option 2: Shell Configuration

Add to your `~/.zshrc` or `~/.bashrc`:

```bash
# Cloudflare Test Credentials
export CLOUDFLARE_ACCOUNT_ID="your-account-id"
export CLOUDFLARE_API_TOKEN="your-api-token"
```

Then reload:
```bash
source ~/.zshrc  # or ~/.bashrc
```

### Option 3: Gradle Properties (Not Recommended for Security)

Add to `local.properties` (this file is in .gitignore):

```properties
cloudflare.test.account.id=your-account-id
cloudflare.test.api.token=your-api-token
```

## Running Integration Tests

### Run All Tests

```bash
# Run all tests including integration tests (all platforms)
./gradlew :shared:allTests
```

### Run Only Integration Tests

```bash
# Run integration tests (they will auto-skip if credentials not set)
./gradlew :shared:cleanAllTests :shared:allTests

# Or use the convenience script
./scripts/run-integration-tests.sh
```

### Run Platform-Specific Tests

```bash
# Android tests only
./gradlew :shared:testDebugUnitTest

# iOS simulator tests (on macOS only)
./gradlew :shared:iosSimulatorArm64Test

# Common tests
./gradlew :shared:jvmTest
```

### Run with Verbose Output

```bash
# See detailed output
./gradlew :shared:test --tests "*Integration*" --info
```

### Run on Android

```bash
# Run Android-specific tests (uses Robolectric)
./gradlew :shared:testDebugUnitTest
```

## What Gets Tested

The integration tests cover:

### ✅ Upload Operations
- `testUploadImage()` - Basic image upload
- `testUploadWithProgressTracking()` - Upload with progress callbacks
- `testUploadImageFromUrl()` - Upload from external URL
- `testUploadWithCustomId()` - Upload with custom image ID

### ✅ Image Management
- `testListImages()` - List paginated images
- `testGetImageDetails()` - Retrieve image information
- `testUpdateImage()` - Update metadata
- `testDeleteImage()` - Delete images

### ✅ Account Operations
- `testGetUsageStats()` - Check quota usage

### ✅ Error Handling
- `testInvalidCredentials()` - Handle auth errors

## Test Output

### Successful Test Output

```
🧪 Testing image upload to Cloudflare...
✅ Upload successful!
   Image ID: abc123-def456-ghi789
   Filename: integration-test.png
   Variants: 3
   Public URL: https://imagedelivery.net/.../public

Cleaned up test image: abc123-def456-ghi789

BUILD SUCCESSFUL
```

### Skipped Tests (No Credentials)

```
⚠️  Skipping integration test - credentials not provided

BUILD SUCCESSFUL
```

### Failed Test Output

```
❌ Upload failed: Authentication error
```

## Cleanup

Integration tests automatically clean up after themselves:

1. Track all uploaded test images
2. Delete them in `@AfterTest` 
3. Print cleanup status

If tests are interrupted, orphaned images may remain. To manually clean them up:

```kotlin
val uploader = CloudflareImageUploader.create(accountId, apiToken)

// List all images
val result = uploader.listImages()
result.onSuccess { response ->
    response.images
        .filter { it.metadata?.get("test") == "true" }
        .forEach { image ->
            uploader.deleteImage(image.id)
        }
}
```

## Continuous Integration

### GitHub Actions Example

```yaml
name: Integration Tests

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  integration-test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run Integration Tests
        env:
          CLOUDFLARE_ACCOUNT_ID: ${{ secrets.CLOUDFLARE_ACCOUNT_ID }}
          CLOUDFLARE_API_TOKEN: ${{ secrets.CLOUDFLARE_API_TOKEN }}
        run: |
          ./gradlew :shared:test --tests "*Integration*"
```

### GitLab CI Example

```yaml
integration-tests:
  stage: test
  script:
    - ./gradlew :shared:test --tests "*Integration*"
  variables:
    CLOUDFLARE_ACCOUNT_ID: $CLOUDFLARE_ACCOUNT_ID
    CLOUDFLARE_API_TOKEN: $CLOUDFLARE_API_TOKEN
  only:
    - main
    - merge_requests
```

## Best Practices

### 1. Use Separate Test Account

Consider using a separate Cloudflare account for testing to avoid mixing test data with production.

### 2. Monitor Quota Usage

Integration tests count toward your Cloudflare Images quota. Each test run uploads several images.

### 3. Run Periodically

Run integration tests:
- Before major releases
- After API updates
- When adding new features
- In CI/CD for main branch only

### 4. Don't Run on Every Commit

Integration tests are slower and use API quota. Run unit tests on every commit, integration tests less frequently.

### 5. Handle Rate Limits

If you get rate limit errors, add delays between tests:

```kotlin
@Test
fun testUpload() = runTest {
    // ... test code ...
    delay(1000) // Wait 1 second
}
```

## Troubleshooting

### Tests Are Skipped

**Problem**: All integration tests show "Skipping integration test"

**Solution**: 
- Verify environment variables are set: `echo $CLOUDFLARE_ACCOUNT_ID`
- Check credentials are correct
- Ensure you exported variables in current shell

### Authentication Errors

**Problem**: Tests fail with "Authentication error" or 401

**Solution**:
- Verify API token has Images:Write permission
- Check token hasn't expired
- Ensure account ID is correct

### Rate Limit Errors

**Problem**: Tests fail with "Rate limit exceeded" or 429

**Solution**:
- Wait a few minutes and retry
- Add delays between tests
- Run fewer tests at once

### Tests Timeout

**Problem**: Tests hang or timeout

**Solution**:
- Check internet connection
- Increase timeout: `timeoutMillis = 120000`
- Verify Cloudflare service status

### Images Not Cleaned Up

**Problem**: Test images remain in account

**Solution**:
Run this cleanup script:

```bash
./gradlew :shared:test --tests "CloudflareImageIntegrationTest.cleanupTestImages"
```

Or manually in Cloudflare dashboard:
1. Go to Images
2. Filter by metadata: `test=true`
3. Bulk delete

## Writing New Integration Tests

### Template for New Tests

```kotlin
@Test
fun testNewFeature() = runTest {
    if (!hasCredentials) {
        println("⚠️  Skipping integration test - credentials not provided")
        return@runTest
    }
    
    println("🧪 Testing new feature...")
    
    // Test code here
    val result = uploader.someOperation()
    
    assertTrue(result.isSuccess, "Operation should succeed")
    
    result.onSuccess { response ->
        println("✅ Test passed!")
        // Track any created images for cleanup
        testImageIds.add(response.id)
    }.onError { message, _ ->
        fail("Test failed: $message")
    }
}
```

### Guidelines

1. Always check `hasCredentials` first
2. Print status messages for clarity
3. Track created images in `testImageIds`
4. Assert expected outcomes
5. Handle both success and error cases
6. Clean up after yourself

## Cost Considerations

Each integration test run:
- Uploads ~8-10 test images
- Makes ~15-20 API requests
- Uses minimal storage (images are small and deleted)
- Counts toward Cloudflare Images quota

**Estimated cost per run**: Negligible (< $0.01)

**Recommended frequency**: 
- Development: As needed
- CI/CD: Main branch only
- Release: Always before release

## Support

If you encounter issues with integration tests:

1. Check this guide first
2. Verify Cloudflare service status
3. Review test output for specific errors
4. Check [Cloudflare Images Documentation](https://developers.cloudflare.com/images/)
5. Open an issue with:
   - Test output
   - Environment details
   - Steps to reproduce

## Summary

```bash
# Quick start
export CLOUDFLARE_ACCOUNT_ID="your-id"
export CLOUDFLARE_API_TOKEN="your-token"
./gradlew :shared:test --tests "*Integration*"

# Expected output
# ✅ All tests pass
# 🧹 Test images cleaned up
# 📊 Test summary displayed
```

Integration tests ensure your library works correctly with the real Cloudflare API, giving you confidence in production deployments!

