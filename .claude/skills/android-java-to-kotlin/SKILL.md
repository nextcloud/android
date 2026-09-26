---
name: android-java-to-kotlin
description: >
  Use when finishing a Java-to-Kotlin conversion in an Android project, when the user
  mentions "java to kotlin", "j2k", "convert java", "migrate java to kotlin", "finish the
  conversion", "make it idiomatic", or when a freshly IDE-converted .kt file needs to be
  turned into clean, modern, idiomatic Kotlin. The developer first runs the IDE converter
  (Android Studio: Code > Convert Java File to Kotlin File), then this skill drives the
  second pass: idiomatic cleanup, fail-fast control flow, coroutines/lifecycleScope,
  modern Android APIs, function decomposition, and a behaviour-locking test.
license: AGPL-3.0-or-later
SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
SPDX-License-Identifier: AGPL-3.0-or-later
metadata:
  author: Nextcloud Android
  version: "1.0.0"
---

# Android Java to Kotlin Conversion (Second Pass)

The IDE converter produces Kotlin that *compiles* but is not *idiomatic*: platform types
everywhere, one giant function per lifecycle callback, nested `if`/`else`, `new Thread`,
`switch`, `TextUtils.isEmpty`, magic numbers. This skill drives the disciplined second
pass that turns that output into clean, modern, testable Kotlin — **without changing
observable behaviour** — and then writes a test that proves behaviour is unchanged.

## The Two-Person Workflow

This skill assumes a hand-off:

1. **Developer** runs the mechanical IDE conversion (`Code > Convert Java File to Kotlin
   File`, or ⌥⇧⌘K), producing a `.kt` file that compiles but is not idiomatic.
2. **You (Claude)** drive everything after that:

   Step 0 Baseline → Step 1 Idiomatic pass → Step 2 Behaviour-locking test → Step 3
   Verify. If Step 3 fails or behaviour drifts, loop back to Step 1.

If you are handed a `.java` file instead, first apply a faithful 1:1 translation to reach
the same starting point, then continue.

## The Prime Directive: Behaviour Must Not Change

Every transformation in this skill is **behaviour-preserving**. You are refactoring for
readability, safety, and modern API usage — not adding features or fixing bugs. If you
spot a real bug, surface it to the developer; do not silently "fix" it inside a
conversion. The behaviour-locking test in Step 3 exists to keep you honest.

## Step 0: Establish Baseline

Before editing anything:

1. Read the whole `.kt` file (and, if you can, the original `.java` via
   `git show <rev>:<path>.java` or the IDE's local history) to understand *what it does*.
2. Write down the **public/observable surface** you must keep intact:
   - Lifecycle callbacks (`onCreate`, `onViewCreated`, `onSaveInstanceState`, …) and their
     ordering of side-effects.
   - Any Parcelable/Bundle keys, intent extras, and `newInstance(...)` factory shapes.
3. Note threading: which work runs off the main thread today (`Thread`, `AsyncTask`,
   `runOnUiThread`, executors) — this maps to coroutines in Step 2.

## Step 1: Idiomatic Pass

Apply, in this order, then re-check the invariants:

1. **Kill platform types.** Give every `!` platform type an explicit nullable/non-null
   type based on the Java source and call sites.
2. **Fail fast.** Replace nested `if`/`else` pyramids and null-checks with guard clauses
   and `require`/`requireNotNull`/`?: return`. See [FAIL-FAST.md](references/FAIL-FAST.md).
3. **Decompose.** Break each oversized lifecycle callback / `setupView`-style method into
   small, single-purpose private functions named for their intent. See
   [ANDROID-IDIOMS.md](references/ANDROID-IDIOMS.md).
4. **Modern concurrency.** Replace `new Thread`, `AsyncTask`, `runOnUiThread`, and
   callback pairs with `lifecycleScope` + `suspend` + `withContext`. See
   [CONCURRENCY.md](references/CONCURRENCY.md).
5. **Modern Android + Kotlin idioms.** Scope functions (`run`/`apply`/`let`), extension
   functions, `when`/`partition`/`filter` over `switch`, string templates, `isNullOrEmpty`,
   view/KTX extensions. See [ANDROID-IDIOMS.md](references/ANDROID-IDIOMS.md). Retire
   deprecated Android APIs (`onActivityResult`, options-menu overrides,
   `java.util.Observable`) — see [DEPRECATED-APIS.md](references/DEPRECATED-APIS.md).
6. **Project conventions.** SPDX header, no magic numbers (`companion object` +
   `const val`), resources not hardcoded strings, ≤300 lines/file, ≤120 cols. See
   [PROJECT-CONVENTIONS.md](references/PROJECT-CONVENTIONS.md).
7. **Testability seams.** Extract pure logic (URL building, permission math, partitioning)
   into functions you can unit-test; mark with `@VisibleForTesting` where they must stay
   `internal`/`private`-ish but be reachable from tests.

Do NOT expand scope. Unrelated files stay untouched (AGENTS.md / AI policy).

## Step 2: Write a Behaviour-Locking Test (Mandatory)

A conversion is not complete until a test proves behaviour is unchanged. See
[TESTING.md](references/TESTING.md) for the decision tree. In short:

- **Pure functions you extracted** (e.g. link builders, `isReshareForbidden`,
  partitioning) → fast JVM unit tests in `app/src/test/` (JUnit4 + mockito-kotlin).
- **Fragment/Activity/DB behaviour** → instrumented test in `app/src/androidTest/`
  extending the project's base test class, or a Robolectric test where the project uses it.
- Prefer testing the **seams you just created**. If the Java original had no test, your
  new test is the characterization test that locks current behaviour.

Every test file gets the SPDX header and follows the project's test conventions.

## Step 3: Verify

Run and report real output — never claim green without evidence:

```bash
# Formatting + static analysis on the changed files
./gradlew spotlessKotlinCheck detektGplayDebug lintGplayDebug spotbugsGplayDebug

# Unit tests
./gradlew jacocoTestGplayDebugUnitTest

# Instrumented tests (if you wrote one), scoped to the class
./gradlew createGplayDebugCoverageReport -Pcoverage=true \
  -Pandroid.testInstrumentationRunnerArguments.class=<fully.qualified.TestClass>
```

## Worked Example

[assets/worked-example.md](assets/worked-example.md) is a real before/after from
`FileDetailSharingFragment` (871-line Java fragment → idiomatic Kotlin) showing each
transformation class in context. Read it when you need a concrete pattern.

## Batch Conversion

Convert one file at a time, leaf dependencies first. Report progress per file. Warn the
developer before a change set grows into several thousand lines — split into focused PRs
(AGENTS.md). Each commit needs `Assisted-by: <agent>:<model>`; only the human adds
`Signed-off-by`.
