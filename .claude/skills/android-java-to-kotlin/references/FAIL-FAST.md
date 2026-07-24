<!--
  ~ Nextcloud - Android Client
  ~
  ~ SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
  ~ SPDX-License-Identifier: AGPL-3.0-or-later
-->

# Fail Fast: Guard Clauses Over Nested `if`/`else`

The IDE preserves Java's nested-`if` pyramids. Invert them into guard clauses that return
(or throw) early, leaving the happy path at the lowest indentation. Behaviour is
identical — the branches are the same, only the shape changes.

## Precondition Checks → `require` / `requireNotNull`

`requireNotNull` returns the smart-cast non-null value AND throws
`IllegalArgumentException` with the message — exactly matching the Java `if (x == null)
throw new IllegalArgumentException(...)`.

```kotlin
// BEFORE
if (file == null) throw IllegalArgumentException("File may not be null");
if (user == null) throw IllegalArgumentException("Account may not be null");
fileActivity = (FileActivity) getActivity();
if (fileActivity == null) throw IllegalArgumentException("FileActivity may not be null");

// AFTER
fileActivity = activity as? FileActivity
requireNotNull(file) { "File may not be null" }
requireNotNull(user) { "Account may not be null" }
requireNotNull(fileActivity) { "FileActivity may not be null" }
```

Use `require(condition) { msg }` for boolean preconditions:

```kotlin
require(activity is FileActivity) { "Calling activity must be of type FileActivity" }
```

`check`/`checkNotNull` are the `IllegalStateException` equivalents — use them when the Java
threw `IllegalStateException`. Match the original exception type; that is observable
behaviour.

## Early Return Over Nested Success Path

```kotlin
// BEFORE
private void checkShareViaUser() {
    if (!MDMConfig.INSTANCE.shareViaUser(requireContext())) {
        binding.searchContainer.setVisibility(View.GONE);
    }
}

// AFTER
private fun checkShareViaUser() {
    if (shareViaUser(requireContext())) return
    binding?.searchContainer?.visibility = View.GONE
}
```

## Deeply Nested `if`/`else` → Sequential Guards

The most valuable transformation. A cursor-handling method nested three levels deep
becomes a flat sequence of guard clauses, each handling one failure and returning.

```kotlin
// BEFORE: if (cursor != null) { if (moveToFirst()) { if (columnIndex != -1) {...} else ... } else ... } else ...

// AFTER
private fun handleContactResult(contactUri: Uri) {
    val cursor = fileActivity?.contentResolver?.query(contactUri, projection, null, null, null)
    if (cursor == null) {
        DisplayUtils.showSnackMessage(this, R.string.email_pick_failed)
        Log_OC.e(TAG, "Failed to pick email address as Cursor is null.")
        return
    }
    if (!cursor.moveToFirst()) {
        DisplayUtils.showSnackMessage(this, R.string.email_pick_failed)
        Log_OC.e(TAG, "Failed to pick email address as no Email found.")
        return
    }
    val columnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
    if (columnIndex == -1) {
        DisplayUtils.showSnackMessage(this, R.string.email_pick_failed)
        Log_OC.e(TAG, "Failed to pick email address.")
        cursor.close()
        return
    }
    val email = cursor.getString(columnIndex)
    // ... happy path at base indentation
    cursor.close()
}
```

Watch the cleanup: if the Java relied on falling through to a single `cursor.close()`,
each early return must still close it (or wrap in `use {}`). Missing that changes
behaviour (resource leak) — verify it.

## Nullable-Guard Idioms

```kotlin
val activity = fileActivity ?: return
val clientRepository = activity.clientRepository ?: return
val remotePath = file?.remotePath ?: return
```

Each `?: return` collapses one Java `if (x == null) return;`. Chain them at the top of the
function so the body works with non-null smart-cast locals.

## When NOT to Invert

- Do not turn a genuine two-branch decision (both branches do real work) into a guard if
  it obscures the symmetry — a `when`/`if-else` expression is clearer there.
- Do not change the *order* of side-effects while inverting; the snackbar/log calls above
  must fire in the same cases as before.
