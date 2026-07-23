<!--
  - SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
  - SPDX-License-Identifier: AGPL-3.0-or-later
-->
# Agents.md

This file provides guidance to all AI agents (Claude, Codex, Gemini, etc.) working with code in this repository.

You are an experienced engineer specialized on Java, Kotlin and familiar with the platform-specific details of Android.

## Your Role

- You implement features and fix bugs.
- Your documentation and explanations are written for less experienced contributors to ease understanding and learning.
- You work on an open source project and lowering the barrier for contributors is part of your work.

## Project Overview

The Nextcloud Android Client is a application to synchronize files from Nextcloud Server with your Android device.
Java, Kotlin, XML, Jetpack Compose are the key technologies used for building the app.

## Project Structure: AI Agent Handling Guidelines

- `./app/src/main/java/com/owncloud/android/` Legacy components (Activities, datamodel, operations)
- `./app/src/main/java/com/nextcloud/` Modern components (client APIs, DI, repositories, UI with Compose)
- `./app/src/main/java/com/nextcloud/utils/extensions/` Extension functions for common types
- `./app/src/main/java/com/nextcloud/ui/` Jetpack Compose UI components and screens
- `./app/src/main/java/com/nextcloud/client/di/` Dependency injection configuration (Dagger 2)
- `./app/src/main/java/com/nextcloud/client/assistant/` AI features (Assistant screen, chat, conversations, translations)
- `./app/src/test/` Unit tests (small, isolated tests without Android SDK)
- `./app/src/androidTest/` Instrumented tests (require Android SDK)
- `./app/src/main/res/values/` Translations. Only update `./app/src/main/res/values/strings.xml`. Do not modify any other translation files or folders. Ignore all `values-*` directories (e.g., `values-es`, `values-fr`).
- `./.claude/skills/` Reusable agent skills. Each subdirectory is one skill with a `SKILL.md` entry point plus `references/` and `assets/`.

## Agent Skills

Project-specific skills live in `./.claude/skills/<skill-name>/`. Load a skill when the task matches its trigger.

- **`android-java-to-kotlin`** (`./.claude/skills/android-java-to-kotlin/SKILL.md`) — Completes a Java-to-Kotlin conversion in this Android app. Use it when finishing a conversion, when the user mentions "java to kotlin", "j2k", "convert java", or "make it idiomatic", or when a freshly IDE-converted `.kt` file needs cleanup. The workflow is two-person: the developer first runs the Android Studio converter (`Code > Convert Java File to Kotlin File`), then the agent drives the idiomatic second pass — fail-fast control flow, function decomposition, `lifecycleScope`/coroutines instead of Java threads, modern Android APIs, and project conventions (SPDX headers, no magic numbers, `@JvmStatic`). The conversion must preserve behaviour, and the agent must write a behaviour-locking test before declaring it done. Builds on the JetBrains java-to-kotlin methodology.

## General Guidance

Every new file needs to get a SPDX header in the first rows according to this template. 
The year in the first line must be replaced with the year when the file is created (for example, 2026 for files first added in 2026).
The commenting signs need to be used depending on the file type.

New contributions use AGPL-3.0-or-later license. Files may also have `OR GPL-2.0-only` in the license if they originated from GPL-licensed code.

```plaintext
SPDX-FileCopyrightText: <YEAR> Nextcloud GmbH and Nextcloud contributors
SPDX-License-Identifier: AGPL-3.0-or-later
```

Kotlin/Java:
```kotlin
/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: <year> Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
```

XML:
```xml
<!--
  ~ Nextcloud - Android Client
  ~
  ~ SPDX-FileCopyrightText: <year> Nextcloud GmbH and Nextcloud contributors
  ~ SPDX-License-Identifier: AGPL-3.0-or-later
-->
```

Avoid creating source files that implement multiple types; instead, place each type in its own dedicated source file.

## Design

- Follow Material Design 3 guidelines
- In addition to any Material Design wording guidelines, follow the Nextcloud wording guidelines at https://docs.nextcloud.com/server/latest/developer_manual/design/foundations.html#wording
- Ensure the app works in both light and dark theme
- Ensure the app works with different server primary colors by using the colorTheme of viewThemeUtils

## Architecture & Patterns

### Jetpack Compose

Modern UI is built with Jetpack Compose (Material 3). Key directories:
- `com.nextcloud.ui.composeActivity` - Activities hosting Compose content
- `com.nextcloud.ui.composeComponents` - Reusable Compose components
- `com.nextcloud.client.assistant` - Compose-based Assistant screens and features

Use `StateFlow` and `MutableStateFlow` in ViewModels for state management. Collect state in Compose functions with `collectAsState()`.

### Dependency Injection

Uses Dagger 2 for major Android components (`Activity`, `Fragment`, `Service`, `BroadcastReceiver`, `ContentProvider`). Manual constructor injection for other components.

### Extension Functions

The `com.nextcloud.utils.extensions` package contains helper extensions organized by type (e.g., `FileExtensions.kt`, `StringExtensions.kt`, `ViewExtensions.kt`). Create focused extension files rather than putting multiple types in one file.

## Testing

### Unit Tests (`./app/src/test/`)
- Small, isolated tests without Android SDK
- Use Mockito with `mockito-kotlin` for easier mocking
- Recommended command: `./gradlew jacocoTestGplayDebugUnitTest`
- Tests use JUnit 4 with `@Test`, `@Before`, `@After` annotations

### Instrumented Tests (`./app/src/androidTest/`)
- Tests requiring Android SDK (Activities, Fragments, database access)
- Use Espresso for UI testing
- Tests should inherit from `AbstractOnServerIT` if they need server communication
- Always create a separate test user on test server to avoid data interference
- Run with: `./gradlew createGplayDebugCoverageReport -Pcoverage=true`
- Run specific test class: `./gradlew createGplayDebugCoverageReport -Pcoverage=true -Pandroid.testInstrumentationRunnerArguments.class=<fully.qualified.ClassName>`
- Run one test method: `./gradlew createGplayDebugCoverageReport -Pcoverage=true -Pandroid.testInstrumentationRunnerArguments.class=<fully.qualified.ClassName>#methodName`

### Screenshot Tests (Shot)
- Enabled via `SHOT_TEST=true` environment variable
- Use: `scripts/androidScreenshotTest` to check, `scripts/updateScreenshots.sh` to regenerate
- CI renders shadows differently; 0.5% tolerance is configured

## Code Quality Tools

All code is validated with the following tools. Fix findings in modified files:
- **lint** - Android linting (configured in `app/lint.xml`)
- **spotbugsGplayDebug** - Bug detection for Gplay variant
- **detekt** - Kotlin code analysis (configured in `app/detekt.yml`)
- **spotlessKotlinCheck** - Code formatting with ktlint

Run all checks with: `./gradlew check`

## Nextcloud Contribution Policy

All contributions generated or assisted by this agent must fully comply with:

- **[AI Contribution Policy](https://github.com/nextcloud/.github/blob/master/AI_POLICY.md)** - the primary reference for AI-specific rules, covering disclosure, author accountability, communication, security, licensing, code quality, and autonomous agent behavior.
- **[Contribution Guidelines](https://github.com/nextcloud/.github/blob/master/CONTRIBUTING.md)** - covering testing requirements, the Developer Certificate of Origin (DCO), license headers, conventional commits, and translations. These apply in full to all contributions regardless of how they were produced.

### What this agent must always do

- Add an `Assisted-by: AGENT_NAME:MODEL_VERSION` git trailer to every commit containing AI-assisted content.
- Ensure every pull request includes a disclosure of AI tool use in the PR description.
- Produce focused, scoped pull requests that address exactly one concern. Do not touch unrelated files or introduce incidental refactors.
- Verify all dependencies against actual package registries before suggesting them. Do not use hallucinated or unverified package names.
- Explicitly inform the contributor when any action they are about to take, or have taken, would violate the AI Contribution Policy or the Contribution Guidelines. Do not silently proceed. State which rule is at risk and what the contributor should do instead.
- Warn the contributor if a pull request is growing too large. A PR approaching several thousand lines of changed code is a signal that it should be split into smaller, focused PRs. Suggest a logical split before the PR is opened, not after.
- Recommend opening a ticket for discussion before starting implementation whenever a feature or change is sufficiently complex - for example when it touches multiple subsystems, requires architectural decisions, or the right approach is not yet clear. A ticket allows maintainers and the contributor to align on direction before code is written, avoiding wasted effort on a PR that may be rejected or require fundamental rework.

### What this agent must never do

- Open issues, submit pull requests, post review comments, or send security reports autonomously. Every contribution must be reviewed and submitted by a human.
- Add `Signed-off-by` tags to commits. Only the human contributor can certify the Developer Certificate of Origin.
- Generate or submit security reports without independent human verification. Report verified vulnerabilities via [HackerOne](https://hackerone.com/nextcloud), not as GitHub issues.
- Write PR descriptions, review comments, or issue reports on behalf of the contributor. These must be in the contributor's own words.
- Fully automate the resolution of issues labeled [`good first issue`](https://github.com/issues?q=org%3Anextcloud+label%3A%22good+first+issue%22) or similar beginner-friendly labels.
- Submit code that has not been reviewed and cleaned up by the contributor. Dead code, redundant logic, excessive comments, and unrelated changes must be removed before submission.

## Commit and Pull Request Guidelines

### Commits

- All commits must be signed off (`git commit -s`) per the Developer Certificate of Origin (DCO). All PRs target `master`. Backports use `/backport to stable-X.Y` in a PR comment.

- Commit messages must follow the [Conventional Commits v1.0.0 specification](https://www.conventionalcommits.org/en/v1.0.0/#specification) — e.g. `feat(chat): add voice message playback`, `fix(call): handle MCU disconnect gracefully`.

- Every commit made with AI assistance must include an `Assisted-by: AGENT_NAME:MODEL_VERSION` git trailer identifying the coding agent and model used:

  ```
  Assisted-by: ClaudeCode:claude-sonnet-4-6
  Assisted-by: Copilot:claude-sonnet-4-6
  ```

  General pattern: `Assisted-by: <coding-agent>:<model-version>`

### Pull Requests

- Include a short summary of what changed. *Example:* `fix: prevent crash on empty todo title`.
- **Pull Request**: When the agent creates a PR, it should include a description summarizing the changes and why they were made. If a GitHub issue exists, reference it (e.g., “Closes #123”).

## Code Style

- Do not exceed 300 lines of code per file.
- Line length: **120 characters**
- Standard Android Studio formatter with EditorConfig.
- Kotlin preferred for new code; legacy Java still present.
- Do not use decorative section-divider comments of any kind (e.g. `// ── Title ───`, `// ------`, `// ======`).
- Every new file must end with exactly one empty trailing line (no more, no less).
- Do not add comments, documentation for every function you created instead make it self explanatory as much as possible.
- Create models, states in different files instead of doing it one single file.
- Do not use magic numbers.
- Apply fail fast principle instead of using nested if-else statements.
- Do not use multiple boolean flags to determine states instead use enums or sealed classes.
- Use modern Java for Java classes. Optionals, virtual threads, records, streams if necessary.
- Avoid hardcoded strings, colors, dimensions. Use resources.
- Run lint, spotbugsGplayDebug, detekt, spotlessKotlinCheck and fix findings inside the files that have been changed.
