#!/bin/bash

# Nextcloud Android App - Chunked Upload Instrumented Tests Runner
# This script runs the instrumented tests for chunked upload functionality

echo "🧪 Nextcloud Android - Chunked Upload Instrumented Tests"
echo "========================================================="
echo

# Set Java environment
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.15/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

echo "📱 Checking connected devices..."
adb devices

echo
echo "🔨 Building instrumented tests..."
./gradlew :app:assembleGenericDebugAndroidTest

echo
echo "🧪 Running Chunked Upload Instrumented Tests..."
echo

echo "📊 Running Database Tests..."
./gradlew :app:connectedGenericDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.owncloud.android.ChunkedUploadDatabaseIT

echo
echo "🔄 Running Operation Tests..."  
./gradlew :app:connectedGenericDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.owncloud.android.ChunkedUploadIT

echo
echo "⚙️ Running Worker Tests..."
./gradlew :app:connectedGenericDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nextcloud.client.jobs.upload.FileUploadWorkerInstrumentedTest

echo
echo "✅ All instrumented tests completed!"
echo "📊 Check test reports at: app/build/reports/androidTests/connected/index.html" 