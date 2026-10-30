/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Philipp Hasper <vcs@hasper.info>
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023-2026 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-FileCopyrightText: 2023 Archontis E. Kostis <arxontisk02@gmail.com>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018-2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2012-2013 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2011 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.activity

import android.accounts.AuthenticatorException
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.android.common.core.utils.ecosystem.AccountReceiverCallback
import com.nextcloud.appReview.InAppReviewHelper
import com.nextcloud.client.account.User
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.database.entity.SyncedFolderEntity
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.di.ViewModelFactory
import com.nextcloud.client.editimage.EditImageActivity
import com.nextcloud.client.files.DeepLinkHandler
import com.nextcloud.client.jobs.download.FileDownloadEventBroadcaster
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.jobs.upload.FileUploadEventBroadcaster
import com.nextcloud.client.player.model.file.toPlaybackCollection
import com.nextcloud.client.player.ui.audio.AudioPlayerLauncher
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.utils.IntentUtil
import com.nextcloud.ui.composeActivity.ComposeProcessTextAlias
import com.nextcloud.utils.SnackbarUtil
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getSerializableArgument
import com.nextcloud.utils.extensions.isActive
import com.nextcloud.utils.extensions.lastFragment
import com.nextcloud.utils.extensions.navigateToAllFiles
import com.nextcloud.utils.extensions.setVisibleIf
import com.nextcloud.utils.view.FastScrollUtils
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.authentication.PassCodeManager
import com.owncloud.android.databinding.FilesBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.VirtualFolderType
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.albums.CreateNewAlbumRemoteOperation
import com.owncloud.android.lib.resources.albums.PublicShareLinkAlbumRemoteOperation
import com.owncloud.android.lib.resources.albums.RemoveAlbumRemoteOperation
import com.owncloud.android.lib.resources.albums.RenameAlbumRemoteOperation
import com.owncloud.android.lib.resources.files.RestoreFileVersionRemoteOperation
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.lib.resources.notifications.GetNotificationsRemoteOperation
import com.owncloud.android.operations.CopyFileOperation
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.DownloadType
import com.owncloud.android.operations.FolderRefreshScheduler
import com.owncloud.android.operations.MoveFileOperation
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.operations.RenameFileOperation
import com.owncloud.android.operations.SynchronizeFileOperation
import com.owncloud.android.operations.albums.CopyFileToAlbumOperation
import com.owncloud.android.syncadapter.FileSyncAdapter
import com.owncloud.android.ui.CompletionCallback
import com.owncloud.android.ui.activity.filedisplayactivity.DownloadServiceConnection
import com.owncloud.android.ui.activity.filedisplayactivity.FileDisplayActivityViewModel
import com.owncloud.android.ui.activity.filedisplayactivity.FileDisplayDialogs
import com.owncloud.android.ui.activity.filedisplayactivity.FileDisplayUploadHandler
import com.owncloud.android.ui.activity.filedisplayactivity.FileDownloadIndicator
import com.owncloud.android.ui.activity.filedisplayactivity.LocalIntentReceiver
import com.owncloud.android.ui.asynctasks.FetchRemoteFileTask
import com.owncloud.android.ui.asynctasks.GetRemoteFileTask
import com.owncloud.android.ui.dialog.DeleteBatchTracker
import com.owncloud.android.ui.dialog.SendShareDialog.SendShareDialogDownloader
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment.OnSortingOrderListener
import com.owncloud.android.ui.dialog.StoragePermissionDialogFragment
import com.owncloud.android.ui.events.SearchEvent
import com.owncloud.android.ui.events.SyncEventFinished
import com.owncloud.android.ui.events.TokenPushEvent
import com.owncloud.android.ui.fragment.EmptyListState
import com.owncloud.android.ui.fragment.FileDetailFragment
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.fragment.GalleryFragment
import com.owncloud.android.ui.fragment.GalleryFragmentBottomSheetDialog.MediaState
import com.owncloud.android.ui.fragment.GroupfolderListFragment
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.ui.fragment.SearchType
import com.owncloud.android.ui.fragment.SharedListFragment
import com.owncloud.android.ui.fragment.TaskRetainerFragment
import com.owncloud.android.ui.fragment.UnifiedSearchFragment
import com.owncloud.android.ui.fragment.albums.AlbumItemsFragment
import com.owncloud.android.ui.fragment.albums.AlbumOperationListener
import com.owncloud.android.ui.fragment.albums.AlbumsFragment
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.ui.navigation.NavigatorScreen
import com.owncloud.android.ui.navigation.animator.NavigationAnimator
import com.owncloud.android.ui.preview.media.PreviewMediaActivity
import com.owncloud.android.ui.preview.media.PreviewMediaFragment
import com.owncloud.android.ui.preview.pdf.PreviewPdfFragment
import com.owncloud.android.ui.preview.text.PreviewTextFileFragment
import com.owncloud.android.ui.preview.text.PreviewTextFragment
import com.owncloud.android.ui.preview.text.PreviewTextStringFragment
import com.owncloud.android.utils.DataHolderUtil
import com.owncloud.android.utils.ErrorMessageAdapter
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.PermissionUtil.requestNotificationPermission
import com.owncloud.android.utils.PermissionUtil.requestStoragePermissionIfNeeded
import com.owncloud.android.utils.PushUtils
import com.owncloud.android.utils.theme.CapabilityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Main screen, lists the files of the current account.
 */
@Suppress("ComplexCondition", "ForbiddenComment", "LargeClass")
class FileDisplayActivity :
    FileActivity(),
    FileFragment.ContainerActivity,
    OnEnforceableRefreshListener,
    OnSortingOrderListener,
    SendShareDialogDownloader,
    OnFilesRemovedListener,
    Injectable {

    // region Properties
    @Inject
    lateinit var audioPlayerLauncher: AudioPlayerLauncher

    @Inject
    lateinit var localBroadcastManager: LocalBroadcastManager

    @Inject
    lateinit var composeProcessTextAlias: ComposeProcessTextAlias

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var inAppReviewHelper: InAppReviewHelper

    @Inject
    lateinit var fastScrollUtils: FastScrollUtils

    @Inject
    lateinit var asyncRunner: AsyncRunner

    @Inject
    lateinit var syncedFolderProvider: SyncedFolderProvider

    @Inject
    lateinit var passCodeManager: PassCodeManager

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private val viewModel by viewModels<FileDisplayActivityViewModel> { viewModelFactory }

    private lateinit var binding: FilesBinding
    private lateinit var albumOperationListener: AlbumOperationListener
    private lateinit var folderRefreshScheduler: FolderRefreshScheduler

    private val dialogs by lazy { FileDisplayDialogs(this, viewThemeUtils) }
    private val uploadHandler by lazy { FileDisplayUploadHandler(this, connectivityService, fileDataStorageManager) }

    private val syncReceiver = LocalIntentReceiver(::onSyncBroadcastReceived)
    private val uploadCompletedReceiver = LocalIntentReceiver(::onUploadCompleted)
    private val downloadStartedReceiver = LocalIntentReceiver(::onDownloadStarted)
    private val downloadCompletedReceiver = LocalIntentReceiver(::onDownloadCompleted)

    var lastSslUntrustedServerResult: RemoteOperationResult<*>? = null

    private var waitingToPreview: OCFile? = null
    private var waitingToSend: OCFile? = null

    private var syncState: Parcelable = EmptyListState.LOADING
        set(value) {
            field = value
            listOfFilesFragment?.setEmptyListMessage(value)
        }

    private var pendingSyncFolderOperation: Runnable? = null

    private var drawerMenuItemsToShowHide: List<MenuItem> = emptyList()
    private var searchView: SearchView? = null
    private var searchQuery = ""
    private var wasSearchOpen = false

    private var lastDisplayedAccountName = ""

    // The list fragment lists its directory when it is created, so onResume must not list it a second time.
    private var listFragmentJustCreated = false

    // FileDownloadWorker is started from many places, only this file is previewed right after its download.
    private var immediatePreviewFileId = NO_IMMEDIATE_PREVIEW_FILE_ID

    private var isShowingDetailsFromPreview = false

    val deleteBatchTracker = DeleteBatchTracker(onAllDeletesFinished = {
        (leftFragment as? GalleryFragment)?.onRefresh()
    })
    // endregion

    // region Lifecycle
    @SuppressLint("UnsafeIntentLaunch")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log_OC.v(TAG, "onCreate() start")
        // replaces the launch screen theme
        setTheme(R.style.Theme_ownCloud_Toolbar_Drawer)

        super.onCreate(savedInstanceState)
        lastDisplayedAccountName = preferences.lastDisplayedAccountName.orEmpty()
        albumOperationListener = AlbumOperationListener(this)
        folderRefreshScheduler = FolderRefreshScheduler(this)

        intent?.let {
            handleOpenFileIntents(it)
            handleEcosystemAccountIntent(it)
        }

        restoreInstanceState(savedInstanceState)
        inflateLayout()
        setupViews()
        addTaskRetainerFragmentIfMissing()

        if (intent.getBooleanExtra(EXTRA_RETURN_TO_PREVIEW, false)) {
            NavigationAnimator(this).prepareSlideUpEnter(savedInstanceState) { detailsHeaderImage() }
        }

        // needs the inflated layout
        savedInstanceState?.let { showSortListGroup(it.getBoolean(KEY_IS_SORT_GROUP_VISIBLE)) }

        resetInvalidStoragePath()
        observeWorkers()
        registerBackPressCallback()
        setupDrawer(menuItemId)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // the storage permission is requested once the notification permission is answered
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission(this)
        } else {
            requestStoragePermissionIfNeeded(this)
        }

        if (savedInstanceState == null) {
            addInitialListOfFilesFragment()

            if (intent.getBooleanExtra(EXTRA_RETURN_TO_PREVIEW, false)) {
                handleNavigationIntent(intent)
            }
        }

        migrateLegacyInstantUpload()
        showOutdatedServerWarningIfNeeded()
        updateNotificationButtonVisibility()
    }

    public override fun onStart() {
        super.onStart()

        registerReceivers()

        if (SettingsActivity.isBackPressed) {
            Log_OC.d(TAG, "User returned from settings activity, skipping reset content logic")
            return
        }

        initCurrentFileAndAccount()
    }

    override fun onResume() {
        Log_OC.v(TAG, "onResume() start")

        super.onResume()

        val listFragmentJustCreated = this.listFragmentJustCreated
        this.listFragmentJustCreated = false

        folderRefreshScheduler.start()

        restoreSearchStateIfNeeded()
        highlightCurrentNavigationItem()

        if (SettingsActivity.isBackPressed) {
            Log_OC.d(TAG, "User returned from settings activity, skipping reset content logic")
            return
        }

        isFileDisplayActivityResumed = true

        val currentLeftFragment = leftFragment
        if (currentLeftFragment !is OCFileListFragment) {
            if (currentLeftFragment is FileFragment) {
                super.updateActionBarTitleAndHomeButton(currentLeftFragment.file)
            }
            return
        }

        if (isToolbarStyleSearch) {
            setupHomeSearchToolbarWithSortAndListButtons()
        }
        syncCurrentFolder(ignoreETag = true, ignoreFocus = true)
        refreshFileListOnResume(currentLeftFragment, listFragmentJustCreated)

        inAppReviewHelper.showInAppReview(this)
        updateNotificationButtonVisibility()

        Log_OC.v(TAG, "onResume() end")

        Handler(Looper.getMainLooper()).postDelayed({
            isFileDisplayActivityResumed = false
        }, ON_RESUMED_RESET_DELAY)
    }

    override fun onRestart() {
        super.onRestart()
        checkForNewDevVersionNecessary(applicationContext)
    }

    override fun onStop() {
        Log_OC.v(TAG, "onStop()")
        folderRefreshScheduler.stop()
        unregisterReceivers()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putParcelable(KEY_WAITING_TO_PREVIEW, waitingToPreview)
            putParcelable(KEY_SYNC_STATE, syncState)
            putParcelable(KEY_WAITING_TO_SEND, waitingToSend)
            searchView?.let { putBoolean(KEY_IS_SEARCH_OPEN, !it.isIconified) }
            putString(KEY_SEARCH_QUERY, searchQuery)
            putBoolean(KEY_IS_SORT_GROUP_VISIBLE, sortListGroupVisibility())
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        val fragment = supportFragmentManager
            .findFragmentByTag(PermissionUtil.PERMISSION_CHOICE_DIALOG_TAG) as? StoragePermissionDialogFragment
            ?: return
        val dialog = fragment.dialog ?: return
        if (!dialog.isShowing) return

        dialog.dismiss()
        supportFragmentManager.beginTransaction().remove(fragment).commitNowAllowingStateLoss()
        requestStoragePermissionIfNeeded(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return

        pendingSyncFolderOperation?.let {
            pendingSyncFolderOperation = null
            it.run()
        }
    }
    // endregion

    // region Setup
    private fun restoreInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            waitingToPreview = null
            syncState = EmptyListState.LOADING
            waitingToSend = null
            return
        }

        savedInstanceState.run {
            waitingToPreview = getParcelableArgument(KEY_WAITING_TO_PREVIEW, OCFile::class.java)
            syncState = getParcelableArgument(KEY_SYNC_STATE, Parcelable::class.java) ?: EmptyListState.LOADING
            waitingToSend = getParcelableArgument(KEY_WAITING_TO_SEND, OCFile::class.java)
            searchQuery = getString(KEY_SEARCH_QUERY, "")
            wasSearchOpen = getBoolean(KEY_IS_SEARCH_OPEN, false)
        }
    }

    private fun inflateLayout() {
        binding = FilesBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun setupViews() {
        setupHomeSearchToolbarWithSortAndListButtons()
        mMenuButton.setOnClickListener { openDrawer() }
        findViewById<View>(R.id.toolbar).setOnClickListener { openCurrentFolderActions() }
        mSwitchAccountButton.setOnClickListener { showManageAccountsDialog() }
        mNotificationButton.setOnClickListener { pushFragment(NavigatorScreen.Notifications) }
        fastScrollUtils.fixAppBarForFastScroll(binding.appbar.appbar, binding.rootLayout)
        listOfFilesFragment?.resetSearchAttributes()
    }

    private fun addTaskRetainerFragmentIfMissing() {
        val tag = TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT
        if (supportFragmentManager.findFragmentByTag(tag) is TaskRetainerFragment) return

        supportFragmentManager.beginTransaction().add(TaskRetainerFragment(), tag).commit()
    }

    private fun resetInvalidStoragePath() {
        val defaultStoragePath = Environment.getExternalStorageDirectory().absolutePath
        val storagePath = preferences.getStoragePath(defaultStoragePath)
        if (preferences.isStoragePathValid() || File(storagePath).exists()) return

        preferences.setStoragePath(defaultStoragePath)
        preferences.setStoragePathValid()
        MainApp.setStoragePath(defaultStoragePath)
        dialogs.showWrongStoragePath()
    }

    private fun migrateLegacyInstantUpload() {
        if (!preferences.instantPictureUploadEnabled() && !preferences.instantVideoUploadEnabled()) return

        preferences.removeLegacyPreferences()
        dialogs.showSyncedFoldersMigrationInfo { pushFragment(NavigatorScreen.AutoUpload) }
    }

    private fun showOutdatedServerWarningIfNeeded() {
        val currentUser = user.orElse(null) ?: return
        val capability = capabilities.orElse(null) ?: return

        val isOutdated = CapabilityUtils.checkOutdatedWarning(
            resources,
            currentUser.server.version,
            capability.extendedSupport.isTrue,
            capability.hasValidSubscription.isTrue
        )
        if (isOutdated) {
            SnackbarUtil.showServerOutdated(this, Snackbar.LENGTH_LONG)
        }
    }

    private fun updateNotificationButtonVisibility() {
        lifecycleScope.launch {
            val client = clientRepository.getNextcloudClient() ?: run {
                Log_OC.e(TAG, "Could not fetch notifications!")
                return@launch
            }

            val hasNotifications = withContext(Dispatchers.IO) {
                val result = GetNotificationsRemoteOperation().execute(client)
                result.isSuccess && result.resultData?.isEmpty() == false
            }
            mNotificationButton.setVisibleIf(hasNotifications)
        }
    }

    private fun observeWorkers() {
        lifecycleScope.launch {
            viewModel.observeOfflineWorker(onComplete = { refreshCurrentDirectory() })
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.observeFolderDownloadWorker(
                    onFileProgress = ::onFolderFileDownloadProgress,
                    onCompleted = ::updateListOfFilesFragment
                )
            }
        }
    }

    private fun addInitialListOfFilesFragment() {
        val searchEvent = intent.getParcelableArgument(OCFileListFragment.SEARCH_EVENT, SearchEvent::class.java)
        val listOfFiles = OCFileListFragment().apply {
            arguments = Bundle().apply {
                searchEvent?.let { putParcelable(OCFileListFragment.SEARCH_EVENT, it) }
                putBoolean(OCFileListFragment.ARG_ALLOW_CONTEXTUAL_ACTIONS, true)
            }
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.left_fragment_container, listOfFiles, TAG_LIST_OF_FILES)
            .commit()

        if (searchEvent == null) {
            listFragmentJustCreated = true
        }
    }

    private fun initCurrentFileAndAccount() {
        val currentUser = user.orElse(null) ?: run {
            Log_OC.w(TAG, "user is not available, cannot init file")
            return
        }

        val currentStorageManager: FileDataStorageManager = storageManager ?: run {
            Log_OC.w(TAG, "storage manager is null, cannot init file")
            return
        }

        val initialFile = resolveInitialFile(currentStorageManager) ?: run {
            Log_OC.e(TAG, "Could not retrieve root folder – cannot continue")
            return
        }

        setFile(initialFile)

        val accountName = currentUser.accountName
        mSwitchAccountButton.tag = accountName

        avatarGenerator.setAccountAvatar(
            currentUser,
            this,
            resources.getDimension(R.dimen.nav_drawer_menu_avatar_radius),
            mSwitchAccountButton
        )

        if (accountName != lastDisplayedAccountName) {
            composeProcessTextAlias.configure()
            Log_OC.d(TAG, "Initializing Fragments in onAccountChanged..")
            refreshListOfFilesAfterAccountChange()
            if (initialFile.isFolder && searchQuery.isEmpty()) {
                startSyncFolderOperation(initialFile, false)
            }
        } else {
            updateActionBarTitleAndHomeButton(if (initialFile.isFolder) null else initialFile)
        }

        saveLastDisplayedAccountName(accountName)
        EventBus.getDefault().post(TokenPushEvent())
        checkForNewDevVersionNecessary(applicationContext)
    }

    private fun resolveInitialFile(storageManager: FileDataStorageManager): OCFile? =
        file?.let { findStoredFile(it, storageManager) } ?: storageManager.getFileByPath(OCFile.ROOT_PATH)

    private fun findStoredFile(file: OCFile, storageManager: FileDataStorageManager): OCFile? {
        if (!file.isDown || file.lastSyncDateForProperties != 0L) {
            return storageManager.getFileByPath(file.remotePath)
        }

        val parentPathLength = file.remotePath.lastIndexOf(file.fileName)
        if (parentPathLength <= 0) return null

        val parentPath = file.remotePath.take(parentPathLength)
        return file.takeIf { storageManager.getFileByPath(parentPath) != null }
    }

    private fun saveLastDisplayedAccountName(accountName: String) {
        preferences.lastDisplayedAccountName = accountName
        lastDisplayedAccountName = accountName
    }

    private fun refreshListOfFilesAfterAccountChange() {
        val listOfFiles = listOfFilesFragment
        if (listOfFiles != null && searchQuery.isEmpty()) {
            if (!listFragmentJustCreated) {
                listOfFiles.listDirectory(getCurrentDir(), file, MainApp.isOnlyOnDevice())
            }
            listFragmentJustCreated = true
        } else {
            Log_OC.e(TAG, "Still have a chance to lose the initialization of list fragment >(")
        }

        resetScrollingAndUpdateActionBar()
    }

    private fun restoreSearchStateIfNeeded() {
        val fragment = ocFileListFragment ?: return
        if (fragment.isSearchFragment) {
            fragment.loadSearchState(fragment.arguments)
        }
    }

    private fun highlightCurrentNavigationItem() {
        val itemId = if (isAlbumsFragment || isAlbumItemsFragment) R.id.nav_album else menuItemId
        highlightNavigationViewItem(itemId)
    }

    private fun refreshFileListOnResume(fragment: OCFileListFragment, listFragmentJustCreated: Boolean) {
        val startFile = getFileFromIntent(intent)?.also { file = it }

        val currentSearchView = searchView
        when {
            currentSearchView != null && searchQuery.isNotEmpty() -> currentSearchView.setQuery(searchQuery, false)

            !fragment.isSearchFragment && startFile == null -> {
                if (!listFragmentJustCreated) {
                    fragment.listDirectory(MainApp.isOnlyOnDevice())
                }
                fragment.registerFabListener()
                updateActionBarTitleAndHomeButton(currentDir)
            }

            else -> {
                fragment.listDirectory(startFile, false)
                updateActionBarTitleAndHomeButton(startFile)
            }
        }
    }
    // endregion

    // region Permissions
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PermissionUtil.PERMISSIONS_POST_NOTIFICATIONS -> requestStoragePermissionIfNeeded(this)

            PermissionUtil.PERMISSIONS_EXTERNAL_STORAGE -> if (grantResults.isPermissionGranted()) {
                EventBus.getDefault().post(TokenPushEvent())
            }

            PermissionUtil.PERMISSIONS_CAMERA -> if (grantResults.isPermissionGranted()) {
                withOCFileListFragment { it.directCameraUpload() }
            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    // If request is cancelled, result arrays are empty.
    private fun IntArray.isPermissionGranted(): Boolean = firstOrNull() == PackageManager.PERMISSION_GRANTED
    // endregion

    // region Intents
    @SuppressLint("UnsafeIntentLaunch")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenFileIntents(intent)
        handleNavigationIntent(intent)
        restartIfRequested(intent)
        handleEcosystemAccountIntent(intent)
    }

    @SuppressLint("UnsafeIntentLaunch")
    private fun handleOpenFileIntents(intent: Intent) {
        if (passCodeManager.isPassCodeEnabled() && passCodeManager.isLocked(this)) return

        when (intent.action) {
            Intent.ACTION_VIEW -> handleViewIntent(intent)
            OPEN_FILE -> openFileFromIntent(intent)
        }
    }

    private fun handleNavigationIntent(intent: Intent) {
        val action = intent.action

        when {
            ACTION_DETAILS.equals(action, ignoreCase = true) -> {
                val detailsFile = getFileFromIntent(intent)
                setFile(detailsFile)
                showDetails(detailsFile)
                isShowingDetailsFromPreview = intent.getBooleanExtra(EXTRA_RETURN_TO_PREVIEW, false)
            }

            Intent.ACTION_SEARCH == action -> handleSearchIntent(intent)

            ALL_FILES == action -> switchToAllFiles()

            LIST_GROUPFOLDERS == action -> {
                Log_OC.d(this, "Switch to list groupfolders fragment")
                replaceLeftFragment(GroupfolderListFragment())
                supportFragmentManager.executePendingTransactions()
            }

            ALBUMS == action -> {
                Log_OC.d(this, "Switch to list albums fragment")
                replaceAlbumFragment()
                supportFragmentManager.executePendingTransactions()
            }

            ON_DEVICE == action -> {
                showOrRefreshFileList()
                listOfFilesFragment?.currentSearchType = SearchType.ON_DEVICE
                updateActionBarTitleAndHomeButton(null)
            }
        }
    }

    private fun switchToAllFiles() {
        Log_OC.d(this, "Switch to oc file fragment")

        // Only an exact OCFileListFragment is kept: re-creating it shows an empty screen, while its subclasses
        // must be replaced. Album screens use their own tags, so leftFragment can still be the detached
        // OCFileListFragment while an album screen is on top.
        val currentLeftFragment = leftFragment
        if (currentLeftFragment != null &&
            (currentLeftFragment::class != OCFileListFragment::class || isAlbumsFragment || isAlbumItemsFragment)
        ) {
            replaceLeftFragment(OCFileListFragment())
            supportFragmentManager.executePendingTransactions()
            listFragmentJustCreated = true
        }

        // the onResume() following this intent already lists and re-syncs the current directory
        browseToRoot(performRefresh = false)
    }

    @SuppressLint("UnsafeIntentLaunch")
    private fun restartIfRequested(intent: Intent) {
        if (intent.action != RESTART) return

        finish()
        startActivity(intent)
    }

    private fun handleSearchIntent(intent: Intent) {
        val searchEvent = intent.getParcelableArgument(OCFileListFragment.SEARCH_EVENT, SearchEvent::class.java)
            ?: return

        val searchFragment = when (searchEvent.searchType) {
            SearchRemoteOperation.SearchType.PHOTO_SEARCH -> {
                Log_OC.d(this, "Switch to photo search fragment")
                GalleryFragment()
            }

            SearchRemoteOperation.SearchType.SHARED_FILTER -> {
                Log_OC.d(this, "Switch to shared fragment")
                SharedListFragment()
            }

            else -> {
                Log_OC.d(this, "Switch to oc file search fragment")
                OCFileListFragment()
            }
        }
        replaceLeftFragment(searchFragment.apply { arguments = searchEvent.getBundle() })

        listOfFilesFragment?.run {
            isSearchFragment = true
            updateSearchType(searchEvent)
        }
    }

    private fun openFileFromIntent(intent: Intent) {
        val fileToOpen = getFileFromIntent(intent) ?: run {
            Log_OC.e(TAG, "Can't open file intent, file is null")
            return
        }

        val currentLeftFragment = leftFragment
        if (currentLeftFragment !is OCFileListFragment || currentLeftFragment is GalleryFragment) {
            val fragmentName = currentLeftFragment?.let { it::class.simpleName } ?: "null"
            Log_OC.w(TAG, "Invalid fragment ($fragmentName). Replacing.")
            replaceLeftFragment(OCFileListFragment(), showSortListGroup = false)
        }

        // waits until the fragment is attached
        Handler(Looper.getMainLooper()).post {
            val fragment = supportFragmentManager.findFragmentByTag(TAG_LIST_OF_FILES) as? OCFileListFragment
                ?: return@post
            replaceLeftFragment(fragment)
            fragment.setFileDepth(fileToOpen)
            updateActionBarTitleAndHomeButton(fileToOpen)
            fragment.onItemClicked(fileToOpen)
        }
    }

    private fun getFileFromIntent(intent: Intent?): OCFile? =
        intent.getParcelableArgument(EXTRA_FILE, OCFile::class.java)
            ?: intent?.getStringExtra(EXTRA_FILE_REMOTE_PATH)
                ?.let { fileDataStorageManager.getFileByDecryptedRemotePath(it) }

    private fun handleEcosystemAccountIntent(intent: Intent) {
        ecosystemManager.receiveAccount(
            intent,
            object : AccountReceiverCallback {
                override fun onAccountReceived(accountName: String) {
                    val receivedUser = accountManager.getUser(accountName).orElse(null) ?: run {
                        Log_OC.w(TAG, "user is not present")
                        SnackbarUtil.show(this@FileDisplayActivity, R.string.account_not_found)
                        return
                    }

                    accountClicked(receivedUser)
                }

                override fun onAccountError(reason: String) {
                    Log_OC.w(TAG, "handleEcosystemIntent: $reason")
                }
            }
        )
    }
    // endregion

    // region Open remote file
    private fun handleViewIntent(intent: Intent) {
        SnackbarUtil.show(this, getString(R.string.retrieving_file))

        val accountName = intent.getStringExtra(KEY_ACCOUNT)
        val fileId = intent.getStringExtra(KEY_FILE_ID)
        val filePath = intent.getStringExtra(KEY_FILE_PATH)

        val intentData = intent.data
        if (accountName == null && fileId == null && intentData != null) {
            openDeepLink(intentData)
            return
        }

        val optionalUser = if (accountName == null) user else userAccountManager.getUser(accountName)
        val targetUser = optionalUser.orElse(null) ?: run {
            SnackbarUtil.show(this, getString(R.string.associated_account_not_found))
            return
        }

        when {
            !fileId.isNullOrEmpty() -> openFileById(targetUser, fileId)
            !filePath.isNullOrEmpty() -> openFileByPath(targetUser, filePath)
            else -> accountClicked(targetUser)
        }
    }

    private fun openDeepLink(uri: Uri) {
        val match = DeepLinkHandler(userAccountManager).parseDeepLink(uri)

        when {
            match == null -> handleDeepLink(uri)
            match.users.isEmpty() -> SnackbarUtil.show(this, getString(R.string.associated_account_not_found))
            match.users.size == SINGLE_USER_SIZE -> openFileById(match.users.first(), match.fileId)
            else -> chooseUserAndOpenFile(match.users, match.fileId)
        }
    }

    private fun chooseUserAndOpenFile(users: List<User>, fileId: String) {
        dismissLoadingDialog()
        dialogs.showAccountChooser(users) { chosenUser ->
            openFileById(chosenUser, fileId)
            showLoadingDialog(getString(R.string.retrieving_file))
        }
    }

    private fun openFileById(user: User, fileId: String) {
        setUser(user)

        val capability = capabilities.orElse(null) ?: run {
            Log_OC.e(TAG, "cannot fetch remote file, capabilities is empty")
            return
        }

        val fileStorageManager = storageManager ?: FileDataStorageManager(user, contentResolver)
        val weakContext = WeakReference<Context>(this)
        val task = FetchRemoteFileTask(user, fileId, fileStorageManager, lifecycleScope, capability, weakContext)
        task.run(showFile = { (fetchedFile, message) ->
            fetchedFile?.let { file = it }
            showFile(fetchedFile, message)
        })
    }

    private fun openFileByPath(user: User, filePath: String) {
        setUser(user)

        val fileStorageManager = storageManager ?: FileDataStorageManager(user, contentResolver)
        lifecycleScope.launch {
            val client = clientRepository.getOwncloudClient() ?: run {
                onFileRequestError(null)
                return@launch
            }

            val task = GetRemoteFileTask(this@FileDisplayActivity, filePath, client, fileStorageManager, user)
            asyncRunner.postQuickTask(
                task,
                { result: GetRemoteFileTask.Result -> onFileRequestResult(result) },
                { throwable: Throwable? -> onFileRequestError(throwable) }
            )
        }
    }

    private fun onFileRequestError(throwable: Throwable?) {
        dismissLoadingDialog()
        SnackbarUtil.show(this, getString(R.string.error_retrieving_file))
        Log_OC.e(TAG, "Requesting file from remote failed!", throwable)
    }

    private fun onFileRequestResult(result: GetRemoteFileTask.Result) {
        dismissLoadingDialog()

        file = result.file

        val fileFragment = OCFileListFragment()
        replaceLeftFragment(fileFragment)

        supportFragmentManager.executePendingTransactions()

        fileFragment.onItemClicked(result.file)
    }

    fun showFile(selectedFile: OCFile?, message: String?) {
        withOCFileListFragment { fragment ->
            dismissLoadingDialog()

            if (message.isNullOrEmpty()) {
                val current = getCurrentDir()
                fragment.listDirectory(current, file, MainApp.isOnlyOnDevice())
                file = current
                updateActionBarTitleAndHomeButton(null)
            } else {
                fragment.view?.let { SnackbarUtil.show(it, message) }
            }

            selectedFile?.let(fragment::onItemClicked)
        }
    }
    // endregion

    // region Fragment navigation
    val leftFragment: Fragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_LIST_OF_FILES)

    val listOfFilesFragment: OCFileListFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_LIST_OF_FILES) as? OCFileListFragment ?: run {
            Log_OC.e(TAG, "Access to unexisting list of files fragment")
            null
        }

    private val topFileListFragment: OCFileListFragment?
        get() = lastFragment() as? OCFileListFragment ?: listOfFilesFragment

    /**
     * Determines which navigation drawer item should be selected.
     *
     * Resolution order:
     * 1) Global app state (static flags in MainApp nav_personal and nav_on_device)
     * 2) Currently visible fragment (and its active child)
     * 3) Fallback to All Files
     */
    override fun getMenuItemId(): Int = MainApp.getMenuItemId() ?: listOfFilesFragment?.menuItemId ?: R.id.nav_all_files

    override fun setFile(file: OCFile?) {
        super.setFile(file)
        file?.takeIf { it.isFolder }?.let { viewModel.setCurrentFolder(it.fileId) }
    }

    private fun replaceLeftFragment(fragment: Fragment, showSortListGroup: Boolean = true) {
        isShowingDetailsFromPreview = false
        prepareFragmentBeforeCommit(showSortListGroup)
        commitFragment(fragment)
    }

    private fun prepareFragmentBeforeCommit(showSortListGroup: Boolean) {
        searchView?.post { searchView?.setQuery(searchQuery, true) }
        setDrawerIndicatorEnabled(false)

        // the media screen sets a subtitle that must not stay on other screens
        clearToolbarSubtitle()

        showSortListGroup(showSortListGroup)
    }

    private fun commitFragment(fragment: Fragment): Boolean {
        val fragmentManager = supportFragmentManager
        if (!isActive() || fragmentManager.isDestroyed || fragmentManager.isStateSaved) {
            Log_OC.d(TAG, "${fragment.javaClass.simpleName} not committed, skipping")
            return false
        }

        fragmentManager.beginTransaction()
            .addToBackStack(null)
            .replace(R.id.left_fragment_container, fragment, TAG_LIST_OF_FILES)
            .commit()

        Log_OC.d(TAG, "${fragment.javaClass.simpleName} committed, pending transaction")

        return true
    }

    private fun withOCFileListFragment(onFragmentReady: (OCFileListFragment) -> Unit) {
        val currentLeftFragment = leftFragment
        if (currentLeftFragment is OCFileListFragment) {
            onFragmentReady(currentLeftFragment)
            return
        }

        val listOfFiles = OCFileListFragment().apply {
            arguments = Bundle().apply { putBoolean(OCFileListFragment.ARG_ALLOW_CONTEXTUAL_ACTIONS, true) }
        }

        runOnUiThread {
            val fragmentManager = supportFragmentManager
            if (fragmentManager.isStateSaved || fragmentManager.isDestroyed) return@runOnUiThread

            prepareFragmentBeforeCommit(true)
            if (commitFragment(listOfFiles)) {
                fragmentManager.executePendingTransactions()
                onFragmentReady(listOfFiles)
            }
        }
    }

    fun showFileActions(file: OCFile?) {
        dismissLoadingDialog()
        withOCFileListFragment { fragment ->
            browseUp(fragment)
            file?.let { fragment.onOverflowIconClicked(it, null) }
        }
    }

    override fun showFiles(onDeviceOnly: Boolean, personalFiles: Boolean) {
        super.showFiles(onDeviceOnly, personalFiles)
        showOrRefreshFileList()
    }

    private fun showOrRefreshFileList() {
        val fileList = listOfFilesFragment
        if (fileList != null &&
            fileList !is GalleryFragment &&
            fileList !is SharedListFragment &&
            // needed to show the offline files screen when navigating from Albums to Offline Files
            !isAlbumsFragment &&
            !isAlbumItemsFragment
        ) {
            fileList.refreshDirectory()
        } else {
            replaceLeftFragment(OCFileListFragment())
        }
    }

    fun browseToRoot(performRefresh: Boolean = true) {
        listOfFilesFragment?.let {
            val root = storageManager.getFileByPath(OCFile.ROOT_PATH)
            it.resetSearchAttributes()
            file = root
            if (performRefresh) {
                it.listDirectory(root, MainApp.isOnlyOnDevice())
                startSyncFolderOperation(root, false)
            }
        }

        binding.fabMain.setImageResource(R.drawable.ic_plus)
        resetScrollingAndUpdateActionBar()
    }

    override fun onBrowsedDownTo(directory: OCFile?) {
        file = directory
        resetScrollingAndUpdateActionBar()
        startSyncFolderOperation(directory, ignoreETag = false, syncChangedDownloadedFiles = true)
        startMetadataSyncForCurrentDir()
    }

    private fun browseUp(listOfFiles: OCFileListFragment) {
        listOfFiles.onBrowseUp()
        val currentFile = listOfFiles.currentFile

        file = currentFile

        currentFile?.let {
            listOfFiles.setFabVisible(it.canCreateFileAndFolder())
            listOfFiles.registerFabListener()
        }

        resetScrollingAndUpdateActionBar()
        startMetadataSyncForCurrentDir()
    }

    override fun showDetails(file: OCFile?) {
        showDetails(file, 0)
    }

    override fun showDetails(file: OCFile?, activeTab: Int) {
        val currentUser = user.orElse(null) ?: return

        resetScrolling(true)

        replaceLeftFragment(FileDetailFragment.newInstance(file, currentUser, activeTab), showSortListGroup = false)
        // swap now, so no frame shows the list without its sort row
        supportFragmentManager.executePendingTransactions()
        updateActionBarForFile(file)
    }

    fun performUnifiedSearch(query: String, listOfHiddenFiles: ArrayList<String>?) {
        val unifiedSearchFragment =
            UnifiedSearchFragment.newInstance(query, listOfHiddenFiles, currentDir?.decryptedRemotePath)
        replaceLeftFragment(unifiedSearchFragment, showSortListGroup = false)
    }

    fun refreshCurrentDirectory() {
        val currentDir = getCurrentDir()?.let { storageManager.getFileByDecryptedRemotePath(it.remotePath) }
        topFileListFragment?.listDirectory(currentDir, MainApp.isOnlyOnDevice())
    }

    fun updateListOfFilesFragment() {
        listOfFilesFragment?.listDirectory(MainApp.isOnlyOnDevice())
    }

    fun resetSearchView() {
        listOfFilesFragment?.isSearchFragment = false
    }

    override fun onSortingOrderChosen(selection: FileSortOrder?) {
        listOfFilesFragment?.fileListLayoutManager?.sortFiles(selection)
    }

    override fun onFilesRemoved() {
        refreshCurrentDirectory()
    }
    // endregion

    // region Toolbar and scrolling
    protected fun resetScrollingAndUpdateActionBar() {
        updateActionBarTitleAndHomeButton(file)
        resetScrolling(true)
    }

    /**
     * Prevents content scrolling and toolbar collapse
     */
    @VisibleForTesting
    fun lockScrolling() {
        binding.appbar.appbar.setExpanded(true, false)
        binding.appbar.toolbarFrame.updateLayoutParams<AppBarLayout.LayoutParams> {
            scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
        }
    }

    /**
     * Resets content scrolling and toolbar collapse
     */
    @VisibleForTesting
    fun resetScrolling(expandAppBar: Boolean) {
        binding.appbar.toolbarFrame.updateLayoutParams<AppBarLayout.LayoutParams> {
            scrollFlags =
                AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
        }
        if (expandAppBar) {
            binding.appbar.appbar.setExpanded(true, false)
        }
    }

    public override fun updateActionBarTitleAndHomeButton(chosenFile: OCFile?) {
        super.updateActionBarTitleAndHomeButton(chosenFile ?: file)
        supportInvalidateOptionsMenu()
    }

    override fun isDrawerIndicatorAvailable(): Boolean = isRoot(getCurrentDir())

    fun configureToolbarForPreview(file: OCFile) {
        lockScrolling()
        updateActionBarForFile(file)
    }

    fun setMainFabVisible(visible: Boolean) {
        binding.fabMain.isVisible = visible
    }
    // endregion

    // region Options menu and search
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_file_display, menu)

        menu.findItem(R.id.action_select_all).isVisible = false
        val searchMenuItem = menu.findItem(R.id.action_search)
        searchView = searchMenuItem.actionView as? SearchView
        searchMenuItem.isVisible = false
        mSearchText.setOnClickListener {
            showSearchView()
            searchView?.postDelayed({
                searchView?.isIconified = false
                searchView?.requestFocusFromTouch()
            }, SEARCH_VIEW_FOCUS_DELAY)
        }

        drawerMenuItemsToShowHide = listOf(searchMenuItem)

        searchView?.let { setupSearchView(it) }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val drawerOpen = isDrawerOpen
        drawerMenuItemsToShowHide.forEach { it.isVisible = !drawerOpen }

        menu?.findItem(R.id.action_current_folder_actions)?.isVisible = canShowCurrentFolderActions()

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            if (shouldOpenDrawer()) openDrawer() else navigateBack()
            true
        }

        R.id.action_select_all -> {
            listOfFilesFragment?.selectAllFiles(true)
            true
        }

        R.id.action_current_folder_actions -> {
            openCurrentFolderActions()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    private fun canShowCurrentFolderActions(): Boolean = leftFragment is OCFileListFragment &&
        !isDrawerOpen &&
        !isSearchOpen() &&
        !isRoot(getCurrentDir()) &&
        listOfFilesFragment?.isSearchFragment == false &&
        !isAlbumItemsFragment

    private fun openCurrentFolderActions() {
        if (canShowCurrentFolderActions()) {
            listOfFilesFragment?.openCurrentFolderActionsMenu()
        }
    }

    private fun shouldOpenDrawer(): Boolean = !isDrawerOpen &&
        !isSearchOpen() &&
        isRoot(getCurrentDir()) &&
        leftFragment is OCFileListFragment &&
        !isAlbumItemsFragment

    private fun setupSearchView(view: SearchView) {
        viewThemeUtils.androidx.themeToolbarSearchView(view)

        if (searchQuery.isNotEmpty()) {
            view.post { restoreSearchQuery() }
        }

        view.setOnCloseListener {
            onSearchViewClosed(view)
            true
        }

        view.findViewById<View>(androidx.appcompat.R.id.search_edit_frame)?.let {
            disableDrawerIndicatorWhenVisible(it)
        }
    }

    private fun restoreSearchQuery() {
        searchView?.run {
            isIconified = false
            setQuery(searchQuery, true)
            clearFocus()
        }
    }

    private fun onSearchViewClosed(view: SearchView) {
        if (!view.query.isNullOrEmpty()) {
            view.post { searchView?.setQuery("", true) }
            return
        }

        view.onActionViewCollapsed()
        setDrawerIndicatorEnabled(isDrawerIndicatorAvailable) // order matters
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mDrawerToggle.syncState()

        listOfFilesFragment?.run {
            isSearchFragment = false
            refreshDirectory()
        }
    }

    private fun disableDrawerIndicatorWhenVisible(searchEditFrame: View) {
        searchEditFrame.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            var oldVisibility: Int = UNKNOWN_VISIBILITY

            override fun onGlobalLayout() {
                val currentVisibility = searchEditFrame.visibility
                if (currentVisibility == oldVisibility) return

                if (currentVisibility == View.VISIBLE) {
                    setDrawerIndicatorEnabled(false)
                }
                oldVisibility = currentVisibility
            }
        })
    }

    fun isSearchOpen(): Boolean =
        searchView?.findViewById<View?>(androidx.appcompat.R.id.search_edit_frame)?.isVisible == true

    private fun closeSearch() {
        val currentLeftFragment = leftFragment
        val currentSearchView = searchView ?: return
        if (!isSearchOpen()) return

        currentSearchView.run {
            clearFocus()
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
            setQuery("", false)
            onActionViewCollapsed()
        }

        if (isRoot(getCurrentDir()) && currentLeftFragment is OCFileListFragment) {
            currentLeftFragment.adapter?.let { adapter ->
                currentLeftFragment.performSearch("", adapter.listOfHiddenFiles, true)
            }
            hideSearchView(getCurrentDir())
            setDrawerIndicatorEnabled(isDrawerIndicatorAvailable)
        }

        if (currentLeftFragment is UnifiedSearchFragment) {
            showSortListGroup(false)
            supportFragmentManager.popBackStack()
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery = query
    }
    // endregion

    // region Back navigation
    private fun registerBackPressCallback() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // disabled while navigating, so a nested back press is handled by the system
                    navigateBack(before = { isEnabled = false }, after = { isEnabled = true })
                }
            }
        )
    }

    private fun navigateBack(before: () -> Unit = {}, after: () -> Unit = {}) {
        val currentLeftFragment = leftFragment

        before()
        when {
            isSearchOpen() -> closeSearch()

            isDrawerOpen -> closeDrawer()

            isShowingDetailsFromPreview && currentLeftFragment is FileDetailFragment ->
                NavigationAnimator(this).finishWithSlideDown()

            isAlbumItemsFragment -> popBackStackAndResetUi()

            currentLeftFragment is OCFileListFragment -> navigateBackInFileList(currentLeftFragment)

            else -> popBackStackAndResetUi()
        }
        after()
    }

    private fun navigateBackInFileList(fragment: OCFileListFragment) {
        if (!isRoot(getCurrentDir())) {
            browseUp(fragment)
            return
        }

        if (fragment.shouldNavigateBackToAllFiles()) navigateToAllFiles() else finish()
    }

    private fun popBackStackAndResetUi() {
        binding.fabMain.setImageResource(R.drawable.ic_plus)
        resetScrolling(true)
        showSortListGroup(false)
        showBottomNavigationBar(true)
        supportFragmentManager.popBackStack()
    }
    // endregion

    // region Activity results
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            data != null && requestCode in selectContentFromAppsRequestCodes &&
                resultCode in selectContentFromAppsResultCodes ->
                uploadHandler.uploadContentFromApps(
                    data,
                    resultCode,
                    autoRename = requestCode == REQUEST_CODE__SELECT_CONTENT_FROM_APPS_AUTO_RENAME
                )

            data != null && requestCode == REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM &&
                resultCode in selectFilesFromFileSystemResultCodes ->
                uploadHandler.uploadFilesFromFileSystem(data, resultCode)

            requestCode in uploadFromCameraRequestCodes && resultCode in uploadFromCameraResultCodes ->
                uploadHandler.uploadFromCamera(isVideo = requestCode == REQUEST_CODE__UPLOAD_FROM_VIDEO_CAMERA)

            requestCode == REQUEST_CODE__MOVE_OR_COPY_FILES && resultCode == RESULT_OK ->
                listOfFilesFragment?.exitSelectionMode()

            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
    // endregion

    // region Broadcast receivers
    private fun registerReceivers() {
        Log_OC.d(TAG, "registering receivers")

        localBroadcastManager.run {
            registerReceiver(
                uploadCompletedReceiver,
                IntentFilter(FileUploadEventBroadcaster.ACTION_UPLOAD_COMPLETED)
            )
            registerReceiver(
                downloadStartedReceiver,
                IntentFilter(FileDownloadEventBroadcaster.ACTION_DOWNLOAD_ENQUEUED)
            )
            registerReceiver(
                downloadCompletedReceiver,
                IntentFilter(FileDownloadEventBroadcaster.ACTION_DOWNLOAD_COMPLETED)
            )

            val syncIntentFilter = IntentFilter(FileSyncAdapter.EVENT_FULL_SYNC_START).apply {
                addAction(FileSyncAdapter.EVENT_FULL_SYNC_END)
                addAction(FileSyncAdapter.EVENT_FULL_SYNC_FOLDER_CONTENTS_SYNCED)
                addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED)
                addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED)
            }
            registerReceiver(syncReceiver, syncIntentFilter)
        }
    }

    private fun unregisterReceivers() {
        Log_OC.d(TAG, "unregistering receivers")

        localBroadcastManager.run {
            unregisterReceiver(syncReceiver)
            unregisterReceiver(uploadCompletedReceiver)
            unregisterReceiver(downloadStartedReceiver)
            unregisterReceiver(downloadCompletedReceiver)
        }
    }

    private fun isCurrentAccount(accountName: String?): Boolean = accountName != null && accountName == account?.name

    private fun isInCurrentDir(remotePath: String?): Boolean {
        val currentDirRemotePath = getCurrentDir()?.remotePath ?: return false
        return remotePath?.startsWith(currentDirRemotePath) == true
    }
    // endregion

    // region Folder sync

    /**
     * Starts a "light sync" of the given folder: properties of its files (including shares) are updated, but only
     * the contents of files marked to be kept in sync are downloaded.
     *
     * @param ignoreETag  fetches the folder even if its eTag didn't change
     * @param ignoreFocus reloads the file list even without window focus, e.g. while a detail view has the focus
     */
    @JvmOverloads
    fun startSyncFolderOperation(
        folder: OCFile?,
        ignoreETag: Boolean,
        ignoreFocus: Boolean = false,
        syncChangedDownloadedFiles: Boolean = false
    ) {
        Log_OC.d(TAG, "startSyncFolderOperation called, ignoreEtag: $ignoreETag, ignoreFocus: $ignoreFocus")

        if (searchQuery.isNotEmpty() || user.isEmpty) return

        val syncFolder = Runnable { executeSyncFolderOperation(folder, ignoreETag, syncChangedDownloadedFiles) }

        // The refresh must not run while another window floats over the activity, e.g. a dialog that is being
        // dismissed or a rotation. Rather than waiting a fixed delay run right away when it already has focus
        // and replay the request on the next focus gain.
        if (ignoreFocus || hasWindowFocus()) {
            pendingSyncFolderOperation = null
            syncFolder.run()
        } else {
            pendingSyncFolderOperation = syncFolder
        }
    }

    private fun executeSyncFolderOperation(folder: OCFile?, ignoreETag: Boolean, syncChangedDownloadedFiles: Boolean) {
        val folder = folder ?: return
        val currentUser = user.orElse(null) ?: return

        syncState = EmptyListState.LOADING

        RefreshFolderOperation(
            folder,
            System.currentTimeMillis(),
            false,
            ignoreETag,
            storageManager,
            currentUser,
            applicationContext,
            syncChangedDownloadedFiles
        ).execute(
            account,
            this,
            { _, _ -> onSyncFinished() },
            handler,
            null
        )

        fetchRecommendedFilesIfNeeded(ignoreETag, folder)
    }

    private fun syncCurrentFolder(ignoreETag: Boolean, ignoreFocus: Boolean) {
        val listOfFiles = listOfFilesFragment ?: return
        if (listOfFiles.isSearchFragment) return

        val folder = listOfFiles.currentFile ?: return
        startSyncFolderOperation(folder, ignoreETag, ignoreFocus)
    }

    override fun onRefresh(ignoreETag: Boolean) {
        syncCurrentFolder(ignoreETag, ignoreFocus = false)
    }

    override fun onRefresh() {
        syncCurrentFolder(ignoreETag = true, ignoreFocus = false)
    }

    override fun onSavedCertificate() {
        startSyncFolderOperation(getCurrentDir(), false)
    }

    @SuppressLint("VisibleForTests")
    private fun onSyncBroadcastReceived(intent: Intent) {
        try {
            val event = intent.action
            Log_OC.d(TAG, "Received broadcast $event")

            val accountName = intent.getStringExtra(FileSyncAdapter.EXTRA_ACCOUNT_NAME)
            val syncFolderRemotePath = intent.getStringExtra(FileSyncAdapter.EXTRA_FOLDER_PATH)
            val syncResultId = intent.getStringExtra(FileSyncAdapter.EXTRA_RESULT)
            val syncResult = DataHolderUtil.getInstance().retrieve(syncResultId)
            val isSameAccount = isCurrentAccount(accountName) && storageManager != null
            val fileList = listOfFilesFragment

            if (isSameAccount) {
                handleSyncEvent(event, syncFolderRemotePath, syncResultId, fileList, syncResult)
            }

            if (syncResult is RemoteOperationResult<*> &&
                syncResult.code == RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED
            ) {
                lastSslUntrustedServerResult = syncResult
            }
        } catch (_: RuntimeException) {
            deleteSyncResultSafely(intent)
        } finally {
            onSyncFinished()
        }
    }

    // avoid app crashes after changing the serial id of RemoteOperationResult in owncloud library
    // with broadcast notifications pending to process
    private fun deleteSyncResultSafely(intent: Intent) {
        try {
            DataHolderUtil.getInstance().delete(intent.getStringExtra(FileSyncAdapter.EXTRA_RESULT))
        } catch (_: RuntimeException) {
            Log_OC.i(TAG, "Ignoring error deleting data")
        }
    }

    private fun handleSyncEvent(
        event: String?,
        syncFolderRemotePath: String?,
        syncResultId: String?,
        fileList: OCFileListFragment?,
        syncResult: Any?
    ) {
        if (FileSyncAdapter.EVENT_FULL_SYNC_START == event) return

        // Single folder events are only sent when the folder's content or sharees actually changed
        val storedFile = file?.remotePath?.let { storageManager.getFileByPath(it) }
        val currentDir = getCurrentDir()?.remotePath?.let { storageManager.getFileByPath(it) }
        val isRootSynced = OCFile.ROOT_PATH == syncFolderRemotePath

        if (currentDir == null && !isRootSynced) {
            browseToParentOfRemovedFolder(syncFolderRemotePath)
        } else if (currentDir != null) {
            val fileAfterSync = resolveFileAfterSync(storedFile, currentDir)
            fileList?.let { listDirectoryIfSynced(it, currentDir, syncFolderRemotePath) }
            file = fileAfterSync
        }

        if (isRootSynced) {
            startMetadataSyncForRoot()
        }

        handleSyncResult(event, syncResult)
        DataHolderUtil.getInstance().delete(syncResultId)
        fileList?.let { updateScrollingAfterSync(it) }
    }

    private fun resolveFileAfterSync(storedFile: OCFile?, currentDir: OCFile): OCFile? {
        if (storedFile == null && file?.isFolder == false) {
            resetScrollingAndUpdateActionBar()
            return currentDir
        }

        return storedFile
    }

    private fun browseToParentOfRemovedFolder(removedFolderRemotePath: String?) {
        SnackbarUtil.show(this, R.string.sync_current_folder_was_removed, removedFolderRemotePath)
        val fileList = topFileListFragment ?: return
        val parent = fileList.parentFolderFinder
            .getParentOnFirstParentRemoved(removedFolderRemotePath, storageManager) ?: return

        fileList.listDirectory(parent, MainApp.isOnlyOnDevice())
        updateActionBarTitleAndHomeButton(parent)
        file = parent
    }

    private fun listDirectoryIfSynced(fileList: OCFileListFragment, currentDir: OCFile, syncedRemotePath: String?) {
        if (currentDir.remotePath != syncedRemotePath) return

        fileList.listDirectory(currentDir, MainApp.isOnlyOnDevice())
    }

    private fun updateScrollingAfterSync(fileList: OCFileListFragment) {
        if (syncState == EmptyListState.LOADING || fileList.isLoading) return

        if (fileList.isEmpty) {
            lockScrolling()
            return
        }

        resetScrolling(false)
    }

    private fun handleSyncResult(event: String?, syncResult: Any?) {
        if (RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED != event) return
        if (syncResult !is RemoteOperationResult<*>) return

        when {
            syncResult.isSuccess -> hideInfoBox()
            syncResult.isAuthenticationError() -> requestCredentialsUpdate()
            else -> showSyncError(syncResult)
        }
    }

    private fun RemoteOperationResult<*>.isAuthenticationError(): Boolean =
        code == RemoteOperationResult.ResultCode.UNAUTHORIZED || (isException && exception is AuthenticatorException)

    private fun showSyncError(syncResult: RemoteOperationResult<*>) {
        when (syncResult.code) {
            RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED -> showUntrustedCertDialog(syncResult)
            RemoteOperationResult.ResultCode.MAINTENANCE_MODE -> showInfoBox(R.string.maintenance_mode)
            RemoteOperationResult.ResultCode.NO_NETWORK_CONNECTION -> showInfoBox(R.string.offline_mode)
            RemoteOperationResult.ResultCode.HOST_NOT_AVAILABLE -> showInfoBox(R.string.host_not_available)
            RemoteOperationResult.ResultCode.SIGNING_TOS_NEEDED -> dialogs.showTermsOfServiceIfNotShown()
            RemoteOperationResult.ResultCode.OUT_OF_MEMORY -> syncState = EmptyListState.OUT_OF_MEMORY
            else -> Unit
        }
    }

    private fun onSyncFinished() {
        if (syncState != EmptyListState.LOADING) return

        syncState = when {
            MainApp.isOnlyOnDevice() -> EmptyListState.ONLY_ON_DEVICE

            listOfFilesFragment?.searchEvent?.searchType == SearchRemoteOperation.SearchType.FAVORITE_SEARCH ->
                SearchType.FAVORITE_SEARCH

            else -> SearchType.NO_SEARCH
        }
    }

    private fun fetchRecommendedFilesIfNeeded(ignoreETag: Boolean, folder: OCFile?) {
        val capability = capabilities.orElse(null) ?: return
        if (folder?.isRootDirectory == false || capability.recommendations.isFalse) return

        val accountName = user.orElse(null)?.accountName ?: return
        val fileList = listOfFilesFragment
        lifecycleScope.launch {
            val recommendedFiles = withContext(Dispatchers.IO) {
                filesRepository.fetchRecommendedFiles(accountName, ignoreETag, storageManager)
            }
            fileList?.adapter?.updateRecommendedFiles(recommendedFiles)
        }
    }

    private fun startMetadataSyncForRoot() {
        backgroundJobManager.startMetadataSyncJob(OCFile.ROOT_PATH, folderAlreadySynced = true)
    }

    private fun startMetadataSyncForCurrentDir() {
        val currentDirPath = file?.decryptedRemotePath ?: return
        backgroundJobManager.startMetadataSyncJob(currentDirPath)
    }
    // endregion

    // region Uploads
    private fun onUploadCompleted(intent: Intent) {
        Log_OC.d(TAG, "upload finish received broadcast")

        val uploadedRemotePath = intent.getStringExtra(FileUploadEventBroadcaster.EXTRA_REMOTE_PATH)
        val isSameAccount = isCurrentAccount(intent.getStringExtra(FileUploadEventBroadcaster.EXTRA_ACCOUNT_NAME))

        if (isSameAccount && isInCurrentDir(uploadedRemotePath)) {
            updateListOfFilesFragment()
        }

        val uploadSucceeded = intent.getBooleanExtra(FileUploadEventBroadcaster.EXTRA_UPLOAD_RESULT, false)
        val currentFile = file
        val oldRemotePath = intent.getStringExtra(FileUploadEventBroadcaster.EXTRA_OLD_REMOTE_PATH)
        val renamedInUpload = currentFile != null && currentFile.remotePath == oldRemotePath
        val isSameFile = currentFile != null && (currentFile.remotePath == uploadedRemotePath || renamedInUpload)

        val fileDetailFragment = leftFragment as? FileDetailFragment
        if (isSameAccount && isSameFile && fileDetailFragment != null) {
            updateFileDetailsAfterUpload(fileDetailFragment, uploadedRemotePath, uploadSucceeded, renamedInUpload)
        }

        refreshVisibleAlbumScreen()
    }

    private fun updateFileDetailsAfterUpload(
        fileDetailFragment: FileDetailFragment,
        uploadedRemotePath: String?,
        uploadSucceeded: Boolean,
        renamedInUpload: Boolean
    ) {
        if (uploadSucceeded) {
            file = storageManager.getFileByPath(uploadedRemotePath)
        } else {
            // TODO remove upload progress bar after upload failed.
            Log_OC.d(TAG, "Remove upload progress bar after upload failed")
        }

        if (renamedInUpload && !uploadedRemotePath.isNullOrBlank()) {
            val newName = File(uploadedRemotePath).name
            SnackbarUtil.show(this, R.string.filedetails_renamed_in_upload_msg, newName)
        }

        if (uploadSucceeded || file?.fileExists() == true) {
            fileDetailFragment.updateFileDetails(false, true)
        } else {
            onBackPressedDispatcher.onBackPressed()
        }

        if (uploadSucceeded) {
            file?.let { previewUploadedFile(it) }
        }
    }

    private fun previewUploadedFile(uploadedFile: OCFile) {
        when {
            PreviewMediaFragment.canBePreviewed(uploadedFile) -> startImagePreview(uploadedFile, true)
            PreviewTextFileFragment.canBePreviewed(uploadedFile) -> startTextPreview(uploadedFile, true)
        }
    }

    private fun refreshVisibleAlbumScreen() {
        when {
            isAlbumsFragment ->
                (supportFragmentManager.findFragmentByTag(AlbumsFragment.TAG) as? AlbumsFragment)?.refreshAlbums()

            isAlbumItemsFragment ->
                (supportFragmentManager.findFragmentByTag(AlbumItemsFragment.TAG) as? AlbumItemsFragment)
                    ?.refreshData()
        }
    }
    // endregion

    // region Downloads
    fun setFileIDForImmediatePreview(fileId: Long) {
        immediatePreviewFileId = fileId
    }

    private fun onDownloadStarted(intent: Intent) {
        Log_OC.d(TAG, "download worker started")
        getFileOfDownloadEvent(intent)?.let { updateFileDownloadIndicator(FileDownloadIndicator.Downloading, it) }
        requestDownloadOfWaitingPreview()
    }

    private fun onDownloadCompleted(intent: Intent) {
        Log_OC.d(TAG, "file download completed received")
        getFileOfDownloadEvent(intent)?.let { updateFileDownloadIndicator(FileDownloadIndicator.Downloaded, it) }

        if (immediatePreviewFileId == NO_IMMEDIATE_PREVIEW_FILE_ID) {
            Log_OC.d(TAG, "updating ui for file download")
            updateUiAfterDownload(intent)
            return
        }

        Log_OC.d(TAG, "updating ui immediate file preview")

        val downloadedRemotePath = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_REMOTE_PATH)
        val downloadedFile = storageManager.getFileByDecryptedRemotePath(downloadedRemotePath) ?: return
        if (immediatePreviewFileId != downloadedFile.fileId || !downloadedFile.isDown) return

        immediatePreviewFileId = NO_IMMEDIATE_PREVIEW_FILE_ID
        if (PreviewMediaFragment.canBePreviewed(downloadedFile)) {
            startImagePreview(downloadedFile, downloadedFile.isDown)
        } else {
            previewFile(downloadedFile, null)
        }
    }

    private fun updateUiAfterDownload(intent: Intent) {
        val downloadedRemotePath = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_REMOTE_PATH)
        val downloadBehaviour = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_DOWNLOAD_BEHAVIOUR)
        val accountName = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_ACCOUNT_NAME)

        if (isCurrentAccount(accountName) && isInCurrentDir(downloadedRemotePath)) {
            updateListOfFilesFragment()
            val downloadEvent = intent.action
            if (downloadEvent != null && downloadedRemotePath != null) {
                refreshDetailsFragmentIfVisible(
                    downloadEvent,
                    downloadedRemotePath,
                    intent.getBooleanExtra(FileDownloadEventBroadcaster.EXTRA_DOWNLOAD_RESULT, false)
                )
            }
        }

        waitingToSend?.let { previousFile ->
            val refreshedFile = storageManager.getFileByRemoteId(previousFile.remoteId)
            waitingToSend = refreshedFile
            if (refreshedFile?.isDown == true && OCFileListFragment.DOWNLOAD_SEND == downloadBehaviour) {
                val packageName = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_PACKAGE_NAME) ?: return
                val activityName = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_ACTIVITY_NAME) ?: return
                sendDownloadedFile(packageName, activityName)
            }
        }

        waitingToPreview?.let { previousFile ->
            val refreshedFile = storageManager.getFileByRemoteId(previousFile.remoteId)
            waitingToPreview = refreshedFile
            if (refreshedFile?.isDown == true && EditImageActivity.OPEN_IMAGE_EDITOR == downloadBehaviour) {
                startImageEditor(refreshedFile)
            }
        }
    }

    private fun getFileOfDownloadEvent(intent: Intent): OCFile? {
        val remotePath = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_REMOTE_PATH)
        return fileDataStorageManager.getFileByDecryptedRemotePath(remotePath)
    }

    private fun updateFileDownloadIndicator(indicator: FileDownloadIndicator, file: OCFile) {
        ocFileListFragment?.adapter?.updateFileIndicator(indicator.iconId, file)
    }

    private fun onFolderFileDownloadProgress(fileId: Long, previousFileId: Long?) {
        val adapter = ocFileListFragment?.adapter ?: return
        previousFileId?.let { storageManager.getFileById(it) }?.let(adapter::replaceFile)
        adapter.updateFileIndicator(fileId, FileDownloadIndicator.Downloading.iconId)
    }

    protected fun refreshDetailsFragmentIfVisible(
        downloadEvent: String,
        downloadedRemotePath: String,
        success: Boolean
    ) {
        val detailFragment = leftFragment as? FileDetailFragment ?: return
        val fileInFragment = detailFragment.file

        if (fileInFragment != null && downloadedRemotePath != fileInFragment.remotePath) {
            waitingToPreview = null
            return
        }

        when (downloadEvent) {
            FileDownloadEventBroadcaster.ACTION_DOWNLOAD_ENQUEUED -> {
                detailFragment.listenForTransferProgress()
                detailFragment.updateFileDetails(true, false)
            }

            FileDownloadEventBroadcaster.ACTION_DOWNLOAD_COMPLETED -> {
                val isWaitedPreview = waitingToPreview?.remotePath == downloadedRemotePath
                val previewStarted = isWaitedPreview && startPreviewOfWaitingFile(success)
                waitingToPreview = null
                if (!previewStarted) {
                    detailFragment.updateFileDetails(false, success)
                }
            }
        }
    }

    private fun startPreviewOfWaitingFile(downloadSucceeded: Boolean): Boolean {
        if (!downloadSucceeded) return false

        waitingToPreview = waitingToPreview?.let { storageManager.getFileById(it.fileId) }
        val fileToPreview = waitingToPreview ?: return false

        return when {
            MimeTypeUtil.isVideo(fileToPreview) -> {
                startImagePreview(fileToPreview, true)
                true
            }

            canPreviewInAudioPlayer(fileToPreview) -> {
                startAudioPreview(fileToPreview, true, true)
                true
            }

            MimeTypeUtil.isVCard(fileToPreview.mimeType) -> {
                startContactListFragment(fileToPreview)
                true
            }

            PreviewTextFileFragment.canBePreviewed(fileToPreview) -> {
                startTextPreview(fileToPreview, true)
                true
            }

            MimeTypeUtil.isPDF(fileToPreview) -> {
                startPdfPreview(fileToPreview)
                true
            }

            else -> {
                fileOperationsHelper.openFile(fileToPreview)
                false
            }
        }
    }

    private fun requestDownloadOfWaitingPreview() {
        val previousFile = waitingToPreview ?: return
        if (storageManager == null) return

        val refreshedFile = storageManager.getFileById(previousFile.fileId)
        waitingToPreview = refreshedFile
        if (refreshedFile?.isDown == false) {
            downloadWaitingPreview()
        }
    }

    private fun downloadWaitingPreview() {
        val currentUser = user.orElse(null) ?: return
        waitingToPreview?.let {
            FileDownloadHelper.instance().downloadFileIfNotStartedBefore(currentUser, it)
        }
    }

    private fun downloadFileIfNotDownloading(
        file: OCFile,
        downloadBehaviour: String,
        packageName: String,
        activityName: String
    ) {
        val currentUser = user.orElse(null) ?: return
        val downloadHelper = FileDownloadHelper.instance()
        if (downloadHelper.isDownloading(currentUser, file)) return

        downloadHelper.downloadFile(
            currentUser,
            file,
            downloadBehaviour,
            DownloadType.DOWNLOAD,
            activityName,
            packageName,
            null
        )
    }

    override fun newTransferenceServiceConnection(): ServiceConnection = DownloadServiceConnection(this)

    override fun onTransferStateChanged(file: OCFile, downloading: Boolean, uploading: Boolean) {
        updateListOfFilesFragment()
        val fileDetailFragment = leftFragment as? FileDetailFragment ?: return
        if (file != fileDetailFragment.file) return
        val currentUser = user.orElse(null) ?: return

        when {
            downloading || uploading -> fileDetailFragment.updateFileDetails(file, currentUser)
            !file.fileExists() -> resetScrollingAndUpdateActionBar()
            else -> fileDetailFragment.updateFileDetails(false, true)
        }
    }

    fun cancelTransference(file: OCFile) {
        fileOperationsHelper.cancelTransference(file)
        if (waitingToPreview?.remotePath == file.remotePath) {
            waitingToPreview = null
        }
        if (waitingToSend?.remotePath == file.remotePath) {
            waitingToSend = null
        }
        onTransferStateChanged(file, false, false)
    }

    fun cancelTransference(files: MutableCollection<OCFile>) {
        files.forEach { cancelTransference(it) }
    }

    override fun downloadFile(file: OCFile, packageName: String, activityName: String) {
        startDownloadForSending(file, OCFileListFragment.DOWNLOAD_SEND, packageName, activityName)
    }

    /**
     * Downloads the file and shares it with the given app once the download finishes.
     */
    fun startDownloadForSending(file: OCFile, downloadBehaviour: String, packageName: String, activityName: String) {
        waitingToSend = file
        downloadFileIfNotDownloading(file, downloadBehaviour, packageName, activityName)
    }

    private fun sendDownloadedFile(packageName: String, activityName: String) {
        val fileToSend = waitingToSend ?: run {
            Log_OC.e(TAG, "Trying to send a NULL OCFile")
            return
        }

        val sendIntent = IntentUtil.createSendIntent(this, fileToSend).apply {
            component = ComponentName(packageName, activityName)
        }
        val sendTitle = getString(R.string.activity_chooser_send_file_title)
        startActivity(Intent.createChooser(sendIntent, sendTitle))

        waitingToSend = null
    }

    /**
     * Downloads the file and previews or opens it once the download finishes.
     *
     * @param parentFolder folder containing [file]
     */
    fun startDownloadForPreview(file: OCFile, parentFolder: OCFile?) {
        if (!file.isFileEligibleForImmediatePreview) {
            user.orElse(null)?.let {
                replaceLeftFragment(FileDetailFragment.newInstance(file, parentFolder, it), showSortListGroup = false)
            }
        }

        configureToolbarForPreview(file)
        waitingToPreview = file
        downloadWaitingPreview()
        setFile(file)
    }
    // endregion

    // region Remote operations
    override fun onRemoteOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        super.onRemoteOperationFinish(operation, result)

        when (operation) {
            is RemoveFileOperation -> onRemoveFileOperationFinish(operation, result)

            is RenameFileOperation -> onRenameFileOperationFinish(operation, result)

            is SynchronizeFileOperation -> onSynchronizeFileOperationFinish(operation, result)

            is CreateFolderOperation -> onCreateFolderOperationFinish(operation, result)

            is MoveFileOperation -> onMoveFileOperationFinish(operation, result)

            is CopyFileOperation -> onCopyFileOperationFinish(operation, result)

            is RestoreFileVersionRemoteOperation -> onRestoreFileVersionOperationFinish(result)

            is CreateNewAlbumRemoteOperation -> albumOperationListener.onCreateAlbumOperationFinish(operation, result)

            is CopyFileToAlbumOperation -> albumOperationListener.onCopyAlbumFileOperationFinish(operation, result)

            is RenameAlbumRemoteOperation -> albumOperationListener.onRenameAlbumOperationFinish(operation, result)

            is RemoveAlbumRemoteOperation -> albumOperationListener.onRemoveAlbumOperationFinish(operation, result)

            is PublicShareLinkAlbumRemoteOperation ->
                albumOperationListener.onAlbumPublicLinkOperationFinish(operation, result)
        }
    }

    private fun onRemoveFileOperationFinish(operation: RemoveFileOperation, result: RemoteOperationResult<*>) {
        deleteBatchTracker.onSingleDeleteFinished()
        if (!result.isSuccess && result.isSslRecoverableException) {
            showUntrustedCertDialogFor(result)
            return
        }

        if (!result.isSuccess) {
            Log_OC.e(TAG, "deletion failed")
            return
        }

        val removedFile = operation.file
        file?.let { playbackModel.stopPlaying(it) }
        val currentLeftFragment = leftFragment

        val isRemovedFileStillStored = storageManager.fileExists(removedFile.fileId)
        if (currentLeftFragment is FileFragment &&
            !isRemovedFileStillStored &&
            removedFile == currentLeftFragment.file
        ) {
            file = storageManager.getFileById(removedFile.parentId)
            resetScrollingAndUpdateActionBar()
        }

        if (currentLeftFragment is OCFileListFragment) {
            updateFileListAfterRemoval(currentLeftFragment, removedFile, operation.onlyLocalCopy)
        }

        supportInvalidateOptionsMenu()
        fetchRecommendedFilesIfNeeded(ignoreETag = true, currentDir)
    }

    private fun updateFileListAfterRemoval(fragment: OCFileListFragment, removedFile: OCFile, onlyLocalCopy: Boolean) {
        val adapter = fragment.adapter ?: return
        if (onlyLocalCopy) {
            adapter.notifyItemChanged(removedFile)
            return
        }

        adapter.removeFile(removedFile)
        if (adapter.isEmpty) {
            val emptyState = fragment.searchEvent?.toSearchType() ?: SearchType.NO_SEARCH
            fragment.setEmptyListMessage(emptyState)
        }
    }

    override fun onAutoUploadFolderRemoved(
        entities: List<SyncedFolderEntity>,
        filesToRemove: List<OCFile>,
        onlyLocalCopy: Boolean
    ) {
        dialogs.showAutoUploadFolderDeleteConfirmation {
            removeAutoUploadFolders(entities, filesToRemove, onlyLocalCopy)
        }
    }

    private fun removeAutoUploadFolders(
        entities: List<SyncedFolderEntity>,
        filesToRemove: List<OCFile>,
        onlyLocalCopy: Boolean
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            entities.mapNotNull { it.id?.toLong() }.forEach {
                fileUploadHelper.removeEntityFromUploadEntities(it)
                syncedFolderProvider.deleteSyncedFolder(it)
            }

            connectivityService.isNetworkAndServerAvailable { isAvailable ->
                when {
                    isAvailable -> fileOperationsHelper?.removeFiles(filesToRemove, onlyLocalCopy, true)
                    onlyLocalCopy -> fileOperationsHelper?.removeFiles(filesToRemove, true, true)
                    else -> filesToRemove.forEach { fileDataStorageManager.addRemoveFileOfflineOperation(it) }
                }
                onFilesRemoved()
            }
        }
    }

    private fun onRestoreFileVersionOperationFinish(result: RemoteOperationResult<*>) {
        if (!result.isSuccess) {
            SnackbarUtil.show(this, R.string.file_version_restored_error)
            return
        }

        val restoredFile = getFile()

        // the new version is only downloaded when the old one was downloaded
        if (restoredFile?.isDown == true) {
            fileOperationsHelper.removeFiles(listOf(restoredFile), true, true)
            showSyncLoadingDialog(restoredFile.isFolder)
            fileOperationsHelper.syncFileOrFolder(restoredFile)
        }

        val parent = restoredFile?.let { storageManager.getFileById(it.parentId) }
        startSyncFolderOperation(parent, ignoreETag = true, ignoreFocus = true)

        (leftFragment as? FileDetailFragment)?.fileDetailActivitiesFragment?.reload()
    }

    private fun onMoveFileOperationFinish(operation: MoveFileOperation, result: RemoteOperationResult<*>) {
        if (!result.isSuccess) {
            showErrorCauseMessage(result, operation)
        }
    }

    private fun onCopyFileOperationFinish(operation: CopyFileOperation, result: RemoteOperationResult<*>) {
        if (!result.isSuccess) {
            showErrorCauseMessage(result, operation)
            return
        }

        updateListOfFilesFragment()
        refreshGalleryFragmentIfNeeded()
    }

    private fun onRenameFileOperationFinish(operation: RenameFileOperation, result: RemoteOperationResult<*>) {
        val currentUser = user.orElse(null)
        val renamedFile = operation.file
        if (!result.isSuccess || currentUser == null) {
            SnackbarUtil.show(this, ErrorMessageAdapter.getErrorCauseMessage(result, operation, resources))

            if (result.isSslRecoverableException) {
                showUntrustedCertDialogFor(result)
            }
            return
        }

        (leftFragment as? FileFragment)?.let { refreshFileFragmentAfterRename(it, renamedFile, currentUser) }

        val parentDir = storageManager.getFileById(renamedFile.parentId)
        val isRenamedInCurrentDir = parentDir != null && parentDir == currentDir

        // the parent check fails in the Shared/Favorites root, so the root is always refreshed too
        if (currentDir?.isRootDirectory == true || isRenamedInCurrentDir) {
            val fileList = topFileListFragment

            // OCFileSearchTask may still run during rename, so use a single refresh path to avoid
            // flicker/inconsistent filenames.
            if (fileList?.isSearchFragment == true) {
                fileList.restartSearch()
            } else {
                fileList?.adapter?.updateFile(renamedFile)
            }
        }

        refreshGalleryFragmentIfNeeded()
        fetchRecommendedFilesIfNeeded(ignoreETag = true, currentDir)
    }

    private fun refreshFileFragmentAfterRename(fragment: FileFragment, renamedFile: OCFile, user: User) {
        if (fragment.file != renamedFile) return

        when (fragment) {
            is FileDetailFragment -> {
                fragment.updateFileDetails(renamedFile, user)
                showDetails(renamedFile)
            }

            is PreviewTextFileFragment -> {
                fragment.updateFile(renamedFile)
                if (PreviewTextFileFragment.canBePreviewed(renamedFile)) {
                    startTextPreview(renamedFile, true)
                } else {
                    fileOperationsHelper.openFile(renamedFile)
                }
            }
        }
    }

    private fun onSynchronizeFileOperationFinish(
        operation: SynchronizeFileOperation,
        result: RemoteOperationResult<*>
    ) {
        if (!result.isSuccess || !operation.transferWasRequested) return

        val syncedFile = operation.localFile ?: return
        onTransferStateChanged(syncedFile, true, true)
        supportInvalidateOptionsMenu()
        refreshVisibleFileDetails()
    }

    private fun onCreateFolderOperationFinish(operation: CreateFolderOperation, result: RemoteOperationResult<*>) {
        if (!result.isSuccess) {
            if (RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS == result.code) {
                SnackbarUtil.show(this, R.string.folder_already_exists)
            } else {
                showErrorCauseMessage(result, operation)
            }
            return
        }

        val fileList = listOfFilesFragment
        val createdFolder = storageManager.getFileByDecryptedRemotePath(operation.remotePath)
        if (!operation.shouldEncrypt()) {
            createdFolder?.let { fileList?.onItemClicked(it) }
            return
        }

        if (createdFolder == null) {
            Log_OC.e(
                TAG,
                "onCreateFolderOperationFinish(): file not saved after create folder operation, cannot encrypt"
            )
            return
        }
        fileOperationsHelper.toggleEncryption(createdFolder, true)
    }

    private fun refreshVisibleFileDetails() {
        val details = leftFragment as? FileFragment ?: return

        details.file?.let {
            val refreshedFile = storageManager.getFileByPath(it.remotePath)
            if (details is PreviewTextFragment) {
                (details as? PreviewTextFileFragment)?.updateFile(refreshedFile)
            } else {
                showDetails(refreshedFile)
            }
        }
        supportInvalidateOptionsMenu()
    }

    private fun refreshGalleryFragmentIfNeeded() {
        if (topFileListFragment is GalleryFragment) {
            startPhotoSearch(R.id.nav_gallery)
        }
    }

    private fun showUntrustedCertDialogFor(result: RemoteOperationResult<*>) {
        lastSslUntrustedServerResult = result
        showUntrustedCertDialog(result)
    }

    private fun showErrorCauseMessage(result: RemoteOperationResult<*>, operation: RemoteOperation<*>) {
        try {
            SnackbarUtil.show(this, ErrorMessageAdapter.getErrorCauseMessage(result, operation, resources))
        } catch (e: Resources.NotFoundException) {
            Log_OC.e(TAG, "Error while trying to show fail message ", e)
        }
    }
    // endregion

    // region Previews
    fun canPreviewInAudioPlayer(file: OCFile): Boolean = MimeTypeUtil.isAudio(file)

    fun canPreviewInMediaPager(file: OCFile): Boolean =
        PreviewMediaFragment.canBePreviewed(file) || MimeTypeUtil.isVideo(file)

    /**
     * Downloaded and non-downloaded images are handled later in
     * [com.owncloud.android.ui.preview.media.PreviewMediaPagerAdapter.fragmentFor]
     */
    fun previewImageWithSearchContext(
        file: OCFile,
        searchFragment: Boolean,
        currentSearchType: SearchType?,
        sourceView: View?
    ) {
        val type = if (searchFragment) {
            when (currentSearchType) {
                SearchType.FAVORITE_SEARCH -> VirtualFolderType.FAVORITE
                SearchType.GALLERY_SEARCH -> VirtualFolderType.GALLERY
                else -> VirtualFolderType.NONE
            }
        } else {
            null
        }

        val mediaState = if (type == VirtualFolderType.GALLERY) {
            (listOfFilesFragment as? GalleryFragment)?.currentMediaState
        } else {
            null
        }

        startImagePreview(file, true, type, mediaState, sourceView)
    }

    fun previewFile(file: OCFile, setFabVisible: CompletionCallback?) {
        if (!file.isDown) {
            Log_OC.d(TAG, "File is not downloaded, cannot be previewed")
            return
        }

        when {
            MimeTypeUtil.isVCard(file) -> startContactListFragment(file)

            MimeTypeUtil.isPDF(file) -> startPdfPreview(file)

            PreviewTextFileFragment.canBePreviewed(file) -> {
                setFabVisible?.onComplete(false)
                fileOperationsHelper.startSyncForOutdatedFileAndIntent(file, textPreviewIntent(file))
            }

            canPreviewInAudioPlayer(file) -> {
                setFabVisible?.onComplete(false)
                if (file.isDownloading) {
                    fileOperationsHelper.startSyncForOutdatedFileAndIntent(file, audioPreviewIntent(file))
                } else {
                    startAudioPlayer(file)
                }
            }

            else -> fileOperationsHelper.openFile(file)
        }
    }

    private fun textPreviewIntent(file: OCFile?): Intent = Intent().apply {
        putExtra(EXTRA_FILE, file)
        putExtra(TEXT_PREVIEW, true)
    }

    fun startImagePreview(
        file: OCFile,
        showPreview: Boolean,
        type: VirtualFolderType? = null,
        mediaState: MediaState? = null,
        sourceView: View? = null
    ) {
        val intent = imagePreviewIntent(file, type, mediaState) ?: return

        if (showPreview) {
            NavigationAnimator(this).scaleUp(intent, sourceView)
        } else {
            syncFileThenStart(file, intent)
        }
    }

    private fun imagePreviewIntent(file: OCFile, type: VirtualFolderType?, mediaState: MediaState?): Intent? {
        val currentUser = user.orElse(null) ?: run {
            Log_OC.e(TAG, "cannot start image preview")
            return null
        }

        return Intent(this, PreviewMediaActivity::class.java).apply {
            putExtra(EXTRA_FILE, file)
            putExtra(EXTRA_LIVE_PHOTO_FILE, file.livePhotoVideo)
            putExtra(EXTRA_USER, currentUser)
            type?.let { putExtra(PreviewMediaActivity.EXTRA_VIRTUAL_TYPE, it) }
            mediaState?.let { putExtra(PreviewMediaActivity.EXTRA_MEDIA_STATE, it) }
        }
    }

    private fun syncFileThenStart(file: OCFile, intent: Intent) {
        FileOperationsHelper(this, userAccountManager, connectivityService, editorUtils)
            .startSyncForFileAndIntent(file, intent)
    }

    /**
     * Plays the audio file, synchronizing it first when it is not available yet.
     */
    fun startAudioPreview(file: OCFile, showPreview: Boolean, streamMedia: Boolean) {
        if (user.isEmpty) return

        if ((showPreview && file.isDown && !file.isDownloading) || streamMedia) {
            startAudioPlayer(file)
        } else {
            syncFileThenStart(file,audioPreviewIntent(file))
        }
    }

    private fun startAudioPlayer(file: OCFile) {
        val collection = listOfFilesFragment
            ?.takeIf { it.isSearchFragment }
            ?.currentSearchType
            .toPlaybackCollection()
        audioPlayerLauncher.launch(this, file, collection)
    }

    private fun audioPreviewIntent(file: OCFile): Intent = Intent().apply {
        putExtra(EXTRA_FILE, file)
        putExtra(AUDIO_PREVIEW, true)
    }

    fun startTextPreview(file: OCFile, showPreview: Boolean) {
        val currentUser = user.orElse(null) ?: return

        if (showPreview) {
            val fragment = PreviewTextFileFragment.create(currentUser, file, wasSearchOpen, searchQuery)
            replaceLeftFragment(fragment, showSortListGroup = false)
            configureToolbarForPreview(file)
            showBottomNavigationBar(false)
        } else {
            syncFileThenStart(file, textPreviewIntent(file))
        }
    }

    fun startRichWorkspacePreview(folder: OCFile) {
        configureToolbarForPreview(folder)
        val textPreviewFragment = PreviewTextStringFragment().apply {
            arguments = Bundle().apply { putParcelable(EXTRA_FILE, folder) }
        }
        replaceLeftFragment(textPreviewFragment, showSortListGroup = false)
    }

    fun startContactListFragment(file: OCFile) {
        val currentUser = user.orElse(null) ?: return
        ContactsPreferenceActivity.startActivityWithContactsFile(this, currentUser, file)
    }

    fun startPdfPreview(file: OCFile) {
        // prefer third party PDF apps
        if (fileOperationsHelper.canOpenFile(file)) {
            fileOperationsHelper.openFile(file)
            return
        }

        replaceLeftFragment(PreviewPdfFragment.newInstance(file), showSortListGroup = false)
        configureToolbarForPreview(file)
        setMainFabVisible(false)
    }

    /**
     * Opens the image editor, downloading the image first when it is not available locally.
     */
    fun startImageEditor(file: OCFile) {
        if (!file.isDown) {
            waitingToPreview = file
            downloadFileIfNotDownloading(file, EditImageActivity.OPEN_IMAGE_EDITOR, packageName, javaClass.simpleName)
            return
        }

        val editImageIntent = Intent(this, EditImageActivity::class.java).apply {
            putExtra(EditImageActivity.EXTRA_FILE, file)
        }
        startActivity(editImageIntent)
    }

    private fun detailsHeaderImage(): View? =
        previewImageView?.takeIf { it.isShown && it.isLaidOut && it.drawable != null }

    private fun galleryThumbnailOf(file: OCFile): View? = leftFragment?.view
        ?.findViewWithTag<View>(file.fileId)
        ?.takeIf { it.isShown && ViewCompat.getTransitionName(it) == NavigationAnimator.sharedElementName(file) }
    // endregion

    // region EventBus
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessageEvent(event: SearchEvent) {
        when (event.searchType) {
            SearchRemoteOperation.SearchType.PHOTO_SEARCH -> {
                Log_OC.d(this, "Switch to photo search fragment")
                replaceLeftFragment(GalleryFragment())
            }

            SearchRemoteOperation.SearchType.SHARED_FILTER -> {
                Log_OC.d(this, "Switch to Shared fragment")
                replaceLeftFragment(SharedListFragment())
            }

            else -> Unit
        }

        listOfFilesFragment?.updateSearchType(event)
        updateActionBarTitleAndHomeButton(null)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: SyncEventFinished) {
        val intent = event.intent
        val extras = intent.extras ?: return
        val syncedFile = extras.getParcelableArgument(EXTRA_FILE, OCFile::class.java) ?: return

        when {
            intent.getBooleanExtra(TEXT_PREVIEW, false) -> startTextPreview(syncedFile, true)

            intent.getBooleanExtra(AUDIO_PREVIEW, false) -> startAudioPreview(syncedFile, true, true)

            extras.containsKey(PreviewMediaActivity.EXTRA_VIRTUAL_TYPE) -> startImagePreview(
                syncedFile,
                true,
                extras.getSerializableArgument(PreviewMediaActivity.EXTRA_VIRTUAL_TYPE, VirtualFolderType::class.java),
                extras.getSerializableArgument(PreviewMediaActivity.EXTRA_MEDIA_STATE, MediaState::class.java),
                galleryThumbnailOf(syncedFile)
            )

            else -> startImagePreview(syncedFile, true, sourceView = galleryThumbnailOf(syncedFile))
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessageEvent(event: TokenPushEvent) {
        if (preferences.isKeysReInitEnabled()) {
            PushUtils.pushRegistrationToServer(userAccountManager, preferences.getPushToken())
        } else {
            PushUtils.reinitKeys(userAccountManager)
        }
    }
    // endregion

    companion object {
        const val RESTART = "RESTART"
        const val ALL_FILES = "ALL_FILES"
        const val LIST_GROUPFOLDERS = "LIST_GROUPFOLDERS"
        const val ALBUMS = "ALBUMS"
        const val SINGLE_USER_SIZE = 1
        const val OPEN_FILE = "NC_OPEN_FILE"
        const val ON_DEVICE = "ON_DEVICE"

        const val TAG_PUBLIC_LINK = "PUBLIC_LINK"
        const val FTAG_CHOOSER_DIALOG = "CHOOSER_DIALOG"
        const val KEY_FILE_ID = "KEY_FILE_ID"
        const val KEY_FILE_PATH = "KEY_FILE_PATH"
        const val KEY_ACCOUNT = "KEY_ACCOUNT"
        const val KEY_IS_SORT_GROUP_VISIBLE = "KEY_IS_SORT_GROUP_VISIBLE"

        private const val KEY_WAITING_TO_PREVIEW = "WAITING_TO_PREVIEW"
        private const val KEY_SYNC_STATE = "SYNC_STATE"
        private const val KEY_WAITING_TO_SEND = "WAITING_TO_SEND"

        private const val ON_RESUMED_RESET_DELAY = 10000L
        private const val SEARCH_VIEW_FOCUS_DELAY = 100L
        private const val NO_IMMEDIATE_PREVIEW_FILE_ID = -1L
        private const val UNKNOWN_VISIBILITY = -1

        const val ACTION_DETAILS = "com.owncloud.android.ui.activity.action.DETAILS"
        const val EXTRA_RETURN_TO_PREVIEW = "RETURN_TO_PREVIEW"

        val REQUEST_CODE__SELECT_CONTENT_FROM_APPS = REQUEST_CODE__LAST_SHARED + 1

        val REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM = REQUEST_CODE__LAST_SHARED + 2

        val REQUEST_CODE__MOVE_OR_COPY_FILES = REQUEST_CODE__LAST_SHARED + 3

        val REQUEST_CODE__UPLOAD_FROM_CAMERA = REQUEST_CODE__LAST_SHARED + 5

        val REQUEST_CODE__UPLOAD_FROM_VIDEO_CAMERA = REQUEST_CODE__LAST_SHARED + 6

        val REQUEST_CODE__SELECT_CONTENT_FROM_APPS_AUTO_RENAME = REQUEST_CODE__LAST_SHARED + 7

        private val selectContentFromAppsRequestCodes = setOf(
            REQUEST_CODE__SELECT_CONTENT_FROM_APPS,
            REQUEST_CODE__SELECT_CONTENT_FROM_APPS_AUTO_RENAME
        )
        private val selectContentFromAppsResultCodes = setOf(RESULT_OK, UploadFilesActivity.RESULT_OK_AND_MOVE)
        private val selectFilesFromFileSystemResultCodes = setOf(
            RESULT_OK,
            UploadFilesActivity.RESULT_OK_AND_MOVE,
            UploadFilesActivity.RESULT_OK_AND_DO_NOTHING,
            UploadFilesActivity.RESULT_OK_AND_DELETE
        )
        private val uploadFromCameraRequestCodes = setOf(
            REQUEST_CODE__UPLOAD_FROM_CAMERA,
            REQUEST_CODE__UPLOAD_FROM_VIDEO_CAMERA
        )
        private val uploadFromCameraResultCodes = setOf(RESULT_OK, UploadFilesActivity.RESULT_OK_AND_DELETE)

        private val TAG: String = FileDisplayActivity::class.java.simpleName

        const val TAG_LIST_OF_FILES = "LIST_OF_FILES"

        const val TEXT_PREVIEW = "TEXT_PREVIEW"
        const val AUDIO_PREVIEW = "AUDIO_PREVIEW"

        const val KEY_IS_SEARCH_OPEN = "IS_SEARCH_OPEN"
        const val KEY_SEARCH_QUERY = "SEARCH_QUERY"

        fun openFileIntent(context: Context, user: User, file: OCFile): Intent =
            Intent(context, PreviewMediaActivity::class.java).apply {
                putExtra(EXTRA_FILE, file)
                putExtra(EXTRA_USER, user)
            }
    }
}
