<!--
  ~ Nextcloud - Android Client
  ~
  ~ SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
  ~ SPDX-License-Identifier: AGPL-3.0-or-later
-->

# Templates

## Worker

```kotlin
companion object {
    const val PROGRESS_PERCENT = "PROGRESS_PERCENT"
}

// suspend context
setProgress(workDataOf(PROGRESS_PERCENT to percent))

// non-suspend callback, inside the existing throttle
if (percent != lastPercent && (currentTime - lastUpdateTime) >= minProgressUpdateInterval) {
    notificationManager.updateDownloadProgress(percent, totalToTransfer)
    setProgressAsync(workDataOf(PROGRESS_PERCENT to percent))
    lastUpdateTime = currentTime
}
```

## Work name (BackgroundJobManagerImpl companion)

```kotlin
fun formatFileDownloadTag(accountName: String, fileId: Long): String =
    JOB_FOLDER_DOWNLOAD + accountName + fileId
```

Use it in `startXJob` (`enqueueUniqueWork(tag, …)`), in `cancelXJob`, and in the ViewModel.

## ViewModel

```kotlin
class FileDetailFragmentViewModel @Inject constructor(private val workManager: WorkManager) : ViewModel() {

    private val downloadWorkName = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val downloadProgress: Flow<Int> = downloadWorkName
        .filterNotNull()
        .flatMapLatest { workManager.getWorkInfosForUniqueWorkFlow(it) }
        .mapNotNull { workInfos -> workInfos.firstOrNull { it.state == WorkInfo.State.RUNNING }?.downloadPercent() }
        .distinctUntilChanged()

    fun observeDownloadProgress(user: User, file: OCFile) {
        downloadWorkName.value = BackgroundJobManagerImpl.formatFileDownloadTag(user.accountName, file.fileId)
    }

    private fun WorkInfo.downloadPercent(): Int? = progress.keyValueMap[FileDownloadWorker.PROGRESS_PERCENT] as? Int
}
```

State-only variant (no progress published), from `FileDisplayActivityViewModel`:

```kotlin
suspend fun observeOfflineWorker(onComplete: () -> Unit) {
    workManager
        .getWorkInfosByTagFlow(BackgroundJobManagerImpl.formatClassTag(OfflineOperationsWorker::class))
        .collect { workInfos ->
            if (workInfos.any { it.state == WorkInfo.State.SUCCEEDED }) {
                onComplete()
            }
        }
}
```

Prefer exposing a `Flow` property over a `suspend fun` with a callback: the consumer decides
the lifecycle, and Java can collect it through `collectWhenStarted`.

## DI (`di/ViewModelModule.kt`)

```kotlin
@Binds
@IntoMap
@ViewModelKey(FileDetailFragmentViewModel::class)
abstract fun fileDetailFragmentViewModel(vm: FileDetailFragmentViewModel): ViewModel
```

## Kotlin consumer (fragment)

```kotlin
@Inject lateinit var vmFactory: ViewModelFactory
private val viewModel by viewModels<FileDetailFragmentViewModel> { vmFactory }

override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    viewModel.observeDownloadProgress(user, file)
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.downloadProgress.collect(::onDownloadProgress)
        }
    }
}
```

## Java consumer (fragment)

```java
@Inject ViewModelFactory vmFactory;
private FileDetailFragmentViewModel viewModel;

@Override
public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = new ViewModelProvider(this, vmFactory).get(FileDetailFragmentViewModel.class);
}

@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    viewModel.observeDownloadProgress(user, getFile());
    LifecycleOwnerExtensionsKt.collectWhenStarted(getViewLifecycleOwner(),
                                                  viewModel.getDownloadProgress(),
                                                  this::onDownloadProgress);
}

private void onDownloadProgress(int percent) { … }
```

`collectWhenStarted` (`utils/extensions/LifecycleOwnerExtensions.kt`) wraps
`lifecycleScope.launch { repeatOnLifecycle(STARTED) { flow.collect(collector::accept) } }`
and takes an `androidx.core.util.Consumer`, so a Java method reference fits.
