#!/bin/bash

# Simple script to test if your Cloudflare API token works

echo "🔐 Cloudflare API Token Tester"
echo "==============================="
echo ""

# Read token from environment variables or ask for it
if [ -n "$CLOUDFLARE_API_TOKEN" ] && [ -n "$CLOUDFLARE_ACCOUNT_ID" ]; then
    TOKEN="$CLOUDFLARE_API_TOKEN"
    ACCOUNT_ID="$CLOUDFLARE_ACCOUNT_ID"
elif [ -f "testScripts/run-integration-tests.sh" ]; then
    TOKEN=$(grep 'export CLOUDFLARE_API_TOKEN=' testScripts/run-integration-tests.sh | head -1 | cut -d'"' -f2)
    ACCOUNT_ID=$(grep 'export CLOUDFLARE_ACCOUNT_ID=' testScripts/run-integration-tests.sh | head -1 | cut -d'"' -f2)
else
    read -p "Enter your API Token: " TOKEN
    read -p "Enter your Account ID: " ACCOUNT_ID
fi

echo "Testing with:"
echo "  Account ID: $ACCOUNT_ID"
echo "  Token: ${TOKEN:0:8}..."
echo ""

echo "📡 Testing API connection..."
echo ""

RESPONSE=$(curl -s -X GET \
  "https://api.cloudflare.com/client/v4/accounts/$ACCOUNT_ID/images/v1/stats" \
  -H "Authorization: Bearer $TOKEN")

echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
echo ""

if echo "$RESPONSE" | grep -q '"success": true'; then
    echo "✅ SUCCESS! Your token works!"
    echo ""
    echo "📊 Your Cloudflare Images stats:"
    echo "$RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(f\"   Used: {data['result']['count']['current']} / {data['result']['count']['allowed']}\")" 2>/dev/null
    echo ""
    echo "🎉 You can now run: ./testScripts/run-integration-tests.sh"
    exit 0
else
    echo "❌ FAILED! Token is not working"
    echo ""
    echo "Common issues:"
    echo "  1. Token doesn't have 'Cloudflare Images → Edit' permission"
    echo "  2. Token is expired or invalid"
    echo "  3. Account ID is wrong"
    echo ""
    echo "📖 Read documentation/CREATE_TOKEN_GUIDE.md for step-by-step instructions"
    echo ""
    exit 1
fi

