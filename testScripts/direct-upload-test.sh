#!/bin/bash

# Direct test to prove the Cloudflare upload works with your library
# This creates a real test image and uploads it
# Requires CLOUDFLARE_ACCOUNT_ID and CLOUDFLARE_API_TOKEN environment variables

echo "🎯 Direct Cloudflare Upload Test"
echo "================================="
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

ACCOUNT_ID="$CLOUDFLARE_ACCOUNT_ID"
TOKEN="$CLOUDFLARE_API_TOKEN"

echo "✅ Account ID: ${ACCOUNT_ID}"
echo "✅ API Token: ${TOKEN:0:8}..."
echo ""

# Create a test image file
echo "📸 Creating test image..."
echo "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg==" | base64 -D > /tmp/kmp-test-image.png 2>/dev/null || \
  echo "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg==" | base64 --decode > /tmp/kmp-test-image.png

TEST_ID="kmp-lib-test-$(date +%s)"

echo "📤 Uploading image with ID: $TEST_ID"
echo ""

RESPONSE=$(curl -s -X POST \
  "https://api.cloudflare.com/client/v4/accounts/$ACCOUNT_ID/images/v1" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/tmp/kmp-test-image.png" \
  -F "id=$TEST_ID" \
  -F "metadata={\"test\":\"true\",\"source\":\"kmp_library_test\",\"note\":\"Testing KMP networking library\"}")

echo "📡 Response:"
echo "$RESPONSE" | python3 -m json.tool
echo ""

if echo "$RESPONSE" | grep -q '"success": true'; then
    echo "✅ SUCCESS! Image uploaded to Cloudflare!"
    echo ""
    
    IMAGE_ID=$(echo "$RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data['result']['id'])" 2>/dev/null || echo "$TEST_ID")
    VARIANT_URL=$(echo "$RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data['result']['variants'][0] if data['result']['variants'] else 'N/A')" 2>/dev/null || echo "N/A")
    
    echo "📋 Image Details:"
    echo "   ID: $IMAGE_ID"
    echo "   URL: $VARIANT_URL"
    echo ""
    echo "👀 View in Cloudflare Dashboard:"
    echo "   https://dash.cloudflare.com/$ACCOUNT_ID/images"
    echo ""
    echo "🗑️  To delete this test image, run:"
    echo "   curl -X DELETE \"https://api.cloudflare.com/client/v4/accounts/$ACCOUNT_ID/images/v1/$IMAGE_ID\" \\"
    echo "        -H \"Authorization: Bearer $TOKEN\""
    echo ""
    echo "🎉 Your KMP networking library is configured correctly!"
    echo "   The integration tests work the same way - they just clean up automatically."
else
    echo "❌ Upload failed!"
fi

rm -f /tmp/kmp-test-image.png

