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

* `ANDROID_HOME=/usr/lib/android-sdk`
* `sudo apt-get update -y`
* `sudo apt-get install -y unzip wget openjdk-17-jdk vim`
* `wget https://dl.google.com/android/repository/commandlinetools-linux-6858069_latest.zip -O /tmp/commandlinetools.zip`
* `cd /tmp && unzip commandlinetools.zip`
* `sudo mkdir -p /usr/lib/android-sdk/cmdline-tools/`
* `cd /tmp/ && sudo mv cmdline-tools/ latest/ && sudo mv latest/ /usr/lib/android-sdk/cmdline-tools/`
* `sudo mkdir /usr/lib/android-sdk/licenses/`
* `sudo chmod -R 755 /usr/lib/android-sdk/`
* `mkdir -p "$HOME/.gradle"`
* `echo "org.gradle.jvmargs=-Xmx6g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8 -XX:+UseParallelGC -XX:MaxMetaspaceSize=1g" > "$HOME/.gradle/gradle.properties"`
* `echo "org.gradle.caching=true" >> "$HOME/.gradle/gradle.properties"`
* `echo "org.gradle.parallel=true" >> "$HOME/.gradle/gradle.properties"`
* `echo "org.gradle.configureondemand=true" >> "$HOME/.gradle/gradle.properties"`
* `echo "kapt.incremental.apt=true" >> "$HOME/.gradle/gradle.properties"`
