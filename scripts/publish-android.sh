#!/bin/bash

# Quick script to publish Android library to JFrog Artifactory
# Usage: ./scripts/publish-android.sh [version]

set -e

VERSION=${1:-"1.0.0"}

echo "📦 Publishing Android library version ${VERSION} to JFrog Artifactory..."

# Check if credentials are set
if [ -z "$ARTIFACTORY_URL" ] && [ -z "$(grep -E '^artifactory.url=' gradle.properties 2>/dev/null)" ]; then
    echo "❌ Error: ARTIFACTORY_URL not set"
    echo ""
    echo "Set it via environment variable:"
    echo "  export ARTIFACTORY_URL='https://your-company.jfrog.io/artifactory/libs-release-local'"
    echo "  export ARTIFACTORY_USERNAME='your-username'"
    echo "  export ARTIFACTORY_PASSWORD='your-password'"
    echo ""
    echo "Or add to gradle.properties:"
    echo "  artifactory.url=..."
    echo "  artifactory.username=..."
    echo "  artifactory.password=..."
    exit 1
fi

# Build and publish
echo "🔨 Building and publishing..."
./gradlew :cloudflareImagesKMP:assembleRelease \
    :cloudflareImagesKMP:publishReleasePublicationToArtifactoryRepository \
    -Plibrary.version=$VERSION

echo ""
echo "✅ Android library published successfully!"
echo ""
echo "📍 Artifact: com.kmpnetworking:cloudflareImagesKMP:${VERSION}"
echo ""
echo "📝 To use in your project, add to build.gradle.kts:"
echo ""
echo "repositories {"
echo "    maven {"
echo "        url = uri(\"https://your-company.jfrog.io/artifactory/libs-release-local\")"
echo "        credentials {"
echo "            username = ..."
echo "            password = ..."
echo "        }"
echo "    }"
echo "}"
echo ""
echo "dependencies {"
echo "    implementation(\"com.kmpnetworking:cloudflareImagesKMP:${VERSION}\")"
echo "}"
echo ""


