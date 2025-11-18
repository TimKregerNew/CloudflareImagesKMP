#!/bin/bash

# Script to update Package.swift with the checksum from a built XCFramework
# Usage: ./scripts/update-package-swift.sh [version] [checksum]

set -e

VERSION=${1:-"1.0.0"}
CHECKSUM=${2:-""}

if [ -z "$CHECKSUM" ]; then
    echo "❌ Error: Checksum not provided"
    echo ""
    echo "Usage: ./scripts/update-package-swift.sh [version] [checksum]"
    echo ""
    echo "Example:"
    echo "  ./scripts/update-package-swift.sh 1.0.0 abc123def456..."
    echo ""
    exit 1
fi

PACKAGE_FILE="Package.swift"

if [ ! -f "$PACKAGE_FILE" ]; then
    echo "❌ Error: Package.swift not found"
    exit 1
fi

echo "📝 Updating Package.swift..."
echo "   Version: ${VERSION}"
echo "   Checksum: ${CHECKSUM:0:20}..."

# Update the URL with version
sed -i '' "s|releases/download/[0-9.]*/cloudflareImagesKMP|releases/download/${VERSION}/cloudflareImagesKMP|g" "$PACKAGE_FILE"

# Update the checksum
sed -i '' "s|checksum: \"REPLACE_WITH_ACTUAL_CHECKSUM\"|checksum: \"${CHECKSUM}\"|g" "$PACKAGE_FILE"

echo "✅ Package.swift updated successfully!"
echo ""
echo "📋 Changes:"
echo "   - URL version: ${VERSION}"
echo "   - Checksum: ${CHECKSUM}"
echo ""
echo "💡 Next: Review and commit the changes"
echo "   git diff Package.swift"
echo "   git add Package.swift"
echo "   git commit -m \"Update Package.swift for version ${VERSION}\""

