#!/bin/bash

# This script will ACTUALLY run the integration tests with your credentials
# and upload a real image to Cloudflare (then clean it up)
# Requires CLOUDFLARE_ACCOUNT_ID and CLOUDFLARE_API_TOKEN environment variables

echo "🔐 Cloudflare Integration Test"
echo "==============================="
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
echo "🚀 Running integration test..."
echo ""

# Pass environment variables to Gradle and run test
./gradlew :cloudflareImagesKMP:cleanTest :cloudflareImagesKMP:testDebugUnitTest \
  --tests "*CloudflareImageIntegrationTest.testUploadImage" \
  -Dcloudflare.account.id="$CLOUDFLARE_ACCOUNT_ID" \
  -Dcloudflare.api.token="$CLOUDFLARE_API_TOKEN" \
  --info \
  2>&1 | grep -E "(Testing|Upload|successful|Image ID|Filename|Public URL|Cleaned up|Skipping|Error|failed)" || echo "Check logs above"

echo ""
echo "📊 View detailed results:"
echo "   open cloudflareImagesKMP/build/reports/tests/testDebugUnitTest/index.html"
echo ""
echo "👀 Check Cloudflare Dashboard:"
echo "   https://dash.cloudflare.com/$CLOUDFLARE_ACCOUNT_ID/images"
echo ""

