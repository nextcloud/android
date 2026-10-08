<!--
  ~ Nextcloud - Android Client
  ~
  ~ SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
  ~ SPDX-License-Identifier: AGPL-3.0-or-later
-->

# Worked Example: `FileDownloadProgressEvent` → `FileDetailFragmentViewModel`

## Inventory findings

- Event: `ui/events/FileDownloadProgressEvent(percent: Int)` on a dedicated bus,
  `ui/events/EventBusFactory.downloadProgressEventBus`. Neither had other users.
- Poster: `FileDownloadWorker.onTransferProgress`, on every transfer chunk, unthrottled, with
  no file information.
- Subscriber: `FileDetailFragment` (Java). It registered the bus in `onStart`, unregistered
  it in `onStop`, and `@Subscribe(threadMode = MAIN) onDownloadProgress` showed the progress
  block with the "downloading" text and the percent.
- Enqueue: `BackgroundJobManagerImpl.startFileDownloadJob` →
  `enqueueUniqueWork(startFileDownloadJobTag(accountName, fileId), KEEP, request)`, and the
  same string as a tag. The builder was `private`.
- Parallel mechanism: `containerActivity.getFileDownloadProgressListener()` /
  `FileDownloadWorker.FileDownloadProgressListener`. The field in `FileActivity` is never
  assigned, so it is always `null`, which is why the bus was added (commit `37df0dca9d`).
  Removed in the same change, since the ViewModel replaces it.
- Folders use `FolderDownloadWorker`, which never posted the event.

## Changes

| File | Change |
|---|---|
| `jobs/BackgroundJobManagerImpl.kt` | `startFileDownloadJobTag` → public companion `formatFileDownloadTag`, used by start and cancel |
| `jobs/download/FileDownloadWorker.kt` | `PROGRESS_PERCENT` key; `setProgressAsync(workDataOf(PROGRESS_PERCENT to percent))` inside the notification throttle; bus post removed |
| `ui/fragment/filedetail/FileDetailFragmentViewModel.kt` | new; `downloadProgress: Flow<Int>` keyed by `formatFileDownloadTag` |
| `di/ViewModelModule.kt` | `@Binds @IntoMap @ViewModelKey(FileDetailFragmentViewModel::class)` |
| `utils/extensions/LifecycleOwnerExtensions.kt` | new; `collectWhenStarted(flow, Consumer)` for Java consumers |
| `ui/fragment/FileDetailFragment.java` | `ViewModelFactory` inject, VM in `onCreate`, `observeDownloadProgress` in `onViewCreated` and `updateFileDetails(file, user)`, `collectWhenStarted` on the view lifecycle, `@Subscribe` → `private void onDownloadProgress(int)`, bus register/unregister removed |
| `ui/events/FileDownloadProgressEvent.kt`, `ui/events/EventBusFactory.kt` | deleted |
| `FileDownloadWorker.FileDownloadProgressListener` and its plumbing | deleted: inner class, worker field, `ComponentsGetter.getFileDownloadProgressListener()`, `FileActivity` field and getter, the two `= null` assignments in `FileDisplayActivity`, the download branches of `FileDetailFragment.listenForTransferProgress`/`leaveTransferProgress`, the `TestActivity` override and the `FileMenuFilterIT` mock |

## Intended behaviour differences

- Progress is shown only for the file the details screen is about, not for whichever file is
  downloading.
- The UI updates at the notification throttle (750 ms) instead of on every chunk.

## Decisions

- **Cold `Flow` + `collectWhenStarted`, not `LiveData`.** `LiveData` replayed a stale
  percent after a finish that happened while the view was stopped (see `PITFALLS.md`).
- **The ViewModel is created in `onCreate`, not `onViewCreated`.** The activity can call
  `updateFileDetails(file, user)` before the view exists, and the ViewModel must survive view
  re-creation from the back stack.
- **The finished state is still handled by the existing broadcast path.**
  `FileDisplayActivity` calls `updateFileDetails(false, true)` on `ACTION_DOWNLOAD_COMPLETED`.
  Moving that to `WorkInfo.State.SUCCEEDED` is a possible follow-up, not part of this change.
