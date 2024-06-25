/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020-2024 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2013 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.preview

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.ActionBar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.editimage.EditImageActivity
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.jobs.download.FileDownloadWorker
import com.nextcloud.client.jobs.download.FileDownloadWorker.Companion.getDownloadFinishMessage
import com.nextcloud.client.jobs.upload.FileUploadWorker.Companion.getUploadFinishMessage
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.model.WorkerState
import com.nextcloud.model.WorkerStateLiveData
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getSerializableArgument
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
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.fragment.GalleryFragment
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.utils.MimeTypeUtil
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.io.Serializable
import javax.inject.Inject
import kotlin.math.max

/**
 * Holds a swiping gallery where image files contained in an Nextcloud directory are shown.
 */
class PreviewImageActivity : FileActivity(), FileFragment.ContainerActivity, OnRemoteOperationListener, Injectable {
    private var livePhotoFile: OCFile? = null
    private var viewPager: ViewPager2? = null
    private var previewImagePagerAdapter: PreviewImagePagerAdapter? = null
    private var savedPosition = 0
    private var hasSavedPosition = false
    private var requestWaitingForBinder = false
    private var downloadFinishReceiver: DownloadFinishReceiver? = null
    private var fullScreenAnchorView: View? = null
    private var isDownloadWorkStarted = false

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var localBroadcastManager: LocalBroadcastManager

    private var actionBar: ActionBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        actionBar = supportActionBar

        if (savedInstanceState != null && !savedInstanceState.getBoolean(
                KEY_SYSTEM_VISIBLE,
                true
            ) && actionBar != null
        ) {
            actionBar?.hide()
        }

        setContentView(R.layout.preview_image_activity)

        livePhotoFile = intent.getParcelableArgument(EXTRA_LIVE_PHOTO_FILE, OCFile::class.java)

        setupDrawer()

        val chosenFile = intent.getParcelableArgument(EXTRA_FILE, OCFile::class.java)
        updateActionBarTitleAndHomeButton(chosenFile)

        if (actionBar != null) {
            viewThemeUtils.files.setWhiteBackButton(this, actionBar!!)
            actionBar?.setDisplayHomeAsUpEnabled(true)
        }

        fullScreenAnchorView = window.decorView
        // to keep our UI controls visibility in line with system bars visibility
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        requestWaitingForBinder = savedInstanceState?.getBoolean(KEY_WAITING_FOR_BINDER) ?: false

        observeWorkerState()
    }

    fun toggleActionBarVisibility(hide: Boolean) {
        if (actionBar == null) {
            return
        }

        if (hide) {
            actionBar?.hide()
        } else {
            actionBar?.show()
        }
    }

    private fun initViewPager(user: User) {
        val virtualFolderType = intent.getSerializableArgument(EXTRA_VIRTUAL_TYPE, Serializable::class.java)
        if (virtualFolderType != null && virtualFolderType !== VirtualFolderType.NONE) {
            val type = virtualFolderType as VirtualFolderType

            previewImagePagerAdapter = PreviewImagePagerAdapter(
                this,
                type,
                user,
                storageManager
            )
        } else {
            // get parent from path
            var parentFolder = storageManager.getFileById(file.parentId)

            if (parentFolder == null) {
                // should not be necessary
                parentFolder = storageManager.getFileByEncryptedRemotePath(OCFile.ROOT_PATH)
            }

            previewImagePagerAdapter = PreviewImagePagerAdapter(
                this,
                livePhotoFile,
                parentFolder,
                user,
                storageManager,
                MainApp.isOnlyOnDevice(),
                preferences
            )
        }

        viewPager = findViewById(R.id.fragmentPager)

        var position = if (hasSavedPosition) savedPosition else previewImagePagerAdapter?.getFilePosition(file)
        position = position?.toDouble()?.let { max(it, 0.0).toInt() }

        viewPager?.setAdapter(previewImagePagerAdapter)
        viewPager?.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                selectPage(position)
            }
        })
        if (position != null) {
            viewPager?.setCurrentItem(position, false)
        }

        if (position == 0 && !file.isDown) {
            // this is necessary because mViewPager.setCurrentItem(0) just after setting the
            // adapter does not result in a call to #onPageSelected(0)
            requestWaitingForBinder = true
        }
    }

    override fun onBackPressed() {
        sendRefreshSearchEventBroadcast()
        super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != android.R.id.home) {
            return super.onOptionsItemSelected(item)
        }

        sendRefreshSearchEventBroadcast()

        if (isDrawerOpen) {
            closeDrawer()
        } else {
            backToDisplayActivity()
        }

        return true
    }

    private fun sendRefreshSearchEventBroadcast() {
        val intent = Intent(GalleryFragment.REFRESH_SEARCH_EVENT_RECEIVER)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    public override fun onStart() {
        super.onStart()
        val optionalUser = user
        if (optionalUser.isPresent) {
            var file: OCFile? = file ?: throw IllegalStateException("Instanced with a NULL OCFile")
            // / Validate handled file (first image to preview)
            require(MimeTypeUtil.isImage(file)) { "Non-image file passed as argument" }

            // Update file according to DB file, if it is possible
            if (file!!.fileId > FileDataStorageManager.ROOT_PARENT_ID) {
                file = storageManager.getFileById(file.fileId)
            }

            if (file != null) {
                // / Refresh the activity according to the Account and OCFile set
                setFile(file) // reset after getting it fresh from storageManager
                updateActionBarTitle(getFile().fileName)
                // if (!stateWasRecovered) {
                initViewPager(optionalUser.get())

                // }
            } else {
                // handled file not in the current Account
                finish()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_WAITING_FOR_BINDER, requestWaitingForBinder)
        outState.putBoolean(KEY_SYSTEM_VISIBLE, isSystemUIVisible)
    }

    override fun onRemoteOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        super.onRemoteOperationFinish(operation, result)

        if (operation is RemoveFileOperation) {
            val deletePosition = viewPager?.currentItem ?: return
            val nextPosition = if (deletePosition > 0) deletePosition - 1 else 0

            previewImagePagerAdapter?.let {
                if (it.itemCount <= 1) {
                    finish()
                    return
                }
            }

            viewPager?.setCurrentItem(nextPosition, true)
            previewImagePagerAdapter?.delete(deletePosition)
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
        WorkerStateLiveData.instance().observe(this) { state: WorkerState? ->
            if (state is WorkerState.Download) {
                Log_OC.d(TAG, "Download worker started")
                isDownloadWorkStarted = true

                if (requestWaitingForBinder) {
                    requestWaitingForBinder = false
                    Log_OC.d(
                        TAG,
                        "Simulating reselection of current page after connection " +
                            "of download binder"
                    )
                    selectPage(viewPager?.currentItem)
                }
            } else {
                Log_OC.d(TAG, "Download worker stopped")
                isDownloadWorkStarted = false
            }
        }
    }

    override fun onResume() {
        super.onResume()

        downloadFinishReceiver = DownloadFinishReceiver()
        val downloadIntentFilter = IntentFilter(getDownloadFinishMessage())
        localBroadcastManager.registerReceiver(downloadFinishReceiver!!, downloadIntentFilter)

        val uploadFinishReceiver = UploadFinishReceiver()
        val uploadIntentFilter = IntentFilter(getUploadFinishMessage())
        localBroadcastManager.registerReceiver(uploadFinishReceiver, uploadIntentFilter)
    }

    public override fun onPause() {
        if (downloadFinishReceiver != null) {
            localBroadcastManager.unregisterReceiver(downloadFinishReceiver!!)
            downloadFinishReceiver = null
        }

        super.onPause()
    }

    private fun backToDisplayActivity() {
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

    @JvmOverloads
    fun requestForDownload(file: OCFile?, downloadBehaviour: String? = null) {
        if (file == null) return
        val user = user.orElseThrow { RuntimeException() }
        FileDownloadHelper.instance().downloadFileIfNotStartedBefore(user, file)
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
        hasSavedPosition = true

        val currentFile = previewImagePagerAdapter?.getFileAt(position)

        if (!isDownloadWorkStarted) {
            requestWaitingForBinder = true
        } else {
            if (currentFile != null) {
                if (currentFile.isEncrypted && !currentFile.isDown &&
                    previewImagePagerAdapter?.pendingErrorAt(position) == false
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
            previewNewImage(intent)
        }
    }

    private inner class UploadFinishReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            previewNewImage(intent)
        }
    }

    @Suppress("NestedBlockDepth", "ReturnCount")
    private fun previewNewImage(intent: Intent) {
        val accountName = intent.getStringExtra(FileDownloadWorker.EXTRA_ACCOUNT_NAME)
        val downloadedRemotePath = intent.getStringExtra(FileDownloadWorker.EXTRA_REMOTE_PATH)
        val downloadBehaviour = intent.getStringExtra(OCFileListFragment.DOWNLOAD_BEHAVIOUR)

        if (account.name != accountName || downloadedRemotePath == null) {
            return
        }

        val file = storageManager.getFileByEncryptedRemotePath(downloadedRemotePath)
        val downloadWasFine = intent.getBooleanExtra(FileDownloadWorker.EXTRA_DOWNLOAD_RESULT, false)

        if (EditImageActivity.OPEN_IMAGE_EDITOR == downloadBehaviour) {
            startImageEditor(file)
        } else {
            val position = previewImagePagerAdapter?.getFilePosition(file) ?: return

            if (position >= 0) {
                if (downloadWasFine) {
                    previewImagePagerAdapter?.updateFile(position, file)
                } else {
                    previewImagePagerAdapter?.updateWithDownloadError(position)
                }
                previewImagePagerAdapter?.notifyItemChanged(position)
            } else if (downloadWasFine) {
                val user = user

                if (user.isPresent) {
                    initViewPager(user.get())
                    val newPosition = previewImagePagerAdapter?.getFilePosition(file) ?: return
                    if (newPosition >= 0) {
                        viewPager?.currentItem = newPosition
                    }
                }
            }
        }
    }

    val isSystemUIVisible: Boolean
        get() = supportActionBar == null || supportActionBar?.isShowing == true

    fun toggleFullScreen() {
        if (fullScreenAnchorView == null) return
        val visible = (
            fullScreenAnchorView!!.systemUiVisibility
                and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            ) == 0

        if (visible) {
            hideSystemUI(fullScreenAnchorView!!)
        } else {
            showSystemUI(fullScreenAnchorView!!)
        }
    }

    fun startImageEditor(file: OCFile) {
        if (file.isDown) {
            val editImageIntent = Intent(this, EditImageActivity::class.java)
            editImageIntent.putExtra(EditImageActivity.EXTRA_FILE, file)
            startActivity(editImageIntent)
        } else {
            requestForDownload(file, EditImageActivity.OPEN_IMAGE_EDITOR)
        }
    }

    override fun onBrowsedDownTo(folder: OCFile) {
        // TODO Auto-generated method stub
    }

    override fun onTransferStateChanged(file: OCFile, downloading: Boolean, uploading: Boolean) {
        // TODO Auto-generated method stub
    }

    private fun hideSystemUI(anchorView: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            anchorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hides NAVIGATION BAR; Android >= 4.0
                    or View.SYSTEM_UI_FLAG_FULLSCREEN // hides STATUS BAR;     Android >= 4.1
                    or View.SYSTEM_UI_FLAG_IMMERSIVE // stays interactive;    Android >= 4.4
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE // draw full window;     Android >= 4.1
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN // draw full window;     Android >= 4.1
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
    }

    private fun showSystemUI(anchorView: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.show(WindowInsets.Type.systemBars())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
                }
            }
        } else {
            @Suppress("DEPRECATION")
            anchorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE // draw full window;     Android >= 4.1
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN // draw full window;     Android >= 4.1
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
    }

    companion object {
        val TAG: String = PreviewImageActivity::class.java.simpleName
        const val EXTRA_VIRTUAL_TYPE: String = "EXTRA_VIRTUAL_TYPE"
        private const val KEY_WAITING_FOR_BINDER = "WAITING_FOR_BINDER"
        private const val KEY_SYSTEM_VISIBLE = "TRUE"

        fun previewFileIntent(context: Context?, user: User?, file: OCFile?): Intent {
            return Intent(context, PreviewImageActivity::class.java).apply {
                putExtra(EXTRA_FILE, file)
                putExtra(EXTRA_USER, user)
            }
        }
    }
}
