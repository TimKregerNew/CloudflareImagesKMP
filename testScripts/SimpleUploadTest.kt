// Simple standalone test to verify Cloudflare upload works
// Run with: kotlinc -script SimpleUploadTest.kt
// Requires CLOUDFLARE_ACCOUNT_ID and CLOUDFLARE_API_TOKEN environment variables

import java.net.HttpURLConnection
import java.net.URL
import java.io.DataOutputStream

val ACCOUNT_ID = System.getenv("CLOUDFLARE_ACCOUNT_ID") ?: run {
    System.err.println("❌ ERROR: CLOUDFLARE_ACCOUNT_ID environment variable not set")
    System.err.println("   Set it with: export CLOUDFLARE_ACCOUNT_ID=\"your-account-id\"")
    System.exit(1)
    ""
}

val API_TOKEN = System.getenv("CLOUDFLARE_API_TOKEN") ?: run {
    System.err.println("❌ ERROR: CLOUDFLARE_API_TOKEN environment variable not set")
    System.err.println("   Set it with: export CLOUDFLARE_API_TOKEN=\"your-api-token\"")
    System.exit(1)
    ""
}

println("🧪 Testing Cloudflare Images Upload")
println("====================================")
println()

// Create a minimal 1x1 PNG image
val testImage = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
    0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
    0x08, 0x02, 0x00, 0x00, 0x00,
    0x90.toByte(), 0x77.toByte(), 0x53.toByte(), 0xDE.toByte(),
    0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54,
    0x08, 0x99.toByte(), 0x63, 0xF8.toByte(), 0xCF.toByte(), 0xC0.toByte(),
    0x00, 0x00, 0x00, 0x03, 0x00, 0x01,
    0x00.toByte(), 0x18.toByte(), 0xDD.toByte(), 0x8D.toByte(),
    0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
    0xAE.toByte(), 0x42, 0x60, 0x82.toByte()
)

val boundary = "----WebKitFormBoundary" + System.currentTimeMillis()
val url = URL("https://api.cloudflare.com/client/v4/accounts/$ACCOUNT_ID/images/v1")

println("📤 Uploading test image...")

val connection = url.openConnection() as HttpURLConnection
connection.requestMethod = "POST"
connection.doOutput = true
connection.setRequestProperty("Authorization", "Bearer $API_TOKEN")
connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

val output = DataOutputStream(connection.outputStream)

// Write multipart form data
output.writeBytes("--$boundary\r\n")
output.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"test.png\"\r\n")
output.writeBytes("Content-Type: image/png\r\n\r\n")
output.write(testImage)
output.writeBytes("\r\n")

// Add metadata
output.writeBytes("--$boundary\r\n")
output.writeBytes("Content-Disposition: form-data; name=\"metadata\"\r\n\r\n")
output.writeBytes("test:true\r\n")

output.writeBytes("--$boundary--\r\n")
output.flush()
output.close()

val responseCode = connection.responseCode
println("📡 Response Code: $responseCode")
println()

if (responseCode == 200) {
    val response = connection.inputStream.bufferedReader().readText()
    println("✅ SUCCESS! Image uploaded!")
    println()
    println("Response:")
    println(response)
    println()
    println("👀 Check your Cloudflare dashboard:")
    println("   https://dash.cloudflare.com/$ACCOUNT_ID/images")
} else {
    val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
    println("❌ FAILED!")
    println()
    println("Error:")
    println(error)
}

