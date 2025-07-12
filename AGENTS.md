# AGENTS.MD

This document outlines the setup for Java and the Android SDK for agentic AI.

## Java Development Kit (JDK)

1.  **Installation**:
    *   Download and install a recent version of the Java Development Kit (JDK), for instance from OpenJDK or Oracle.
    *   Follow the installation instructions for your operating system.

2.  **Configuration**:
    *   Set the `JAVA_HOME` environment variable to the installation directory of your JDK.
    *   Add the `bin` directory of your JDK installation to your system's `PATH` environment variable.

## Android SDK

### 1. Download the Command Line Tools

*   Navigate to the [Android Studio download page](https://developer.android.com/studio).
*   Locate the "Command Line Tools only" section and download the appropriate zip file for your operating system.
*   Create a directory to hold the SDK (e.g., `C:\android-sdk` or `~/android-sdk`).
*   Extract the downloaded zip file into this directory, resulting in a structure like `android_sdk/cmdline-tools`.

### 2. Install SDK Components

*   Open a command prompt or terminal and navigate to the `android_sdk/cmdline-tools/latest/bin` directory (or similar, depending on your extraction).
*   Use the `sdkmanager` tool to install the necessary components:
    *   **Platforms**: `sdkmanager "platforms;android-34"` (replace 34 with your desired API level).
    *   **Platform Tools**: `sdkmanager "platform-tools"`.
    *   **Build Tools**: `sdkmanager "build-tools;34.0.0"` (replace 34.0.0 with your desired build tools version).
    *   **Emulator (optional)**: `sdkmanager "emulator"`.
*   You'll likely be prompted to accept licenses, which you can do by typing `y` and pressing Enter.

### 3. Configure the Environment

*   **PATH**:
    *   Add the `android_sdk/cmdline-tools/latest/bin` directory (or the equivalent) to your system's `PATH` environment variable. This allows you to run `sdkmanager` and other tools from any location in your terminal.
*   **ANDROID_HOME**:
    *   Consider setting the `ANDROID_HOME` environment variable to the root of your SDK directory (`android_sdk` in our example).
*   **JAVA_HOME**:
    *   Ensure that you have Java Development Kit (JDK) installed and that the `JAVA_HOME` environment variable is set correctly.
