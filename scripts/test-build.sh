#!/bin/bash

# Test build script to simulate CI process locally
set -e

echo "🔧 Testing local build process..."

echo "📦 Installing webview dependencies..."
cd webview
npm ci
cd ..

echo "🏗️ Building project with SBT..."
sbt clean compile packageArtifactZip

echo "✅ Build completed successfully!"

# Check if the artifact was created
if [ -f target/CodeEpiphany-*.zip ]; then
    echo "📄 Plugin artifact created:"
    ls -la target/CodeEpiphany-*.zip
else
    echo "❌ Plugin artifact not found!"
    exit 1
fi

echo "�� All tests passed!" 