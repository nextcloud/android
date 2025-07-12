<!--
 ~ SPDX-FileCopyrightText: 2025 Nextcloud GmbH and Nextcloud contributors
 ~ SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
-->
# AGENTS.MD

This `AGENTS.md` file provides guidelines for Jules and other AI agents interacting with this codebase, including which directories are safe to read from or write to.

This document outlines the setup for Java and the Android SDK for agentic AI, files and folder to ignore and general rules to stick to.

## Project Overview

The Nextcloud Android Client is a tool to synchronize files from Nextcloud Server with your computer and vice versa.

## Project Structure: AI Agent Handling Guidelines

| Directory                    | Description                                         | Agent Action         |
|------------------------------|-----------------------------------------------------|----------------------|
| `/app/src/main/res/values-*` | Translation files from Transifex.                   | Do not modify        |

## General Guidance

Every new file needs to get a SPDX header in the first rows according to this template. 
The year needs to be adjusted accordingly. The commenting signs need to be used depending on the file type.
```
SPDX-FileCopyrightText: 2025 Nextcloud GmbH and Nextcloud contributors
SPDX-License-Identifier: GPL-2.0-or-later
```

## Commit & PR Guidelines
- **Commits**: Follow Conventional Commits format. Use `feat: ...`, `fix: ...`, or `refactor: ...` as appropriate in the commit message prefix.
- Include a short summary of what changed. *Example:* `fix: prevent crash on empty todo title`.
- **Pull Request**: When the agent creates a PR, it should include a description summarizing the changes and why they were made. If a GitHub issue exists, reference it (e.g., “Closes #123”).

## Setup

### Java Development Kit (JDK)

1.  **Installation**:
    *   Download and install a recent version of the Java Development Kit (JDK), for instance from OpenJDK or Oracle.
    *   Follow the installation instructions for your operating system.

2.  **Configuration**:
    *   Set the `JAVA_HOME` environment variable to the installation directory of your JDK.
    *   Add the `bin` directory of your JDK installation to your system's `PATH` environment variable.

### Android SDK

#### 1. Download the Command Line Tools

*   Navigate to the [Android Studio download page](https://developer.android.com/studio).
*   Locate the "Command Line Tools only" section and download the appropriate zip file for your operating system.
*   Create a directory to hold the SDK (e.g., `~/android-sdk`).
*   Extract the downloaded zip file into this directory, resulting in a structure like `android_sdk/cmdline-tools`.

#### 2. Install SDK Components

*   Open a command prompt or terminal and navigate to the `android_sdk/cmdline-tools/latest/bin` directory (or similar, depending on your extraction).
*   Use the `sdkmanager` tool to install the necessary components:
    *   **Platforms**: `sdkmanager "platforms;android-34"` (replace 34 with your desired API level).
    *   **Platform Tools**: `sdkmanager "platform-tools"`.
    *   **Build Tools**: `sdkmanager "build-tools;34.0.0"` (replace 34.0.0 with your desired build tools version).
    *   **Emulator (optional)**: `sdkmanager "emulator"`.
*   You'll likely be prompted to accept licenses, which you can do by typing `y` and pressing Enter.

#### 3. Configure the Environment

*   **PATH**:
    *   Add the `android_sdk/cmdline-tools/latest/bin` directory (or the equivalent) to your system's `PATH` environment variable. This allows you to run `sdkmanager` and other tools from any location in your terminal.
*   **ANDROID_HOME**:
    *   Consider setting the `ANDROID_HOME` environment variable to the root of your SDK directory (`android_sdk` in our example).
*   **JAVA_HOME**:
    *   Ensure that you have Java Development Kit (JDK) installed and that the `JAVA_HOME` environment variable is set correctly.
