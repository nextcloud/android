<!--
  ~ Nextcloud - Android Client
  ~
  ~ SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
  ~ SPDX-License-Identifier: AGPL-3.0-or-later
-->

# Worked Example: `UploadListActivity` → `UploadListFragment`

This conversion was the hardest one so far, because the Activity extended `FileActivity`.

## Inventory findings

- Base class `FileActivity`. The adapter called `activity.fileOperationsHelper.checkCurrentCredentials(user)`
  for uploads that failed with `CREDENTIAL_ERROR`. The result came back through
  `onRemoteOperationFinish` → `requestCredentialsUpdate` → `CheckRemoteWipeTask` →
  `performCredentialsUpdate` → `startActivityForResult(AuthenticatorActivity)` →
  `onActivityResult` → `FilesSyncHelper.restartUploadsIfNeeded`.
- Entry points: drawer `nav_uploads`, `FileUploaderIntents.openUploadListIntent` (upload
  notifications, with `EXTRA_USER`), `AppWideNotificationManager.getUploadListPendingIntent`,
  `SyncConflictNotificationBroadcastReceiver`.
- `EXTRA_USER` was consumed by `FileActivity.onCreate` → `setUser`. `EXTRA_FILE` was
  ignored (the Activity set `file = null`).
- Menu: global pause/resume toggle. A `LocalBroadcastManager` receiver for upload events.
  `UploadWarningCard` registered in `onCreate` and unregistered in `onDestroy`.

## Decisions

- Host stays a `DrawerActivity`. The credential flow moves into the
  fragment.
- To avoid copying `FileActivity` logic, `ui/helpers/CredentialsUpdateHelper.kt` was
  extracted. `FileActivity.performCredentialsUpdate`, `CheckRemoteWipeTask` and the fragment
  all use it.

## Resulting files

| File | Role |
|---|---|
| `ui/fragment/uploadList/UploadListFragment.kt` | lifecycle, list setup, receiver, warning card, `EXTRA_USER` |
| `ui/fragment/uploadList/UploadListMenuProvider.kt` | global pause toggle |
| `ui/fragment/uploadList/UploadListConflictHandler.kt` | sync-conflict check and retry |
| `ui/fragment/uploadList/UploadListCredentialsHandler.kt` | check credentials → remote wipe → re-login |
| `ui/helpers/CredentialsUpdateHelper.kt` | credential steps shared with `FileActivity` |
| `res/layout/fragment_upload_list.xml` | old layout without `DrawerLayout`/toolbar/drawer |

## Collaborator changes

- `UploadListAdapter(activity: FileActivity)` → `DrawerActivity`.
  `fileOperationsHelper.checkCurrentCredentials(user)` → `itemOnClick.onCredentialErrorClick(user)`.
- `UploadListAdapterHelper(activity: FileActivity)` → `DrawerActivity`;
  `activity.accountManager` → `activity.userAccountManager`.
- `UploadListItemOnClick` gained `onCredentialErrorClick(user: User)`.

## Entry point changes

```kotlin
// FileUploaderIntents
val intent = NavigatorActivity.intent(context, NavigatorScreen.UploadList).apply {
    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    putExtra(FileActivity.EXTRA_USER, operation?.user)
}

// AppWideNotificationManager, SyncConflictNotificationBroadcastReceiver
val intent = NavigatorActivity.intent(context, NavigatorScreen.UploadList).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
}
```

```java
// DrawerActivity.onNavigationItemClicked
} else if (itemId == R.id.nav_uploads) {
    resetOnlyPersonalAndOnDevice();
    pushFragment(NavigatorScreen.UploadList.INSTANCE);
```

## Cleanup

- `ComponentsModule`: `uploadListActivity()` removed, `uploadListFragment()` added.
- Manifest entry removed. `UploadListActivity.kt` and `upload_list_layout.xml` deleted.
- `FileUploadEventBroadcaster` KDoc links updated.
- `UploadListActivityActivityIT` → `UploadListFragmentIT`, with screenshots `git mv`'d.
