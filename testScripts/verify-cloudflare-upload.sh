#!/bin/bash

# Simple script to verify Cloudflare upload works
# This will upload ONE test image and NOT delete it
# Requires CLOUDFLARE_ACCOUNT_ID and CLOUDFLARE_API_TOKEN environment variables

echo "📤 Cloudflare Upload Verification"
echo "=================================="
echo ""

# Check if environment variables are set
if [ -z "$CLOUDFLARE_ACCOUNT_ID" ] || [ -z "$CLOUDFLARE_API_TOKEN" ]; then
    echo "❌ ERROR: Cloudflare credentials not found!"
    echo ""
    echo "Please set the following environment variables:"
    echo ""
    echo "  export CLOUDFLARE_ACCOUNT_ID=\"your-account-id\""
    echo "  export CLOUDFLARE_API_TOKEN=\"your-api-token\""
    echo ""
    exit 1
fi

echo "✅ Account ID: ${CLOUDFLARE_ACCOUNT_ID}"
echo "✅ API Token: ${CLOUDFLARE_API_TOKEN:0:8}..."
echo ""

echo "Creating test image..."
cat > /tmp/test-upload.kt << 'EOF'
import com.kmpnetworking.cloudflare.*
import com.kmpnetworking.NetworkResult
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val uploader = CloudflareImageUploader.create(
        accountId = System.getenv("CLOUDFLARE_ACCOUNT_ID") ?: "",
        apiToken = System.getenv("CLOUDFLARE_API_TOKEN") ?: "",
        enableLogging = true
    )
    
    println("🧪 Testing upload to Cloudflare...")
    
    // Create a minimal 1x1 PNG
    val testImageBytes = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        0x00, 0x00, 0x00, 0x0D,
        0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x00, 0x01,
        0x00, 0x00, 0x00, 0x01,
        0x08, 0x02, 0x00, 0x00, 0x00,
        0x90.toByte(), 0x77.toByte(), 0x53.toByte(), 0xDE.toByte(),
        0x00, 0x00, 0x00, 0x0C,
        0x49, 0x44, 0x41, 0x54,
        0x08, 0x99.toByte(), 0x63, 0xF8.toByte(), 0xCF.toByte(), 0xC0.toByte(),
        0x00, 0x00, 0x00, 0x03, 0x00, 0x01,
        0x00.toByte(), 0x18.toByte(), 0xDD.toByte(), 0x8D.toByte(),
        0x00, 0x00, 0x00, 0x00,
        0x49, 0x45, 0x4E, 0x44,
        0xAE.toByte(), 0x42, 0x60, 0x82.toByte()
    )
    
    val imageData = ImageDataFactory.fromBytes(
        bytes = testImageBytes,
        mimeType = "image/png",
        fileName = "verification-test.png"
    )
    
    val result = uploader.uploadImage(
        imageData = imageData,
        id = "verification-test-${System.currentTimeMillis()}",
        metadata = mapOf(
            "test" to "true",
            "purpose" to "manual_verification",
            "note" to "You can delete this image from Cloudflare dashboard"
        )
    )
    
    result.onSuccess { response ->
        println("")
        println("✅ SUCCESS! Image uploaded to Cloudflare!")
        println("")
        println("📋 Details:")
        println("   Image ID: ${response.id}")
        println("   Filename: ${response.filename}")
        println("   Uploaded: ${response.uploaded}")
        println("   Variants: ${response.variants.size}")
        println("")
        println("🔗 Public URL:")
        println("   ${response.publicUrl}")
        println("")
        println("👀 Check your Cloudflare dashboard:")
        println("   https://dash.cloudflare.com/${System.getenv("CLOUDFLARE_ACCOUNT_ID")}/images")
        println("")
        println("🗑️  To delete this test image:")
        println("   Delete image ID: ${response.id}")
        println("")
    }.onError { message, exception ->
        println("")
        println("❌ FAILED to upload!")
        println("   Error: $message")
        println("")
        if (exception != null) {
            println("   Details: ${exception.message}")
        }
        println("")
    }
    
    uploader.close()
}
EOF

echo "Running verification test..."
echo ""

# Force clean build and run only the upload test
./gradlew :cloudflareImagesKMP:cleanTest
./gradlew :cloudflareImagesKMP:testDebugUnitTest --tests "*CloudflareImageIntegrationTest.testUploadImage" --rerun-tasks 2>&1 | grep -E "(🧪|✅|❌|Image ID|Public URL|Error)"

echo ""
echo "📊 Full test results:"
echo "   open cloudflareImagesKMP/build/reports/tests/testDebugUnitTest/index.html"
echo ""

