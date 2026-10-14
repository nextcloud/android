---
name: android-eventbus-to-workmanager-flow
description: >
  Use when replacing a GreenRobot EventBus event that a WorkManager worker posts (progress,
  state, completion) with WorkManager's own observation APIs, consumed through a ViewModel
  Flow; when the user mentions "event bus", "EventBus", "EventBusFactory", "@Subscribe",
  "worker progress", "setProgress", "observe worker", "getWorkInfosForUniqueWorkFlow" or
  "listen to the worker through the viewmodel". Covers publishing progress from the worker,
  building the ViewModel Flow keyed by the work name, DI registration, collecting the Flow
  from Kotlin or legacy Java fragments/activities, and deleting the event classes.
license: AGPL-3.0-or-later
SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
SPDX-License-Identifier: AGPL-3.0-or-later
metadata:
  author: Nextcloud Android
  version: "1.0.0"
---

# EventBus → WorkManager Flow via ViewModel

Workers used to tell the UI what they were doing by posting GreenRobot EventBus events
(`EventBusFactory.downloadProgressEventBus.post(FileDownloadProgressEvent(percent))`). That
has three problems: the event is global (every subscriber gets every worker's events,
whatever file it is about), it is lost when nobody is registered, and the subscriber has to
register and unregister by hand in `onStart`/`onStop`.

WorkManager already stores each worker's state and progress and exposes them as `Flow`s. The
UI asks WorkManager about **one specific piece of work**, through a ViewModel, and collects
the Flow with the lifecycle. Official guide:
https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/observe

Converted so far: `FileDownloadProgressEvent` → `FileDetailFragmentViewModel.downloadProgress`
(see `assets/worked-example.md`). The earlier `FileDisplayActivityViewModel.observeOfflineWorker`
is the simplest form of the same pattern (state only, Kotlin consumer).

## The Prime Directive: Behaviour Must Not Change

The screen must show the same thing at the same moments as before. The only intended
differences are the ones the global bus got wrong: a screen now only reacts to the work it is
about. Write them down for the PR description. If you find an unrelated bug, report it
instead of fixing it in the same change.

## Workflow

### Step 0: Inventory (do not edit yet)

1. `git grep -n "<EventName>"` across `app/src`. List every **poster** (usually a worker) and
   every **subscriber** (`@Subscribe fun on…(event: <EventName>)`).
2. If the event lives on a dedicated bus (`EventBusFactory.<x>EventBus`), also grep for the
   bus. When it has no other users, delete it together with the event.
3. For each poster, find how its work is enqueued in `BackgroundJobManagerImpl`: the
   **unique work name** (`enqueueUniqueWork(name, …)`) and the **tags** (`oneTimeRequestBuilder`
   adds `formatNameTag`, `formatClassTag`, `formatUserTag`, plus any custom tag). The UI
   must be able to rebuild exactly one of these from what it already knows (user, file id…).
4. List what each event field carries and what the subscriber does with it. Decide whether
   it is **progress** (while running → `setProgress`), a **result** (at the end →
   `Result.success(outputData)`), or only **state** (`WorkInfo.state`, nothing to publish).
5. Look for older parallel mechanisms the subscriber also uses for the same purpose (bound
   listeners, `LocalBroadcastManager`). If you can prove one is dead (e.g. a field that is
   only ever set to `null`), remove it with all its plumbing, including test doubles in
   `app/src/debug` and mocks in `androidTest`. Otherwise leave it and mention it in the summary.

### Step 1: Make the work addressable

If the unique work name / tag builder is `private` in `BackgroundJobManagerImpl`, move it
into the `companion object` next to `formatNameTag`/`formatClassTag` as a public
`format…Tag(...)` function, and have the enqueue and cancel call sites use it. The UI and
the scheduler must build the name with the same function, never with a copied string.

### Step 2: Publish from the worker

- Add the `Data` key as a `const val` in the worker's `companion object`
  (`const val PROGRESS_PERCENT = "PROGRESS_PERCENT"`).
- From a `suspend` context call `setProgress(workDataOf(KEY to value))`. From a non-suspend
  callback (e.g. `OnDatatransferProgressListener.onTransferProgress`) call
  `setProgressAsync(...)`.
- **Throttle it.** WorkManager writes progress to its database. Publish only when the value
  changed, and reuse an existing throttle (the notification update interval) when the
  worker has one.
- Results go in `Result.success(workDataOf(...))` and are read from `WorkInfo.outputData`.
- Delete the `post(...)` call and the event imports.

### Step 3: ViewModel

One ViewModel per screen, in a package next to the screen
(`ui/fragment/<screen>/<Screen>FragmentViewModel.kt`, mirroring
`ui/activity/filedisplayactivity/FileDisplayActivityViewModel.kt`). Template:
`references/VIEWMODEL-TEMPLATE.md`.

- Constructor-inject `WorkManager` (`@Inject constructor(private val workManager: WorkManager)`).
- Hold the work name the screen is interested in a `MutableStateFlow<String?>`. A setter
  function (`observeDownloadProgress(user, file)`) builds it with the Step 1 function.
- Expose a **cold** `Flow<T>`: `filterNotNull()` → `flatMapLatest { getWorkInfosForUniqueWorkFlow(it) }`
  → map the `WorkInfo` list to the UI value → `distinctUntilChanged()`. Use
  `getWorkInfoByIdFlow(id)` when you have the request id, `getWorkInfosByTagFlow(tag)` when
  the work is not unique.
- Progress is only present while `state == RUNNING`. It is empty after the work finishes, so
  filter on `RUNNING` and drop missing keys (`progress.keyValueMap[KEY] as? Int`).
- Register it in `di/ViewModelModule.kt` with `@Binds @IntoMap @ViewModelKey(X::class)`.
  Without this, `ViewModelFactory` throws at runtime.

### Step 4: Consumer

- Get the ViewModel from `ViewModelFactory`: Kotlin `by viewModels { vmFactory }`, Java
  `new ViewModelProvider(this, vmFactory).get(X.class)` in `onCreate`. Injection of
  `Injectable` fragments happens before `onAttach`, so `vmFactory` is ready in `onCreate`.
- Tell the ViewModel what to watch where the screen learns its subject (`onViewCreated`,
  and every public "show this other file" method such as `updateFileDetails(file, user)`).
- Collect with the **view** lifecycle in fragments:
  - Kotlin: `viewLifecycleOwner.lifecycleScope.launch { viewLifecycleOwner.repeatOnLifecycle(STARTED) { vm.flow.collect { … } } }`
  - Java: `LifecycleOwnerExtensionsKt.collectWhenStarted(getViewLifecycleOwner(), viewModel.getX(), this::onX);`
    (`utils/extensions/LifecycleOwnerExtensions.kt`). Do not switch to `LiveData` for Java
    callers. See `references/PITFALLS.md` for why.
- Turn the old `@Subscribe public void onX(Event e)` into `private void onX(T value)`. Keep
  its body unchanged apart from reading the value directly.
- Remove the `register`/`unregister` lines for the bus. Keep `EventBus.getDefault()` registration
  if the class still has other `@Subscribe` methods.

### Step 5: Delete and verify

1. Delete the event class (and the dedicated bus if it has no users left).
2. `git grep -n "<EventName>\|<busName>"` returns nothing.
3. `./gradlew :app:compileGplayDebugJavaWithJavac spotlessKotlinCheck detekt`, then
   `lintGplayDebug spotbugsGplayDebug`. Fix findings in every file you touched.
4. Manual check (hand it to the developer if you cannot run the app): start the work and
   open the screen, so the value shows up; open the screen first, then start the work; rotate
   while it runs; leave the screen for more than 5 s, let the work finish, come back (no stale
   value); open the screen for a *different* subject while work runs for another one (it must
   not react).

## References

- `references/VIEWMODEL-TEMPLATE.md`: ViewModel, worker and consumer snippets.
- `references/PITFALLS.md`: what went wrong or nearly went wrong.
- `assets/worked-example.md`: `FileDownloadProgressEvent` → `FileDetailFragmentViewModel`.
