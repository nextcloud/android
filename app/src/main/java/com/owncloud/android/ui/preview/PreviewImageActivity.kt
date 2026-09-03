/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Philipp Hasper <vcs@hasper.info>
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.preview

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.nextcloud.client.account.User
import com.nextcloud.client.database.entity.SyncedFolderEntity
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.editimage.EditImageActivity
import com.nextcloud.client.jobs.download.FileDownloadEventBroadcaster
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.jobs.download.FileDownloadWorker
import com.nextcloud.client.jobs.download.SendShareDownloader
import com.nextcloud.client.player.model.file.PlaybackCollection
import com.nextcloud.client.player.model.file.toPlaybackCollection
import com.nextcloud.client.player.ui.MediaNavigator
import com.nextcloud.client.player.ui.VideoPictureInPicture
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.model.WorkerState
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getSerializableArgument
import com.nextcloud.utils.extensions.observeWorker
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.VirtualFolderType
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.operations.SynchronizeFileOperation
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.OnFilesRemovedListener
import com.owncloud.android.ui.dialog.SendShareDialog
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.fragment.GalleryFragment
import com.owncloud.android.ui.fragment.GalleryFragmentBottomSheetDialog.MediaState
import com.owncloud.android.ui.preview.model.PreviewImageActivityState
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min

/**
 * Holds a swiping gallery where image and video files contained in a Nextcloud directory are shown.
 */
@Suppress("TooManyFunctions")
class PreviewImageActivity :
    FileActivity(),
    FileFragment.ContainerActivity,
    OnRemoteOperationListener,
    OnFilesRemovedListener,
    SendShareDialog.SendShareDialogDownloader,
    MediaNavigator,
    Injectable {
    private var livePhotoFile: OCFile? = null
    private var viewPager: ViewPager2? = null
    private var previewMediaPagerAdapter: PreviewMediaPagerAdapter? = null
    private var savedPosition: Int? = null
    private var initViewPagerJob: Job? = null

    private val onPageChangeCallback = object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            selectPage(position)
        }
    }

    private val sendShareDownloader by lazy { SendShareDownloader(this, localBroadcastManager) }

    private val downloadStartReceiver = DownloadStartReceiver()
    private val downloadFinishReceiver = DownloadFinishReceiver()

    private val windowInsetsController: WindowInsetsControllerCompat by lazy {
        WindowCompat.getInsetsController(window, window.decorView)
    }

    private var isDownloadWorkStarted = false
    private var screenState = PreviewImageActivityState.Idle

    private val pictureInPicture by lazy { VideoPictureInPicture(this, playbackModel, autoEnter = false) }
    private var wasSystemUiVisibleBeforePictureInPicture = true
    private var keepPlaybackOnFinish = false

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var localBroadcastManager: LocalBroadcastManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null &&
            !savedInstanceState.getBoolean(
                KEY_SYSTEM_VISIBLE,
                true
            ) &&
            supportActionBar != null
        ) {
            supportActionBar?.hide()
        }

        setContentView(R.layout.preview_image_activity)

        livePhotoFile = intent.getParcelableArgument(EXTRA_LIVE_PHOTO_FILE, OCFile::class.java)

        setupDrawer(menuItemId)

        val chosenFile = intent.getParcelableArgument(EXTRA_FILE, OCFile::class.java)

        supportActionBar?.let {
            updateActionBarTitleAndHomeButton(chosenFile)
            viewThemeUtils.files.setWhiteBackButton(this, it)
            it.setDisplayHomeAsUpEnabled(true)
            it.setBackgroundDrawable(R.color.black.toDrawable())
        }

        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        val requestWaitingForBinder = savedInstanceState?.getBoolean(KEY_WAITING_FOR_BINDER) ?: false
        if (requestWaitingForBinder) {
            screenState = PreviewImageActivityState.WaitingForBinder
        }

        observeWorkerState()
        applyDisplayCutOutTopPadding()
        handleBackPress()

        lifecycle.addObserver(sendShareDownloader)
        sendShareDownloader.restoreState(savedInstanceState)
    }

    override fun downloadFile(file: OCFile, packageName: String, activityName: String) {
        sendShareDownloader.downloadFile(file, packageName, activityName)
    }

    override fun getMenuItemId(): Int = R.id.nav_gallery

    private fun applyDisplayCutOutTopPadding() {
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            updatePagerDisplayCutOutPadding(isInPictureInPictureMode, insets.displayCutout?.safeInsetTop ?: 0)
            view.onApplyWindowInsets(insets)
        }
    }

    private fun updatePagerDisplayCutOutPadding(inPictureInPictureMode: Boolean, safeInsetTop: Int) {
        val pager = viewPager ?: findViewById(R.id.fragmentPager) ?: return
        val topPadding = if (inPictureInPictureMode) 0 else safeInsetTop

        pager.setPadding(pager.paddingLeft, topPadding, pager.paddingRight, pager.paddingBottom)

        if (topPadding > 0) {
            pager.setBackgroundColor(ContextCompat.getColor(this, R.color.black))
        }
    }

    private fun displayCutOutSafeInsetTop(): Int =
        ViewCompat.getRootWindowInsets(window.decorView)?.displayCutout?.safeInsetTop ?: 0

    fun toggleActionBarVisibility(hide: Boolean) {
        if (hide) {
            supportActionBar?.hide()
        } else {
            supportActionBar?.show()
        }
    }

    private fun initViewPager(user: User) {
        initViewPagerJob?.cancel()
        initViewPagerJob = lifecycleScope.launch {
            showViewPager(user, loadMediaFiles())
        }
    }

    private suspend fun loadMediaFiles(): List<OCFile> {
        val loader = PreviewMediaFilesLoader(storageManager, preferences)
        val virtualFolderType = intent.getSerializableArgument(EXTRA_VIRTUAL_TYPE, VirtualFolderType::class.java)

        if (virtualFolderType != null && virtualFolderType !== VirtualFolderType.NONE) {
            val mediaState = intent.getSerializableArgument(EXTRA_MEDIA_STATE, MediaState::class.java)
            return loader.forVirtualFolder(virtualFolderType, mediaState)
        }

        return loader.forFolder(file?.parentId, MainApp.isOnlyOnDevice())
    }

    private fun showViewPager(user: User, mediaFiles: List<OCFile>) {
        val adapter = PreviewMediaPagerAdapter(
            this,
            mediaFiles,
            playbackCollection(),
            user,
            livePhotoFile
        )
        previewMediaPagerAdapter = adapter

        val position = (savedPosition ?: file?.let { adapter.getFilePosition(it) })?.coerceAtLeast(0)

        if (savedPosition == null) {
            adapter.autoplayFileId = file?.fileId
        }

        val pager = findViewById<ViewPager2>(R.id.fragmentPager)
        viewPager = pager
        pager.adapter = adapter
        pager.unregisterOnPageChangeCallback(onPageChangeCallback)
        pager.registerOnPageChangeCallback(onPageChangeCallback)
        if (position != null) {
            pager.setCurrentItem(position, false)
        }

        if (position == 0 && file?.isDown == false) {
            // this is necessary because mViewPager.setCurrentItem(0) just after setting the
            // adapter does not result in a call to #onPageSelected(0)
            screenState = PreviewImageActivityState.WaitingForBinder
        }
    }

    private fun playbackCollection(): PlaybackCollection {
        val virtualFolderType = intent.getSerializableArgument(EXTRA_VIRTUAL_TYPE, VirtualFolderType::class.java)
        return virtualFolderType
            ?.takeIf { it !== VirtualFolderType.NONE }
            ?.toPlaybackCollection()
            ?: PlaybackCollection.FOLDER
    }

    override fun onFilesRemoved() {
        initViewPager()
    }

    override fun onAutoUploadFolderRemoved(
        entities: List<SyncedFolderEntity>,
        filesToRemove: List<OCFile>,
        onlyLocalCopy: Boolean
    ) = Unit

    fun initViewPager() {
        if (user.isPresent) {
            initViewPager(user.get())
        }
    }

    fun updateViewPagerAfterDeletionAndAdvanceForward() {
        val deletePosition = viewPager?.currentItem ?: return
        previewMediaPagerAdapter?.let { adapter ->
            val nextPosition = min(deletePosition, adapter.itemCount - 1)
            viewPager?.setCurrentItem(nextPosition, true)
            adapter.delete(deletePosition)
            // Page needs to be reselected after the adapter has been updated. Otherwise, wrong title is shown
            selectPage(nextPosition)
        }
    }

    private fun handleBackPress() {
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                sendRefreshSearchEventBroadcast()
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != android.R.id.home) {
            return super.onOptionsItemSelected(item)
        }

        sendRefreshSearchEventBroadcast()

        if (isDrawerOpen) {
            closeDrawer()
        } else {
            onBackPressedDispatcher.onBackPressed()
        }

        return true
    }

    private fun sendRefreshSearchEventBroadcast() {
        val intent = Intent(GalleryFragment.REFRESH_SEARCH_EVENT_RECEIVER)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    public override fun onStart() {
        super.onStart()
        registerReceivers()

        val optionalUser = user
        if (optionalUser.isPresent) {
            var file: OCFile? = file ?: throw IllegalStateException("Instanced with a NULL OCFile")
            // / Validate handled file (first media item to preview)
            require(MimeTypeUtil.isImageOrVideo(file)) { "Non-image/video file passed as argument" }

            // Update file according to DB file, if it is possible
            if (file!!.fileId > FileDataStorageManager.ROOT_PARENT_ID) {
                file = storageManager.getFileById(file.fileId)
            }

            if (file != null) {
                // / Refresh the activity according to the Account and OCFile set
                setFile(file) // reset after getting it fresh from storageManager
                updateActionBarTitle(getFile()?.fileName)
                if (previewMediaPagerAdapter == null) {
                    initViewPager(optionalUser.get())
                }
            } else {
                // handled file not in the current Account
                finish()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_WAITING_FOR_BINDER, screenState == PreviewImageActivityState.WaitingForBinder)
        outState.putBoolean(KEY_SYSTEM_VISIBLE, isSystemUIVisible)
        sendShareDownloader.saveState(outState)
    }

    override fun onRemoteOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        super.onRemoteOperationFinish(operation, result)

        if (operation is RemoveFileOperation) {
            previewMediaPagerAdapter?.let {
                if (it.itemCount <= 1) {
                    backToDisplayActivity()
                    return
                }
            }

            if (result.isSuccess) {
                updateViewPagerAfterDeletionAndAdvanceForward()
            }
        } else if (operation is SynchronizeFileOperation) {
            onSynchronizeFileOperationFinish(result)
        }
    }

    private fun onSynchronizeFileOperationFinish(result: RemoteOperationResult<*>) {
        if (result.isSuccess) {
            supportInvalidateOptionsMenu()
        }
    }

    private fun observeWorkerState() {
        observeWorker { state: WorkerState? ->
            when (state) {
                else -> {
                    Log_OC.d(TAG, "Worker stopped")
                    isDownloadWorkStarted = false
                }
            }
        }
    }

    private fun setDownloadedItem(downloadedFile: OCFile?) {
        val adapter = previewMediaPagerAdapter ?: return
        val position = savedPosition ?: return

        if (downloadedFile != null && adapter.getFileAt(position)?.fileId == downloadedFile.fileId) {
            adapter.updateFile(position, downloadedFile)
            adapter.notifyItemChanged(position)
        }
    }

    private fun selectPageOnDownload() {
        screenState = PreviewImageActivityState.Idle
        Log_OC.d(
            TAG,
            "Simulating reselection of current page after connection " +
                "of download binder"
        )
        selectPage(viewPager?.currentItem)
    }

    private fun onImageDownloadComplete(downloadedFile: OCFile?) {
        dismissLoadingDialog()
        screenState = PreviewImageActivityState.Idle
        file = downloadedFile
        file?.let {
            startEditImageActivity(it)
        }
    }

    private fun registerReceivers() {
        localBroadcastManager.run {
            val downloadStartIntentFilter = IntentFilter(FileDownloadEventBroadcaster.ACTION_DOWNLOAD_ENQUEUED)
            registerReceiver(downloadStartReceiver, downloadStartIntentFilter)

            val downloadFinishIntentFilter = IntentFilter(FileDownloadEventBroadcaster.ACTION_DOWNLOAD_COMPLETED)
            registerReceiver(downloadFinishReceiver, downloadFinishIntentFilter)
        }
    }

    private fun unregisterReceivers() {
        localBroadcastManager.run {
            unregisterReceiver(downloadStartReceiver)
            unregisterReceiver(downloadFinishReceiver)
        }
    }

    public override fun onStop() {
        unregisterReceivers()
        super.onStop()
    }

    private fun backToDisplayActivity() {
        sendRefreshSearchEventBroadcast()
        finish()
    }

    @SuppressFBWarnings("DLS")
    override fun showDetails(file: OCFile) {
        val intent = Intent(this, FileDisplayActivity::class.java).apply {
            setAction(FileDisplayActivity.ACTION_DETAILS)
            putExtra(EXTRA_FILE, file)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        startActivity(intent)
        finish()
    }

    override fun showDetails(file: OCFile, activeTab: Int) {
        showDetails(file)
    }

    fun requestForDownload(file: OCFile?) {
        if (file == null) return
        val user = user.orElseThrow { RuntimeException() }
        FileDownloadHelper.instance().downloadFileIfNotStartedBefore(user, file)
    }

    fun showFilePage(localId: Long): Boolean {
        val position = previewMediaPagerAdapter?.getPositionByLocalId(localId) ?: NO_POSITION
        if (position < 0) return false
        if (viewPager?.currentItem != position) {
            viewPager?.setCurrentItem(position, true)
        }

        return true
    }

    fun finishKeepingPlayback() {
        keepPlaybackOnFinish = true
        finish()
    }

    override val hasNext: Boolean
        get() = currentPage() + 1 < (previewMediaPagerAdapter?.itemCount ?: 0)

    override val hasPrevious: Boolean
        get() = currentPage() > 0

    override fun showNext() = showPage(currentPage() + 1)

    override fun showPrevious() = showPage(currentPage() - 1)

    private fun currentPage(): Int = viewPager?.currentItem ?: 0

    private fun showPage(position: Int) {
        val itemCount = previewMediaPagerAdapter?.itemCount ?: return
        if (position < 0 || position >= itemCount) return

        viewPager?.setCurrentItem(position, true)
    }

    fun enterPictureInPicture(): Boolean = viewPager?.let { pictureInPicture.enter(it) } == true

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        updatePagerDisplayCutOutPadding(isInPictureInPictureMode, displayCutOutSafeInsetTop())

        if (isInPictureInPictureMode) {
            wasSystemUiVisibleBeforePictureInPicture = isSystemUIVisible
            toggleActionBarVisibility(true)
            return
        }

        toggleActionBarVisibility(!wasSystemUiVisibleBeforePictureInPicture)

        if (lifecycle.currentState != Lifecycle.State.CREATED) return

        if (!keepPlaybackOnFinish) {
            playbackModel.release()
        }
        finish()
    }

    /**
     * This method will be invoked when a new page becomes selected. Animation is not necessarily
     * complete.
     *
     * @param position        Position index of the new selected page
     */
    fun selectPage(position: Int?) {
        if (position == null) return
        savedPosition = position

        val currentFile = previewMediaPagerAdapter?.getFileAt(position)

        if (!isDownloadWorkStarted) {
            screenState = PreviewImageActivityState.WaitingForBinder
        } else {
            if (currentFile != null) {
                if (currentFile.isEncrypted &&
                    !currentFile.isDown &&
                    previewMediaPagerAdapter?.pendingErrorAt(position) == false
                ) {
                    requestForDownload(currentFile)
                }

                // Call to reset image zoom to initial state
                // ((PreviewImagePagerAdapter) mViewPager.getAdapter()).resetZoom();
            }
        }

        if (currentFile != null) {
            updateActionBarTitle(currentFile.fileName)
            setDrawerIndicatorEnabled(false)
        }
    }

    private fun updateActionBarTitle(title: String?) {
        supportActionBar?.title = title
    }

    /**
     * Class waiting for broadcast events from the [FileDownloadWorker] service.
     *
     *
     * Updates the UI when a download is started or finished, provided that it is relevant for the
     * folder displayed in the gallery.
     */
    private inner class DownloadFinishReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log_OC.d(TAG, "Download worker stopped")
            isDownloadWorkStarted = false
            val accountName = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_ACCOUNT_NAME)
            val downloadedRemotePath = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_REMOTE_PATH)
            if (account.name != accountName || downloadedRemotePath == null) {
                return
            }
            val file = storageManager.getFileByEncryptedRemotePath(downloadedRemotePath)

            if (screenState == PreviewImageActivityState.Edit) {
                onImageDownloadComplete(file)
            } else {
                setDownloadedItem(file)
            }
        }
    }

    private inner class DownloadStartReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log_OC.d(TAG, "Download worker started")
            isDownloadWorkStarted = true

            if (screenState == PreviewImageActivityState.WaitingForBinder) {
                selectPageOnDownload()
            }
        }
    }

    val isSystemUIVisible: Boolean
        get() = supportActionBar == null || supportActionBar?.isShowing == true

    fun toggleFullScreen() {
        val rootInsets = ViewCompat.getRootWindowInsets(window.decorView) ?: return

        // the content is laid out edge to edge so that showing and hiding the bars does not move it
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (rootInsets.isVisible(WindowInsetsCompat.Type.systemBars())) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun startImageEditor(file: OCFile) {
        if (file.isDown) {
            startEditImageActivity(file)
        } else {
            showLoadingDialog(getString(R.string.preview_image_downloading_image_for_edit))
            screenState = PreviewImageActivityState.Edit
            requestForDownload(file)
        }
    }

    private fun startEditImageActivity(file: OCFile) {
        if (!file.isDown) {
            DisplayUtils.showSnackMessage(this, R.string.preview_image_file_is_not_downloaded)
            return
        }

        val intent = Intent(this, EditImageActivity::class.java).apply {
            putExtra(EditImageActivity.EXTRA_FILE, file)
        }
        startActivity(intent)
    }

    override fun onBrowsedDownTo(folder: OCFile) = Unit
    override fun onTransferStateChanged(file: OCFile, downloading: Boolean, uploading: Boolean) = Unit

    companion object {
        val TAG: String = PreviewImageActivity::class.java.simpleName
        const val EXTRA_VIRTUAL_TYPE: String = "EXTRA_VIRTUAL_TYPE"
        const val EXTRA_MEDIA_STATE: String = "EXTRA_MEDIA_STATE"
        private const val KEY_WAITING_FOR_BINDER = "WAITING_FOR_BINDER"
        private const val KEY_SYSTEM_VISIBLE = "TRUE"
        private const val NO_POSITION = -1

        fun previewFileIntent(context: Context?, user: User?, file: OCFile?): Intent =
            Intent(context, PreviewImageActivity::class.java).apply {
                putExtra(EXTRA_FILE, file)
                putExtra(EXTRA_USER, user)
            }
    }
}
