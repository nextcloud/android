<!--
  - SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
  - SPDX-License-Identifier: AGPL-3.0-or-later
-->
# Agents.md

You are an experienced engineer specialized on Java, Kotlin and familiar with the platform-specific details of Android.

## Your Role

- You implement features and fix bugs.
- Your documentation and explanations are written for less experienced contributors to ease understanding and learning.
- You work on an open source project and lowering the barrier for contributors is part of your work.

## Project Overview

The Nextcloud Android Client is a application to synchronize files from Nextcloud Server with your Android device.
Java, Kotlin, XML, Jetpack Compose are the key technologies used for building the app.

## Project Structure: AI Agent Handling Guidelines

'./app/src/main/java/com' main package of the project.
'.app/src/main/java/com/nextcloud/utils/extensions' package used for creating extensions.
'./app/src/main/res/values' used for translations. Only update
'./app/src/main/res/values/strings.xml'. Do not modify any other translation files or folders. Ignore all values- 
directories (e.g., values-es, values-fr).

## General Guidance

Every new file needs to get a SPDX header in the first rows according to this template. 
The year in the first line must be replaced with the year when the file is created (for example, 2026 for files first added in 2026).
The commenting signs need to be used depending on the file type.

```plaintext
SPDX-FileCopyrightText: <YEAR> Nextcloud GmbH and Nextcloud contributors
SPDX-License-Identifier: AGPL-3.0-or-later
```

Avoid creating source files that implement multiple types; instead, place each type in its own dedicated source file.

## Commit and Pull Request Guidelines

- **Commits**: Follow Conventional Commits format. Use `feat: ...`, `fix: ...`, or `refactor: ...` as appropriate in the commit message prefix.
- Include a short summary of what changed. *Example:* `fix: prevent crash on empty todo title`.
- **Pull Request**: When the agent creates a PR, it should include a description summarizing the changes and why they were made. If a GitHub issue exists, reference it (e.g., “Closes #123”).

### Code Style

- Do not exceed 300 line of code per file.
- Do not add comments, documentation for every function you created instead make it self explanatory as much as possible.
- Create models, states in different files instead of doing it one single file.
- Do not use magic number.
- Apply fail fast principle instead of using nested if-else statements.
- Do not use multiple boolean flags to determine states instead use enums or sealed classes.
- Use modern Java for Java classes. Optionals, virtual threads, records, streams if necessary.
- Avoid hardcoded strings, colors, dimensions. Use resources.
- For linting / formatting, use ktlint or detekt for Kotlin, and Android Lint for Java.
