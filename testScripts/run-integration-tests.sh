#!/bin/bash

# Quick script to run integration tests with credentials
# Requires CLOUDFLARE_ACCOUNT_ID and CLOUDFLARE_API_TOKEN environment variables

echo "🔐 Checking Cloudflare credentials..."

# Check if environment variables are set
if [ -z "$CLOUDFLARE_ACCOUNT_ID" ] || [ -z "$CLOUDFLARE_API_TOKEN" ]; then
    echo "❌ ERROR: Cloudflare credentials not found!"
    echo ""
    echo "Please set the following environment variables:"
    echo ""
    echo "  export CLOUDFLARE_ACCOUNT_ID=\"your-account-id\""
    echo "  export CLOUDFLARE_API_TOKEN=\"your-api-token\""
    echo ""
    echo "Or run manually:"
    echo "  CLOUDFLARE_ACCOUNT_ID=\"your-account-id\" CLOUDFLARE_API_TOKEN=\"your-token\" ./gradlew :cloudflareImagesKMP:allTests"
    echo ""
    exit 1
fi

echo "✅ Account ID: ${CLOUDFLARE_ACCOUNT_ID}"
echo "✅ API Token: ${CLOUDFLARE_API_TOKEN:0:8}..."
echo ""

echo "🚀 Running integration tests..."
./gradlew :cloudflareImagesKMP:allTests --info | grep -E "(🧪|✅|❌|Skipping|Upload|Image)"

echo ""
echo "📊 Check full test results:"
echo "   open cloudflareImagesKMP/build/reports/tests/testDebugUnitTest/index.html"

