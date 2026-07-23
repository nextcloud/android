<!--
  ~ Nextcloud - Android Client
  ~
  ~ SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
  ~ SPDX-License-Identifier: AGPL-3.0-or-later
-->

# Android Post-Conversion Checklist

Walk this after Step 2–3, before declaring done. Complements the generic
[checklist.md](checklist.md) (compilation, annotations, imports, nullability, collections).

## Behaviour Preserved (Prime Directive)
- [ ] Same public / `@VisibleForTesting` signatures as before (Java callers still compile)
- [ ] Same exception types AND messages for the same preconditions
- [ ] Same Bundle keys, intent extras, and `newInstance(...)` wiring
- [ ] Same branch outcomes and same order of side-effects (snackbars, logs, DB writes)
- [ ] No feature added, no bug silently fixed inside the conversion

## Fail Fast
- [ ] Nested `if`/`else` pyramids replaced with guard clauses / early returns
- [ ] `require` / `requireNotNull` / `check` used for preconditions (matching Java throw type)
- [ ] Resource cleanup (`cursor.close()`, streams) still runs on every early-return path
      (or converted to `use {}`)

## Function Decomposition
- [ ] Oversized lifecycle callbacks split into small, intent-named private functions
- [ ] Duplicated blocks factored into parameterized helpers
- [ ] File ≤300 lines (or split raised with developer / justified suppression noted)

## Concurrency
- [ ] `new Thread` / `AsyncTask` / `runOnUiThread` replaced with `lifecycleScope` + `suspend`
- [ ] Correct dispatcher (`IO` for disk/net/DB, `Main` for views)
- [ ] `binding == null` / lifecycle guards preserved inside `launch`
- [ ] No `GlobalScope`; `CancellationException` not swallowed
- [ ] Timing-sensitive callers checked (sync DB read → async is flagged if observable)

## Modern Android + Kotlin Idioms
- [ ] Platform types (`!`) all given explicit nullability
- [ ] Scope functions (`run`/`apply`/`let`) replace repeated `binding.`/`viewThemeUtils.` chains
- [ ] `switch` → `when` / `filter` + `partition`
- [ ] `TextUtils.isEmpty` → `isNullOrEmpty`; verbose utils → extension functions / KTX
- [ ] Getter/setter method calls → Kotlin property access

## Project Conventions
- [ ] SPDX header replaced with current template + correct year
- [ ] Magic numbers → `const val` in `companion object`
- [ ] No hardcoded strings/colors/dimens (resources only; strings.xml only)
- [ ] `@JvmStatic` / `@JvmField` / `@JvmOverloads` / `@Throws` where Java calls in
- [ ] ≤120 cols, one type per file, exactly one trailing newline
- [ ] No decorative divider comments

## Tests (Mandatory)
- [ ] Behaviour-locking test written (unit for pure logic, instrumented for components)
- [ ] Existing tests for this class still pass
- [ ] Test file has SPDX header and follows project test conventions
- [ ] Test asserts the observable contract, not implementation details

## Verification Run
- [ ] `spotlessKotlinCheck` clean on changed files
- [ ] `detektGplayDebug` clean on changed files
- [ ] `lintGplayDebug` clean on changed files
- [ ] `spotbugsGplayDebug` clean on changed files
- [ ] Unit tests green (`jacocoTestGplayDebugUnitTest`)
- [ ] Actual command output reported (no unverified "it's green")

## Git History
- [ ] `git mv` rename committed separately from content change
- [ ] Conventional Commit message + `Assisted-by:` trailer
- [ ] No `Signed-off-by` added by the agent; no autonomous PR/issue
