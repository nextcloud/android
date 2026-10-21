<!--
  ~ Nextcloud - Android Client
  ~
  ~ SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
  ~ SPDX-License-Identifier: AGPL-3.0-or-later
-->

# Pitfalls

## `LiveData` replays a stale value

The first draft exposed `asLiveData()` so the Java fragment could `observe(...)`. `LiveData`
keeps its last value and stops collecting upstream 5 s after the last observer goes away. If
the download finishes during those 5 s, the cached value is the last running percent, and
the next observer gets it right away. The progress block is shown again for a download that
has already finished. A cold `Flow` collected with `repeatOnLifecycle` asks WorkManager again
on every start and only gets the current state. Use `collectWhenStarted` from Java instead
of `LiveData`.

## Progress disappears when the work finishes

`WorkInfo.progress` is empty as soon as the state leaves `RUNNING`. Do not read progress
from `SUCCEEDED`/`FAILED` infos, and do not treat the missing key as `0`. Use `mapNotNull`
and let the existing "finished" path (broadcast, `updateFileDetails(false, true)`) hide the
UI, as it did before. Put final values in `Result.success(outputData)` if the UI needs them.

## Every `setProgress` is a database write

WorkManager persists progress. Calling it on every transfer chunk writes to the database
hundreds of times per file. Publish only on change and inside the worker's existing
throttle (`minProgressUpdateInterval`).

## Unique work name vs tag

`getWorkInfosForUniqueWorkFlow(name)` matches the `enqueueUniqueWork` name.
`getWorkInfosByTagFlow(tag)` matches `addTag`. In `startFileDownloadJob` the same string
is used for both, but that is not true for every job. Check the enqueue call. Never build the
string by hand in the ViewModel. Make the builder public in the
`BackgroundJobManagerImpl` companion.

## The bus was global; the Flow is not

`FileDownloadProgressEvent` reached `FileDetailFragment` for **any** running download, so
the details of file A could show the progress of file B. The ViewModel is keyed by
account + file id, so it only reacts to its own file. This is the intended change; mention it
in the PR. Check which worker really runs for each subject: folders are downloaded by
`FolderDownloadWorker`, not `FileDownloadWorker`, and never posted the event.

## The subject can change without a new view

`FileDetailFragment.updateFileDetails(OCFile, User)` swaps the file on a live fragment. Call
`viewModel.observeX(...)` there too. Thanks to `flatMapLatest`, the old work's Flow is
cancelled automatically. Guard with `viewModel != null` because the activity can call it on a
fragment that has not reached `onCreate` yet.

## Missing ViewModel binding

Forgetting `@Binds @IntoMap @ViewModelKey(X::class)` in `ViewModelModule` compiles, but
`ViewModelFactory` throws `IllegalArgumentException` when the screen opens.

## Keep other `@Subscribe` methods registered

`FileDetailFragment` still receives `FavoriteEvent` on `EventBus.getDefault()`. Remove only
the registration for the bus you are replacing. Removing the default registration silently
stops the remaining events. The other way round fails too: if the last `@Subscribe` method
goes away but `register(this)` stays, EventBus throws `EventBusException` ("has no public
methods with the @Subscribe annotation") when the screen starts.
