#!/bin/bash

# Script to run Cloudflare integration tests
# Usage: ./scripts/run-integration-tests.sh

set -e

echo "🧪 Cloudflare Images Integration Tests"
echo "======================================"
echo ""

# Check if credentials are set
if [ -z "$CLOUDFLARE_ACCOUNT_ID" ] || [ -z "$CLOUDFLARE_API_TOKEN" ]; then
    echo "⚠️  Warning: Cloudflare credentials not found in environment"
    echo ""
    echo "Integration tests will be skipped."
    echo "To run integration tests, set these environment variables:"
    echo ""
    echo "  export CLOUDFLARE_ACCOUNT_ID=\"your-account-id\""
    echo "  export CLOUDFLARE_API_TOKEN=\"your-api-token\""
    echo ""
    echo "See documentation/CLOUDFLARE_SETUP.md for how to get these credentials."
    echo ""
    read -p "Continue with tests anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 0
    fi
else
    echo "✅ Credentials found"
    echo "   Account ID: ${CLOUDFLARE_ACCOUNT_ID:0:8}..."
    echo "   API Token: ${CLOUDFLARE_API_TOKEN:0:8}..."
    echo ""
fi

# Run the tests
echo "🚀 Running integration tests..."
echo ""

./gradlew :cloudflareImagesKMP:cleanAllTests :cloudflareImagesKMP:allTests || {
    echo ""
    echo "❌ Tests failed!"
    echo ""
    echo "Common issues:"
    echo "  - Invalid credentials"
    echo "  - Network connectivity"
    echo "  - Cloudflare service issues"
    echo "  - Rate limiting"
    echo ""
    echo "Check documentation/INTEGRATION_TESTS.md for troubleshooting."
    exit 1
}

echo ""
echo "✅ All integration tests passed!"
echo ""
echo "📊 Test Report:"
echo "   Location: cloudflareImagesKMP/build/reports/tests/test/index.html"
echo ""

