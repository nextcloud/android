<!--
  ~ Nextcloud - Android Client
  ~
  ~ SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
  ~ SPDX-License-Identifier: AGPL-3.0-or-later
-->

# Concurrency: Java Threads → Coroutines & `lifecycleScope`

Replace `new Thread`, `AsyncTask`, executors, and `runOnUiThread`/`Handler.post`
callback-passing with structured coroutines scoped to the component lifecycle. This is
behaviour-preserving *and* fixes leaks: `lifecycleScope`/`viewModelScope` cancel
automatically when the owner is destroyed, so no work touches a dead view.

## Choose the Right Scope

| Context | Scope |
|---|---|
| `Fragment` (UI work tied to view) | `viewLifecycleOwner.lifecycleScope` (preferred) or `lifecycleScope` |
| `Activity` | `lifecycleScope` |
| `ViewModel` | `viewModelScope` |
| No lifecycle owner | inject a `CoroutineScope` / use a repository suspend fun |

Use `Dispatchers.IO` for disk/network/DB, `Dispatchers.Main` (or `withContext(Main)`) to
touch views.

## `AsyncTask` → Coroutines

`AsyncTask` is deprecated. Map its callbacks onto a single `launch`:

| `AsyncTask` | Coroutine |
|---|---|
| `onPreExecute()` | code before `withContext(IO)` (runs on Main) |
| `doInBackground()` | `withContext(Dispatchers.IO) { ... }` |
| `onPostExecute(result)` | code after, back on `Dispatchers.Main` |
| `isCancelled()` | `isActive` (or `ensureActive()`) |
| `cancel(true)` | `job.cancel()` |
| the `AsyncTask` instance field | the returned `Job` |

A self-contained task class stops extending `AsyncTask` and holds the lifecycle owner so
its scope cancels with the screen:

```kotlin
// BEFORE: class GallerySearchTask extends AsyncTask<Void, Void, Result> { ... }
class GallerySearchTask(
    private val fragment: GalleryFragment,
    private val user: User,
    private val storageManager: FileDataStorageManager,
    private val endDate: Long,
    private val limit: Int
) {
    fun execute(): Job = fragment.lifecycleScope.launch(Dispatchers.IO) {
        if (!isActive) return@launch
        val context = fragment.context ?: return@launch
        val result = performSearch(context)
        withContext(Dispatchers.Main) {
            fragment.searchCompleted(result.emptySearch, result.lastTimestamp)
        }
    }
}
```

The caller keeps the `Job` and cancels it with the view:

```kotlin
private var photoSearchTask: Job? = null
// ...
photoSearchTask = GallerySearchTask(this, user, storageManager, endDate, limit).execute()

override fun onDestroyView() {
    photoSearchTask?.cancel()
    photoSearchTask = null
}
```

Because `lifecycleScope` cancels on destroy, the old `WeakReference<Fragment>` leak-guard
disappears — a direct `fragment` reference plus `fragment.context ?: return@launch` is
enough. (PR #16908)

## `AsyncTask` Behind a Callback API → `runCatching` + `fold`

When a service method hid an `AsyncTask` that set mutable fields (a boolean success flag,
`errorMessage`, result fields) and dispatched them in `onPostExecute`, keep the public
callback signature but drive it from a coroutine. Return an **immutable** result from the
background function and branch with `fold`:

```kotlin
private data class ActivitiesResult(
    val activities: List<Any>,
    val client: NextcloudClient,
    val lastGiven: Long
)

override fun getActivities(lastGiven: Long, callback: ActivitiesServiceCallback) {
    scope.launch {
        runCatching { withContext(Dispatchers.IO) { fetchActivities(lastGiven) } }
            .fold(
                onSuccess = { (activities, client, updated) ->
                    callback.onLoaded(activities, client, updated)
                },
                onFailure = { callback.onError(it.message ?: "") }
            )
    }
}
```

`runCatching` / `fold` replaces the boolean-success + `errorMessage`-field idiom; the
background function `fetchActivities` throws on failure instead of returning `false`, and
the immutable `ActivitiesResult` destructures straight into `onSuccess`. (PR #16654)

> **Scope caveat:** that PR launched on an ad-hoc `CoroutineScope(Dispatchers.Main)` with
> no lifecycle owner — it never cancels and can leak. Prefer an injected `CoroutineScope`
> or expose a `suspend` function the presenter runs on `lifecycleScope` / `viewModelScope`.
> Flag ad-hoc scopes in review.

## `new Thread { ... runOnUiThread(...) }` → `lifecycleScope.launch` + `withContext`

```kotlin
// BEFORE
private void fetchE2EECounter(Runnable onComplete) {
    new Thread(() -> {
        try {
            OwnCloudClient client = clientFactory.create(user);
            Object metadata = RefreshFolderOperation.getDecryptedFolderMetadata(true, file, client, user, ctx);
            if (metadata instanceof DecryptedFolderMetadataFile m) {
                file.setE2eCounter(m.getMetadata().getCounter());
                fileDataStorageManager.saveFile(file);
            }
        } catch (Exception e) { Log_OC.e(TAG, "..." + e.getMessage()); }
        Activity a = getActivity();
        if (a != null) a.runOnUiThread(onComplete);
    }).start();
}

// AFTER: background work is a suspend fun on IO; the UI reaction runs on Main
private suspend fun fetchE2EECounter(): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
        val context = requireContext()
        val client = clientFactory.create(user)
        val metadata = RefreshFolderOperation.getDecryptedFolderMetadata(true, file, client, user, context)
        if (metadata is DecryptedFolderMetadataFile) {
            file?.setE2eCounter(metadata.metadata.counter)
            fileDataStorageManager?.saveFile(file)
        }
        true
    } catch (e: Exception) {
        Log_OC.e(TAG, "Error refreshing E2E counter: " + e.message)
        false
    }
}

// caller
lifecycleScope.launch {
    val ok = fetchE2EECounter()
    if (!ok) return@launch
    withContext(Dispatchers.Main) { /* update views */ }
}
```

Notes:
- The old code returned nothing and drove the UI via a `Runnable`. The idiomatic version
  returns a `Boolean` result and lets the caller decide — same observable outcome, no
  callback threading.
- `getActivity() != null` guard becomes `lifecycleScope` cancellation + `binding?`/`?:
  return` guards. Preserve any "is the view still alive?" check as a `binding == null`
  guard inside `launch`.

## Callback-Pair API → `suspend` Result

Repositories that took `onSuccess`/`onError` lambdas become `suspend` functions returning
a result, awaited inside a coroutine.

```kotlin
// BEFORE
shareRepository.fetchSharees(remotePath, onSuccess = { ... }, onError = { ... });

// AFTER
lifecycleScope.launch {
    val result = shareRepository.fetchSharees(remotePath)
    if (binding == null) return@launch
    if (result) { refreshSharesFromDB(); stopLoadingAnimationAndShowShareContainer() }
    else { stopLoadingAnimationAndShowShareContainer(); showError() }
}
```

## Moving DB/IO off the Main Thread

Wrap the blocking DB read in `withContext(Dispatchers.IO)` and hop back to `Main` to touch
adapters/views. This can be a *behaviour improvement* (fewer main-thread stalls) — flag it
to the developer if the Java version was doing DB work on the main thread synchronously,
since timing changes can be observable (e.g. tests that assumed synchronous population).

```kotlin
private suspend fun loadAndPartitionShares(): Pair<List<OCShare>, List<OCShare>> =
    withContext(Dispatchers.IO) {
        val shares = fileDataStorageManager?.getSharesWithForAFile(file?.remotePath, user?.accountName)
            ?: emptyList()
        val (external, internal) = shares.filter { it.shareType != null }
            .partition { it.shareType in externalShareTypes }
        internal to external
    }
```

## Pitfalls

- Never launch on `GlobalScope` — it outlives the screen and leaks.
- An ad-hoc `CoroutineScope(Dispatchers.Main)` created inside a service/presenter has the
  same problem: no lifecycle owner, no cancellation, a leak. Use an injected scope or a
  `suspend` function the caller runs on `lifecycleScope` / `viewModelScope`.
- Keep `try/catch` semantics: coroutine cancellation throws `CancellationException`;
  rethrow it (don't swallow in a broad `catch (e: Exception)` that logs and continues) or
  catch `Exception` only around the real work, not around the whole `launch`.
- Preserve exception-to-UI mapping exactly (same snackbar, same log tag/message).
