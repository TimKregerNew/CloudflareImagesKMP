#!/bin/bash

# Script to build and package XCFramework for Swift Package Manager distribution
# Usage: ./scripts/build-xcframework.sh [version]

set -e

VERSION=${1:-"1.0.0"}
FRAMEWORK_NAME="cloudflareImagesKMP"
BUILD_DIR="cloudflareImagesKMP/build"
XCFRAMEWORK_DIR="${BUILD_DIR}/XCFrameworks/release"
XCFRAMEWORK_PATH="${XCFRAMEWORK_DIR}/${FRAMEWORK_NAME}.xcframework"
OUTPUT_DIR="build/spm"
ZIP_FILE="${OUTPUT_DIR}/${FRAMEWORK_NAME}.xcframework.zip"

echo "🚀 Building XCFramework for version ${VERSION}"

# Clean previous builds
echo "🧹 Cleaning previous builds..."
rm -rf "${BUILD_DIR}/bin"
rm -rf "${XCFRAMEWORK_DIR}"
rm -rf "${OUTPUT_DIR}"
mkdir -p "${OUTPUT_DIR}"

# Build for all iOS architectures
echo "🔨 Building for iOS Device (arm64)..."
./gradlew :cloudflareImagesKMP:linkReleaseFrameworkIosArm64

echo "🔨 Building for iOS Simulator (arm64)..."
./gradlew :cloudflareImagesKMP:linkReleaseFrameworkIosSimulatorArm64

echo "🔨 Building for iOS Simulator (x64)..."
./gradlew :cloudflareImagesKMP:linkReleaseFrameworkIosX64

# Create XCFramework
echo "📦 Creating XCFramework..."
./gradlew :cloudflareImagesKMP:createXCFramework

# Verify XCFramework was created
if [ ! -d "${XCFRAMEWORK_PATH}" ]; then
    echo "❌ Error: XCFramework not found at ${XCFRAMEWORK_PATH}"
    exit 1
fi

# Zip the XCFramework
echo "🗜️  Zipping XCFramework..."
mkdir -p "${OUTPUT_DIR}"
ABS_ZIP_FILE="$(cd "$(dirname "${ZIP_FILE}")" && pwd)/$(basename "${ZIP_FILE}")"
cd "${XCFRAMEWORK_DIR}"
zip -r -q "${ABS_ZIP_FILE}" "${FRAMEWORK_NAME}.xcframework"
cd - > /dev/null

# Calculate checksum
echo "🔐 Calculating checksum..."
CHECKSUM=$(swift package compute-checksum "${ZIP_FILE}")

echo ""
echo "✅ XCFramework built successfully!"
echo ""
echo "📍 Location: ${ZIP_FILE}"
echo "📏 Size: $(du -h "${ZIP_FILE}" | cut -f1)"
echo "🔐 Checksum: ${CHECKSUM}"
echo ""
echo "📝 Next steps:"
echo "1. Upload ${ZIP_FILE} to GitHub Releases with tag v${VERSION}"
echo "   - Go to: https://github.com/TimKregerNew/CloudflareImagesKMP/releases/new"
echo "   - Tag: v${VERSION}"
echo "   - Title: Version ${VERSION}"
echo "   - Upload: ${ZIP_FILE}"
echo ""
echo "2. Update Package.swift with the checksum:"
echo "   - Replace REPLACE_WITH_ACTUAL_CHECKSUM with: ${CHECKSUM}"
echo "   - Update version in URL if needed: ${VERSION}"
echo ""
echo "3. Commit and push Package.swift update:"
echo "   git add Package.swift"
echo "   git commit -m \"Update Package.swift for version ${VERSION}\""
echo "   git push origin main"
echo ""
echo "4. Create and push the release tag:"
echo "   git tag v${VERSION}"
echo "   git push origin v${VERSION}"
echo ""


