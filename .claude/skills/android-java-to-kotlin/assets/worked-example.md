# Worked Example: FileDetailSharingFragment

A real conversion of an 871-line Java fragment
(`com.owncloud.android.ui.fragment.FileDetailSharingFragment`) into idiomatic Kotlin. Each
section shows one transformation class from the skill, in context. Behaviour is unchanged
throughout; the only new capability (`@VisibleForTesting createInternalLink`) is a
testability seam that returns the same URL the Java produced.

## A. SPDX Header Rewrite

```diff
-/*
- * Nextcloud Android client application
- *
- * @author Andy Scherzinger
- * ...
- * Copyright (C) 2018 Andy Scherzinger
- * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
- */
+/*
+ * Nextcloud - Android Client
+ *
+ * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
+ * SPDX-License-Identifier: AGPL-3.0-or-later
+ */
```

## B. Fail-Fast Preconditions

```diff
-if (file == null) throw new IllegalArgumentException("File may not be null");
-if (user == null) throw new IllegalArgumentException("Account may not be null");
-fileActivity = (FileActivity) getActivity();
-if (fileActivity == null) throw new IllegalArgumentException("FileActivity may not be null");
+fileActivity = (activity as FileActivity?)
+requireNotNull(file) { "File may not be null" }
+requireNotNull(user) { "Account may not be null" }
+requireNotNull(fileActivity) { "FileActivity may not be null" }
```

## C. Decomposition of `onViewCreated`

The Java `onViewCreated` inlined adapter creation (duplicated for internal/external),
layout managers, listeners, and the fetch kick-off. It became a readable sequence plus
extracted helpers `getUserId()`, `setupInternalShares()`, `setupExternalShares()`,
`createShareListAdapter(userId, type)`, `startAnimation()`. The two near-identical adapter
blocks collapsed into one parameterized factory:

```kotlin
private fun createShareListAdapter(userId: String, type: SharesType): ShareeListAdapter =
    ShareeListAdapter(
        fileActivity!!, ArrayList(), this, userId, user, viewThemeUtils,
        (file?.isEncrypted == true), type
    ).apply { setHasStableIds(true) }
```

## D. `new Thread` + `runOnUiThread` → `lifecycleScope` + `suspend`

```diff
-private void fetchE2EECounter(Runnable onComplete) {
-    new Thread(() -> {
-        try { ... fileDataStorageManager.saveFile(file); }
-        catch (Exception e) { Log_OC.e(TAG, "..." + e.getMessage()); }
-        Activity a = getActivity();
-        if (a != null) a.runOnUiThread(onComplete);
-    }).start();
-}
+private suspend fun fetchE2EECounter(): Boolean = withContext(Dispatchers.IO) {
+    return@withContext try {
+        val client = clientFactory.create(user)
+        val metadata = RefreshFolderOperation.getDecryptedFolderMetadata(true, file, client, user, requireContext())
+        if (metadata is DecryptedFolderMetadataFile) {
+            file?.setE2eCounter(metadata.metadata.counter)
+            fileDataStorageManager?.saveFile(file)
+        }
+        true
+    } catch (e: Exception) {
+        Log_OC.e(TAG, "Error refreshing E2E counter: " + e.message)
+        false
+    }
+}
```

Caller now `lifecycleScope.launch { if (!fetchE2EECounter()) return@launch; withContext(Main){ ... } }`.
The callback-pair `fetchSharees(onSuccess, onError)` was likewise turned into a `suspend`
call returning `Boolean`.

## E. `switch` Bucketing → `partition` + Set

```diff
-for (OCShare share : shares) {
-    if (share.getShareType() != null) {
-        switch (share.getShareType()) {
-            case PUBLIC_LINK: case FEDERATED_GROUP: case FEDERATED: case EMAIL:
-                externalShares.add(share); break;
-            default: internalShares.add(share); break;
-        }
-    }
-}
+private val externalShareTypes = setOf(
+    ShareType.PUBLIC_LINK, ShareType.FEDERATED_GROUP, ShareType.FEDERATED, ShareType.EMAIL
+)
+val (external, internal) = shares
+    .filter { it.shareType != null }
+    .partition { it.shareType in externalShareTypes }
```

## F. Nested `if` Cursor Handling → Guard Clauses

The three-level-nested `handleContactResult` became flat sequential guards, each showing
the snackbar + log and returning, with `cursor.close()` preserved on every path. See
[FAIL-FAST.md](../references/FAIL-FAST.md) section "Deeply Nested".

## G. Scope Functions & KTX

```diff
-final LinearLayout shimmerLayout = binding.shimmerLayout.getRoot();
-shimmerLayout.clearAnimation();
-shimmerLayout.setVisibility(View.GONE);
-binding.shareContainer.setVisibility(View.VISIBLE);
+binding?.run {
+    shimmerLayout.root.run { clearAnimation(); visibility = View.GONE }
+    shareContainer.visibility = View.VISIBLE
+}
```

```diff
-for (int i = 0; i < viewGroup.getChildCount(); i++) { toggleSearchViewEnable(viewGroup.getChildAt(i), enable); }
+for (i in 0..<view.size) { toggleSearchViewEnable(view.getChildAt(i), enable) }   // androidx.core.view.size
```

## H. Constants + Companion + `@JvmStatic`

```diff
-private static final String TAG = "FileDetailSharingFragment";
-ViewExtensionsKt.setVisibleIf(binding.sharesListInternalShowAll, adapter.shares.size() > 3);
+companion object {
+    private const val TAG = "FileDetailSharingFragment"
+    private const val MIN_SHOW_ALL_VISIBLE_ITEM_COUNT = 3
+    @JvmStatic fun newInstance(file: OCFile?, user: User?) = FileDetailSharingFragment().apply { ... }
+}
+binding.sharesListInternalShowAll.setVisibleIf(internalShares.size > MIN_SHOW_ALL_VISIBLE_ITEM_COUNT)
```

## I. Testability Seam

`createInternalLink` was inlined string concatenation in Java. It was extracted to a pure
`@VisibleForTesting internal fun createInternalLink(user, file, capabilities): String` that
returns the identical URL — enabling the unit test in
[TESTING.md](../references/TESTING.md) without a device.

## J. Detekt Suppression (Last Resort)

The class is genuinely large and can't be fully split within one conversion, so a
file-level suppression documents the debt honestly:

```kotlin
@Suppress("TooManyFunctions", "LargeClass", "TooGenericExceptionCaught", "ReturnCount")
class FileDetailSharingFragment : Fragment(), ... {
```

Prefer real decomposition; use this only when splitting is out of scope, and tell the
developer.
