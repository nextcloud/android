# Project Conventions (Nextcloud Android)

Apply these during Step 2 so the converted file passes review and the quality gates. They
are enforced by `spotlessKotlinCheck`, `detekt`, `lint`, and `spotbugsGplayDebug`.

## SPDX Header (every new/renamed file)

The IDE keeps the old license block. Replace it with the current template. The year is the
year the Kotlin file is created. New contributions are `AGPL-3.0-or-later`; keep
`OR GPL-2.0-only` only if the original file carried it.

```kotlin
/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: <YEAR> Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
```

If the developer wants personal attribution (as in the real conversion), the form
`SPDX-FileCopyrightText: <YEAR> <Name> <email>` is also used — match what the developer
asks for; default to the "Nextcloud GmbH and Nextcloud contributors" line.

## Structural Rules

- **≤300 lines per file.** If decomposition pushes past it, split responsibilities into
  separate files/collaborators and tell the developer. A file/class-level
  `@Suppress("LargeClass", "TooManyFunctions")` is a last resort for legacy god-classes.
- **≤120 columns per line.**
- **One top-level type per file.** Extract models, states, sealed classes, and listener
  interfaces into their own files rather than nesting many types in one.
- **Exactly one trailing newline** at end of file.

## No Magic Numbers / Hardcoded Resources

- Extract literals into named `const val` in a `companion object`
  (`MIN_SHOW_ALL_VISIBLE_ITEM_COUNT = 3`, `INTERNAL_LINK_PATH_PRETTY = "/f/"`).
- Strings, colors, dimens come from resources (`R.string.*`, `R.dimen.*`), never inline.
- Only `app/src/main/res/values/strings.xml` may be edited for strings; never touch
  `values-*` translation folders.

## Comments & Naming

- No decorative divider comments (`// ==== ====`, `// ---- Title ----`). `// region` /
  `// endregion` for IDE folding is allowed and should match the file's existing style.
- Prefer self-explanatory names over per-function KDoc. Preserve genuinely informative
  Javadoc as KDoc (invariant 4); drop noise.
- Do not use multiple boolean flags to model state — use an `enum`/sealed class.

## Modern Java Interop

When the file still has Java callers, keep the Java-facing API clean:
`@JvmStatic` for factory/companion functions, `@JvmField` for exposed constants,
`@JvmOverloads` for defaulted params, `@Throws` for checked exceptions.

## Git & Commits (developer-driven)

- Preserve history: the rename `git mv Foo.java Foo.kt` should be a **separate commit**
  from the content change so `git blame` follows through.
- Conventional Commits (`refactor(sharing): convert FileDetailSharingFragment to Kotlin`).
- Every AI-assisted commit needs an `Assisted-by: <agent>:<model>` trailer.
- Only the human contributor adds `Signed-off-by` (DCO). You must never add it, and never
  open PRs/issues autonomously (AI policy).

## Quality Gate

```bash
./gradlew spotlessKotlinCheck detektGplayDebug lintGplayDebug spotbugsGplayDebug \
          jacocoTestGplayDebugUnitTest
```

Fix every finding in the files you changed before declaring done.
