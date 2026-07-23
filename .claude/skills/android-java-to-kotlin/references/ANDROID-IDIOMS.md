<!--
  ~ Nextcloud - Android Client
  ~
  ~ SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
  ~ SPDX-License-Identifier: AGPL-3.0-or-later
-->

# Android + Kotlin Idioms

Transformations applied during the idiomatic pass. Every one is behaviour-preserving.
Examples are drawn from a real fragment conversion.

## 1. Decompose Oversized Functions

The IDE keeps the Java structure: one enormous `onViewCreated`/`setupView` that inflates,
themes, wires listeners, and kicks off loading in a single 80-line block. Split by
intent into small private functions. The lifecycle callback becomes a readable table of
contents.

```kotlin
// BEFORE: onViewCreated does everything inline (adapters, layout managers, listeners, fetch)

// AFTER
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    fileActivity ?: return
    fileDataStorageManager = fileActivity?.storageManager
    fileOperationsHelper = fileActivity?.fileOperationsHelper

    startAnimation()
    val userId = getUserId()
    setupInternalShares(userId)
    setupExternalShares(userId)
    binding?.pickContactEmailBtn?.setOnClickListener { checkContactPermission() }
    fetchSharees()
    setupView()
}
```

Rules:
- One function = one reason to change. Name it for *what it accomplishes*
  (`setupInternalShares`, `themeView`, `disableE2EEShareForV1`), not *how*.
- Factor duplicated blocks into a parameterized helper
  (`createShareListAdapter(userId, SharesType.INTERNAL)`).
- Keep files ≤300 lines (project rule). Heavy decomposition sometimes means splitting a
  god-class into collaborators — raise that with the developer rather than exceeding 300.

## 2. Scope Functions Over Repetition

Replace repeated `binding.x` / `viewThemeUtils.material.y` chains with `run`/`apply`/`with`.

```kotlin
// BEFORE
viewThemeUtils.material.themeSearchCardView(binding.searchCardWrapper);
viewThemeUtils.material.colorMaterialButtonPrimaryOutlined(binding.sendCopyBtn);
viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(binding.sharesListInternalShowAll);

// AFTER
binding.run {
    viewThemeUtils.material.run {
        themeSearchCardView(searchCardWrapper)
        colorMaterialButtonPrimaryOutlined(sendCopyBtn)
        colorMaterialButtonPrimaryBorderless(sharesListInternalShowAll)
    }
}
```

Use `apply {}` when configuring and returning the receiver:

```kotlin
ShareeListAdapter(fileActivity!!, ArrayList(), this, userId, user, viewThemeUtils, encrypted, type)
    .apply { setHasStableIds(true) }
```

## 3. `switch` → `when` / `filter` + `partition`

Collapse a `switch` that sorts items into buckets into a declarative pipeline with a
constant `Set`.

```kotlin
// BEFORE: for-loop with switch(shareType) adding to internalShares / externalShares

// AFTER
private val externalShareTypes = setOf(
    ShareType.PUBLIC_LINK, ShareType.FEDERATED_GROUP, ShareType.FEDERATED, ShareType.EMAIL
)

val (external, internal) = shares
    .filter { it.shareType != null }
    .partition { it.shareType in externalShareTypes }
```

## 4. Extension Functions & KTX

Import members directly and lean on AndroidX KTX instead of verbose Java utilities.

| Java / verbose | Idiomatic Kotlin |
|---|---|
| `TextUtils.isEmpty(s)` | `s.isNullOrEmpty()` |
| `BundleExtensionsKt.getParcelableArgument(b, k, T.class)` | `b.getParcelableArgument(k, T::class.java)` |
| `for (int i = 0; i < vg.getChildCount(); i++)` | `for (i in 0..<view.size)` (`androidx.core.view.size`) |
| manual getter/setter methods | Kotlin property access (`view.visibility = View.GONE`) |
| free-standing util call | receiver extension (`externalShares.mergeDistinctByToken(publicShares)`) |

Domain-specific extensions read best as receivers on the relevant type:

```kotlin
private fun OCCapability?.isPasswordEnforced(): Boolean =
    this?.filesSharingPublicPasswordEnforced?.isTrue == true &&
        filesSharingPublicAskForOptionalPassword.isTrue
```

## 5. Null Safety Instead of Platform Types

The IDE leaves `!` platform types and defensive Java null-checks. Replace with `?.`,
`?:`, and Kotlin's `require`/`requireNotNull`. A nullable `binding` (cleared in
`onDestroyView`) is the canonical Android case — guard it with `?.` / `?: return`.

```kotlin
// BEFORE
if (binding == null) return;
final LinearLayout shimmer = binding.shimmerLayout.getRoot();
shimmer.clearAnimation();

// AFTER
binding?.run {
    shimmerLayout.root.run {
        clearAnimation()
        visibility = View.GONE
    }
    shareContainer.visibility = View.VISIBLE
}
```

## 6. Constants & Companion Object

Move `static final` and magic literals into a `companion object`; use `const val` for
compile-time constants. Add `@JvmStatic` to factory methods still called from Java.

```kotlin
companion object {
    private const val TAG = "FileDetailSharingFragment"
    private const val ARG_FILE = "FILE"
    private const val MIN_SHOW_ALL_VISIBLE_ITEM_COUNT = 3
    private const val INTERNAL_LINK_PATH_PRETTY = "/f/"

    @JvmStatic
    fun newInstance(file: OCFile?, user: User?) = FileDetailSharingFragment().apply {
        arguments = Bundle().apply {
            putParcelable(ARG_FILE, file)
            putParcelable(ARG_USER, user)
        }
    }
}
```

## 7. Detekt Suppressions Are a Last Resort

If a legacy god-class genuinely cannot be split within the conversion's scope, a
file/class-level `@Suppress("TooManyFunctions", "LargeClass", ...)` is acceptable — but
prefer real decomposition and tell the developer what you suppressed and why.

## 8. Optional: `// region` Organization

For large classes, grouping members under `// region <name>` / `// endregion` (lifecycle,
private methods, overrides, companion) aids IDE folding. This is IDE structure, not a
decorative divider. Match the surrounding file's existing style; do not introduce ASCII
banner comments (`// ==== ====`), which the project forbids.
