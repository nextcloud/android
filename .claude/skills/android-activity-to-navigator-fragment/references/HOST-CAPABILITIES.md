<!--
  ~ Nextcloud - Android Client
  ~
  ~ SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
  ~ SPDX-License-Identifier: AGPL-3.0-or-later
-->

# What `NavigatorActivity` Provides

`NavigatorActivity` extends `DrawerActivity → ToolbarActivity → BaseActivity`. It does
**not** extend `FileActivity`. Decided to keep it that way: making every
navigator screen a `FileActivity` would bind `OperationsService` for all of them and show
`FileActivity`'s offline info box on screens that never had one.

## Available from the host

- `BaseActivity`: `user`, `setUser(user)`, `capabilities`, `storageManager`,
  `clientRepository`, `userAccountManager`, `account`.
- `ToolbarActivity`: toolbar setup, `showInfoBox`/`hideInfoBox` (only called by
  `FileActivity`), sort and grid toolbar buttons (`R.id.sort_button`,
  `R.id.switch_grid_view_button`).
- `DrawerActivity`: `appPreferences`, `openDrawer`/`closeDrawer`/`isDrawerOpen`,
  `setDrawerIndicatorEnabled`, `pushFragment`, `fetchExternalLinks`.

## Not available: `FileActivity`-only features

| `FileActivity` feature | How to replace it in a fragment |
|---|---|
| `fileOperationsHelper.*` (queues operations on `OperationsService`) | Run the `RemoteOperation`/`SyncOperation` directly in `lifecycleScope` on `Dispatchers.IO` with a client for the right user |
| `onRemoteOperationFinish(op, result)` override | Handle the result inline after the coroutine returns |
| `requestCredentialsUpdate(account)` → `CheckRemoteWipeTask` → `performCredentialsUpdate` | `CredentialsUpdateHelper` (`isRemoteWipeRequested`, `invalidateCredentials`, `createUpdateCredentialsIntent`) + `registerForActivityResult` |
| `onActivityResult(REQUEST_CODE__UPDATE_CREDENTIALS, RESULT_OK)` | the `ActivityResultLauncher` callback |
| `showLoadingDialog` / `dismissLoadingDialog` | `LoadingDialog.newInstance(msg).show(childFragmentManager, TAG)` and `dismissAllowingStateLoss()` |
| `EXTRA_USER` → `setUser` in `onCreate` | fragment reads `activity?.intent` and calls `navigatorActivity?.setUser` |
| `EXTRA_FILE` → `file` | read it in the fragment if it is used, otherwise drop it from the callers |
| `connectivityService`, `backgroundJobManager`, `accountManager` fields | `@Inject lateinit var` in the fragment |

## De-duplication rule

When a fragment needs logic that a base Activity implements privately, **do not copy it**.
Extract the reusable steps into a Kotlin helper (e.g. `ui/helpers/CredentialsUpdateHelper.kt`).
Have the original Activity code call the helper too, so there is one implementation.
Only the orchestration specific to the fragment (coroutines, result launcher, dialog) should
live in the fragment's package.
