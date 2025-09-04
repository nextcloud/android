/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023-2024 TSI-mc <surinder.kumar@t-systems.com>
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
import android.content.DialogInterface
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
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager.BadTokenException
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.appReview.InAppReviewHelper
import com.nextcloud.client.account.User
import com.nextcloud.client.appinfo.AppInfo
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.core.Clock
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.editimage.EditImageActivity
import com.nextcloud.client.files.DeepLinkHandler
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.jobs.download.FileDownloadWorker
import com.nextcloud.client.jobs.download.FileDownloadWorker.Companion.getDownloadAddedMessage
import com.nextcloud.client.jobs.download.FileDownloadWorker.Companion.getDownloadFinishMessage
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.jobs.upload.FileUploadWorker.Companion.getUploadFinishMessage
import com.nextcloud.client.media.PlayerServiceConnection
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.utils.IntentUtil
import com.nextcloud.model.WorkerState
import com.nextcloud.model.WorkerState.DownloadFinished
import com.nextcloud.model.WorkerState.DownloadStarted
import com.nextcloud.model.WorkerState.OfflineOperationsCompleted
import com.nextcloud.model.WorkerState.UploadFinished
import com.nextcloud.model.WorkerStateLiveData
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.isActive
import com.nextcloud.utils.extensions.lastFragment
import com.nextcloud.utils.extensions.logFileSize
import com.nextcloud.utils.fileNameValidator.FileNameValidator.checkFolderPath
import com.nextcloud.utils.view.FastScrollUtils
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.FilesBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.VirtualFolderType
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.RestoreFileVersionRemoteOperation
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.lib.resources.notifications.GetNotificationsRemoteOperation
import com.owncloud.android.operations.CopyFileOperation
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.DownloadType
import com.owncloud.android.operations.MoveFileOperation
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.operations.RenameFileOperation
import com.owncloud.android.operations.SynchronizeFileOperation
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.syncadapter.FileSyncAdapter
import com.owncloud.android.ui.CompletionCallback
import com.owncloud.android.ui.asynctasks.CheckAvailableSpaceTask
import com.owncloud.android.ui.asynctasks.CheckAvailableSpaceTask.CheckAvailableSpaceListener
import com.owncloud.android.ui.asynctasks.FetchRemoteFileTask
import com.owncloud.android.ui.asynctasks.GetRemoteFileTask
import com.owncloud.android.ui.dialog.SendShareDialog
import com.owncloud.android.ui.dialog.SendShareDialog.SendShareDialogDownloader
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment.OnSortingOrderListener
import com.owncloud.android.ui.dialog.StoragePermissionDialogFragment
import com.owncloud.android.ui.dialog.TermsOfServiceDialog
import com.owncloud.android.ui.events.SearchEvent
import com.owncloud.android.ui.events.SyncEventFinished
import com.owncloud.android.ui.events.TokenPushEvent
import com.owncloud.android.ui.fragment.FileDetailFragment
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.fragment.GalleryFragment
import com.owncloud.android.ui.fragment.GroupfolderListFragment
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.ui.fragment.SearchType
import com.owncloud.android.ui.fragment.SharedListFragment
import com.owncloud.android.ui.fragment.TaskRetainerFragment
import com.owncloud.android.ui.fragment.UnifiedSearchFragment
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.ui.helpers.UriUploader
import com.owncloud.android.ui.interfaces.TransactionInterface
import com.owncloud.android.ui.preview.PreviewImageActivity
import com.owncloud.android.ui.preview.PreviewImageFragment
import com.owncloud.android.ui.preview.PreviewMediaActivity
import com.owncloud.android.ui.preview.PreviewMediaFragment
import com.owncloud.android.ui.preview.PreviewMediaFragment.Companion.newInstance
import com.owncloud.android.ui.preview.PreviewTextFileFragment
import com.owncloud.android.ui.preview.PreviewTextFragment
import com.owncloud.android.ui.preview.PreviewTextStringFragment
import com.owncloud.android.ui.preview.pdf.PreviewPdfFragment.Companion.newInstance
import com.owncloud.android.utils.DataHolderUtil
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.ErrorMessageAdapter
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.PermissionUtil.requestExternalStoragePermission
import com.owncloud.android.utils.PermissionUtil.requestNotificationPermission
import com.owncloud.android.utils.PushUtils
import com.owncloud.android.utils.StringUtils
import com.owncloud.android.utils.theme.CapabilityUtils
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.util.function.Supplier
import javax.inject.Inject

/**
 * Displays, what files the user has available in his Nextcloud. This is the main view.
 */
@Suppress(
    "ComplexCondition",
    "SpreadOperator",
    "ForbiddenComment",
    "ReturnCount",
    "LargeClass",
    "NestedBlockDepth",
    "TooManyFunctions"
)
class FileDisplayActivity :
    FileActivity(),
    FileFragment.ContainerActivity,
    OnEnforceableRefreshListener,
    OnSortingOrderListener,
    SendShareDialogDownloader,
    Injectable {
    private lateinit var binding: FilesBinding

    private var mSyncBroadcastReceiver: SyncBroadcastReceiver? = null
    private var mUploadFinishReceiver: UploadFinishReceiver? = null
    private var mDownloadFinishReceiver: DownloadFinishReceiver? = null
    private var mLastSslUntrustedServerResult: RemoteOperationResult<*>? = null

    private var mWaitingToPreview: OCFile? = null

    private var mSyncInProgress = false

    private var mWaitingToSend: OCFile? = null

    private var mDrawerMenuItemstoShowHideList: MutableCollection<MenuItem>? = null

    private var searchQuery: String? = ""
    private var searchOpen = false

    private var searchView: SearchView? = null
    private var mPlayerConnection: PlayerServiceConnection? = null
    private var lastDisplayedAccountName: String? = null

    @Inject
    lateinit var localBroadcastManager: LocalBroadcastManager

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

    /**
     * Indicates whether the downloaded file should be previewed immediately. Since `FileDownloadWorker` can be
     * triggered from multiple sources, this helps determine if an automatic preview is needed after download.
     */
    private var fileIDForImmediatePreview: Long = -1

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

        intent?.let {
            handleCommonIntents(it)
            handleAccountSwitchIntent(it)
        }

        loadSavedInstanceState(savedInstanceState)

        /** USER INTERFACE */
        initLayout()
        initUI()
        initTaskRetainerFragment()

        // Restoring after UI has been inflated.
        if (savedInstanceState != null) {
            showSortListGroup(savedInstanceState.getBoolean(KEY_IS_SORT_GROUP_VISIBLE))
        }

        mPlayerConnection = PlayerServiceConnection(this)

        checkStoragePath()

        initSyncBroadcastReceiver()
        observeWorkerState()
        startMetadataSyncForRoot()
    }

    private fun loadSavedInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            mWaitingToPreview =
                savedInstanceState.getParcelableArgument(KEY_WAITING_TO_PREVIEW, OCFile::class.java)
            mSyncInProgress = savedInstanceState.getBoolean(KEY_SYNC_IN_PROGRESS)
            mWaitingToSend = savedInstanceState.getParcelableArgument(KEY_WAITING_TO_SEND, OCFile::class.java)
            searchQuery = savedInstanceState.getString(KEY_SEARCH_QUERY)
            searchOpen = savedInstanceState.getBoolean(KEY_IS_SEARCH_OPEN, false)
        } else {
            mWaitingToPreview = null
            mSyncInProgress = false
            mWaitingToSend = null
        }
    }

    private fun initLayout() {
        // Inflate and set the layout view
        binding = FilesBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())
    }

    private fun initUI() {
        setupHomeSearchToolbarWithSortAndListButtons()
        mMenuButton.setOnClickListener { v: View? -> openDrawer() }
        mSwitchAccountButton.setOnClickListener { v: View? -> showManageAccountsDialog() }
        mNotificationButton.setOnClickListener { v: View? -> startActivity(NotificationsActivity::class.java) }
        fastScrollUtils.fixAppBarForFastScroll(binding.appbar.appbar, binding.rootLayout)
    }

    private fun initTaskRetainerFragment() {
        // Init Fragment without UI to retain AsyncTask across configuration changes
        val fm = supportFragmentManager
        var taskRetainerFragment =
            fm.findFragmentByTag(TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT) as TaskRetainerFragment?
        if (taskRetainerFragment == null) {
            taskRetainerFragment = TaskRetainerFragment()
            fm.beginTransaction().add(taskRetainerFragment, TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT).commit()
        } // else, Fragment already created and retained across configuration change
    }

    private fun checkStoragePath() {
        val newStorage = Environment.getExternalStorageDirectory().absolutePath
        val storagePath = preferences.getStoragePath(newStorage)
        if (!preferences.isStoragePathValid() && !File(storagePath).exists()) {
            // falling back to default
            preferences.setStoragePath(newStorage)
            preferences.setStoragePathValid()
            MainApp.setStoragePath(newStorage)

            try {
                val builder = MaterialAlertDialogBuilder(this, R.style.Theme_ownCloud_Dialog)
                    .setTitle(R.string.wrong_storage_path)
                    .setMessage(R.string.wrong_storage_path_desc)
                    .setPositiveButton(
                        R.string.dialog_close
                    ) { dialog: DialogInterface?, which: Int -> dialog?.dismiss() }
                    .setIcon(R.drawable.ic_settings)

                viewThemeUtils.dialog.colorMaterialAlertDialogBackground(applicationContext, builder)

                builder.create().show()
            } catch (e: BadTokenException) {
                Log_OC.e(TAG, "Error showing wrong storage info, so skipping it: " + e.message)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val fragment =
                supportFragmentManager.findFragmentByTag(
                    PermissionUtil.PERMISSION_CHOICE_DIALOG_TAG
                ) as StoragePermissionDialogFragment?
            if (fragment != null) {
                val dialog = fragment.dialog

                if (dialog != null && dialog.isShowing) {
                    dialog.dismiss()
                    supportFragmentManager.beginTransaction().remove(fragment).commitNowAllowingStateLoss()
                    requestExternalStoragePermission(this, viewThemeUtils)
                }
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // handle notification permission on API level >= 33
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // request notification permission first and then prompt for storage permissions
            // storage permissions handled in onRequestPermissionsResult
            requestNotificationPermission(this)
        } else {
            requestExternalStoragePermission(this, viewThemeUtils)
        }

        if (intent.getParcelableArgument(
                OCFileListFragment.SEARCH_EVENT,
                SearchEvent::class.java
            ) != null
        ) {
            switchToSearchFragment(savedInstanceState)
            setupDrawer()
        } else {
            createMinFragments(savedInstanceState)
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
        if (preferences.instantPictureUploadEnabled() || preferences.instantVideoUploadEnabled()) {
            preferences.removeLegacyPreferences()
            // show info pop-up
            MaterialAlertDialogBuilder(this, R.style.Theme_ownCloud_Dialog).setTitle(R.string.drawer_synced_folders)
                .setMessage(
                    R.string.synced_folders_new_info
                ).setPositiveButton(
                    R.string.drawer_open
                ) { dialog: DialogInterface?, which: Int ->
                    // show instant upload
                    val syncedFoldersIntent = Intent(applicationContext, SyncedFoldersActivity::class.java)
                    dialog?.dismiss()
                    startActivity(syncedFoldersIntent)
                }.setNegativeButton(
                    R.string.drawer_close
                ) { dialog: DialogInterface?, which: Int -> dialog?.dismiss() }
                .setIcon(
                    R.drawable.nav_synced_folders
                ).show()
        }
    }

    private fun checkOutdatedServer() {
        val user = getUser()
        // show outdated warning
        if (user.isPresent &&
            CapabilityUtils.checkOutdatedWarning(
                getResources(),
                user.get().server.version,
                capabilities.extendedSupport.isTrue
            )
        ) {
            DisplayUtils.showServerOutdatedSnackbar(this, Snackbar.LENGTH_LONG)
        }
    }

    private fun checkNotifications() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = GetNotificationsRemoteOperation()
                    .execute(clientFactory.createNextcloudClient(accountManager.user))

                if (result.isSuccess && result.getResultData()?.isEmpty() == false) {
                    runOnUiThread { mNotificationButton.visibility = View.VISIBLE }
                } else {
                    runOnUiThread { mNotificationButton.visibility = View.GONE }
                }
            } catch (_: CreationException) {
                Log_OC.e(TAG, "Could not fetch notifications!")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            // handle notification permission on API level >= 33
            PermissionUtil.PERMISSIONS_POST_NOTIFICATIONS ->
                // dialogue was dismissed -> prompt for storage permissions
                requestExternalStoragePermission(this, viewThemeUtils)

            // If request is cancelled, result arrays are empty.
            PermissionUtil.PERMISSIONS_EXTERNAL_STORAGE ->
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    EventBus.getDefault().post(TokenPushEvent())
                    // toggle on is save since this is the only scenario this code gets accessed
                }

            // If request is cancelled, result arrays are empty.
            PermissionUtil.PERMISSIONS_CAMERA ->
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    getOCFileListFragmentFromFile(object : TransactionInterface {
                        override fun onOCFileListFragmentComplete(fragment: OCFileListFragment) {
                            fragment.directCameraUpload()
                        }
                    })
                }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun switchToSearchFragment(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            val listOfFiles = OCFileListFragment()
            val args = Bundle()

            args.putParcelable(
                OCFileListFragment.SEARCH_EVENT,
                intent
                    .getParcelableArgument(
                        OCFileListFragment.SEARCH_EVENT,
                        SearchEvent::class.java
                    )
            )
            args.putBoolean(OCFileListFragment.ARG_ALLOW_CONTEXTUAL_ACTIONS, true)

            listOfFiles.setArguments(args)
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.left_fragment_container, listOfFiles, TAG_LIST_OF_FILES)
            transaction.commit()
        } else {
            supportFragmentManager.findFragmentByTag(TAG_LIST_OF_FILES)
        }
    }

    private fun createMinFragments(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            val listOfFiles = OCFileListFragment()
            val args = Bundle()
            args.putBoolean(OCFileListFragment.ARG_ALLOW_CONTEXTUAL_ACTIONS, true)
            listOfFiles.setArguments(args)
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.left_fragment_container, listOfFiles, TAG_LIST_OF_FILES)
            transaction.commit()
        } else {
            supportFragmentManager.findFragmentByTag(TAG_LIST_OF_FILES)
        }
    }

    private fun initFragments() {
        /** First fragment */
        val listOfFiles = this.listOfFilesFragment
        if (listOfFiles != null && TextUtils.isEmpty(searchQuery)) {
            listOfFiles.listDirectory(getCurrentDir(), file, MainApp.isOnlyOnDevice(), false)
        } else {
            Log_OC.e(TAG, "Still have a chance to lose the initialization of list fragment >(")
        }

        /** reset views */
        resetTitleBarAndScrolling()
    }

    // region Handle Intents
    @SuppressLint("UnsafeIntentLaunch")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCommonIntents(intent)
        handleSpecialIntents(intent)
        handleRestartIntent(intent)
    }

    private fun handleSpecialIntents(intent: Intent) {
        val action = intent.action

        when {
            ACTION_DETAILS.equals(action, ignoreCase = true) -> {
                val file = intent.getParcelableArgument(EXTRA_FILE, OCFile::class.java)
                setFile(file)
                showDetails(file)
            }

            Intent.ACTION_SEARCH == action -> handleSearchIntent(intent)

            ALL_FILES == action -> {
                Log_OC.d(this, "Switch to oc file fragment")
                menuItemId = R.id.nav_all_files
                leftFragment = OCFileListFragment()
                supportFragmentManager.executePendingTransactions()
                browseToRoot()
            }

            LIST_GROUPFOLDERS == action -> {
                Log_OC.d(this, "Switch to list groupfolders fragment")
                menuItemId = R.id.nav_groupfolders
                leftFragment = GroupfolderListFragment()
                supportFragmentManager.executePendingTransactions()
            }
        }
    }

    @SuppressLint("UnsafeIntentLaunch")
    private fun handleCommonIntents(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> handleOpenFileViaIntent(intent)
            OPEN_FILE -> {
                supportFragmentManager.executePendingTransactions()
                onOpenFileIntent(intent)
            }
        }
    }

    private fun handleRestartIntent(intent: Intent) {
        if (intent.action != RESTART) {
            return
        }

        finish()
        startActivity(intent)
    }

    private fun handleAccountSwitchIntent(intent: Intent) {
        if (intent.action != RESTART) {
            return
        }

        val accountName = accountManager.user.accountName
        val message = getString(R.string.logged_in_as)
        val snackBarMessage = String.format(message, accountName)
        DisplayUtils.showSnackMessage(this, snackBarMessage)
    }

    private fun handleSearchIntent(intent: Intent) {
        val searchEvent = intent.getParcelableArgument(
            OCFileListFragment.SEARCH_EVENT,
            SearchEvent::class.java
        ) ?: return

        when (searchEvent.searchType) {
            SearchRemoteOperation.SearchType.PHOTO_SEARCH -> {
                Log_OC.d(this, "Switch to photo search fragment")
                val bundle = Bundle().apply {
                    putParcelable(OCFileListFragment.SEARCH_EVENT, searchEvent)
                }
                leftFragment = GalleryFragment().apply {
                    arguments = bundle
                }
            }

            SearchRemoteOperation.SearchType.SHARED_FILTER -> {
                Log_OC.d(this, "Switch to shared fragment")
                val bundle = Bundle().apply {
                    putParcelable(OCFileListFragment.SEARCH_EVENT, searchEvent)
                }
                leftFragment = SharedListFragment().apply {
                    arguments = bundle
                }
            }

            else -> {
                Log_OC.d(this, "Switch to oc file search fragment")
                val bundle = Bundle().apply {
                    putParcelable(OCFileListFragment.SEARCH_EVENT, searchEvent)
                }
                leftFragment = OCFileListFragment().apply {
                    arguments = bundle
                }
            }
        }
    }
    // endregion

    private fun onOpenFileIntent(intent: Intent) {
        val extra = intent.getStringExtra(EXTRA_FILE)
        val file = storageManager.getFileByDecryptedRemotePath(extra)
        if (file != null) {
            val fileFragment: OCFileListFragment?
            val leftFragment = this.leftFragment
            if (leftFragment is OCFileListFragment) {
                fileFragment = leftFragment
            } else {
                fileFragment = OCFileListFragment()
                this.leftFragment = fileFragment
            }
            fileFragment.onItemClicked(file)
        }
    }

    private fun setLeftFragment(fragment: Fragment?, showSortListGroup: Boolean) {
        if (fragment == null) {
            return
        }

        prepareFragmentBeforeCommit(showSortListGroup)
        commitFragment(
            fragment,
            object : CompletionCallback {
                override fun onComplete(isFragmentCommitted: Boolean) {
                    Log_OC.d(
                        TAG,
                        "Left fragment committed: $isFragmentCommitted"
                    )
                }
            }
        )
    }

    private fun prepareFragmentBeforeCommit(showSortListGroup: Boolean) {
        searchView?.post { searchView?.setQuery(searchQuery, true) }
        setDrawerIndicatorEnabled(false)

        // clear the subtitle while navigating to any other screen from Media screen
        clearToolbarSubtitle()

        showSortListGroup(showSortListGroup)
    }

    private fun commitFragment(fragment: Fragment, callback: CompletionCallback) {
        val fragmentManager = supportFragmentManager
        if (this.isActive() && !fragmentManager.isDestroyed) {
            val transaction = fragmentManager.beginTransaction()
            transaction.addToBackStack(null)
            transaction.replace(R.id.left_fragment_container, fragment, TAG_LIST_OF_FILES)
            transaction.commit()
            callback.onComplete(true)
        } else {
            callback.onComplete(false)
        }
    }

    private fun getOCFileListFragmentFromFile(transaction: TransactionInterface) {
        val leftFragment = this.leftFragment

        if (leftFragment is OCFileListFragment) {
            transaction.onOCFileListFragmentComplete(leftFragment)
            return
        }

        val listOfFiles = OCFileListFragment()
        val args = Bundle()
        args.putBoolean(OCFileListFragment.ARG_ALLOW_CONTEXTUAL_ACTIONS, true)
        listOfFiles.setArguments(args)

        runOnUiThread {
            val fm = supportFragmentManager
            if (!fm.isStateSaved && !fm.isDestroyed) {
                prepareFragmentBeforeCommit(true)
                commitFragment(
                    listOfFiles,
                    object : CompletionCallback {
                        override fun onComplete(value: Boolean) {
                            if (value) {
                                Log_OC.d(TAG, "OCFileListFragment committed, executing pending transaction")
                                fm.executePendingTransactions()
                                transaction.onOCFileListFragmentComplete(listOfFiles)
                            } else {
                                Log_OC.d(
                                    TAG,
                                    "OCFileListFragment not committed, skipping executing " +
                                        "pending transaction"
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    fun showFileActions(file: OCFile?) {
        dismissLoadingDialog()
        getOCFileListFragmentFromFile(object : TransactionInterface {
            override fun onOCFileListFragmentComplete(fragment: OCFileListFragment) {
                browseUp(fragment)
                fragment.onOverflowIconClicked(file, null)
            }
        })
    }

    var leftFragment: Fragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_LIST_OF_FILES)

        /**
         * Replaces the first fragment managed by the activity with the received as a parameter.
         *
         * @param fragment New Fragment to set.
         */
        private set(fragment) {
            setLeftFragment(fragment, true)
        }

    @get:Deprecated("")
    val listOfFilesFragment: OCFileListFragment?
        get() {
            val listOfFiles =
                supportFragmentManager.findFragmentByTag(TAG_LIST_OF_FILES)
            if (listOfFiles is OCFileListFragment) {
                return listOfFiles
            }
            Log_OC.e(TAG, "Access to unexisting list of files fragment")
            return null
        }

    protected fun resetTitleBarAndScrolling() {
        updateActionBarTitleAndHomeButton(null)
        resetScrolling(true)
    }

    fun updateListOfFilesFragment(fromSearch: Boolean) {
        val fileListFragment = this.listOfFilesFragment
        fileListFragment?.listDirectory(MainApp.isOnlyOnDevice(), fromSearch)
    }

    fun resetSearchView() {
        val fileListFragment = this.listOfFilesFragment
        fileListFragment?.isSearchFragment = false
    }

    protected fun refreshDetailsFragmentIfVisible(
        downloadEvent: String,
        downloadedRemotePath: String,
        success: Boolean
    ) {
        val leftFragment = this.leftFragment
        if (leftFragment is FileDetailFragment) {
            val waitedPreview = mWaitingToPreview != null && mWaitingToPreview?.remotePath == downloadedRemotePath
            val fileInFragment = leftFragment.file
            if (fileInFragment != null && downloadedRemotePath != fileInFragment.remotePath) {
                // the user browsed to other file ; forget the automatic preview
                mWaitingToPreview = null
            } else if (downloadEvent == getDownloadAddedMessage()) {
                // grant that the details fragment updates the progress bar
                leftFragment.listenForTransferProgress()
                leftFragment.updateFileDetails(true, false)
            } else if (downloadEvent == getDownloadFinishMessage()) {
                //  update the details panel
                var detailsFragmentChanged = false
                if (waitedPreview) {
                    if (success) {
                        // update the file from database, for the local storage path
                        mWaitingToPreview = mWaitingToPreview?.fileId?.let { storageManager.getFileById(it) }

                        if (PreviewMediaActivity.Companion.canBePreviewed(mWaitingToPreview)) {
                            mWaitingToPreview?.let {
                                startMediaPreview(it, 0, true, true, true, true)
                                detailsFragmentChanged = true
                            }
                        } else if (MimeTypeUtil.isVCard(mWaitingToPreview?.mimeType)) {
                            startContactListFragment(mWaitingToPreview)
                            detailsFragmentChanged = true
                        } else if (PreviewTextFileFragment.canBePreviewed(mWaitingToPreview)) {
                            startTextPreview(mWaitingToPreview, true)
                            detailsFragmentChanged = true
                        } else if (MimeTypeUtil.isPDF(mWaitingToPreview)) {
                            mWaitingToPreview?.let {
                                startPdfPreview(it)
                                detailsFragmentChanged = true
                            }
                        } else {
                            fileOperationsHelper.openFile(mWaitingToPreview)
                        }
                    }
                    mWaitingToPreview = null
                }
                if (!detailsFragmentChanged) {
                    leftFragment.updateFileDetails(false, success)
                }
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (mDrawerMenuItemstoShowHideList != null) {
            val drawerOpen = isDrawerOpen
            for (menuItem in mDrawerMenuItemstoShowHideList) {
                menuItem.isVisible = !drawerOpen
            }
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.activity_file_display, menu)

        menu.findItem(R.id.action_select_all).isVisible = false
        val searchMenuItem = menu.findItem(R.id.action_search)
        searchView = MenuItemCompat.getActionView(searchMenuItem) as SearchView?
        searchMenuItem.isVisible = false
        mSearchText.setOnClickListener { v: View? ->
            showSearchView()
            searchView?.isIconified = false
        }

        searchView?.let { viewThemeUtils.androidx.themeToolbarSearchView(it) }

        // populate list of menu items to show/hide when drawer is opened/closed
        mDrawerMenuItemstoShowHideList = ArrayList<MenuItem>(1)
        mDrawerMenuItemstoShowHideList?.add(searchMenuItem)

        // focus the SearchView
        if (!TextUtils.isEmpty(searchQuery)) {
            searchView?.post {
                searchView?.isIconified = false
                searchView?.setQuery(searchQuery, true)
                searchView?.clearFocus()
            }
        }

        val mSearchEditFrame = searchView?.findViewById<View>(androidx.appcompat.R.id.search_edit_frame)

        searchView?.setOnCloseListener {
            if (TextUtils.isEmpty(searchView?.query.toString())) {
                searchView?.onActionViewCollapsed()
                setDrawerIndicatorEnabled(isDrawerIndicatorAvailable) // order matters
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                mDrawerToggle.syncState()

                val ocFileListFragment = this.listOfFilesFragment
                if (ocFileListFragment != null) {
                    ocFileListFragment.isSearchFragment = false
                    ocFileListFragment.refreshDirectory()
                }
            } else {
                searchView?.post { searchView?.setQuery("", true) }
            }
            true
        }

        val vto = mSearchEditFrame?.viewTreeObserver
        vto?.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            var oldVisibility: Int = -1

            override fun onGlobalLayout() {
                val currentVisibility = mSearchEditFrame.visibility

                if (currentVisibility != oldVisibility) {
                    if (currentVisibility == View.VISIBLE) {
                        setDrawerIndicatorEnabled(false)
                    }

                    oldVisibility = currentVisibility
                }
            }
        })

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var retval = true

        val itemId = item.itemId

        if (itemId == android.R.id.home) {
            if (!isDrawerOpen &&
                !isSearchOpen() &&
                isRoot(getCurrentDir()) &&
                this.leftFragment is OCFileListFragment
            ) {
                openDrawer()
            } else {
                onBackPressed()
            }
        } else if (itemId == R.id.action_select_all) {
            val fragment = this.listOfFilesFragment
            fragment?.selectAllFiles(true)
        } else {
            retval = super.onOptionsItemSelected(item)
        }

        return retval
    }

    /**
     * Called, when the user selected something for uploading
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null &&
            requestCode == REQUEST_CODE__SELECT_CONTENT_FROM_APPS &&
            (resultCode == RESULT_OK || resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE)
        ) {
            requestUploadOfContentFromApps(data, resultCode)
        } else if (data != null &&
            requestCode == REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM &&
            (
                resultCode == RESULT_OK ||
                    resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE ||
                    resultCode == UploadFilesActivity.RESULT_OK_AND_DO_NOTHING ||
                    resultCode == UploadFilesActivity.RESULT_OK_AND_DELETE
                )
        ) {
            requestUploadOfFilesFromFileSystem(data, resultCode)
        } else if ((
                requestCode == REQUEST_CODE__UPLOAD_FROM_CAMERA ||
                    requestCode == REQUEST_CODE__UPLOAD_FROM_VIDEO_CAMERA
                ) &&
            (resultCode == RESULT_OK || resultCode == UploadFilesActivity.RESULT_OK_AND_DELETE)
        ) {
            CheckAvailableSpaceTask(
                object : CheckAvailableSpaceListener {
                    override fun onCheckAvailableSpaceStart() {
                        Log_OC.d(this, "onCheckAvailableSpaceStart")
                    }

                    override fun onCheckAvailableSpaceFinish(
                        hasEnoughSpaceAvailable: Boolean,
                        vararg filesToUpload: String?
                    ) {
                        Log_OC.d(this, "onCheckAvailableSpaceFinish")

                        if (hasEnoughSpaceAvailable) {
                            val file = File(filesToUpload[0])
                            val renamedFile = if (requestCode == REQUEST_CODE__UPLOAD_FROM_CAMERA) {
                                File(file.parent + OCFile.PATH_SEPARATOR + FileOperationsHelper.getCapturedImageName())
                            } else {
                                File(file.parent + OCFile.PATH_SEPARATOR + FileOperationsHelper.getCapturedVideoName())
                            }

                            if (!file.renameTo(renamedFile)) {
                                DisplayUtils.showSnackMessage(
                                    this@FileDisplayActivity,
                                    R.string.error_uploading_direct_camera_upload
                                )
                                return
                            }

                            requestUploadOfFilesFromFileSystem(
                                renamedFile.parentFile?.absolutePath,
                                arrayOf(renamedFile.absolutePath),
                                FileUploadWorker.LOCAL_BEHAVIOUR_DELETE
                            )
                        }
                    }
                },
                *arrayOf<String>(
                    FileOperationsHelper.createCameraFile(
                        this@FileDisplayActivity,
                        requestCode == REQUEST_CODE__UPLOAD_FROM_VIDEO_CAMERA
                    ).absolutePath
                )
            ).execute()
        } else if (requestCode == REQUEST_CODE__MOVE_OR_COPY_FILES && resultCode == RESULT_OK) {
            exitSelectionMode()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun exitSelectionMode() {
        val ocFileListFragment = this.listOfFilesFragment
        ocFileListFragment?.exitSelectionMode()
    }

    private fun requestUploadOfFilesFromFileSystem(data: Intent, resultCode: Int) {
        val filePaths = data.getStringArrayExtra(UploadFilesActivity.EXTRA_CHOSEN_FILES) ?: return
        val basePath = data.getStringExtra(UploadFilesActivity.LOCAL_BASE_PATH)
        requestUploadOfFilesFromFileSystem(basePath, filePaths, resultCode)
    }

    private fun getRemotePaths(directory: String?, filePaths: Array<String>, localBasePath: String): Array<String> =
        Array(filePaths.size) { j ->
            val relativePath = StringUtils.removePrefix(filePaths[j], localBasePath)
            (directory ?: "") + relativePath
        }

    private fun requestUploadOfFilesFromFileSystem(localBasePath: String?, filePaths: Array<String>, resultCode: Int) {
        var localBasePath = localBasePath
        if (localBasePath != null) {
            if (!localBasePath.endsWith("/")) {
                localBasePath = "$localBasePath/"
            }

            val remotePathBase = getCurrentDir().remotePath
            val decryptedRemotePaths = getRemotePaths(remotePathBase, filePaths, localBasePath)

            val behaviour = when (resultCode) {
                UploadFilesActivity.RESULT_OK_AND_MOVE -> FileUploadWorker.LOCAL_BEHAVIOUR_MOVE
                UploadFilesActivity.RESULT_OK_AND_DELETE -> FileUploadWorker.LOCAL_BEHAVIOUR_DELETE
                else -> FileUploadWorker.LOCAL_BEHAVIOUR_FORGET
            }

            connectivityService.isNetworkAndServerAvailable { result: Boolean? ->
                if (result == true) {
                    val isValidFolderPath = checkFolderPath(remotePathBase, capabilities, this)
                    if (!isValidFolderPath) {
                        DisplayUtils.showSnackMessage(
                            this,
                            R.string.file_name_validator_error_contains_reserved_names_or_invalid_characters
                        )
                        return@isNetworkAndServerAvailable
                    }

                    FileUploadHelper.Companion.instance().uploadNewFiles(
                        user.orElseThrow(
                            Supplier { RuntimeException() }
                        ),
                        filePaths,
                        decryptedRemotePaths,
                        behaviour,
                        true,
                        UploadFileOperation.CREATED_BY_USER,
                        false,
                        false,
                        NameCollisionPolicy.ASK_USER
                    )
                } else {
                    fileDataStorageManager.addCreateFileOfflineOperation(filePaths, decryptedRemotePaths)
                }
            }
        } else {
            Log_OC.d(TAG, "User clicked on 'Update' with no selection")
            DisplayUtils.showSnackMessage(this, R.string.filedisplay_no_file_selected)
        }
    }

    private fun requestUploadOfContentFromApps(contentIntent: Intent, resultCode: Int) {
        val streamsToUpload = ArrayList<Parcelable?>()

        if (contentIntent.clipData != null && (contentIntent.clipData?.itemCount ?: 0) > 0) {
            for (i in 0..<(contentIntent.clipData?.itemCount ?: 0)) {
                streamsToUpload.add(contentIntent.clipData?.getItemAt(i)?.uri)
            }
        } else {
            streamsToUpload.add(contentIntent.data)
        }

        val behaviour =
            if (resultCode ==
                UploadFilesActivity.RESULT_OK_AND_MOVE
            ) {
                FileUploadWorker.LOCAL_BEHAVIOUR_MOVE
            } else {
                FileUploadWorker.LOCAL_BEHAVIOUR_COPY
            }

        val currentDir = getCurrentDir()
        val remotePath = if (currentDir != null) currentDir.remotePath else OCFile.ROOT_PATH

        val uploader = UriUploader(
            this,
            streamsToUpload,
            remotePath,
            user.orElseThrow(
                Supplier { RuntimeException() }
            ),
            behaviour,
            false, // Not show waiting dialog while file is being copied from private storage
            null // Not needed copy temp task listener
        )

        uploader.uploadUris()
    }

    private fun isSearchOpen(): Boolean {
        if (searchView == null) {
            return false
        } else {
            val mSearchEditFrame = searchView?.findViewById<View?>(androidx.appcompat.R.id.search_edit_frame)
            return mSearchEditFrame != null && mSearchEditFrame.isVisible
        }
    }

    private val isRootDirectory: Boolean
        get() {
            val currentDir = getCurrentDir()
            return (currentDir == null || currentDir.parentId == FileDataStorageManager.ROOT_PARENT_ID.toLong())
        }

    /*
     * BackPressed priority/hierarchy:
     *    1. close search view if opened
     *    2. close drawer if opened
     *    3. if it is OCFileListFragment and it's in Root -> (finish Activity) or it's not Root -> (browse up)
     *    4. otherwise pop up the fragment and sortGroup view visibility and call super.onBackPressed()
     */
    @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING") // TODO Apply fail fast principle
    override fun onBackPressed() {
        if (isSearchOpen()) {
            resetSearchAction()
            return
        }

        if (isDrawerOpen) {
            super.onBackPressed()
            return
        }

        if (this.leftFragment is OCFileListFragment) {
            if (isRoot(getCurrentDir())) {
                finish()
            } else {
                browseUp(leftFragment as OCFileListFragment)
            }
        } else {
            popBack()
        }
    }

    private fun browseUp(listOfFiles: OCFileListFragment) {
        listOfFiles.onBrowseUp()
        val currentFile = listOfFiles.currentFile

        file = currentFile
        listOfFiles.setFabVisible(currentFile.canCreateFileAndFolder())
        listOfFiles.registerFabListener()
        resetTitleBarAndScrolling()
        setDrawerAllFiles()
        startMetadataSyncForCurrentDir()
    }

    private fun resetSearchAction() {
        val leftFragment = this.leftFragment
        if (!isSearchOpen() || searchView == null) {
            return
        }

        searchView?.setQuery("", true)
        searchView?.onActionViewCollapsed()
        searchView?.clearFocus()

        if (isRoot(getCurrentDir()) && leftFragment is OCFileListFragment) {
            // Remove the list to the original state

            val listOfHiddenFiles = leftFragment.adapter.listOfHiddenFiles
            leftFragment.performSearch("", listOfHiddenFiles, true)

            hideSearchView(getCurrentDir())
            setDrawerIndicatorEnabled(isDrawerIndicatorAvailable)
        }

        if (leftFragment is UnifiedSearchFragment) {
            showSortListGroup(false)
            super.onBackPressed()
        }
    }

    /**
     * Use this method when want to pop the fragment on back press. It resets Scrolling (See
     * [with true][.resetScrolling] and pop the visibility for sortListGroup (See
     * [with false][.showSortListGroup]. At last call to super.onBackPressed()
     */
    private fun popBack() {
        binding.fabMain.setImageResource(R.drawable.ic_plus)
        resetScrolling(true)
        showSortListGroup(false)
        super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // responsibility of restore is preferred in onCreate() before than in
        // onRestoreInstanceState when there are Fragments involved
        super.onSaveInstanceState(outState)
        mWaitingToPreview.logFileSize(TAG)
        outState.putParcelable(KEY_WAITING_TO_PREVIEW, mWaitingToPreview)
        outState.putBoolean(KEY_SYNC_IN_PROGRESS, mSyncInProgress)
        // outState.putBoolean(FileDisplayActivity.KEY_REFRESH_SHARES_IN_PROGRESS,
        // mRefreshSharesInProgress);
        outState.putParcelable(KEY_WAITING_TO_SEND, mWaitingToSend)
        if (searchView != null) {
            outState.putBoolean(KEY_IS_SEARCH_OPEN, searchView?.isIconified == false)
        }
        outState.putString(KEY_SEARCH_QUERY, searchQuery)
        outState.putBoolean(KEY_IS_SORT_GROUP_VISIBLE, sortListGroupVisibility())
        Log_OC.v(TAG, "onSaveInstanceState() end")
    }

    override fun onResume() {
        Log_OC.v(TAG, "onResume() start")
        super.onResume()
        isFileDisplayActivityResumed = true

        // Instead of onPostCreate, starting the loading in onResume for children fragments
        val leftFragment = this.leftFragment

        // Listen for sync messages
        if (leftFragment !is OCFileListFragment || !leftFragment.isSearchFragment) {
            initSyncBroadcastReceiver()
        }

        if (leftFragment !is OCFileListFragment) {
            if (leftFragment is FileFragment) {
                super.updateActionBarTitleAndHomeButton(leftFragment.file)
            }
            return
        }

        val ocFileListFragment = leftFragment

        ocFileListFragment.setLoading(mSyncInProgress)
        syncAndUpdateFolder(ignoreETag = true, ignoreFocus = true)

        var startFile: OCFile? = null
        if (intent != null) {
            val fileArgs = intent.getParcelableArgument(EXTRA_FILE, OCFile::class.java)
            if (fileArgs != null) {
                startFile = fileArgs
                file = startFile
            }
        }

        // refresh list of files
        if (searchView != null && !TextUtils.isEmpty(searchQuery)) {
            searchView?.setQuery(searchQuery, false)
        } else if (!ocFileListFragment.isSearchFragment && startFile == null) {
            updateListOfFilesFragment(false)
            ocFileListFragment.registerFabListener()
        } else {
            ocFileListFragment.listDirectory(startFile, false, false)
            updateActionBarTitleAndHomeButton(startFile)
        }

        // Listen for upload messages
        val uploadIntentFilter = IntentFilter(getUploadFinishMessage())
        mUploadFinishReceiver = UploadFinishReceiver()
        localBroadcastManager.registerReceiver(mUploadFinishReceiver!!, uploadIntentFilter)

        // Listen for download messages
        val downloadIntentFilter = IntentFilter(getDownloadAddedMessage())
        downloadIntentFilter.addAction(getDownloadFinishMessage())
        mDownloadFinishReceiver = DownloadFinishReceiver()
        mDownloadFinishReceiver?.let {
            localBroadcastManager.registerReceiver(it, downloadIntentFilter)
        }

        checkAndSetMenuItemId()

        if (menuItemId == Menu.NONE) {
            setDrawerAllFiles()
        } else {
            configureToolbar()
        }

        // show in-app review dialog to user
        inAppReviewHelper.showInAppReview(this)

        checkNotifications()

        Log_OC.v(TAG, "onResume() end")

        Handler(Looper.getMainLooper()).postDelayed({
            isFileDisplayActivityResumed = false
        }, ON_RESUMED_RESET_DELAY)
    }

    private fun checkAndSetMenuItemId() {
        if (MainApp.isOnlyPersonFiles()) {
            menuItemId = R.id.nav_personal_files
        } else if (MainApp.isOnlyOnDevice()) {
            menuItemId = R.id.nav_on_device
        } else if (menuItemId == Menu.NONE) {
            menuItemId = R.id.nav_all_files
        }
    }

    private fun configureToolbar() {
        // Other activities menuItemIds must be excluded to show correct toolbar.
        val excludedMenuItemIds = ArrayList<Int>().apply {
            add(R.id.nav_community)
            add(R.id.nav_trashbin)
            add(R.id.nav_uploads)
            add(R.id.nav_activity)
            add(R.id.nav_settings)
            add(R.id.nav_assistant)
        }

        var isSearchEventExists = false
        if (this.leftFragment is OCFileListFragment) {
            isSearchEventExists = (fileListFragment?.getSearchEvent() != null)
        }

        if (this.leftFragment !is GalleryFragment &&
            (
                !isSearchEventExists ||
                    menuItemId == R.id.nav_all_files ||
                    menuItemId == R.id.nav_personal_files ||
                    excludedMenuItemIds.contains(menuItemId)
                )
        ) {
            setupHomeSearchToolbarWithSortAndListButtons()
        } else {
            setupToolbar()
        }
    }

    private fun setDrawerAllFiles() {
        checkAndSetMenuItemId()
        setNavigationViewItemChecked()

        if (MainApp.isOnlyOnDevice()) {
            setupToolbar()
        } else {
            setupHomeSearchToolbarWithSortAndListButtons()
        }
    }

    fun initSyncBroadcastReceiver() {
        if (mSyncBroadcastReceiver == null) {
            val syncIntentFilter = IntentFilter(FileSyncAdapter.EVENT_FULL_SYNC_START).apply {
                addAction(FileSyncAdapter.EVENT_FULL_SYNC_END)
                addAction(FileSyncAdapter.EVENT_FULL_SYNC_FOLDER_CONTENTS_SYNCED)
                addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED)
                addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED)
            }

            mSyncBroadcastReceiver = SyncBroadcastReceiver()
            mSyncBroadcastReceiver?.let {
                localBroadcastManager.registerReceiver(it, syncIntentFilter)
            }
        }
    }

    override fun onPause() {
        Log_OC.v(TAG, "onPause() start")
        if (mSyncBroadcastReceiver != null) {
            localBroadcastManager.unregisterReceiver(mSyncBroadcastReceiver!!)
            mSyncBroadcastReceiver = null
        }
        if (mUploadFinishReceiver != null) {
            localBroadcastManager.unregisterReceiver(mUploadFinishReceiver!!)
            mUploadFinishReceiver = null
        }
        if (mDownloadFinishReceiver != null) {
            localBroadcastManager.unregisterReceiver(mDownloadFinishReceiver!!)
            mDownloadFinishReceiver = null
        }

        super.onPause()
        Log_OC.v(TAG, "onPause() end")
    }

    override fun onSortingOrderChosen(selection: FileSortOrder?) {
        val ocFileListFragment = this.listOfFilesFragment
        ocFileListFragment?.sortFiles(selection)
    }

    override fun downloadFile(file: OCFile?, packageName: String?, activityName: String?) {
        if (packageName != null && activityName != null) {
            startDownloadForSending(file, OCFileListFragment.DOWNLOAD_SEND, packageName, activityName)
        }
    }

    // region SyncBroadcastReceiver
    private inner class SyncBroadcastReceiver : BroadcastReceiver() {
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
                val sameAccount =
                    account != null && accountName != null && accountName == account.name && storageManager != null
                val fileListFragment: OCFileListFragment? = this@FileDisplayActivity.listOfFilesFragment

                // endregion
                if (sameAccount) {
                    handleSyncEvent(event, syncFolderRemotePath, id, fileListFragment, syncResult)
                }

                if (syncResult is RemoteOperationResult<*> &&
                    syncResult.code == RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED
                ) {
                    mLastSslUntrustedServerResult = syncResult
                }
            } catch (e: java.lang.RuntimeException) {
                safelyDeleteResult(intent)
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
            mSyncInProgress = true
            return
        }

        var currentFile = if (file == null) null else storageManager.getFileByPath(file.remotePath)
        val currentDir =
            if (getCurrentDir() == null) null else storageManager.getFileByPath(getCurrentDir().remotePath)
        val isSyncFolderRemotePathRoot = OCFile.ROOT_PATH == syncFolderRemotePath

        if (currentDir == null && !isSyncFolderRemotePathRoot) {
            handleRemovedFolder(syncFolderRemotePath)
        } else if (currentDir != null) {
            currentFile = handleRemovedFileFromServer(currentFile, currentDir)
            updateFileList(fileListFragment, currentDir, syncFolderRemotePath)
            file = currentFile
        }

        handleSyncResult(event, syncResult)

        DataHolderUtil.getInstance().delete(id)

        mSyncInProgress =
            FileSyncAdapter.EVENT_FULL_SYNC_END != event &&
            RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED != event
        Log_OC.d(TAG, "Setting progress visibility to $mSyncInProgress")

        handleScrollBehaviour(fileListFragment)
        setBackgroundText()
    }

    private fun handleRemovedFileFromServer(currentFile: OCFile?, currentDir: OCFile?): OCFile? {
        if (currentFile == null && !file.isFolder) {
            resetTitleBarAndScrolling()
            return currentDir
        }

        return currentFile
    }

    private fun handleRemovedFolder(syncFolderRemotePath: String?) {
        DisplayUtils.showSnackMessage(this, R.string.sync_current_folder_was_removed, syncFolderRemotePath)
        browseToRoot()
    }

    private fun updateFileList(
        ocFileListFragment: OCFileListFragment?,
        currentDir: OCFile,
        syncFolderRemotePath: String?
    ) {
        if (currentDir.remotePath != syncFolderRemotePath) {
            return
        }

        if (ocFileListFragment == null) {
            return
        }

        ocFileListFragment.listDirectory(currentDir, MainApp.isOnlyOnDevice(), false)
    }

    private fun handleScrollBehaviour(ocFileListFragment: OCFileListFragment?) {
        if (ocFileListFragment == null) {
            return
        }

        ocFileListFragment.setLoading(mSyncInProgress)
        if (mSyncInProgress || ocFileListFragment.isLoading) {
            return
        }

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
        if (syncResult !is RemoteOperationResult<*>) {
            return
        }

        when (syncResult.code) {
            RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED -> showUntrustedCertDialog(syncResult)
            RemoteOperationResult.ResultCode.MAINTENANCE_MODE -> showInfoBox(R.string.maintenance_mode)
            RemoteOperationResult.ResultCode.NO_NETWORK_CONNECTION -> showInfoBox(R.string.offline_mode)
            RemoteOperationResult.ResultCode.HOST_NOT_AVAILABLE -> showInfoBox(R.string.host_not_available)
            RemoteOperationResult.ResultCode.SIGNING_TOS_NEEDED -> showTermsOfServiceDialog()
            else -> {}
        }
    }

    private fun showTermsOfServiceDialog() {
        if (supportFragmentManager.findFragmentByTag(DIALOG_TAG_SHOW_TOS) == null) {
            TermsOfServiceDialog().show(supportFragmentManager, DIALOG_TAG_SHOW_TOS)
        }
    }

    private fun checkForRemoteOperationError(syncResult: Any?): Boolean {
        if (syncResult !is RemoteOperationResult<*>) {
            return false
        }

        return RemoteOperationResult.ResultCode.UNAUTHORIZED == syncResult.code ||
            (syncResult.isException && syncResult.exception is AuthenticatorException)
    }

    /**
     * Show a text message on screen view for notifying user if content is loading or folder is empty
     */
    private fun setBackgroundText() {
        val ocFileListFragment = this.listOfFilesFragment
        if (ocFileListFragment != null) {
            if (mSyncInProgress ||
                file.fileLength > 0 &&
                storageManager.getFolderContent(
                    file,
                    false
                ).isEmpty()
            ) {
                ocFileListFragment.setEmptyListLoadingMessage()
            } else {
                if (MainApp.isOnlyOnDevice()) {
                    ocFileListFragment.setMessageForEmptyList(
                        R.string.file_list_empty_headline,
                        R.string.file_list_empty_on_device,
                        R.drawable.ic_list_empty_folder,
                        true
                    )
                } else {
                    connectivityService.isNetworkAndServerAvailable { result: Boolean? ->
                        if (result == true) {
                            ocFileListFragment.setEmptyListMessage(SearchType.NO_SEARCH)
                        } else {
                            ocFileListFragment.setEmptyListMessage(SearchType.OFFLINE_MODE)
                        }
                    }
                }
            }
        } else {
            Log_OC.e(TAG, "OCFileListFragment is null")
        }
    }

    // endregion
    /**
     * Once the file upload has finished -> update view
     */
    private inner class UploadFinishReceiver : BroadcastReceiver() {
        /**
         * Once the file upload has finished -> update view
         *
         *
         * [BroadcastReceiver] to enable upload feedback in UI
         */
        override fun onReceive(context: Context?, intent: Intent) {
            val uploadedRemotePath = intent.getStringExtra(FileUploadWorker.EXTRA_REMOTE_PATH)
            val accountName = intent.getStringExtra(FileUploadWorker.ACCOUNT_NAME)
            val account = getAccount()
            val sameAccount = accountName != null && account != null && accountName == account.name
            val currentDir = getCurrentDir()
            val isDescendant =
                currentDir != null && uploadedRemotePath != null && uploadedRemotePath.startsWith(currentDir.remotePath)

            if (sameAccount && isDescendant) {
                val linkedToRemotePath = intent.getStringExtra(FileUploadWorker.EXTRA_LINKED_TO_PATH)
                if (linkedToRemotePath == null || isAscendant(linkedToRemotePath)) {
                    updateListOfFilesFragment(false)
                }
            }

            val uploadWasFine = intent.getBooleanExtra(FileUploadWorker.EXTRA_UPLOAD_RESULT, false)

            var renamedInUpload = false
            var sameFile = false
            if (file != null) {
                renamedInUpload =
                    file.remotePath == intent.getStringExtra(FileUploadWorker.EXTRA_OLD_REMOTE_PATH)
                sameFile = file.remotePath == uploadedRemotePath || renamedInUpload
            }

            if (sameAccount && sameFile && this@FileDisplayActivity.leftFragment is FileDetailFragment) {
                val fileDetailFragment = leftFragment as FileDetailFragment
                if (uploadWasFine) {
                    file = storageManager.getFileByPath(uploadedRemotePath)
                } else {
                    // TODO remove upload progress bar after upload failed.
                    Log_OC.d(TAG, "Remove upload progress bar after upload failed")
                }
                if (renamedInUpload && !uploadedRemotePath.isNullOrBlank()) {
                    val newName = File(uploadedRemotePath).name
                    DisplayUtils.showSnackMessage(
                        this@FileDisplayActivity,
                        R.string.filedetails_renamed_in_upload_msg,
                        newName
                    )
                }

                if (uploadWasFine || file != null && file.fileExists()) {
                    fileDetailFragment.updateFileDetails(false, true)
                } else {
                    onBackPressed()
                }

                // Force the preview if the file is an image or text file
                if (uploadWasFine) {
                    val ocFile = file
                    if (PreviewImageFragment.canBePreviewed(ocFile)) {
                        startImagePreview(file, true)
                    } else if (PreviewTextFileFragment.canBePreviewed(ocFile)) {
                        startTextPreview(ocFile, true)
                    }
                    // TODO what about other kind of previews?
                }
            }

            val ocFileListFragment: OCFileListFragment? = this@FileDisplayActivity.listOfFilesFragment
            ocFileListFragment?.setLoading(false)
        }

        // TODO refactor this receiver, and maybe DownloadFinishReceiver; this method is duplicated :S
        fun isAscendant(linkedToRemotePath: String): Boolean {
            val currentDir = getCurrentDir()
            return currentDir != null && currentDir.remotePath.startsWith(linkedToRemotePath)
        }
    }

    /**
     * Class waiting for broadcast events from the [FileDownloadWorker] service.
     *
     *
     * Updates the UI when a download is started or finished, provided that it is relevant for the current folder.
     */
    private inner class DownloadFinishReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val sameAccount = isSameAccount(intent)
            val downloadedRemotePath = intent.getStringExtra(FileDownloadWorker.EXTRA_REMOTE_PATH)
            val downloadBehaviour = intent.getStringExtra(OCFileListFragment.DOWNLOAD_BEHAVIOUR)
            val isDescendant = isDescendant(downloadedRemotePath)

            if (sameAccount && isDescendant) {
                val linkedToRemotePath = intent.getStringExtra(FileDownloadWorker.EXTRA_LINKED_TO_PATH)
                if (linkedToRemotePath == null || isAscendant(linkedToRemotePath)) {
                    updateListOfFilesFragment(false)
                }

                val intentAction = intent.action
                if (intentAction != null && downloadedRemotePath != null) {
                    refreshDetailsFragmentIfVisible(
                        intentAction,
                        downloadedRemotePath,
                        intent.getBooleanExtra(FileDownloadWorker.EXTRA_DOWNLOAD_RESULT, false)
                    )
                }
            }

            if (mWaitingToSend != null) {
                // update file after downloading
                mWaitingToSend = storageManager.getFileByRemoteId(mWaitingToSend?.remoteId)
                if (mWaitingToSend != null &&
                    mWaitingToSend?.isDown == true &&
                    OCFileListFragment.DOWNLOAD_SEND == downloadBehaviour
                ) {
                    val packageName = intent.getStringExtra(SendShareDialog.PACKAGE_NAME) ?: return
                    val activityName = intent.getStringExtra(SendShareDialog.ACTIVITY_NAME) ?: return
                    sendDownloadedFile(packageName, activityName)
                }
            }

            if (mWaitingToPreview != null) {
                mWaitingToPreview = storageManager.getFileByRemoteId(mWaitingToPreview?.remoteId)
                if (mWaitingToPreview != null &&
                    mWaitingToPreview?.isDown == true &&
                    EditImageActivity.OPEN_IMAGE_EDITOR == downloadBehaviour
                ) {
                    mWaitingToPreview?.let {
                        startImageEditor(it)
                    }
                }
            }
        }

        fun isDescendant(downloadedRemotePath: String?): Boolean {
            val currentDir = getCurrentDir()
            return currentDir != null &&
                downloadedRemotePath != null &&
                downloadedRemotePath.startsWith(currentDir.remotePath)
        }

        fun isAscendant(linkedToRemotePath: String): Boolean {
            val currentDir = getCurrentDir()
            return currentDir != null && currentDir.remotePath.startsWith(linkedToRemotePath)
        }

        fun isSameAccount(intent: Intent): Boolean {
            val accountName = intent.getStringExtra(FileDownloadWorker.EXTRA_ACCOUNT_NAME)
            return accountName != null && account != null && accountName == account.name
        }
    }

    fun browseToRoot() {
        val listOfFiles = this.listOfFilesFragment
        if (listOfFiles != null) { // should never be null, indeed
            val root = storageManager.getFileByPath(OCFile.ROOT_PATH)
            listOfFiles.listDirectory(root, MainApp.isOnlyOnDevice(), false)
            file = listOfFiles.currentFile
            startSyncFolderOperation(root, false)
        }
        binding.fabMain.setImageResource(R.drawable.ic_plus)
        resetTitleBarAndScrolling()
    }

    override fun onBrowsedDownTo(directory: OCFile?) {
        file = directory
        resetTitleBarAndScrolling()
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
        val currentUser = user.orElseThrow(Supplier { RuntimeException() })

        resetScrolling(true)

        val detailFragment: Fragment = FileDetailFragment.newInstance(file, currentUser, activeTab)
        setLeftFragment(detailFragment, false)
        configureToolbarForPreview(file)
    }

    /**
     * Prevents content scrolling and toolbar collapse
     */
    @VisibleForTesting
    fun lockScrolling() {
        binding.appbar.appbar.setExpanded(true, false)
        val appbarParams = binding.appbar.toolbarFrame.layoutParams as AppBarLayout.LayoutParams
        appbarParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL)
        binding.appbar.toolbarFrame.layoutParams = appbarParams
    }

    /**
     * Resets content scrolling and toolbar collapse
     */
    @VisibleForTesting
    fun resetScrolling(expandAppBar: Boolean) {
        val appbarParams = binding.appbar.toolbarFrame.layoutParams as AppBarLayout.LayoutParams
        appbarParams.setScrollFlags(
            AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
        )
        binding.appbar.toolbarFrame.layoutParams = appbarParams
        if (expandAppBar) {
            binding.appbar.appbar.setExpanded(true, false)
        }
    }

    public override fun updateActionBarTitleAndHomeButton(chosenFile: OCFile?) {
        var chosenFile = chosenFile
        if (chosenFile == null) {
            chosenFile = file // if no file is passed, current file decides
        }
        super.updateActionBarTitleAndHomeButton(chosenFile)
    }

    override fun isDrawerIndicatorAvailable(): Boolean = isRoot(getCurrentDir())

    private fun observeWorkerState() {
        WorkerStateLiveData.Companion.instance().observe(
            this,
            Observer { state: WorkerState? ->
                when (state) {
                    is DownloadStarted -> {
                        Log_OC.d(TAG, "Download worker started")
                        handleDownloadWorkerState()
                    }

                    is DownloadFinished -> {
                        fileDownloadProgressListener = null
                        previewFile(state)
                    }

                    is UploadFinished -> {
                        refreshList()
                    }

                    is OfflineOperationsCompleted -> {
                        refreshCurrentDirectory()
                    }

                    else -> {
                    }
                }
            }
        )
    }

    private fun previewFile(finishedState: DownloadFinished) {
        if (fileIDForImmediatePreview == -1L) {
            return
        }

        val currentFile = finishedState.currentFile
        if (currentFile == null) {
            return
        }

        if (fileIDForImmediatePreview != currentFile.fileId || !currentFile.isDown) {
            return
        }

        fileIDForImmediatePreview = -1
        if (PreviewImageFragment.canBePreviewed(currentFile)) {
            startImagePreview(currentFile, currentFile.isDown)
        } else {
            previewFile(currentFile, null)
        }
    }

    fun previewImageWithSearchContext(file: OCFile, searchFragment: Boolean, currentSearchType: SearchType?) {
        // preview image - it handles the download, if needed
        if (searchFragment) {
            val type = when (currentSearchType) {
                SearchType.FAVORITE_SEARCH -> VirtualFolderType.FAVORITE
                SearchType.GALLERY_SEARCH -> VirtualFolderType.GALLERY
                else -> VirtualFolderType.NONE
            }

            startImagePreview(file, type, file.isDown)
        } else {
            startImagePreview(file, file.isDown)
        }
    }

    fun previewFile(file: OCFile, setFabVisible: CompletionCallback?) {
        if (!file.isDown) {
            Log_OC.d(TAG, "File is not downloaded, cannot be previewed")
            return
        }

        if (MimeTypeUtil.isVCard(file)) {
            startContactListFragment(file)
        } else if (MimeTypeUtil.isPDF(file)) {
            startPdfPreview(file)
        } else if (PreviewTextFileFragment.canBePreviewed(file)) {
            setFabVisible?.onComplete(false)
            startTextPreview(file, false)
        } else if (PreviewMediaActivity.Companion.canBePreviewed(file)) {
            setFabVisible?.onComplete(false)
            startMediaPreview(file, 0, true, true, false, true)
        } else {
            fileOperationsHelper.openFile(file)
        }
    }

    fun refreshCurrentDirectory() {
        val currentDir =
            if (getCurrentDir() !=
                null
            ) {
                storageManager.getFileByDecryptedRemotePath(getCurrentDir().remotePath)
            } else {
                null
            }

        val lastFragment = lastFragment()

        var fileListFragment: OCFileListFragment? = null
        if (lastFragment is OCFileListFragment) {
            fileListFragment = lastFragment
        }
        if (fileListFragment == null) {
            fileListFragment = listOfFilesFragment
        }
        fileListFragment?.listDirectory(currentDir, MainApp.isOnlyOnDevice(), false)
    }

    private fun handleDownloadWorkerState() {
        if (mWaitingToPreview != null && storageManager != null) {
            mWaitingToPreview = mWaitingToPreview?.fileId?.let { storageManager.getFileById(it) }
            if (mWaitingToPreview != null && mWaitingToPreview?.isDown == false) {
                requestForDownload()
            }
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
                fileDownloadProgressListener = null
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
            is RemoveFileOperation -> {
                onRemoveFileOperationFinish(operation, result)
            }

            is RenameFileOperation -> {
                onRenameFileOperationFinish(operation, result)
            }

            is SynchronizeFileOperation -> {
                onSynchronizeFileOperationFinish(operation, result)
            }

            is CreateFolderOperation -> {
                onCreateFolderOperationFinish(operation, result)
            }

            is MoveFileOperation -> {
                onMoveFileOperationFinish(operation, result)
            }

            is CopyFileOperation -> {
                onCopyFileOperationFinish(operation, result)
            }

            is RestoreFileVersionRemoteOperation -> {
                onRestoreFileVersionOperationFinish(result)
            }
        }
    }

    private val fileListFragment: OCFileListFragment?
        get() = if (lastFragment() is OCFileListFragment) lastFragment() as OCFileListFragment else listOfFilesFragment

    private fun refreshGalleryFragmentIfNeeded() {
        val fileListFragment = this.fileListFragment
        if (fileListFragment is GalleryFragment) {
            startPhotoSearch(R.id.nav_gallery)
        }
    }

    private fun refreshShowDetails() {
        val details = this.leftFragment
        if (details is FileFragment) {
            var file = details.file
            if (file != null) {
                file = storageManager.getFileByPath(file.remotePath)
                if (details is PreviewTextFragment) {
                    // Refresh  OCFile of the fragment
                    (details as PreviewTextFileFragment).updateFile(file)
                } else {
                    showDetails(file)
                }
            }
            supportInvalidateOptionsMenu()
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to remove a file.
     *
     * @param operation Removal operation performed.
     * @param result    Result of the removal.
     */
    private fun onRemoveFileOperationFinish(operation: RemoveFileOperation, result: RemoteOperationResult<*>) {
        if (!operation.isInBackground) {
            DisplayUtils.showSnackMessage(
                this,
                ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources())
            )
        }

        if (result.isSuccess) {
            val removedFile = operation.file
            tryStopPlaying(removedFile)
            val leftFragment = this.leftFragment

            // check if file is still available, if so do nothing
            val fileAvailable = storageManager.fileExists(removedFile.fileId)
            if (leftFragment is FileFragment && !fileAvailable && removedFile == leftFragment.file) {
                file = storageManager.getFileById(removedFile.parentId)
                resetTitleBarAndScrolling()
            }
            val parentFile = storageManager.getFileById(removedFile.parentId)
            if (parentFile != null && parentFile == getCurrentDir()) {
                updateListOfFilesFragment(false)
            } else if (this.leftFragment is GalleryFragment) {
                val galleryFragment = leftFragment as GalleryFragment
                galleryFragment.onRefresh()
            }
            supportInvalidateOptionsMenu()
            refreshGalleryFragmentIfNeeded()
            fetchRecommendedFilesIfNeeded(ignoreETag = true, currentDir)
        } else {
            if (result.isSslRecoverableException) {
                mLastSslUntrustedServerResult = result
                showUntrustedCertDialog(mLastSslUntrustedServerResult)
            }
        }
    }

    private fun onRestoreFileVersionOperationFinish(result: RemoteOperationResult<*>) {
        if (result.isSuccess) {
            val file = getFile()

            // delete old local copy
            if (file.isDown) {
                val list: MutableList<OCFile?> = ArrayList()
                list.add(file)
                fileOperationsHelper.removeFiles(list, true, true)

                // download new version, only if file was previously download
                fileOperationsHelper.syncFile(file)
            }

            val parent = storageManager.getFileById(file.parentId)
            startSyncFolderOperation(parent, ignoreETag = true, ignoreFocus = true)

            val leftFragment = this.leftFragment
            if (leftFragment is FileDetailFragment) {
                leftFragment.getFileDetailActivitiesFragment().reload()
            }

            DisplayUtils.showSnackMessage(this, R.string.file_version_restored_successfully)
        } else {
            DisplayUtils.showSnackMessage(this, R.string.file_version_restored_error)
        }
    }

    private fun tryStopPlaying(file: OCFile) {
        // placeholder for stop-on-delete future code
        if (mPlayerConnection != null && MimeTypeUtil.isAudio(file) && mPlayerConnection?.isPlaying() == true) {
            mPlayerConnection?.stop(file)
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
            try {
                DisplayUtils.showSnackMessage(
                    this,
                    ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources())
                )
            } catch (e: Resources.NotFoundException) {
                Log_OC.e(TAG, "Error while trying to show fail message ", e)
            }
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to copy a file.
     *
     * @param operation Copy operation performed.
     * @param result    Result of the copy operation.
     */
    private fun onCopyFileOperationFinish(operation: CopyFileOperation?, result: RemoteOperationResult<*>) {
        if (result.isSuccess) {
            updateListOfFilesFragment(false)
            refreshGalleryFragmentIfNeeded()
        } else {
            try {
                DisplayUtils.showSnackMessage(
                    this,
                    ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources())
                )
            } catch (e: Resources.NotFoundException) {
                Log_OC.e(TAG, "Error while trying to show fail message ", e)
            }
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to rename a file.
     *
     * @param operation Renaming operation performed.
     * @param result    Result of the renaming.
     */
    private fun onRenameFileOperationFinish(operation: RenameFileOperation, result: RemoteOperationResult<*>) {
        val optionalUser = user
        val renamedFile = operation.file
        if (result.isSuccess && optionalUser.isPresent) {
            val currentUser = optionalUser.get()
            val leftFragment = this.leftFragment
            if (leftFragment is FileFragment) {
                if (leftFragment is FileDetailFragment && renamedFile == leftFragment.file) {
                    leftFragment.updateFileDetails(renamedFile, currentUser)
                    showDetails(renamedFile)
                } else if (leftFragment is PreviewMediaFragment && renamedFile == leftFragment.file) {
                    leftFragment.updateFile(renamedFile)
                    if (PreviewMediaFragment.canBePreviewed(renamedFile)) {
                        val position = leftFragment.position
                        startMediaPreview(renamedFile, position, true, true, true, false)
                    } else {
                        fileOperationsHelper.openFile(renamedFile)
                    }
                } else if (leftFragment is PreviewTextFragment && renamedFile == leftFragment.file) {
                    (leftFragment as PreviewTextFileFragment).updateFile(renamedFile)
                    if (PreviewTextFileFragment.canBePreviewed(renamedFile)) {
                        startTextPreview(renamedFile, true)
                    } else {
                        fileOperationsHelper.openFile(renamedFile)
                    }
                }
            }

            val file = storageManager.getFileById(renamedFile.parentId)
            if (file != null && file == getCurrentDir()) {
                updateListOfFilesFragment(false)
            }
            refreshGalleryFragmentIfNeeded()
            fetchRecommendedFilesIfNeeded(ignoreETag = true, currentDir)
        } else {
            DisplayUtils.showSnackMessage(
                this,
                ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources())
            )

            if (result.isSslRecoverableException) {
                mLastSslUntrustedServerResult = result
                showUntrustedCertDialog(mLastSslUntrustedServerResult)
            }
        }
    }

    private fun onSynchronizeFileOperationFinish(
        operation: SynchronizeFileOperation,
        result: RemoteOperationResult<*>
    ) {
        if (result.isSuccess && operation.transferWasRequested()) {
            val syncedFile = operation.localFile
            onTransferStateChanged(syncedFile, true, true)
            supportInvalidateOptionsMenu()
            refreshShowDetails()
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying create a new folder
     *
     * @param operation Creation operation performed.
     * @param result    Result of the creation.
     */
    private fun onCreateFolderOperationFinish(operation: CreateFolderOperation, result: RemoteOperationResult<*>) {
        if (result.isSuccess) {
            val fileListFragment = this.listOfFilesFragment
            fileListFragment?.onItemClicked(storageManager.getFileByDecryptedRemotePath(operation.getRemotePath()))
        } else {
            try {
                if (RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS == result.code) {
                    DisplayUtils.showSnackMessage(this, R.string.folder_already_exists)
                } else {
                    DisplayUtils.showSnackMessage(
                        this,
                        ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources())
                    )
                }
            } catch (e: Resources.NotFoundException) {
                Log_OC.e(TAG, "Error while trying to show fail message ", e)
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onTransferStateChanged(file: OCFile, downloading: Boolean, uploading: Boolean) {
        updateListOfFilesFragment(false)
        val leftFragment = this.leftFragment
        val optionalUser = user
        if (leftFragment is FileDetailFragment && file == leftFragment.file && optionalUser.isPresent) {
            val currentUser = optionalUser.get()
            if (downloading || uploading) {
                leftFragment.updateFileDetails(file, currentUser)
            } else {
                if (!file.fileExists()) {
                    resetTitleBarAndScrolling()
                } else {
                    leftFragment.updateFileDetails(false, true)
                }
            }
        }
    }

    private fun requestForDownload() {
        val user = user.orElseThrow(Supplier { RuntimeException() })
        mWaitingToPreview?.let {
            FileDownloadHelper.Companion.instance().downloadFileIfNotStartedBefore(user, it)
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
     * @param folder     Folder to refresh.
     * @param ignoreETag If 'true', the data from the server will be fetched and sync'ed even if the eTag didn't
     * change.
     */
    @JvmOverloads
    fun startSyncFolderOperation(folder: OCFile?, ignoreETag: Boolean, ignoreFocus: Boolean = false) {
        Log_OC.d(TAG, "startSyncFolderOperation called, ignoreEtag: $ignoreETag, ignoreFocus: $ignoreFocus")

        // the execution is slightly delayed to allow the activity get the window focus if it's being started
        // or if the method is called from a dialog that is being dismissed

        if (TextUtils.isEmpty(searchQuery) && user.isPresent) {
            handler.postDelayed({
                val user = getUser()
                if (!ignoreFocus && !hasWindowFocus() || !user.isPresent) {
                    // do not refresh if the user rotates the device while another window has focus
                    // or if the current user is no longer valid
                    return@postDelayed
                }

                val currentSyncTime = System.currentTimeMillis()
                mSyncInProgress = true

                // perform folder synchronization
                val refreshFolderOperation: RemoteOperation<*> = RefreshFolderOperation(
                    folder,
                    currentSyncTime,
                    false,
                    ignoreETag,
                    storageManager,
                    user.get(),
                    applicationContext
                )
                refreshFolderOperation.execute(
                    account,
                    MainApp.getAppContext(),
                    this@FileDisplayActivity,
                    null,
                    null
                )

                fetchRecommendedFilesIfNeeded(ignoreETag, folder)

                val fragment = this.listOfFilesFragment
                if (fragment != null && fragment !is GalleryFragment) {
                    fragment.setLoading(true)
                }
                setBackgroundText()
            }, DELAY_TO_REQUEST_REFRESH_OPERATION_LATER)
        }
    }

    private fun fetchRecommendedFilesIfNeeded(ignoreETag: Boolean, folder: OCFile?) {
        if (folder?.isRootDirectory == false || capabilities == null || capabilities.recommendations.isFalse) {
            return
        }

        if (user.isPresent) {
            val accountName = user.get().accountName
            val fragment = this.listOfFilesFragment
            lifecycleScope.launch(Dispatchers.IO) {
                val recommendedFiles = filesRepository.fetchRecommendedFiles(accountName, ignoreETag, storageManager)
                withContext(Dispatchers.Main) {
                    fragment?.adapter?.updateRecommendedFiles(recommendedFiles)
                }
            }
        }
    }

    private fun requestForDownload(file: OCFile, downloadBehaviour: String, packageName: String, activityName: String) {
        val currentUser = user.orElseThrow(Supplier { RuntimeException() })
        if (!FileDownloadHelper.Companion.instance().isDownloading(currentUser, file)) {
            FileDownloadHelper.Companion.instance().downloadFile(
                currentUser,
                file,
                downloadBehaviour,
                DownloadType.DOWNLOAD,
                activityName,
                packageName,
                null
            )
        }
    }

    private fun sendDownloadedFile(packageName: String, activityName: String) {
        val waitingToSend = mWaitingToSend
        if (waitingToSend != null) {
            val sendIntent = IntentUtil.createSendIntent(this, waitingToSend)
            sendIntent.component = ComponentName(packageName, activityName)

            // Show dialog
            val sendTitle = getString(R.string.activity_chooser_send_file_title)
            startActivity(Intent.createChooser(sendIntent, sendTitle))
        } else {
            Log_OC.e(TAG, "Trying to send a NULL OCFile")
        }

        mWaitingToSend = null
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
        mWaitingToSend = file
        mWaitingToSend?.let {
            requestForDownload(it, downloadBehaviour, packageName, activityName)
        }
    }

    fun startImagePreview(file: OCFile, showPreview: Boolean) {
        val showDetailsIntent = Intent(this, PreviewImageActivity::class.java)
        showDetailsIntent.putExtra(EXTRA_FILE, file)
        showDetailsIntent.putExtra(EXTRA_LIVE_PHOTO_FILE, file.livePhotoVideo)
        showDetailsIntent.putExtra(
            EXTRA_USER,
            user.orElseThrow(Supplier { RuntimeException() })
        )
        if (showPreview) {
            startActivity(showDetailsIntent)
        } else {
            val fileOperationsHelper =
                FileOperationsHelper(this, userAccountManager, connectivityService, editorUtils)
            fileOperationsHelper.startSyncForFileAndIntent(file, showDetailsIntent)
        }
    }

    fun startImagePreview(file: OCFile, type: VirtualFolderType?, showPreview: Boolean) {
        val showDetailsIntent = Intent(this, PreviewImageActivity::class.java)
        showDetailsIntent.putExtra(EXTRA_FILE, file)
        showDetailsIntent.putExtra(EXTRA_LIVE_PHOTO_FILE, file.livePhotoVideo)
        showDetailsIntent.putExtra(
            EXTRA_USER,
            user.orElseThrow(Supplier { RuntimeException() })
        )
        showDetailsIntent.putExtra(PreviewImageActivity.EXTRA_VIRTUAL_TYPE, type)

        if (showPreview) {
            startActivity(showDetailsIntent)
        } else {
            val fileOperationsHelper = FileOperationsHelper(
                this,
                userAccountManager,
                connectivityService,
                editorUtils
            )
            fileOperationsHelper.startSyncForFileAndIntent(file, showDetailsIntent)
        }
    }

    /**
     * Stars the preview of an already down media [OCFile].
     *
     * @param file                  Media [OCFile] to preview.
     * @param startPlaybackPosition Media position where the playback will be started, in milliseconds.
     * @param autoplay              When 'true', the playback will start without user interactions.
     */
    fun startMediaPreview(
        file: OCFile,
        startPlaybackPosition: Long,
        autoplay: Boolean,
        showPreview: Boolean,
        streamMedia: Boolean,
        showInActivity: Boolean
    ) {
        val user = getUser()
        if (!user.isPresent) {
            return // not reachable under normal conditions
        }
        val actualUser = user.get()
        if (showPreview && file.isDown && !file.isDownloading || streamMedia) {
            if (showInActivity) {
                startMediaActivity(file, startPlaybackPosition, autoplay, actualUser)
            } else {
                configureToolbarForPreview(file)
                val mediaFragment: Fragment = newInstance(file, user.get(), startPlaybackPosition, autoplay, false)
                setLeftFragment(mediaFragment, false)
            }
        } else {
            val previewIntent = Intent()
            previewIntent.putExtra(EXTRA_FILE, file)
            previewIntent.putExtra(PreviewMediaFragment.EXTRA_START_POSITION, startPlaybackPosition)
            previewIntent.putExtra(PreviewMediaFragment.EXTRA_AUTOPLAY, autoplay)
            val fileOperationsHelper =
                FileOperationsHelper(this, userAccountManager, connectivityService, editorUtils)
            fileOperationsHelper.startSyncForFileAndIntent(file, previewIntent)
        }
    }

    private fun startMediaActivity(file: OCFile?, startPlaybackPosition: Long, autoplay: Boolean, user: User?) {
        val previewMediaIntent = Intent(this, PreviewMediaActivity::class.java)
        previewMediaIntent.putExtra(PreviewMediaActivity.EXTRA_FILE, file)

        // Safely handle the absence of a user
        if (user != null) {
            previewMediaIntent.putExtra(PreviewMediaActivity.EXTRA_USER, user)
        }

        previewMediaIntent.putExtra(PreviewMediaActivity.EXTRA_START_POSITION, startPlaybackPosition)
        previewMediaIntent.putExtra(PreviewMediaActivity.EXTRA_AUTOPLAY, autoplay)
        startActivity(previewMediaIntent)
    }

    fun configureToolbarForPreview(file: OCFile?) {
        lockScrolling()
        super.updateActionBarTitleAndHomeButton(file)
    }

    /**
     * Starts the preview of a text file [OCFile].
     *
     * @param file Text [OCFile] to preview.
     */
    fun startTextPreview(file: OCFile?, showPreview: Boolean) {
        val optUser = user
        if (!optUser.isPresent) {
            // remnants of old unsafe system; do not crash, silently stop
            return
        }
        val user = optUser.get()
        if (showPreview) {
            val fragment = PreviewTextFileFragment.create(user, file, searchOpen, searchQuery)
            setLeftFragment(fragment, false)
            configureToolbarForPreview(file)
        } else {
            val previewIntent = Intent()
            previewIntent.putExtra(EXTRA_FILE, file)
            previewIntent.putExtra(TEXT_PREVIEW, true)
            val fileOperationsHelper =
                FileOperationsHelper(this, userAccountManager, connectivityService, editorUtils)
            fileOperationsHelper.startSyncForFileAndIntent(file, previewIntent)
        }
    }

    /**
     * Starts rich workspace preview for a folder.
     *
     * @param folder [OCFile] to preview its rich workspace.
     */
    fun startRichWorkspacePreview(folder: OCFile?) {
        val args = Bundle()
        args.putParcelable(EXTRA_FILE, folder)
        configureToolbarForPreview(folder)
        val textPreviewFragment =
            Fragment.instantiate(applicationContext, PreviewTextStringFragment::class.java.name, args)
        setLeftFragment(textPreviewFragment, false)
    }

    fun startContactListFragment(file: OCFile?) {
        val user = user.orElseThrow(Supplier { RuntimeException() })
        ContactsPreferenceActivity.startActivityWithContactsFile(this, user, file)
    }

    fun startPdfPreview(file: OCFile) {
        if (fileOperationsHelper.canOpenFile(file)) {
            // prefer third party PDF apps
            fileOperationsHelper.openFile(file)
        } else {
            val pdfFragment: Fragment = newInstance(file)

            setLeftFragment(pdfFragment, false)
            configureToolbarForPreview(file)
            setMainFabVisible(false)
        }
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
            val currentUser = user
            if (currentUser.isPresent) {
                val detailFragment: Fragment = FileDetailFragment.newInstance(file, parentFolder, currentUser.get())
                setLeftFragment(detailFragment, false)
            }
        }

        configureToolbarForPreview(file)
        mWaitingToPreview = file
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
        if (file.isDown) {
            val editImageIntent = Intent(this, EditImageActivity::class.java)
            editImageIntent.putExtra(EditImageActivity.EXTRA_FILE, file)
            startActivity(editImageIntent)
        } else {
            mWaitingToPreview = file
            requestForDownload(
                file,
                EditImageActivity.OPEN_IMAGE_EDITOR,
                packageName,
                this.javaClass.simpleName
            )
            updateActionBarTitleAndHomeButton(file)
            setFile(file)
        }
    }

    /**
     * Request stopping the upload/download operation in progress over the given [OCFile] file.
     *
     * @param file [OCFile] file which operation are wanted to be cancel
     */
    fun cancelTransference(file: OCFile) {
        fileOperationsHelper.cancelTransference(file)
        if (mWaitingToPreview != null && mWaitingToPreview?.remotePath == file.remotePath) {
            mWaitingToPreview = null
        }
        if (mWaitingToSend != null && mWaitingToSend?.remotePath == file.remotePath) {
            mWaitingToSend = null
        }
        onTransferStateChanged(file, false, false)
    }

    /**
     * Request stopping all upload/download operations in progress over the given [OCFile] files.
     *
     * @param files collection of [OCFile] files which operations are wanted to be cancel
     */
    fun cancelTransference(files: MutableCollection<OCFile>) {
        for (file in files) {
            cancelTransference(file)
        }
    }

    override fun onRefresh(ignoreETag: Boolean) {
        syncAndUpdateFolder(ignoreETag, ignoreFocus = false)
    }

    override fun onRefresh() {
        syncAndUpdateFolder(ignoreETag = true, ignoreFocus = false)
    }

    private fun syncAndUpdateFolder(ignoreETag: Boolean, ignoreFocus: Boolean) {
        val listOfFiles = this.listOfFilesFragment
        if (listOfFiles == null || listOfFiles.isSearchFragment) {
            return
        }

        val folder = listOfFiles.currentFile
        if (folder == null) {
            return
        }

        startSyncFolderOperation(folder, ignoreETag, ignoreFocus)
    }

    override fun showFiles(onDeviceOnly: Boolean, personalFiles: Boolean) {
        super.showFiles(onDeviceOnly, personalFiles)
        if (onDeviceOnly) {
            updateActionBarTitleAndHomeButtonByString(getString(R.string.drawer_item_on_device))
        }
        val ocFileListFragment = this.listOfFilesFragment
        if (ocFileListFragment != null &&
            (ocFileListFragment !is GalleryFragment) &&
            (ocFileListFragment !is SharedListFragment)
        ) {
            ocFileListFragment.refreshDirectory()
        } else {
            this.leftFragment = OCFileListFragment()
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessageEvent(event: SearchEvent) {
        if (SearchRemoteOperation.SearchType.PHOTO_SEARCH == event.searchType) {
            Log_OC.d(this, "Switch to photo search fragment")
            this.leftFragment = GalleryFragment()
        } else if (event.searchType == SearchRemoteOperation.SearchType.SHARED_FILTER) {
            Log_OC.d(this, "Switch to Shared fragment")
            this.leftFragment = SharedListFragment()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: SyncEventFinished) {
        val bundle = event.intent.extras
        val file = bundle?.get(EXTRA_FILE) as OCFile? ?: return

        if (event.intent.getBooleanExtra(TEXT_PREVIEW, false)) {
            startTextPreview(file, true)
        } else if (bundle.containsKey(PreviewMediaFragment.EXTRA_START_POSITION)) {
            val startPosition = bundle.get(PreviewMediaFragment.EXTRA_START_POSITION) as Long
            val autoPlay = bundle.get(PreviewMediaFragment.EXTRA_AUTOPLAY) as Boolean
            startMediaPreview(
                file,
                startPosition,
                autoPlay,
                true,
                true,
                true
            )
        } else if (bundle.containsKey(PreviewImageActivity.EXTRA_VIRTUAL_TYPE)) {
            val virtualType = bundle.get(PreviewImageActivity.EXTRA_VIRTUAL_TYPE) as VirtualFolderType?
            startImagePreview(
                file,
                virtualType,
                true
            )
        } else {
            startImagePreview(file, true)
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessageEvent(event: TokenPushEvent?) {
        if (!preferences.isKeysReInitEnabled()) {
            PushUtils.reinitKeys(userAccountManager)
        } else {
            PushUtils.pushRegistrationToServer(userAccountManager, preferences.getPushToken())
        }
    }

    public override fun onStart() {
        super.onStart()
        val optionalUser = user
        val storageManager = getStorageManager()
        if (optionalUser.isPresent && storageManager != null) {
            /** Check whether the 'main' OCFile handled by the Activity is contained in the */
            // current Account
            var file = getFile()
            // get parent from path
            if (file != null) {
                if (file.isDown && file.lastSyncDateForProperties == 0L) {
                    // upload in progress - right now, files are not inserted in the local
                    // cache until the upload is successful get parent from path
                    val parentPath =
                        file.remotePath.substring(0, file.remotePath.lastIndexOf(file.fileName))
                    if (storageManager.getFileByPath(parentPath) == null) {
                        file = null // not able to know the directory where the file is uploading
                    }
                } else {
                    file = storageManager.getFileByPath(file.remotePath)
                    // currentDir = null if not in the current Account
                }
            }
            if (file == null) {
                // fall back to root folder
                file = storageManager.getFileByPath(OCFile.ROOT_PATH) // never returns null
            }
            setFile(file)

            val user = optionalUser.get()
            setupDrawer()

            mSwitchAccountButton.tag = user.accountName
            DisplayUtils.setAvatar(
                user,
                this,
                getResources().getDimension(R.dimen.nav_drawer_menu_avatar_radius),
                getResources(),
                mSwitchAccountButton,
                this
            )
            val userChanged = (user.accountName != lastDisplayedAccountName)
            if (userChanged) {
                Log_OC.d(TAG, "Initializing Fragments in onAccountChanged..")
                initFragments()
                if (file.isFolder && TextUtils.isEmpty(searchQuery)) {
                    startSyncFolderOperation(file, false)
                }
            } else {
                updateActionBarTitleAndHomeButton(if (file.isFolder) null else file)
            }
        }

        val newLastDisplayedAccountName = optionalUser.orElse(null).accountName
        preferences.lastDisplayedAccountName = newLastDisplayedAccountName
        lastDisplayedAccountName = newLastDisplayedAccountName

        EventBus.getDefault().post(TokenPushEvent())
        checkForNewDevVersionNecessary(applicationContext)
    }

    override fun onRestart() {
        super.onRestart()
        checkForNewDevVersionNecessary(applicationContext)
    }

    fun setSearchQuery(query: String?) {
        searchQuery = query
    }

    private fun handleOpenFileViaIntent(intent: Intent) {
        DisplayUtils.showSnackMessage(this, getString(R.string.retrieving_file))

        val userName = intent.getStringExtra(KEY_ACCOUNT)
        val fileId = intent.getStringExtra(KEY_FILE_ID)
        val filePath = intent.getStringExtra(KEY_FILE_PATH)

        val intentData = intent.data
        if (userName == null && fileId == null && intentData != null) {
            openDeepLink(intentData)
        } else {
            val optionalUser = if (userName == null) user else userAccountManager.getUser(userName)
            if (optionalUser.isPresent) {
                if (!TextUtils.isEmpty(fileId)) {
                    openFile(optionalUser.get(), fileId)
                } else if (!TextUtils.isEmpty(filePath)) {
                    openFileByPath(optionalUser.get(), filePath)
                } else {
                    accountClicked(optionalUser.get().hashCode())
                }
            } else {
                DisplayUtils.showSnackMessage(this, getString(R.string.associated_account_not_found))
            }
        }
    }

    private fun openDeepLink(uri: Uri) {
        val linkHandler = DeepLinkHandler(userAccountManager)
        val match = linkHandler.parseDeepLink(uri)

        if (match == null) {
            handleDeepLink(uri)
        } else if (match.users.isEmpty()) {
            DisplayUtils.showSnackMessage(this, getString(R.string.associated_account_not_found))
        } else if (match.users.size == SINGLE_USER_SIZE) {
            openFile(match.users[0], match.fileId)
        } else {
            selectUserAndOpenFile(match.users.toMutableList(), match.fileId)
        }
    }

    private fun selectUserAndOpenFile(users: MutableList<User?>, fileId: String?) {
        val userNames = arrayOfNulls<CharSequence>(users.size)
        for (i in userNames.indices) {
            userNames[i] = users[i]?.accountName
        }
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.common_choose_account)
            .setItems(userNames) { dialog: DialogInterface?, which: Int ->
                val user = users[which]
                openFile(user, fileId)
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

        var storageManager = getStorageManager()

        if (storageManager == null) {
            storageManager = FileDataStorageManager(user, contentResolver)
        }

        val fetchRemoteFileTask = FetchRemoteFileTask(user, fileId, storageManager, this)
        fetchRemoteFileTask.execute()
    }

    private fun openFileByPath(user: User, filepath: String?) {
        setUser(user)

        if (filepath == null) {
            onFileRequestError(null)
            return
        }

        var storageManager = getStorageManager()

        if (storageManager == null) {
            storageManager = FileDataStorageManager(user, contentResolver)
        }

        val client: OwnCloudClient
        try {
            client = clientFactory.create(user)
        } catch (_: CreationException) {
            onFileRequestError(null)
            return
        }

        val getRemoteFileTask = GetRemoteFileTask(this, filepath, client, storageManager, user)
        asyncRunner.postQuickTask(
            getRemoteFileTask,
            { result: GetRemoteFileTask.Result -> this.onFileRequestResult(result) },
            { throwable: Throwable? -> this.onFileRequestError(throwable) }
        )
    }

    private fun onFileRequestError(throwable: Throwable?) {
        dismissLoadingDialog()
        DisplayUtils.showSnackMessage(this, getString(R.string.error_retrieving_file))
        Log_OC.e(TAG, "Requesting file from remote failed!", throwable)
    }

    private fun onFileRequestResult(result: GetRemoteFileTask.Result) {
        dismissLoadingDialog()

        file = result.file

        val fileFragment = OCFileListFragment()
        this.leftFragment = fileFragment

        supportFragmentManager.executePendingTransactions()

        fileFragment.onItemClicked(result.file)
    }

    fun performUnifiedSearch(query: String, listOfHiddenFiles: ArrayList<String>?) {
        val unifiedSearchFragment = UnifiedSearchFragment.Companion.newInstance(query, listOfHiddenFiles)
        setLeftFragment(unifiedSearchFragment, false)
    }

    fun setMainFabVisible(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        binding.fabMain.visibility = visibility
    }

    fun showFile(selectedFile: OCFile?, message: String?) {
        dismissLoadingDialog()

        getOCFileListFragmentFromFile(object : TransactionInterface {
            override fun onOCFileListFragmentComplete(listOfFiles: OCFileListFragment) {
                if (TextUtils.isEmpty(message)) {
                    val temp = file
                    file = getCurrentDir()
                    listOfFiles.listDirectory(getCurrentDir(), temp, MainApp.isOnlyOnDevice(), false)
                    updateActionBarTitleAndHomeButton(null)
                } else {
                    val view = listOfFiles.view
                    if (view != null) {
                        DisplayUtils.showSnackMessage(view, message)
                    }
                }
                if (selectedFile != null) {
                    listOfFiles.onItemClicked(selectedFile)
                }
            }
        })
    }

    // region MetadataSyncJob
    private fun startMetadataSyncForRoot() {
        backgroundJobManager.startMetadataSyncJob(OCFile.ROOT_PATH)
    }

    private fun startMetadataSyncForCurrentDir() {
        val currentDirId = file?.decryptedRemotePath ?: return
        backgroundJobManager.startMetadataSyncJob(currentDirId)
    }
    // endregion

    companion object {
        const val RESTART: String = "RESTART"
        const val ALL_FILES: String = "ALL_FILES"
        const val LIST_GROUPFOLDERS: String = "LIST_GROUPFOLDERS"
        const val SINGLE_USER_SIZE: Int = 1
        const val OPEN_FILE: String = "NC_OPEN_FILE"

        const val TAG_PUBLIC_LINK: String = "PUBLIC_LINK"
        const val FTAG_CHOOSER_DIALOG: String = "CHOOSER_DIALOG"
        const val KEY_FILE_ID: String = "KEY_FILE_ID"
        const val KEY_FILE_PATH: String = "KEY_FILE_PATH"
        const val KEY_ACCOUNT: String = "KEY_ACCOUNT"
        const val KEY_IS_SORT_GROUP_VISIBLE: String = "KEY_IS_SORT_GROUP_VISIBLE"

        private const val KEY_WAITING_TO_PREVIEW = "WAITING_TO_PREVIEW"
        private const val KEY_SYNC_IN_PROGRESS = "SYNC_IN_PROGRESS"
        private const val KEY_WAITING_TO_SEND = "WAITING_TO_SEND"
        private const val DIALOG_TAG_SHOW_TOS = "DIALOG_TAG_SHOW_TOS"

        private const val ON_RESUMED_RESET_DELAY = 10000L

        const val ACTION_DETAILS: String = "com.owncloud.android.ui.activity.action.DETAILS"

        @JvmField
        val REQUEST_CODE__SELECT_CONTENT_FROM_APPS: Int = REQUEST_CODE__LAST_SHARED + 1

        @JvmField
        val REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM: Int = REQUEST_CODE__LAST_SHARED + 2

        @JvmField
        val REQUEST_CODE__MOVE_OR_COPY_FILES: Int = REQUEST_CODE__LAST_SHARED + 3

        @JvmField
        val REQUEST_CODE__UPLOAD_FROM_CAMERA: Int = REQUEST_CODE__LAST_SHARED + 5

        @JvmField
        val REQUEST_CODE__UPLOAD_FROM_VIDEO_CAMERA: Int = REQUEST_CODE__LAST_SHARED + 6

        protected val DELAY_TO_REQUEST_REFRESH_OPERATION_LATER: Long = DELAY_TO_REQUEST_OPERATIONS_LATER + 350

        private val TAG: String = FileDisplayActivity::class.java.getSimpleName()

        const val TAG_LIST_OF_FILES: String = "LIST_OF_FILES"

        const val TEXT_PREVIEW: String = "TEXT_PREVIEW"

        const val KEY_IS_SEARCH_OPEN: String = "IS_SEARCH_OPEN"
        const val KEY_SEARCH_QUERY: String = "SEARCH_QUERY"

        @JvmStatic
        fun openFileIntent(context: Context?, user: User?, file: OCFile?): Intent {
            val intent = Intent(context, PreviewImageActivity::class.java)
            intent.putExtra(EXTRA_FILE, file)
            intent.putExtra(EXTRA_USER, user)
            return intent
        }
    }
}
