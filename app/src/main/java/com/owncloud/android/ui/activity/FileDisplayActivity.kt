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
import android.content.BroadcastReceiver
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
import android.os.IBinder
import android.os.Looper
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager.BadTokenException
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.SearchView
import androidx.core.util.Function
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.android.common.core.utils.ecosystem.AccountReceiverCallback
import com.nextcloud.appReview.InAppReviewHelper
import com.nextcloud.client.account.User
import com.nextcloud.client.appinfo.AppInfo
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.core.Clock
import com.nextcloud.client.database.entity.SyncedFolderEntity
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.di.ViewModelFactory
import com.nextcloud.client.editimage.EditImageActivity
import com.nextcloud.client.files.DeepLinkHandler
import com.nextcloud.client.jobs.download.FileDownloadEventBroadcaster
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.jobs.download.FileDownloadWorker
import com.nextcloud.client.jobs.upload.FileUploadEventBroadcaster
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.client.player.model.file.toPlaybackCollection
import com.nextcloud.client.player.ui.audio.AudioPlayerLauncher
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.utils.IntentUtil
import com.nextcloud.model.OCUploadLocalPathData
import com.nextcloud.ui.composeActivity.ComposeProcessTextAlias
import com.nextcloud.utils.SnackbarUtil
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getSerializableArgument
import com.nextcloud.utils.extensions.isActive
import com.nextcloud.utils.extensions.isDialogFragmentReady
import com.nextcloud.utils.extensions.lastFragment
import com.nextcloud.utils.extensions.navigateToAllFiles
import com.nextcloud.utils.extensions.setVisibleIf
import com.nextcloud.utils.fileNameValidator.FileNameValidator.checkFolderPath
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
import com.owncloud.android.ui.activity.filedisplayactivity.FileDisplayActivityViewModel
import com.owncloud.android.ui.asynctasks.CheckAvailableSpaceTask
import com.owncloud.android.ui.asynctasks.CheckAvailableSpaceTask.CheckAvailableSpaceListener
import com.owncloud.android.ui.asynctasks.FetchRemoteFileTask
import com.owncloud.android.ui.asynctasks.GetRemoteFileTask
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.DeleteBatchTracker
import com.owncloud.android.ui.dialog.SendShareDialog.SendShareDialogDownloader
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment.OnSortingOrderListener
import com.owncloud.android.ui.dialog.StoragePermissionDialogFragment
import com.owncloud.android.ui.dialog.TermsOfServiceDialog
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
import com.owncloud.android.ui.helpers.UriUploader
import com.owncloud.android.ui.navigation.NavigatorScreen
import com.owncloud.android.ui.navigation.animator.NavigationAnimator
import com.owncloud.android.ui.preview.media.PreviewMediaActivity
import com.owncloud.android.ui.preview.media.PreviewMediaFragment
import com.owncloud.android.ui.preview.text.PreviewTextFileFragment
import com.owncloud.android.ui.preview.text.PreviewTextFragment
import com.owncloud.android.ui.preview.text.PreviewTextStringFragment
import com.owncloud.android.ui.preview.pdf.PreviewPdfFragment
import com.owncloud.android.utils.DataHolderUtil
import com.owncloud.android.utils.ErrorMessageAdapter
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.PermissionUtil.requestNotificationPermission
import com.owncloud.android.utils.PermissionUtil.requestStoragePermissionIfNeeded
import com.owncloud.android.utils.PushUtils
import com.owncloud.android.utils.StringUtils
import com.owncloud.android.utils.UriUtils
import com.owncloud.android.utils.theme.CapabilityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FilenameUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Displays, what files the user has available in his Nextcloud. This is the main view.
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
    private lateinit var binding: FilesBinding

    private val syncReceiver = SyncReceiver()
    private val fileUploadCompletedReceiver = FileUploadCompletedReceiver()

    private val fileDownloadStartedReceiver = FileDownloadStartedReceiver()
    private val fileDownloadCompletedReceiver = FileDownloadCompletedReceiver()

    private lateinit var albumOperationListener: AlbumOperationListener

    var mLastSslUntrustedServerResult: RemoteOperationResult<*>? = null

    private var waitingToPreview: OCFile? = null

    private var syncState: Parcelable = EmptyListState.LOADING
        set(value) {
            field = value
            listOfFilesFragment?.setEmptyListMessage(value)
        }

    private var pendingSyncFolderOperation: Runnable? = null

    private var waitingToSend: OCFile? = null

    private var drawerMenuItemsToShowHide: List<MenuItem> = emptyList()

    private var searchQuery: String? = ""
    private var searchOpen = false

    private var searchView: SearchView? = null
    private var lastDisplayedAccountName: String? = null

    // needed for first time app launch multiple listing directory call
    // because onActivityCreated causes this. Removing list directory call from onActivityCreated
    // causing also empty state thus this flag is used.
    private var listFragmentJustCreated = false

    @Inject
    lateinit var audioPlayerLauncher: AudioPlayerLauncher

    @Inject
    lateinit var localBroadcastManager: LocalBroadcastManager

    @Inject
    lateinit var composeProcessTextAlias: ComposeProcessTextAlias

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var appInfo: AppInfo

    @Inject
    lateinit var inAppReviewHelper: InAppReviewHelper

    @Inject
    lateinit var fastScrollUtils: FastScrollUtils

    @Inject
    lateinit var asyncRunner: AsyncRunner

    @Inject
    lateinit var clock: Clock

    @Inject
    lateinit var syncedFolderProvider: SyncedFolderProvider

    @Inject
    lateinit var passCodeManager: PassCodeManager

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private val viewModel by viewModels<FileDisplayActivityViewModel> { viewModelFactory }

    /**
     * Indicates whether the downloaded file should be previewed immediately. Since `FileDownloadWorker` can be
     * triggered from multiple sources, this helps determine if an automatic preview is needed after download.
     */
    private var fileIDForImmediatePreview: Long = NO_IMMEDIATE_PREVIEW_FILE_ID

    private var isShowingDetailsFromPreview = false

    private lateinit var folderRefreshScheduler: FolderRefreshScheduler

    override fun setFile(file: OCFile?) {
        super.setFile(file)
        file?.takeIf { it.isFolder }?.let { viewModel.setCurrentFolder(it.fileId) }
    }

    fun setFileIDForImmediatePreview(fileIDForImmediatePreview: Long) {
        this.fileIDForImmediatePreview = fileIDForImmediatePreview
    }

    @SuppressLint("UnsafeIntentLaunch")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log_OC.v(TAG, "onCreate() start")
        // Set the default theme to replace the launch screen theme.
        setTheme(R.style.Theme_ownCloud_Toolbar_Drawer)

        super.onCreate(savedInstanceState)
        lastDisplayedAccountName = preferences.lastDisplayedAccountName
        albumOperationListener = AlbumOperationListener(this)
        folderRefreshScheduler = FolderRefreshScheduler(this)

        intent?.let {
            handleCommonIntents(it)
            handleEcosystemIntent(it)
        }

        loadSavedInstanceState(savedInstanceState)

        // USER INTERFACE
        initLayout()
        initUI()
        initTaskRetainerFragment()

        if (intent.getBooleanExtra(EXTRA_RETURN_TO_PREVIEW, false)) {
            NavigationAnimator(this).prepareSlideUpEnter(savedInstanceState) { detailsHeaderImage() }
        }

        // Restoring after UI has been inflated.
        savedInstanceState?.let { showSortListGroup(it.getBoolean(KEY_IS_SORT_GROUP_VISIBLE)) }

        checkStoragePath()
        observeWorkerState()
        handleBackPress()
        setupDrawer(menuItemId)
    }

    /**
     * Determines which navigation drawer item should be selected.
     *
     * Resolution order:
     * 1) Global app state (static flags in MainApp nav_personal and nav_on_device)
     * 2) Currently visible fragment (and its active child)
     * 3) Fallback to All Files
     */
    override fun getMenuItemId(): Int = MainApp.getMenuItemId() ?: listOfFilesFragment?.menuItemId ?: R.id.nav_all_files

    private fun loadSavedInstanceState(savedInstanceState: Bundle?) {
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
            searchQuery = getString(KEY_SEARCH_QUERY)
            searchOpen = getBoolean(KEY_IS_SEARCH_OPEN, false)
        }
    }

    private fun initLayout() {
        binding = FilesBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun initUI() {
        setupHomeSearchToolbarWithSortAndListButtons()
        mMenuButton.setOnClickListener { openDrawer() }
        findViewById<View>(R.id.toolbar).setOnClickListener { openCurrentFolderActions() }
        mSwitchAccountButton.setOnClickListener { showManageAccountsDialog() }
        mNotificationButton.setOnClickListener { pushFragment(NavigatorScreen.Notifications) }
        fastScrollUtils.fixAppBarForFastScroll(binding.appbar.appbar, binding.rootLayout)

        // reset ui states when file display activity created/recrated
        listOfFilesFragment?.resetSearchAttributes()
    }

    private fun initTaskRetainerFragment() {
        // Init Fragment without UI to retain AsyncTask across configuration changes
        val tag = TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT
        if (supportFragmentManager.findFragmentByTag(tag) is TaskRetainerFragment) return

        supportFragmentManager.beginTransaction().add(TaskRetainerFragment(), tag).commit()
    }

    private fun checkStoragePath() {
        val newStorage = Environment.getExternalStorageDirectory().absolutePath
        val storagePath = preferences.getStoragePath(newStorage)
        if (preferences.isStoragePathValid() || File(storagePath).exists()) return

        // falling back to default
        preferences.setStoragePath(newStorage)
        preferences.setStoragePathValid()
        MainApp.setStoragePath(newStorage)
        showWrongStoragePathDialog()
    }

    private fun showWrongStoragePathDialog() {
        try {
            val builder = MaterialAlertDialogBuilder(this, R.style.Theme_ownCloud_Dialog)
                .setTitle(R.string.wrong_storage_path)
                .setMessage(R.string.wrong_storage_path_desc)
                .setPositiveButton(R.string.dialog_close) { dialog, _ -> dialog?.dismiss() }
                .setIcon(R.drawable.ic_settings)

            viewThemeUtils.dialog.colorMaterialAlertDialogBackground(applicationContext, builder)

            builder.create().show()
        } catch (e: BadTokenException) {
            Log_OC.e(TAG, "Error showing wrong storage info, so skipping it: ${e.message}")
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

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // handle notification permission on API level >= 33
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // request notification permission first and then prompt for storage permissions
            // storage permissions handled in onRequestPermissionsResult
            requestNotificationPermission(this)
        } else {
            requestStoragePermissionIfNeeded(this)
        }

        if (savedInstanceState == null) {
            addInitialListOfFilesFragment()
        }

        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_RETURN_TO_PREVIEW, false)) {
            handleSpecialIntents(intent)
        }

        upgradeNotificationForInstantUpload()
        checkOutdatedServer()
        checkNotifications()
    }

    /**
     * For Android 7+. Opens a pop up info for the new instant upload and disabled the old instant upload.
     */
    private fun upgradeNotificationForInstantUpload() {
        // check for Android 6+ if legacy instant upload is activated --> disable + show info
        if (!preferences.instantPictureUploadEnabled() && !preferences.instantVideoUploadEnabled()) return

        preferences.removeLegacyPreferences()
        MaterialAlertDialogBuilder(this, R.style.Theme_ownCloud_Dialog)
            .setTitle(R.string.drawer_synced_folders)
            .setMessage(R.string.synced_folders_new_info)
            .setPositiveButton(R.string.drawer_open) { dialog, _ ->
                dialog?.dismiss()
                pushFragment(NavigatorScreen.AutoUpload)
            }
            .setNegativeButton(R.string.drawer_close) { dialog, _ -> dialog?.dismiss() }
            .setIcon(R.drawable.nav_synced_folders)
            .show()
    }

    private fun checkOutdatedServer() {
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

    private fun checkNotifications() {
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            // dialogue was dismissed -> prompt for storage permissions
            PermissionUtil.PERMISSIONS_POST_NOTIFICATIONS -> requestStoragePermissionIfNeeded(this)

            PermissionUtil.PERMISSIONS_EXTERNAL_STORAGE -> if (grantResults.isPermissionGranted()) {
                EventBus.getDefault().post(TokenPushEvent())
            }

            PermissionUtil.PERMISSIONS_CAMERA -> if (grantResults.isPermissionGranted()) {
                getOCFileListFragmentFromFile { it.directCameraUpload() }
            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    // If request is cancelled, result arrays are empty.
    private fun IntArray.isPermissionGranted(): Boolean = firstOrNull() == PackageManager.PERMISSION_GRANTED

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

    private fun initFragments() {
        val listOfFiles = listOfFilesFragment
        if (listOfFiles != null && searchQuery.isNullOrEmpty()) {
            if (!listFragmentJustCreated) {
                listOfFiles.listDirectory(getCurrentDir(), file, MainApp.isOnlyOnDevice())
            }
            listFragmentJustCreated = true
        } else {
            Log_OC.e(TAG, "Still have a chance to lose the initialization of list fragment >(")
        }

        resetScrollingAndUpdateActionBar()
    }

    // region Handle Intents
    @SuppressLint("UnsafeIntentLaunch")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCommonIntents(intent)
        handleSpecialIntents(intent)
        handleRestartIntent(intent)
        handleEcosystemIntent(intent)
    }

    private fun handleSpecialIntents(intent: Intent) {
        val action = intent.action

        when {
            ACTION_DETAILS.equals(action, ignoreCase = true) -> {
                val file = getFileFromIntent(intent)
                setFile(file)
                showDetails(file)
                isShowingDetailsFromPreview = intent.getBooleanExtra(EXTRA_RETURN_TO_PREVIEW, false)
            }

            Intent.ACTION_SEARCH == action -> handleSearchIntent(intent)

            ALL_FILES == action -> switchToAllFiles()

            LIST_GROUPFOLDERS == action -> {
                Log_OC.d(this, "Switch to list groupfolders fragment")
                leftFragment = GroupfolderListFragment()
                supportFragmentManager.executePendingTransactions()
            }

            ALBUMS == action -> {
                Log_OC.d(this, "Switch to list albums fragment")
                replaceAlbumFragment()
                supportFragmentManager.executePendingTransactions()
            }

            ON_DEVICE == action -> {
                refreshOrInitOCFileListFragment()
                listOfFilesFragment?.currentSearchType = SearchType.ON_DEVICE
                updateActionBarTitleAndHomeButton(null)
            }
        }
    }

    private fun switchToAllFiles() {
        Log_OC.d(this, "Switch to oc file fragment")

        // Replace only if the fragment is NOT exactly OCFileListFragment
        // Using `is OCFileListFragment` would also match subclasses,
        // its needed because reinitializing OCFileListFragment itself causes an empty screen
        val currentLeftFragment = leftFragment
        // Album screens are committed under their own tags, so leftFragment can still
        // point at the detached OCFileListFragment while an album screen is on top
        if (currentLeftFragment != null &&
            (currentLeftFragment::class != OCFileListFragment::class || isAlbumsFragment || isAlbumItemsFragment)
        ) {
            leftFragment = OCFileListFragment()
            supportFragmentManager.executePendingTransactions()
            listFragmentJustCreated = true
        }

        // The onResume() that always follows this same-activity intent redelivery already
        // lists and re-syncs the current directory, so doing it again here is redundant.
        browseToRoot(performRefresh = false)
    }

    @SuppressLint("UnsafeIntentLaunch")
    private fun handleCommonIntents(intent: Intent) {
        if (passCodeManager.isPassCodeEnabled() && passCodeManager.isLocked(this)) return

        when (intent.action) {
            Intent.ACTION_VIEW -> handleOpenFileViaIntent(intent)
            OPEN_FILE -> onOpenFileIntent(intent)
        }
    }

    @SuppressLint("UnsafeIntentLaunch")
    private fun handleRestartIntent(intent: Intent) {
        if (intent.action != RESTART) return

        finish()
        startActivity(intent)
    }

    private fun handleSearchIntent(intent: Intent) {
        val searchEvent = intent.getParcelableArgument(
            OCFileListFragment.SEARCH_EVENT,
            SearchEvent::class.java
        ) ?: return

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
        leftFragment = searchFragment.apply { arguments = searchEvent.getBundle() }

        listOfFilesFragment?.run {
            isSearchFragment = true
            updateSearchType(searchEvent)
        }
    }
    // endregion

    private fun onOpenFileIntent(intent: Intent) {
        val file = getFileFromIntent(intent) ?: run {
            Log_OC.e(TAG, "Can't open file intent, file is null")
            return
        }

        // Ensure we have the correct fragment type
        if (leftFragment !is OCFileListFragment || leftFragment is GalleryFragment) {
            Log_OC.w(
                TAG,
                "Invalid fragment (${leftFragment?.let { it::class.simpleName } ?: "null"}). " +
                    "Replacing."
            )
            setLeftFragment(OCFileListFragment(), false)
        }

        // Ensure fragment is attached before interaction
        Handler(Looper.getMainLooper()).post {
            (supportFragmentManager.findFragmentByTag(TAG_LIST_OF_FILES) as? OCFileListFragment)?.let { fragment ->
                leftFragment = fragment
                fragment.setFileDepth(file)
                updateActionBarTitleAndHomeButton(file)
                fragment.onItemClicked(file)
            }
        }
    }

    private fun setLeftFragment(fragment: Fragment?, showSortListGroup: Boolean) {
        fragment ?: return

        isShowingDetailsFromPreview = false
        prepareFragmentBeforeCommit(showSortListGroup)
        commitFragment(fragment)
    }

    private fun prepareFragmentBeforeCommit(showSortListGroup: Boolean) {
        searchView?.post { searchView?.setQuery(searchQuery, true) }
        setDrawerIndicatorEnabled(false)

        // clear the subtitle while navigating to any other screen from Media screen
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

    private fun getOCFileListFragmentFromFile(onFragmentReady: (OCFileListFragment) -> Unit) {
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
        getOCFileListFragmentFromFile { fragment ->
            browseUp(fragment)
            file?.let { fragment.onOverflowIconClicked(it, null) }
        }
    }

    var leftFragment: Fragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_LIST_OF_FILES)

        // Replaces the first fragment managed by the activity with the received as a parameter.
        private set(fragment) {
            setLeftFragment(fragment, true)
        }

    val listOfFilesFragment: OCFileListFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_LIST_OF_FILES) as? OCFileListFragment ?: run {
            Log_OC.e(TAG, "Access to unexisting list of files fragment")
            null
        }

    protected fun resetScrollingAndUpdateActionBar() {
        updateActionBarTitleAndHomeButton(file)
        resetScrolling(true)
    }

    fun updateListOfFilesFragment() {
        listOfFilesFragment?.listDirectory(MainApp.isOnlyOnDevice())
    }

    fun resetSearchView() {
        listOfFilesFragment?.isSearchFragment = false
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
                val waitedPreview = waitingToPreview?.remotePath == downloadedRemotePath
                val previewStarted = waitedPreview && tryStartWaitingPreview(success)
                waitingToPreview = null
                if (!previewStarted) {
                    detailFragment.updateFileDetails(false, success)
                }
            }
        }
    }

    fun canPreviewInAudioPlayer(file: OCFile?): Boolean = file != null && MimeTypeUtil.isAudio(file)

    private fun tryStartWaitingPreview(success: Boolean): Boolean {
        if (!success) return false

        waitingToPreview = waitingToPreview?.let { storageManager.getFileById(it.fileId) }
        val file = waitingToPreview ?: return false

        return when {
            MimeTypeUtil.isVideo(file) -> {
                startImagePreview(file, true)
                true
            }

            canPreviewInAudioPlayer(file) -> {
                startAudioPreview(file, true, true)
                true
            }

            MimeTypeUtil.isVCard(file.mimeType) -> {
                startContactListFragment(file)
                true
            }

            PreviewTextFileFragment.canBePreviewed(file) -> {
                startTextPreview(file, true)
                true
            }

            MimeTypeUtil.isPDF(file) -> {
                startPdfPreview(file)
                true
            }

            else -> {
                fileOperationsHelper.openFile(file)
                false
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val drawerOpen = isDrawerOpen
        drawerMenuItemsToShowHide.forEach { it.isVisible = !drawerOpen }

        menu?.findItem(R.id.action_current_folder_actions)?.isVisible = canShowCurrentFolderActions()

        return super.onPrepareOptionsMenu(menu)
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

        // populate list of menu items to show/hide when drawer is opened/closed
        drawerMenuItemsToShowHide = listOf(searchMenuItem)

        searchView?.let { setupSearchView(it) }

        return super.onCreateOptionsMenu(menu)
    }

    private fun setupSearchView(view: SearchView) {
        viewThemeUtils.androidx.themeToolbarSearchView(view)

        if (!searchQuery.isNullOrEmpty()) {
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

    private fun shouldOpenDrawer(): Boolean = !isDrawerOpen &&
        !isSearchOpen() &&
        isRoot(getCurrentDir()) &&
        leftFragment is OCFileListFragment &&
        !isAlbumItemsFragment

    /**
     * Called, when the user selected something for uploading
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            data != null && requestCode in selectContentFromAppsRequestCodes &&
                resultCode in selectContentFromAppsResultCodes ->
                requestUploadOfContentFromApps(requestCode, resultCode, data)

            data != null && requestCode == REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM &&
                resultCode in selectFilesFromFileSystemResultCodes ->
                requestUploadOfFilesFromFileSystem(data, resultCode)

            requestCode in uploadFromCameraRequestCodes && resultCode in uploadFromCameraResultCodes ->
                uploadFromCamera(requestCode)

            requestCode == REQUEST_CODE__MOVE_OR_COPY_FILES && resultCode == RESULT_OK -> exitSelectionMode()

            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun uploadFromCamera(requestCode: Int) {
        val isVideo = requestCode == REQUEST_CODE__UPLOAD_FROM_VIDEO_CAMERA
        val cameraFilePath = FileOperationsHelper.createCameraFile(this, isVideo).absolutePath

        val listener = object : CheckAvailableSpaceListener {
            override fun onCheckAvailableSpaceStart() {
                Log_OC.d(this, "onCheckAvailableSpaceStart")
            }

            override fun onCheckAvailableSpaceFinish(hasEnoughSpaceAvailable: Boolean, vararg filesToUpload: String?) {
                Log_OC.d(this, "onCheckAvailableSpaceFinish")
                if (!hasEnoughSpaceAvailable) return

                val capturedFilePath = filesToUpload.firstOrNull() ?: return
                renameAndUploadCapturedFile(File(capturedFilePath), requestCode)
            }
        }

        CheckAvailableSpaceTask(listener, cameraFilePath).execute()
    }

    private fun renameAndUploadCapturedFile(capturedFile: File, requestCode: Int) {
        val capturedFileName = if (requestCode == REQUEST_CODE__UPLOAD_FROM_CAMERA) {
            FileOperationsHelper.getCapturedImageName()
        } else {
            FileOperationsHelper.getCapturedVideoName()
        }
        val renamedFile = File(capturedFile.parent + OCFile.PATH_SEPARATOR + capturedFileName)

        if (!capturedFile.renameTo(renamedFile)) {
            SnackbarUtil.show(this, R.string.error_uploading_direct_camera_upload)
            return
        }

        requestUploadOfFilesFromFileSystem(
            renamedFile.parentFile?.absolutePath,
            arrayOf(renamedFile.absolutePath),
            FileUploadWorker.LOCAL_BEHAVIOUR_DELETE
        )
    }

    private fun exitSelectionMode() {
        listOfFilesFragment?.exitSelectionMode()
    }

    private fun requestUploadOfFilesFromFileSystem(data: Intent, resultCode: Int) {
        val filePaths = data.getStringArrayExtra(UploadFilesActivity.EXTRA_CHOSEN_FILES) ?: return
        val basePath = data.getStringExtra(UploadFilesActivity.LOCAL_BASE_PATH)
        requestUploadOfFilesFromFileSystem(basePath, filePaths, resultCode)
    }

    private fun getRemotePaths(directory: String?, filePaths: Array<String>, localBasePath: String): Array<String> =
        filePaths.map { (directory ?: "") + StringUtils.removePrefix(it, localBasePath) }.toTypedArray()

    private fun requestUploadOfFilesFromFileSystem(localBasePath: String?, filePaths: Array<String>, resultCode: Int) {
        if (localBasePath == null) {
            Log_OC.d(TAG, "User clicked on 'Update' with no selection")
            SnackbarUtil.show(this, R.string.filedisplay_no_file_selected)
            return
        }

        val normalizedLocalBasePath = if (localBasePath.endsWith("/")) localBasePath else "$localBasePath/"
        val remotePathBase = getCurrentDir()?.remotePath
        val decryptedRemotePaths = getRemotePaths(remotePathBase, filePaths, normalizedLocalBasePath)

        val behaviour = when (resultCode) {
            UploadFilesActivity.RESULT_OK_AND_MOVE -> FileUploadWorker.LOCAL_BEHAVIOUR_MOVE
            UploadFilesActivity.RESULT_OK_AND_DELETE -> FileUploadWorker.LOCAL_BEHAVIOUR_DELETE
            else -> FileUploadWorker.LOCAL_BEHAVIOUR_FORGET
        }

        connectivityService.isNetworkAndServerAvailable { isAvailable: Boolean? ->
            if (isAvailable == true) {
                uploadNewFiles(remotePathBase, filePaths, decryptedRemotePaths, behaviour)
            } else {
                addCreateFileOfflineOperation(filePaths, decryptedRemotePaths)
            }
        }
    }

    private fun uploadNewFiles(
        remotePathBase: String?,
        filePaths: Array<String>,
        decryptedRemotePaths: Array<String>,
        behaviour: Int
    ) {
        val capability = capabilities.orElse(null) ?: return

        val isValidFolderPath = remotePathBase?.let { checkFolderPath(it, capability, this) }
        if (isValidFolderPath == false) {
            SnackbarUtil.show(this, R.string.file_name_validator_error_contains_reserved_names_or_invalid_characters)
            return
        }

        val currentUser = user.orElse(null) ?: return
        val data = OCUploadLocalPathData.forFile(
            currentUser,
            filePaths,
            decryptedRemotePaths,
            behaviour,
            createRemoteFolder = true
        )

        FileUploadHelper.instance().uploadNewFiles(data)
    }

    private fun addCreateFileOfflineOperation(filePaths: Array<String>, decryptedRemotePaths: Array<String>) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                fileDataStorageManager.addCreateFileOfflineOperation(filePaths, decryptedRemotePaths)
            }
            refreshCurrentDirectory()
        }
    }

    private fun requestUploadOfContentFromApps(requestCode: Int, resultCode: Int, contentIntent: Intent) {
        val currentUser = user.orElse(null) ?: return

        val clipData = contentIntent.clipData
        val streamsToUpload: List<Parcelable?> = if (clipData != null && clipData.itemCount > 0) {
            List(clipData.itemCount) { clipData.getItemAt(it)?.uri }
        } else {
            listOf(contentIntent.data)
        }

        val behaviour = if (resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE) {
            FileUploadWorker.LOCAL_BEHAVIOUR_MOVE
        } else {
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY
        }

        val remotePath = getCurrentDir()?.remotePath ?: OCFile.ROOT_PATH
        val fileDisplayNameTransformer = if (requestCode == REQUEST_CODE__SELECT_CONTENT_FROM_APPS_AUTO_RENAME) {
            Function<Uri, String?> { timestampedFileNameFor(it) }
        } else {
            null
        }

        UriUploader(
            this,
            streamsToUpload,
            remotePath,
            currentUser,
            behaviour,
            false, // Not show waiting dialog while file is being copied from private storage
            null, // Not needed copy temp task listener
            fileDisplayNameTransformer
        ).uploadUris()
    }

    private fun timestampedFileNameFor(uri: Uri): String? {
        val displayName = UriUtils.getDisplayNameForUri(uri, applicationContext)
        if (displayName.isNullOrEmpty()) return null

        return FileOperationsHelper.getTimestampedFileName("." + FilenameUtils.getExtension(displayName))
    }

    fun isSearchOpen(): Boolean =
        searchView?.findViewById<View?>(androidx.appcompat.R.id.search_edit_frame)?.isVisible == true

    /**
     * Sets up a custom back-press handler for this activity.
     *
     * This callback determines how the back button behaves based on the current UI state:
     * - If the search view is open, it closes it.
     * - If the navigation drawer is open, it closes it.
     * - If the left fragment is an [OCFileListFragment]:
     *   - If in the root directory, it either navigates to "All Files" or finishes the activity.
     *   - Otherwise, it navigates one level up.
     * - Otherwise, it pops the current fragment from the back stack.
     *
     * ### About `isEnabled`
     * `isEnabled` is a property of [OnBackPressedCallback].
     * When `isEnabled = false`, this callback is **temporarily disabled**,
     * allowing the system or other callbacks to handle the back press instead.
     */
    private fun handleBackPress() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackPressImpl(before = {
                        isEnabled = false
                    }, after = {
                        isEnabled = true
                    })
                }
            }
        )
    }

    private fun handleBackPressImpl(before: () -> Unit = {}, after: () -> Unit = {}) {
        val currentLeftFragment = leftFragment

        before()
        when {
            isSearchOpen() -> resetSearchAction()

            isDrawerOpen -> closeDrawer()

            isShowingDetailsFromPreview && currentLeftFragment is FileDetailFragment ->
                NavigationAnimator(this).finishWithSlideDown()

            // pop back if current fragment is AlbumItemsFragment
            isAlbumItemsFragment -> popBack()

            currentLeftFragment is OCFileListFragment -> handleOCFileListFragmentBackPress(currentLeftFragment)

            else -> popBack()
        }
        after()
    }

    private fun handleOCFileListFragmentBackPress(fragment: OCFileListFragment) {
        // Normal folder navigation (go up) also works for shared tab
        if (!isRoot(getCurrentDir())) {
            browseUp(fragment)
            return
        }

        if (fragment.shouldNavigateBackToAllFiles()) navigateToAllFiles() else finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            if (shouldOpenDrawer()) openDrawer() else handleBackPressImpl()
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

    private fun resetSearchAction() {
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
            // Remove the list to the original state
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

    /**
     * Use this method when want to pop the fragment on back press. It resets Scrolling (See
     * [with true][.resetScrolling] and pop the visibility for sortListGroup (See
     * [with false][.showSortListGroup]. At last call to supportFragmentManager.popBackStack()
     */
    private fun popBack() {
        binding.fabMain.setImageResource(R.drawable.ic_plus)
        resetScrolling(true)
        showSortListGroup(false)
        showBottomNavigationBar(true)
        supportFragmentManager.popBackStack()
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

        // Instead of onPostCreate, starting the loading in onResume for children fragments
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
        syncAndUpdateFolder(ignoreETag = true, ignoreFocus = true)
        refreshListOfFiles(currentLeftFragment, listFragmentJustCreated)

        inAppReviewHelper.showInAppReview(this)
        checkNotifications()

        Log_OC.v(TAG, "onResume() end")

        Handler(Looper.getMainLooper()).postDelayed({
            isFileDisplayActivityResumed = false
        }, ON_RESUMED_RESET_DELAY)
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

    private fun refreshListOfFiles(fragment: OCFileListFragment, listFragmentJustCreated: Boolean) {
        // Try to get the OCFile from the intent, if one was provided when launching this activity.
        // 'file' comes from the FileActivity base class and represents the currently opened file or folder.
        // We update it only when a valid file is found in the intent.
        val startFile = getFileFromIntent(intent)?.also { file = it }

        val currentSearchView = searchView
        when {
            currentSearchView != null && !searchQuery.isNullOrEmpty() -> currentSearchView.setQuery(searchQuery, false)

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

    private fun getFileFromIntent(intent: Intent?): OCFile? =
        intent.getParcelableArgument(EXTRA_FILE, OCFile::class.java)
            ?: intent?.getStringExtra(EXTRA_FILE_REMOTE_PATH)
                ?.let { fileDataStorageManager.getFileByDecryptedRemotePath(it) }

    // region local broadcast manager receivers
    private fun registerReceivers() {
        Log_OC.d(TAG, "registering receivers")

        localBroadcastManager.run {
            val uploadFinishedIntent = IntentFilter(FileUploadEventBroadcaster.ACTION_UPLOAD_COMPLETED)
            registerReceiver(fileUploadCompletedReceiver, uploadFinishedIntent)

            val fileDownloadStartedIntentFilter = IntentFilter(FileDownloadEventBroadcaster.ACTION_DOWNLOAD_ENQUEUED)
            registerReceiver(fileDownloadStartedReceiver, fileDownloadStartedIntentFilter)

            val fileDownloadFinishedIntentFilter = IntentFilter(FileDownloadEventBroadcaster.ACTION_DOWNLOAD_COMPLETED)
            registerReceiver(fileDownloadCompletedReceiver, fileDownloadFinishedIntentFilter)

            val syncBroadcastIntentFilter = IntentFilter(FileSyncAdapter.EVENT_FULL_SYNC_START).apply {
                addAction(FileSyncAdapter.EVENT_FULL_SYNC_END)
                addAction(FileSyncAdapter.EVENT_FULL_SYNC_FOLDER_CONTENTS_SYNCED)
                addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED)
                addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED)
            }
            registerReceiver(syncReceiver, syncBroadcastIntentFilter)
        }
    }

    private fun unregisterReceivers() {
        Log_OC.d(TAG, "unregistering receivers")

        localBroadcastManager.run {
            unregisterReceiver(syncReceiver)
            unregisterReceiver(fileUploadCompletedReceiver)
            unregisterReceiver(fileDownloadStartedReceiver)
            unregisterReceiver(fileDownloadCompletedReceiver)
        }
    }
    // endregion

    override fun onStop() {
        Log_OC.v(TAG, "onStop()")
        folderRefreshScheduler.stop()
        unregisterReceivers()
        super.onStop()
    }

    override fun onSortingOrderChosen(selection: FileSortOrder?) {
        listOfFilesFragment?.fileListLayoutManager?.sortFiles(selection)
    }

    override fun downloadFile(file: OCFile, packageName: String, activityName: String) {
        startDownloadForSending(file, OCFileListFragment.DOWNLOAD_SEND, packageName, activityName)
    }

    // region SyncBroadcastReceiver
    private inner class SyncReceiver : BroadcastReceiver() {
        @SuppressLint("VisibleForTests")
        override fun onReceive(context: Context?, intent: Intent) {
            try {
                val event = intent.action
                Log_OC.d(TAG, "Received broadcast $event")

                // region EventData
                val accountName = intent.getStringExtra(FileSyncAdapter.EXTRA_ACCOUNT_NAME)
                val syncFolderRemotePath = intent.getStringExtra(FileSyncAdapter.EXTRA_FOLDER_PATH)
                val id = intent.getStringExtra(FileSyncAdapter.EXTRA_RESULT)
                val syncResult = DataHolderUtil.getInstance().retrieve(id)
                val sameAccount = accountName != null && accountName == account?.name && storageManager != null
                val fileListFragment = this@FileDisplayActivity.listOfFilesFragment

                // endregion
                if (sameAccount) {
                    handleSyncEvent(event, syncFolderRemotePath, id, fileListFragment, syncResult)
                }

                if (syncResult is RemoteOperationResult<*> &&
                    syncResult.code == RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED
                ) {
                    mLastSslUntrustedServerResult = syncResult
                }
            } catch (_: java.lang.RuntimeException) {
                safelyDeleteResult(intent)
            } finally {
                onSyncFinished()
            }
        }
    }

    // avoid app crashes after changing the serial id of RemoteOperationResult in owncloud library
    // with broadcast notifications pending to process
    private fun safelyDeleteResult(intent: Intent) {
        try {
            DataHolderUtil.getInstance().delete(intent.getStringExtra(FileSyncAdapter.EXTRA_RESULT))
        } catch (_: java.lang.RuntimeException) {
            Log_OC.i(TAG, "Ignoring error deleting data")
        }
    }

    private fun handleSyncEvent(
        event: String?,
        syncFolderRemotePath: String?,
        id: String?,
        fileListFragment: OCFileListFragment?,
        syncResult: Any?
    ) {
        if (FileSyncAdapter.EVENT_FULL_SYNC_START == event) {
            return
        }

        // EVENT_SINGLE_FOLDER_CONTENTS_SYNCED fires only when the folder's content actually changed, and
        // EVENT_SINGLE_FOLDER_SHARES_SYNCED only when a sharee actually changed - each is an independent,
        // already-precise signal, so both are handled here (RefreshFolderOperation.java).
        var currentFile = file?.remotePath?.let { storageManager.getFileByPath(it) }
        val currentDir = getCurrentDir()?.remotePath?.let { storageManager.getFileByPath(it) }
        val isSyncFolderRemotePathRoot = OCFile.ROOT_PATH == syncFolderRemotePath

        if (currentDir == null && !isSyncFolderRemotePathRoot) {
            handleRemovedFolder(syncFolderRemotePath)
        } else if (currentDir != null) {
            currentFile = handleRemovedFileFromServer(currentFile, currentDir)
            updateFileList(fileListFragment, currentDir, syncFolderRemotePath)
            file = currentFile
        }

        if (isSyncFolderRemotePathRoot) {
            startMetadataSyncForRoot()
        }

        handleSyncResult(event, syncResult)
        DataHolderUtil.getInstance().delete(id)
        handleScrollBehaviour(fileListFragment)
    }

    private fun handleRemovedFileFromServer(currentFile: OCFile?, currentDir: OCFile?): OCFile? {
        if (currentFile == null && file?.isFolder == false) {
            resetScrollingAndUpdateActionBar()
            return currentDir
        }

        return currentFile
    }

    private fun handleRemovedFolder(syncFolderRemotePath: String?) {
        SnackbarUtil.show(this, R.string.sync_current_folder_was_removed, syncFolderRemotePath)
        fileListFragment?.let {
            it.parentFolderFinder.getParentOnFirstParentRemoved(syncFolderRemotePath, storageManager)?.let { target ->
                it.listDirectory(target, MainApp.isOnlyOnDevice())
                updateActionBarTitleAndHomeButton(target)
                file = target
            }
        }
    }

    private fun updateFileList(
        ocFileListFragment: OCFileListFragment?,
        currentDir: OCFile,
        syncFolderRemotePath: String?
    ) {
        if (currentDir.remotePath != syncFolderRemotePath) return

        ocFileListFragment?.listDirectory(currentDir, MainApp.isOnlyOnDevice())
    }

    private fun handleScrollBehaviour(ocFileListFragment: OCFileListFragment?) {
        ocFileListFragment ?: return
        if (syncState == EmptyListState.LOADING || ocFileListFragment.isLoading) return

        if (ocFileListFragment.isEmpty) {
            lockScrolling()
            return
        }

        resetScrolling(false)
    }

    private fun handleSyncResult(event: String?, syncResult: Any?) {
        if (RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED != event || syncResult == null) {
            return
        }

        if (syncResult is RemoteOperationResult<*> && syncResult.isSuccess) {
            hideInfoBox()
            return
        }

        handleFailedSyncResult(syncResult)
    }

    private fun handleFailedSyncResult(syncResult: Any?) {
        if (checkForRemoteOperationError(syncResult)) {
            requestCredentialsUpdate()
        } else {
            handleNonCredentialSyncErrors(syncResult)
        }
    }

    private fun handleNonCredentialSyncErrors(syncResult: Any?) {
        if (syncResult !is RemoteOperationResult<*>) return

        when (syncResult.code) {
            RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED -> showUntrustedCertDialog(syncResult)
            RemoteOperationResult.ResultCode.MAINTENANCE_MODE -> showInfoBox(R.string.maintenance_mode)
            RemoteOperationResult.ResultCode.NO_NETWORK_CONNECTION -> showInfoBox(R.string.offline_mode)
            RemoteOperationResult.ResultCode.HOST_NOT_AVAILABLE -> showInfoBox(R.string.host_not_available)
            RemoteOperationResult.ResultCode.SIGNING_TOS_NEEDED -> showTermsOfServiceDialog()
            RemoteOperationResult.ResultCode.OUT_OF_MEMORY -> syncState = EmptyListState.OUT_OF_MEMORY
            else -> {}
        }
    }

    private fun showTermsOfServiceDialog() {
        if (supportFragmentManager.findFragmentByTag(DIALOG_TAG_SHOW_TOS) == null) {
            TermsOfServiceDialog().show(supportFragmentManager, DIALOG_TAG_SHOW_TOS)
        }
    }

    private fun checkForRemoteOperationError(syncResult: Any?): Boolean = syncResult is RemoteOperationResult<*> &&
        (
            RemoteOperationResult.ResultCode.UNAUTHORIZED == syncResult.code ||
                (syncResult.isException && syncResult.exception is AuthenticatorException)
            )

    private fun onSyncFinished() {
        if (syncState != EmptyListState.LOADING) return

        syncState = when {
            MainApp.isOnlyOnDevice() -> EmptyListState.ONLY_ON_DEVICE

            listOfFilesFragment?.searchEvent?.searchType == SearchRemoteOperation.SearchType.FAVORITE_SEARCH ->
                SearchType.FAVORITE_SEARCH

            else -> SearchType.NO_SEARCH
        }
    }

    // endregion

    /**
     * Once the file upload has finished -> update view
     */
    private inner class FileUploadCompletedReceiver : BroadcastReceiver() {
        private val tag = "UploadFinishReceiver"

        override fun onReceive(context: Context?, intent: Intent) {
            Log_OC.d(tag, "upload finish received broadcast")

            val uploadedRemotePath = intent.getStringExtra(FileUploadEventBroadcaster.EXTRA_REMOTE_PATH)
            val accountName = intent.getStringExtra(FileUploadEventBroadcaster.EXTRA_ACCOUNT_NAME)
            val sameAccount = accountName != null && accountName == getAccount()?.name
            val currentDirRemotePath = getCurrentDir()?.remotePath
            val isDescendant =
                currentDirRemotePath != null && uploadedRemotePath?.startsWith(currentDirRemotePath) == true

            if (sameAccount && isDescendant) {
                updateListOfFilesFragment()
            }

            val uploadWasFine = intent.getBooleanExtra(FileUploadEventBroadcaster.EXTRA_UPLOAD_RESULT, false)
            val currentFile = file
            val oldRemotePath = intent.getStringExtra(FileUploadEventBroadcaster.EXTRA_OLD_REMOTE_PATH)
            val renamedInUpload = currentFile != null && currentFile.remotePath == oldRemotePath
            val sameFile = currentFile != null && (currentFile.remotePath == uploadedRemotePath || renamedInUpload)

            val fileDetailFragment = leftFragment as? FileDetailFragment
            if (sameAccount && sameFile && fileDetailFragment != null) {
                updateFileDetailsAfterUpload(fileDetailFragment, uploadedRemotePath, uploadWasFine, renamedInUpload)
            }

            refreshAlbumScreensAfterUpload()
        }

        private fun updateFileDetailsAfterUpload(
            fileDetailFragment: FileDetailFragment,
            uploadedRemotePath: String?,
            uploadWasFine: Boolean,
            renamedInUpload: Boolean
        ) {
            if (uploadWasFine) {
                file = storageManager.getFileByPath(uploadedRemotePath)
            } else {
                // TODO remove upload progress bar after upload failed.
                Log_OC.d(TAG, "Remove upload progress bar after upload failed")
            }

            if (renamedInUpload && !uploadedRemotePath.isNullOrBlank()) {
                val newName = File(uploadedRemotePath).name
                SnackbarUtil.show(this@FileDisplayActivity, R.string.filedetails_renamed_in_upload_msg, newName)
            }

            if (uploadWasFine || file?.fileExists() == true) {
                fileDetailFragment.updateFileDetails(false, true)
            } else {
                onBackPressedDispatcher.onBackPressed()
            }

            if (uploadWasFine) {
                file?.let { forcePreviewIfImageOrText(it) }
            }
        }

        private fun forcePreviewIfImageOrText(uploadedFile: OCFile) {
            when {
                PreviewMediaFragment.canBePreviewed(uploadedFile) -> startImagePreview(uploadedFile, true)
                PreviewTextFileFragment.canBePreviewed(uploadedFile) -> startTextPreview(uploadedFile, true)
            }
        }

        private fun refreshAlbumScreensAfterUpload() {
            when {
                isAlbumsFragment ->
                    (supportFragmentManager.findFragmentByTag(AlbumsFragment.TAG) as? AlbumsFragment)?.refreshAlbums()

                isAlbumItemsFragment ->
                    (supportFragmentManager.findFragmentByTag(AlbumItemsFragment.TAG) as? AlbumItemsFragment)
                        ?.refreshData()
            }
        }
    }

    private enum class FileDownloadIndicator(val iconId: Int) {
        Downloading(R.drawable.ic_synchronizing),
        Downloaded(R.drawable.ic_synced)
    }

    private fun updateFileDownloadIndicator(state: FileDownloadIndicator, file: OCFile) {
        ocFileListFragment?.adapter?.updateFileIndicator(state.iconId, file)
    }

    // region FolderDownloadWorker observers
    private fun onFolderFileDownloadProgress(fileId: Long, previousFileId: Long?) {
        val adapter = ocFileListFragment?.adapter ?: return
        previousFileId?.let { storageManager.getFileById(it) }?.let(adapter::replaceFile)
        adapter.updateFileIndicator(fileId, FileDownloadIndicator.Downloading.iconId)
    }

    // endregion

    // region FileDownloadWorker receivers
    private fun getFileFromFileDownloadWorker(intent: Intent): OCFile? {
        val remotePath = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_REMOTE_PATH)
        return fileDataStorageManager.getFileByDecryptedRemotePath(remotePath)
    }

    private inner class FileDownloadStartedReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log_OC.d(TAG, "download worker started")
            getFileFromFileDownloadWorker(intent)?.let {
                updateFileDownloadIndicator(FileDownloadIndicator.Downloading, it)
            }
            handleDownloadWorkerState()
        }
    }

    private inner class FileDownloadCompletedReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            Log_OC.d(TAG, "file download completed received")
            getFileFromFileDownloadWorker(intent)?.let {
                updateFileDownloadIndicator(FileDownloadIndicator.Downloaded, it)
            }

            if (fileIDForImmediatePreview == NO_IMMEDIATE_PREVIEW_FILE_ID) {
                Log_OC.d(TAG, "updating ui for file download")
                updateUIForFileDownload(intent)
                return
            }

            Log_OC.d(TAG, "updating ui immediate file preview")

            val downloadedRemotePath = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_REMOTE_PATH)
            val currentFile = storageManager.getFileByDecryptedRemotePath(downloadedRemotePath) ?: return
            if (fileIDForImmediatePreview != currentFile.fileId || !currentFile.isDown) return

            fileIDForImmediatePreview = NO_IMMEDIATE_PREVIEW_FILE_ID
            if (PreviewMediaFragment.canBePreviewed(currentFile)) {
                startImagePreview(currentFile, currentFile.isDown)
            } else {
                previewFile(currentFile, null)
            }
        }

        private fun updateUIForFileDownload(intent: Intent) {
            val downloadedRemotePath = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_REMOTE_PATH)
            val downloadBehaviour = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_DOWNLOAD_BEHAVIOUR)

            if (isSameAccount(intent) && isDescendant(downloadedRemotePath)) {
                updateListOfFilesFragment()
                val intentAction = intent.action
                if (intentAction != null && downloadedRemotePath != null) {
                    refreshDetailsFragmentIfVisible(
                        intentAction,
                        downloadedRemotePath,
                        intent.getBooleanExtra(FileDownloadEventBroadcaster.EXTRA_DOWNLOAD_RESULT, false)
                    )
                }
            }

            waitingToSend?.let { previousWaitingToSend ->
                // update file after downloading
                val updatedWaitingToSend = storageManager.getFileByRemoteId(previousWaitingToSend.remoteId)
                waitingToSend = updatedWaitingToSend
                if (updatedWaitingToSend?.isDown == true && OCFileListFragment.DOWNLOAD_SEND == downloadBehaviour) {
                    val packageName = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_PACKAGE_NAME) ?: return
                    val activityName = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_ACTIVITY_NAME) ?: return
                    sendDownloadedFile(packageName, activityName)
                }
            }

            waitingToPreview?.let { previousWaitingToPreview ->
                val updatedWaitingToPreview = storageManager.getFileByRemoteId(previousWaitingToPreview.remoteId)
                waitingToPreview = updatedWaitingToPreview
                if (updatedWaitingToPreview?.isDown == true &&
                    EditImageActivity.OPEN_IMAGE_EDITOR == downloadBehaviour
                ) {
                    startImageEditor(updatedWaitingToPreview)
                }
            }
        }

        fun isDescendant(downloadedRemotePath: String?): Boolean {
            val currentDirRemotePath = getCurrentDir()?.remotePath ?: return false
            return downloadedRemotePath?.startsWith(currentDirRemotePath) == true
        }

        fun isSameAccount(intent: Intent): Boolean {
            val accountName = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_ACCOUNT_NAME)
            return accountName != null && accountName == account?.name
        }
    }
    // endregion

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
        startSyncFolderOperation(directory, false)
        startMetadataSyncForCurrentDir()
    }

    /**
     * Shows the information of the [OCFile] received as a parameter.
     *
     * @param file [OCFile] whose details will be shown
     */
    override fun showDetails(file: OCFile?) {
        showDetails(file, 0)
    }

    /**
     * Shows the information of the [OCFile] received as a parameter.
     *
     * @param file      [OCFile] whose details will be shown
     * @param activeTab the active tab in the details view
     */
    override fun showDetails(file: OCFile?, activeTab: Int) {
        val currentUser = user.orElse(null) ?: return

        resetScrolling(true)

        val detailFragment = FileDetailFragment.newInstance(file, currentUser, activeTab)
        setLeftFragment(detailFragment, false)
        // swap now, so no frame shows the list without its sort row
        supportFragmentManager.executePendingTransactions()
        updateActionBarForFile(file)
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
        // if no file is passed, current file decides
        super.updateActionBarTitleAndHomeButton(chosenFile ?: file)
        supportInvalidateOptionsMenu()
    }

    override fun isDrawerIndicatorAvailable(): Boolean = isRoot(getCurrentDir())

    private fun observeWorkerState() {
        lifecycleScope.launch {
            viewModel.observeOfflineWorker(onComplete = {
                refreshCurrentDirectory()
            })
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

    fun canPreviewInMediaPager(file: OCFile?): Boolean =
        PreviewMediaFragment.canBePreviewed(file) || (file != null && MimeTypeUtil.isVideo(file))

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

        val showPreview = file.isDown || MimeTypeUtil.isVideo(file)
        startImagePreview(file, showPreview, type, mediaState, sourceView)
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
                startTextPreview(file, false)
            }

            canPreviewInAudioPlayer(file) -> {
                setFabVisible?.onComplete(false)
                startAudioPreview(file, true, false)
            }

            else -> fileOperationsHelper.openFile(file)
        }
    }

    fun refreshCurrentDirectory() {
        val currentDir = getCurrentDir()?.let { storageManager.getFileByDecryptedRemotePath(it.remotePath) }
        fileListFragment?.listDirectory(currentDir, MainApp.isOnlyOnDevice())
    }

    private fun handleDownloadWorkerState() {
        val previousWaitingToPreview = waitingToPreview ?: return
        if (storageManager == null) return

        val updatedWaitingToPreview = storageManager.getFileById(previousWaitingToPreview.fileId)
        waitingToPreview = updatedWaitingToPreview
        if (updatedWaitingToPreview?.isDown == false) {
            requestForDownload()
        }
    }

    override fun newTransferenceServiceConnection(): ServiceConnection = ListServiceConnection()

    /**
     * Defines callbacks for service binding, passed to bindService()
     * TODO: Check if this can be removed since download and uploads uses work manager now.
     */
    private inner class ListServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) = Unit

        override fun onServiceDisconnected(component: ComponentName) {
            if (component == ComponentName(this@FileDisplayActivity, FileDownloadWorker::class.java)) {
                Log_OC.d(TAG, "Download service disconnected")
            }
        }
    }

    /**
     * Updates the view associated to the activity after the finish of some operation over files in the current
     * account.
     *
     * @param operation Removal operation performed.
     * @param result    Result of the removal.
     */
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

    private val fileListFragment: OCFileListFragment?
        get() = lastFragment() as? OCFileListFragment ?: listOfFilesFragment

    private fun refreshGalleryFragmentIfNeeded() {
        if (fileListFragment is GalleryFragment) {
            startPhotoSearch(R.id.nav_gallery)
        }
    }

    private fun refreshShowDetails() {
        val details = leftFragment as? FileFragment ?: return

        details.file?.let {
            val refreshedFile = storageManager.getFileByPath(it.remotePath)
            if (details is PreviewTextFragment) {
                // Refresh  OCFile of the fragment
                (details as? PreviewTextFileFragment)?.updateFile(refreshedFile)
            } else {
                showDetails(refreshedFile)
            }
        }
        supportInvalidateOptionsMenu()
    }

    val deleteBatchTracker = DeleteBatchTracker(onAllDeletesFinished = {
        (leftFragment as? GalleryFragment)?.onRefresh()
    })

    /**
     * Updates the view associated to the activity after the finish of an operation trying to remove a file.
     *
     * @param operation Removal operation performed.
     * @param result    Result of the removal.
     */
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

        // check if file is still available, if so do nothing
        val fileAvailable = storageManager.fileExists(removedFile.fileId)
        if (currentLeftFragment is FileFragment && !fileAvailable && removedFile == currentLeftFragment.file) {
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

    private fun showUntrustedCertDialogFor(result: RemoteOperationResult<*>) {
        mLastSslUntrustedServerResult = result
        showUntrustedCertDialog(result)
    }

    override fun onAutoUploadFolderRemoved(
        entities: List<SyncedFolderEntity>,
        filesToRemove: List<OCFile>,
        onlyLocalCopy: Boolean
    ) {
        val dialog = ConfirmationDialogFragment.newInstance(
            messageResId = R.string.auto_upload_delete_dialog_description,
            messageArguments = null,
            titleResId = R.string.auto_upload_delete_dialog_title,
            titleIconId = R.drawable.ic_info,
            positiveButtonTextId = R.string.common_delete,
            negativeButtonTextId = R.string.common_cancel,
            neutralButtonTextId = NO_NEUTRAL_BUTTON
        )

        dialog.setOnConfirmationListener(object : ConfirmationDialogFragment.ConfirmationDialogFragmentListener {
            override fun onConfirmation(callerTag: String?) {
                removeAutoUploadFolders(entities, filesToRemove, onlyLocalCopy)
            }

            override fun onNeutral(callerTag: String?) = Unit
            override fun onCancel(callerTag: String?) = Unit
        })

        if (isDialogFragmentReady(dialog)) {
            dialog.show(supportFragmentManager, null)
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

        // delete old local copy
        if (restoredFile?.isDown == true) {
            fileOperationsHelper.removeFiles(listOf(restoredFile), true, true)

            // download new version, only if file was previously download
            showSyncLoadingDialog(restoredFile.isFolder)
            fileOperationsHelper.syncFileOrFolder(restoredFile)
        }

        val parent = restoredFile?.let { storageManager.getFileById(it.parentId) }
        startSyncFolderOperation(parent, ignoreETag = true, ignoreFocus = true)

        (leftFragment as? FileDetailFragment)?.fileDetailActivitiesFragment?.reload()
    }

    private fun showErrorCauseMessage(result: RemoteOperationResult<*>, operation: RemoteOperation<*>?) {
        try {
            SnackbarUtil.show(this, ErrorMessageAdapter.getErrorCauseMessage(result, operation, resources))
        } catch (e: Resources.NotFoundException) {
            Log_OC.e(TAG, "Error while trying to show fail message ", e)
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to move a file.
     *
     * @param operation Move operation performed.
     * @param result    Result of the move operation.
     */
    private fun onMoveFileOperationFinish(operation: MoveFileOperation?, result: RemoteOperationResult<*>) {
        if (!result.isSuccess) {
            showErrorCauseMessage(result, operation)
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to copy a file.
     *
     * @param operation Copy operation performed.
     * @param result    Result of the copy operation.
     */
    private fun onCopyFileOperationFinish(operation: CopyFileOperation?, result: RemoteOperationResult<*>) {
        if (!result.isSuccess) {
            showErrorCauseMessage(result, operation)
            return
        }

        updateListOfFilesFragment()
        refreshGalleryFragmentIfNeeded()
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to rename a file.
     *
     * @param operation Renaming operation performed.
     * @param result    Result of the renaming.
     */
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

        (leftFragment as? FileFragment)?.let {
            onRenameFileOperationFinishForFileFragment(it, renamedFile, currentUser)
        }

        val parentDir = storageManager.getFileById(renamedFile.parentId)
        val isCurrentDirParentDirOfGivenFile = parentDir != null && parentDir == currentDir

        // checking current dir against renamed file's parent will always fail when user is in Shared/Favorites root.
        // thus check is current dir is root or not as well
        if (currentDir?.isRootDirectory == true || isCurrentDirParentDirOfGivenFile) {
            val fragment = fileListFragment

            // OCFileSearchTask may still run during rename, so use a single refresh path to avoid
            // flicker/inconsistent filenames.
            if (fragment?.isSearchFragment == true) {
                fragment.restartSearch()
            } else {
                fragment?.adapter?.updateFile(renamedFile)
            }
        }

        refreshGalleryFragmentIfNeeded()
        fetchRecommendedFilesIfNeeded(ignoreETag = true, currentDir)
    }

    private fun onRenameFileOperationFinishForFileFragment(fragment: FileFragment, ocFile: OCFile, user: User) {
        if (fragment.file != ocFile) return

        when (fragment) {
            is FileDetailFragment -> {
                fragment.updateFileDetails(ocFile, user)
                showDetails(ocFile)
            }

            is PreviewTextFileFragment -> {
                fragment.updateFile(ocFile)
                if (PreviewTextFileFragment.canBePreviewed(ocFile)) {
                    startTextPreview(ocFile, true)
                } else {
                    fileOperationsHelper.openFile(ocFile)
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
        refreshShowDetails()
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying create a new folder
     *
     * @param operation Creation operation performed.
     * @param result    Result of the creation.
     */
    private fun onCreateFolderOperationFinish(operation: CreateFolderOperation, result: RemoteOperationResult<*>) {
        if (!result.isSuccess) {
            if (RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS == result.code) {
                SnackbarUtil.show(this, R.string.folder_already_exists)
            } else {
                showErrorCauseMessage(result, operation)
            }
            return
        }

        val fileListFragment = listOfFilesFragment
        val createdFolder = storageManager.getFileByDecryptedRemotePath(operation.remotePath)
        if (!operation.shouldEncrypt()) {
            createdFolder?.let { fileListFragment?.onItemClicked(it) }
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

    /**
     * {@inheritDoc}
     */
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

    private fun requestForDownload() {
        val currentUser = user.orElse(null) ?: return
        waitingToPreview?.let {
            FileDownloadHelper.instance().downloadFileIfNotStartedBefore(currentUser, it)
        }
    }

    override fun onSavedCertificate() {
        startSyncFolderOperation(getCurrentDir(), false)
    }

    /**
     * Starts an operation to refresh the requested folder.
     *
     *
     * The operation is run in a new background thread created on the fly.
     *
     *
     * The refresh updates is a "light sync": properties of regular files in folder are updated (including associated
     * shares), but not their contents. Only the contents of files marked to be kept-in-sync are synchronized too.
     *
     * @param folder      Folder to refresh.
     * @param ignoreETag  If 'true', the data from the server will be fetched and sync'ed even if the eTag didn't
     * change.
     * @param ignoreFocus reloads file list even without focus, e.g. on tablet mode, focus can still be in detail view
     */
    @JvmOverloads
    fun startSyncFolderOperation(folder: OCFile?, ignoreETag: Boolean, ignoreFocus: Boolean = false) {
        Log_OC.d(TAG, "startSyncFolderOperation called, ignoreEtag: $ignoreETag, ignoreFocus: $ignoreFocus")

        if (!searchQuery.isNullOrEmpty() || user.isEmpty) {
            return
        }

        val syncFolder = Runnable { executeSyncFolderOperation(folder, ignoreETag) }

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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (!hasFocus) {
            return
        }

        pendingSyncFolderOperation?.let {
            pendingSyncFolderOperation = null
            it.run()
        }
    }

    private fun executeSyncFolderOperation(folder: OCFile?, ignoreETag: Boolean) {
        folder ?: return
        val currentUser = user.orElse(null) ?: return

        syncState = EmptyListState.LOADING

        RefreshFolderOperation(
            folder,
            System.currentTimeMillis(),
            false,
            ignoreETag,
            storageManager,
            currentUser,
            applicationContext
        ).execute(
            account,
            this,
            { _, _ -> onSyncFinished() },
            handler,
            null
        )

        fetchRecommendedFilesIfNeeded(ignoreETag, folder)
    }

    private fun fetchRecommendedFilesIfNeeded(ignoreETag: Boolean, folder: OCFile?) {
        val capability = capabilities.orElse(null) ?: return
        if (folder?.isRootDirectory == false || capability.recommendations.isFalse) return

        val accountName = user.orElse(null)?.accountName ?: return
        val fragment = listOfFilesFragment
        lifecycleScope.launch {
            val recommendedFiles = withContext(Dispatchers.IO) {
                filesRepository.fetchRecommendedFiles(accountName, ignoreETag, storageManager)
            }
            fragment?.adapter?.updateRecommendedFiles(recommendedFiles)
        }
    }

    private fun requestForDownload(file: OCFile, downloadBehaviour: String, packageName: String, activityName: String) {
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
     * Requests the download of the received [OCFile] , updates the UI to monitor the download progress and
     * prepares the activity to send the file when the download finishes.
     *
     * @param file         [OCFile] to download and preview.
     * @param packageName
     * @param activityName
     */
    fun startDownloadForSending(file: OCFile?, downloadBehaviour: String, packageName: String, activityName: String) {
        waitingToSend = file
        file?.let { requestForDownload(it, downloadBehaviour, packageName, activityName) }
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
            startSyncForFileAndIntent(file, intent)
        }
    }

    private fun startSyncForFileAndIntent(file: OCFile?, intent: Intent) {
        FileOperationsHelper(this, userAccountManager, connectivityService, editorUtils)
            .startSyncForFileAndIntent(file, intent)
    }

    private fun previewIntentFor(file: OCFile?, previewKey: String): Intent = Intent().apply {
        putExtra(EXTRA_FILE, file)
        putExtra(previewKey, true)
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

    /**
     * Starts the preview of an audio [OCFile], synchronizing it first when it is not available yet.
     */
    fun startAudioPreview(file: OCFile, showPreview: Boolean, streamMedia: Boolean) {
        // not reachable under normal conditions
        if (user.isEmpty) return

        if ((showPreview && file.isDown && !file.isDownloading) || streamMedia) {
            startAudioPlayer(file)
        } else {
            startSyncForFileAndIntent(file, previewIntentFor(file, AUDIO_PREVIEW))
        }
    }

    private fun startAudioPlayer(file: OCFile) {
        val collection = listOfFilesFragment
            ?.takeIf { it.isSearchFragment }
            ?.currentSearchType
            .toPlaybackCollection()
        audioPlayerLauncher.launch(this, file, collection)
    }

    fun configureToolbarForPreview(file: OCFile?) {
        lockScrolling()
        updateActionBarForFile(file)
    }

    /**
     * Starts the preview of a text file [OCFile].
     *
     * @param file Text [OCFile] to preview.
     */
    fun startTextPreview(file: OCFile?, showPreview: Boolean) {
        // remnants of old unsafe system; do not crash, silently stop
        val currentUser = user.orElse(null) ?: return

        if (showPreview) {
            val fragment = PreviewTextFileFragment.create(currentUser, file, searchOpen, searchQuery)
            setLeftFragment(fragment, false)
            configureToolbarForPreview(file)
            showBottomNavigationBar(false)
        } else {
            startSyncForFileAndIntent(file, previewIntentFor(file, TEXT_PREVIEW))
        }
    }

    /**
     * Starts rich workspace preview for a folder.
     *
     * @param folder [OCFile] to preview its rich workspace.
     */
    fun startRichWorkspacePreview(folder: OCFile?) {
        configureToolbarForPreview(folder)
        val textPreviewFragment = PreviewTextStringFragment().apply {
            arguments = Bundle().apply { putParcelable(EXTRA_FILE, folder) }
        }
        setLeftFragment(textPreviewFragment, false)
    }

    fun startContactListFragment(file: OCFile?) {
        val currentUser = user.orElse(null) ?: return
        ContactsPreferenceActivity.startActivityWithContactsFile(this, currentUser, file)
    }

    fun startPdfPreview(file: OCFile) {
        // prefer third party PDF apps
        if (fileOperationsHelper.canOpenFile(file)) {
            fileOperationsHelper.openFile(file)
            return
        }

        setLeftFragment(PreviewPdfFragment.newInstance(file), false)
        configureToolbarForPreview(file)
        setMainFabVisible(false)
    }

    /**
     * Requests the download of the received [OCFile] , updates the UI to monitor the download progress and
     * prepares the activity to preview or open the file when the download finishes.
     *
     * @param file         [OCFile] to download and preview.
     * @param parentFolder [OCFile] containing above file
     */
    fun startDownloadForPreview(file: OCFile, parentFolder: OCFile?) {
        if (!file.isFileEligibleForImmediatePreview) {
            user.orElse(null)?.let {
                setLeftFragment(FileDetailFragment.newInstance(file, parentFolder, it), false)
            }
        }

        configureToolbarForPreview(file)
        waitingToPreview = file
        requestForDownload()
        setFile(file)
    }

    /**
     * Opens EditImageActivity with given file loaded. If file is not available locally, it will be synced before
     * opening the image editor.
     *
     * @param file [OCFile] (image) to be loaded into image editor
     */
    fun startImageEditor(file: OCFile) {
        if (!file.isDown) {
            waitingToPreview = file
            requestForDownload(file, EditImageActivity.OPEN_IMAGE_EDITOR, packageName, javaClass.simpleName)
            return
        }

        val editImageIntent = Intent(this, EditImageActivity::class.java).apply {
            putExtra(EditImageActivity.EXTRA_FILE, file)
        }
        startActivity(editImageIntent)
    }

    /**
     * Request stopping the upload/download operation in progress over the given [OCFile] file.
     *
     * @param file [OCFile] file which operation are wanted to be cancel
     */
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

    /**
     * Request stopping all upload/download operations in progress over the given [OCFile] files.
     *
     * @param files collection of [OCFile] files which operations are wanted to be cancel
     */
    fun cancelTransference(files: MutableCollection<OCFile>) {
        files.forEach { cancelTransference(it) }
    }

    override fun onRefresh(ignoreETag: Boolean) {
        syncAndUpdateFolder(ignoreETag, ignoreFocus = false)
    }

    override fun onRefresh() {
        syncAndUpdateFolder(ignoreETag = true, ignoreFocus = false)
    }

    private fun syncAndUpdateFolder(ignoreETag: Boolean, ignoreFocus: Boolean) {
        val listOfFiles = listOfFilesFragment ?: return
        if (listOfFiles.isSearchFragment) return

        val folder = listOfFiles.currentFile ?: return
        startSyncFolderOperation(folder, ignoreETag, ignoreFocus)
    }

    override fun showFiles(onDeviceOnly: Boolean, personalFiles: Boolean) {
        super.showFiles(onDeviceOnly, personalFiles)
        refreshOrInitOCFileListFragment()
    }

    private fun refreshOrInitOCFileListFragment() {
        val ocFileListFragment = listOfFilesFragment
        if (ocFileListFragment != null &&
            ocFileListFragment !is GalleryFragment &&
            ocFileListFragment !is SharedListFragment &&
            // album fragment check will help in showing offline files screen
            // when navigating from Albums to Offline Files
            !isAlbumsFragment &&
            !isAlbumItemsFragment
        ) {
            ocFileListFragment.refreshDirectory()
        } else {
            leftFragment = OCFileListFragment()
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessageEvent(event: SearchEvent) {
        when (event.searchType) {
            SearchRemoteOperation.SearchType.PHOTO_SEARCH -> {
                Log_OC.d(this, "Switch to photo search fragment")
                leftFragment = GalleryFragment()
            }

            SearchRemoteOperation.SearchType.SHARED_FILTER -> {
                Log_OC.d(this, "Switch to Shared fragment")
                leftFragment = SharedListFragment()
            }

            else -> Unit
        }

        listOfFilesFragment?.updateSearchType(event)
        updateActionBarTitleAndHomeButton(null)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: SyncEventFinished) {
        val intent = event.intent
        val bundle = intent.extras ?: return
        val file = bundle.getParcelableArgument(EXTRA_FILE, OCFile::class.java) ?: return

        when {
            intent.getBooleanExtra(TEXT_PREVIEW, false) -> startTextPreview(file, true)

            intent.getBooleanExtra(AUDIO_PREVIEW, false) -> startAudioPreview(file, true, true)

            bundle.containsKey(PreviewMediaActivity.EXTRA_VIRTUAL_TYPE) -> startImagePreview(
                file,
                true,
                bundle.getSerializableArgument(PreviewMediaActivity.EXTRA_VIRTUAL_TYPE, VirtualFolderType::class.java),
                bundle.getSerializableArgument(PreviewMediaActivity.EXTRA_MEDIA_STATE, MediaState::class.java),
                galleryThumbnailOf(file)
            )

            else -> startImagePreview(file, true, sourceView = galleryThumbnailOf(file))
        }
    }

    private fun detailsHeaderImage(): View? =
        previewImageView?.takeIf { it.isShown && it.isLaidOut && it.drawable != null }

    private fun galleryThumbnailOf(file: OCFile): View? = leftFragment?.view
        ?.findViewWithTag<View>(file.fileId)
        ?.takeIf { it.isShown && ViewCompat.getTransitionName(it) == NavigationAnimator.sharedElementName(file) }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessageEvent(event: TokenPushEvent?) {
        if (preferences.isKeysReInitEnabled()) {
            PushUtils.pushRegistrationToServer(userAccountManager, preferences.getPushToken())
        } else {
            PushUtils.reinitKeys(userAccountManager)
        }
    }

    public override fun onStart() {
        super.onStart()

        registerReceivers()

        if (SettingsActivity.isBackPressed) {
            Log_OC.d(TAG, "User returned from settings activity, skipping reset content logic")
            return
        }

        initFile()
    }

    private fun initFile() {
        val existingUser = user.orElse(null) ?: run {
            Log_OC.w(TAG, "user is not available, cannot init file")
            return
        }

        val currentStorageManager: FileDataStorageManager = storageManager ?: run {
            Log_OC.w(TAG, "storage manager is null, cannot init file")
            return
        }

        val file = resolveInitialFile(currentStorageManager) ?: run {
            Log_OC.e(TAG, "Could not retrieve root folder – cannot continue")
            return
        }

        setFile(file)

        val existingAccountName = existingUser.accountName
        mSwitchAccountButton.tag = existingAccountName

        avatarGenerator.setAccountAvatar(
            existingUser,
            this,
            resources.getDimension(R.dimen.nav_drawer_menu_avatar_radius),
            mSwitchAccountButton
        )
        val userChanged = existingAccountName != lastDisplayedAccountName
        if (userChanged) {
            composeProcessTextAlias.configure()
            Log_OC.d(TAG, "Initializing Fragments in onAccountChanged..")
            initFragments()
            if (file.isFolder && searchQuery.isNullOrEmpty()) {
                startSyncFolderOperation(file, false)
            }
        } else {
            updateActionBarTitleAndHomeButton(if (file.isFolder) null else file)
        }

        setNewLastDisplayedAccountName(existingAccountName)
        EventBus.getDefault().post(TokenPushEvent())
        checkForNewDevVersionNecessary(applicationContext)
    }

    private fun resolveInitialFile(storageManager: FileDataStorageManager): OCFile? =
        getFile()?.let { resolveCurrentFile(it, storageManager) } ?: storageManager.getFileByPath(OCFile.ROOT_PATH)

    private fun resolveCurrentFile(file: OCFile, storageManager: FileDataStorageManager): OCFile? {
        if (!file.isDown || file.lastSyncDateForProperties != 0L) {
            return storageManager.getFileByPath(file.remotePath)
        }

        val parentPathLength = file.remotePath.lastIndexOf(file.fileName)
        if (parentPathLength <= 0) return null

        val parentPath = file.remotePath.take(parentPathLength)
        return file.takeIf { storageManager.getFileByPath(parentPath) != null }
    }

    private fun setNewLastDisplayedAccountName(accountName: String) {
        preferences.lastDisplayedAccountName = accountName
        lastDisplayedAccountName = accountName
    }

    override fun onRestart() {
        super.onRestart()
        checkForNewDevVersionNecessary(applicationContext)
    }

    fun setSearchQuery(query: String?) {
        searchQuery = query
    }

    private fun handleOpenFileViaIntent(intent: Intent) {
        SnackbarUtil.show(this, getString(R.string.retrieving_file))

        val userName = intent.getStringExtra(KEY_ACCOUNT)
        val fileId = intent.getStringExtra(KEY_FILE_ID)
        val filePath = intent.getStringExtra(KEY_FILE_PATH)

        val intentData = intent.data
        if (userName == null && fileId == null && intentData != null) {
            openDeepLink(intentData)
            return
        }

        val optionalUser = if (userName == null) user else userAccountManager.getUser(userName)
        val targetUser = optionalUser.orElse(null) ?: run {
            SnackbarUtil.show(this, getString(R.string.associated_account_not_found))
            return
        }

        when {
            !fileId.isNullOrEmpty() -> openFile(targetUser, fileId)
            !filePath.isNullOrEmpty() -> openFileByPath(targetUser, filePath)
            else -> accountClicked(targetUser)
        }
    }

    private fun openDeepLink(uri: Uri) {
        val match = DeepLinkHandler(userAccountManager).parseDeepLink(uri)

        when {
            match == null -> handleDeepLink(uri)
            match.users.isEmpty() -> SnackbarUtil.show(this, getString(R.string.associated_account_not_found))
            match.users.size == SINGLE_USER_SIZE -> openFile(match.users.first(), match.fileId)
            else -> selectUserAndOpenFile(match.users, match.fileId)
        }
    }

    private fun selectUserAndOpenFile(users: List<User>, fileId: String?) {
        val userNames = users.map { it.accountName }.toTypedArray()
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.common_choose_account)
            .setItems(userNames) { _, which ->
                openFile(users[which], fileId)
                showLoadingDialog(getString(R.string.retrieving_file))
            }

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(applicationContext, builder)

        val dialog = builder.create()
        dismissLoadingDialog()
        dialog.show()
    }

    private fun openFile(user: User?, fileId: String?) {
        setUser(user)

        if (fileId == null) {
            onFileRequestError(null)
            return
        }

        if (user == null) {
            Log_OC.e(TAG, "cannot fetch remote file, user is null")
            return
        }

        val capability = capabilities.orElse(null) ?: run {
            Log_OC.e(TAG, "cannot fetch remote file, capabilities is empty")
            return
        }

        val fileStorageManager = storageManager ?: FileDataStorageManager(user, contentResolver)
        val weakContext = WeakReference<Context>(this)
        val task = FetchRemoteFileTask(user, fileId, fileStorageManager, lifecycleScope, capability, weakContext)
        task.run(showFile = { (ocFile, message) ->
            ocFile?.let { file = it }
            showFile(ocFile, message)
        })
    }

    private fun openFileByPath(user: User, filepath: String?) {
        setUser(user)

        if (filepath == null) {
            onFileRequestError(null)
            return
        }

        val fileStorageManager = storageManager ?: FileDataStorageManager(user, contentResolver)
        val client = try {
            clientFactory.create(user)
        } catch (_: CreationException) {
            onFileRequestError(null)
            return
        }

        val getRemoteFileTask = GetRemoteFileTask(this, filepath, client, fileStorageManager, user)
        asyncRunner.postQuickTask(
            getRemoteFileTask,
            { result: GetRemoteFileTask.Result -> onFileRequestResult(result) },
            { throwable: Throwable? -> onFileRequestError(throwable) }
        )
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
        leftFragment = fileFragment

        supportFragmentManager.executePendingTransactions()

        fileFragment.onItemClicked(result.file)
    }

    fun performUnifiedSearch(query: String, listOfHiddenFiles: ArrayList<String>?) {
        val unifiedSearchFragment =
            UnifiedSearchFragment.newInstance(
                query,
                listOfHiddenFiles,
                currentDir?.decryptedRemotePath
            )
        setLeftFragment(unifiedSearchFragment, false)
    }

    fun setMainFabVisible(visible: Boolean) {
        binding.fabMain.isVisible = visible
    }

    fun showFile(selectedFile: OCFile?, message: String?) {
        getOCFileListFragmentFromFile { fragment ->
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

    override fun onFilesRemoved() {
        refreshCurrentDirectory()
    }

    private fun handleEcosystemIntent(intent: Intent?) {
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

    // region MetadataSyncJob
    private fun startMetadataSyncForRoot() {
        backgroundJobManager.startMetadataSyncJob(OCFile.ROOT_PATH, folderAlreadySynced = true)
    }

    private fun startMetadataSyncForCurrentDir() {
        val currentDirId = file?.decryptedRemotePath ?: return
        backgroundJobManager.startMetadataSyncJob(currentDirId)
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
        private const val DIALOG_TAG_SHOW_TOS = "DIALOG_TAG_SHOW_TOS"

        private const val ON_RESUMED_RESET_DELAY = 10000L
        private const val SEARCH_VIEW_FOCUS_DELAY = 100L
        private const val NO_IMMEDIATE_PREVIEW_FILE_ID = -1L
        private const val UNKNOWN_VISIBILITY = -1
        private const val NO_NEUTRAL_BUTTON = -1

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

        fun openFileIntent(context: Context?, user: User?, file: OCFile?): Intent =
            Intent(context, PreviewMediaActivity::class.java).apply {
                putExtra(EXTRA_FILE, file)
                putExtra(EXTRA_USER, user)
            }
    }
}
