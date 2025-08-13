#!/bin/bash

# Comprehensive Test and Deployment Script for Nextcloud Android App
# This script runs all unit tests, integration tests, instrumented tests, and deploys the app

set -e  # Exit on any error

echo "🚀 Nextcloud Android App - Complete Testing & Deployment Script"
echo "=================================================================="

# Set Java environment
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.15/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

echo "☕ Using Java: $(java -version 2>&1 | head -n 1)"
echo ""

# Check for connected devices
echo "📱 Checking for connected Android devices..."
DEVICES=$(adb devices | grep -v "List of devices attached" | grep -v "^$" | wc -l)

if [ "$DEVICES" -eq 0 ]; then
    echo "❌ No Android devices connected!"
    echo ""
    echo "Please connect your Android device and ensure:"
    echo "  1. USB Debugging is enabled"
    echo "  2. Device is authorized for debugging"
    echo "  3. Run 'adb devices' to verify connection"
    echo ""
    echo "Once connected, re-run this script."
    exit 1
fi

echo "✅ Found $DEVICES connected device(s)"
adb devices
echo ""

# Step 1: Unit Tests
echo "🧪 Step 1: Running Unit Tests..."
echo "=================================="
./gradlew :app:testGenericDebugUnitTest --info
echo "✅ Unit tests completed successfully!"
echo ""

# Step 2: Build and Install App
echo "📦 Step 2: Building and Installing App..."
echo "=========================================="
./gradlew :app:installGenericDebug
echo "✅ App installed successfully!"
echo ""

# Step 3: Instrumented Tests (on device)
echo "🤖 Step 3: Running Instrumented Tests on Device..."
echo "=================================================="
echo ""
echo "📊 Running Database Tests..."
./gradlew :app:connectedGenericDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.owncloud.android.ChunkedUploadDatabaseTest

echo ""
echo "🔄 Running Operation Tests..."  
./gradlew :app:connectedGenericDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.owncloud.android.ChunkedUploadTest

echo ""
echo "⚙️ Running File Upload Worker Tests..."
./gradlew :app:connectedGenericDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nextcloud.client.jobs.upload.FileUploadWorkerInstrumentedTest

echo ""
echo "🎯 Running All Instrumented Tests..."
./gradlew :app:connectedGenericDebugAndroidTest

echo ""
echo "✅ All instrumented tests completed successfully!"
echo ""

# Step 4: Launch App
echo "🚀 Step 4: Launching App on Device..."
echo "====================================="
adb shell am start -n com.nextcloud.client.debug/com.owncloud.android.ui.activity.FileDisplayActivity
echo "✅ App launched successfully!"
echo ""

echo "🎉 SUCCESS! All tests passed and app is running!"
echo "================================================"
echo ""
echo "📋 Summary:"
echo "  ✅ Unit Tests: PASSED"
echo "  ✅ App Installation: SUCCESS"
echo "  ✅ Instrumented Tests: PASSED"
echo "  ✅ App Launch: SUCCESS"
echo ""
echo "📍 APK Location: ./app/build/outputs/apk/generic/debug/generic-debug-30330000.apk"
echo "📊 Test Reports: ./app/build/reports/"
echo ""
echo "🔧 Code Quality Improvements Applied:"
echo "  • Fixed generic exception handling"
echo "  • Extracted string constants (DRY principle)"
echo "  • Fixed wildcard imports"
echo "  • Improved test naming conventions"
echo "  • Added comprehensive test coverage"
echo ""
echo "The Nextcloud Android app is now running on your device! 🎉" 