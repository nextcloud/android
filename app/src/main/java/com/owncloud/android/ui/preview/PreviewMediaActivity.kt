/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   @author Chris Narkiewicz
 *   @author Andy Scherzinger
 *   @author TSI-mc
 *   @author Parneet Singh
 *   Copyright (C) 2016 ownCloud Inc.
 *   Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *   Copyright (C) 2020 Andy Scherzinger
 *   Copyright (C) 2023 TSI-mc
 *   Copyright (C) Parneet Singh
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.preview

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.media.ExoplayerListener
import com.nextcloud.client.media.NextcloudExoPlayer.createNextcloudExoplayer
import com.nextcloud.client.media.PlayerServiceConnection
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.common.NextcloudClient
import com.nextcloud.ui.fileactions.FileActionsBottomSheet.Companion.newInstance
import com.nextcloud.ui.fileactions.FileActionsBottomSheet.ResultListener
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityPreviewMediaBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.files.StreamMediaFileOperation
import com.owncloud.android.files.services.FileDownloader
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.operations.SynchronizeFileOperation
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment
import com.owncloud.android.ui.dialog.SendShareDialog
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.ErrorMessageAdapter
import com.owncloud.android.utils.MimeTypeUtil
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * This activity shows a preview of a downloaded media file (audio or video).
 *
 *
 * Trying to get an instance with NULL [OCFile] or ownCloud [User] values will produce an [ ].
 *
 *
 * By now, if the [OCFile] passed is not downloaded, an [IllegalStateException] is generated on
 * instantiation too.
 */
class PreviewMediaActivity

    : FileActivity(), FileFragment.ContainerActivity, OnRemoteOperationListener,
    SendShareDialog.SendShareDialogDownloader, Injectable {
    private var user: User? = null
    private var savedPlaybackPosition: Long = 0
    private var autoplay = true
    private val prepared = false
    private var mediaPlayerServiceConnection: PlayerServiceConnection? = null
    private var videoUri: Uri? = null

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    private lateinit var binding: ActivityPreviewMediaBinding
    private var emptyListView: ViewGroup? = null
    private var exoPlayer: ExoPlayer? = null
    private var nextcloudClient: NextcloudClient? = null
    private lateinit var windowInsetsController: WindowInsetsControllerCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.materialToolbar)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        applyWindowInsets()
        val bundle = intent
        file = bundle!!.getParcelableExtra(FILE)
        user = bundle.getParcelableExtra(USER)
        savedPlaybackPosition = bundle.getLongExtra(PLAYBACK_POSITION, 0L)
        autoplay = bundle.getBooleanExtra(AUTOPLAY, true)
        mediaPlayerServiceConnection = PlayerServiceConnection(this)

        var file = file
        if (savedInstanceState == null) {
            checkNotNull(file) { "Instanced with a NULL OCFile" }
            checkNotNull(user) { "Instanced with a NULL ownCloud Account" }
        } else {
            file = savedInstanceState.getParcelable(EXTRA_FILE)
            setFile(file)
            user = savedInstanceState.getParcelable(EXTRA_USER)
            savedPlaybackPosition = savedInstanceState.getInt(EXTRA_PLAY_POSITION).toLong()
            autoplay = savedInstanceState.getBoolean(EXTRA_PLAYING)
        }
        if (file != null) {
            if (MimeTypeUtil.isVideo(file)) {
                binding.exoplayerView.visibility = View.VISIBLE
                binding.imagePreview.visibility = View.GONE
                binding.root.setBackgroundColor(resources.getColor(R.color.black, null))
            } else {
                binding.exoplayerView.visibility = View.GONE
                binding.imagePreview.visibility = View.VISIBLE
                extractAndSetCoverArt(file)
            }
        }
        updateActionBarTitleAndHomeButton(file)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        emptyListView = binding.emptyView.emptyListView
        setLoadingView()

        viewThemeUtils.files.themeActionBar(this, supportActionBar!!)

        viewThemeUtils.platform.themeStatusBar(
            this
        )
    }

    private fun setLoadingView() {
        binding.progress.visibility = View.VISIBLE
        binding.emptyView.emptyListView.visibility = View.GONE
    }

    private fun setVideoErrorMessage(headline: String, @StringRes message: Int) {
        binding.emptyView.emptyListViewHeadline.text = headline
        binding.emptyView.emptyListViewText.setText(message)
        binding.emptyView.emptyListIcon.setImageResource(R.drawable.file_movie)
        binding.emptyView.emptyListViewText.visibility = View.VISIBLE
        binding.emptyView.emptyListIcon.visibility = View.VISIBLE
        binding.progress.visibility = View.GONE
        binding.emptyView.emptyListView.visibility = View.VISIBLE
    }

    /**
     * tries to read the cover art from the audio file and sets it as cover art.
     *
     * @param file audio file with potential cover art
     */
    private fun extractAndSetCoverArt(file: OCFile) {
        if (MimeTypeUtil.isAudio(file)) {
            if (file.storagePath == null) {
                setThumbnailForAudio(file)
            } else {
                try {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(file.storagePath)
                    val data = mmr.embeddedPicture
                    if (data != null) {
                        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                        binding.imagePreview.setImageBitmap(bitmap) //associated cover art in bitmap
                    } else {
                        setThumbnailForAudio(file)
                    }
                } catch (t: Throwable) {
                    setGenericThumbnail()
                }
            }
        }
    }

    private fun setThumbnailForAudio(file: OCFile) {
        val thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
            ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.remoteId
        )
        if (thumbnail != null) {
            binding.imagePreview.setImageBitmap(thumbnail)
        } else {
            setGenericThumbnail()
        }
    }

    /**
     * Set generic icon (logo) as placeholder for thumbnail in preview.
     */
    private fun setGenericThumbnail() {
        val logo = AppCompatResources.getDrawable(this, R.drawable.logo)
        if (logo != null) {
            if (!resources.getBoolean(R.bool.is_branded_client)) {
                // only colour logo of non-branded client
                DrawableCompat.setTint(logo, resources.getColor(R.color.primary, this.theme))
            }
            binding.imagePreview.setImageDrawable(logo)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log_OC.v(TAG, "onSaveInstanceState")
        outState.putParcelable(EXTRA_FILE, file)
        outState.putParcelable(EXTRA_USER, user)
        if (MimeTypeUtil.isVideo(file) && exoPlayer != null) {
            savedPlaybackPosition = exoPlayer!!.currentPosition
            autoplay = exoPlayer!!.isPlaying
            outState.putLong(EXTRA_PLAY_POSITION, savedPlaybackPosition)
            outState.putBoolean(EXTRA_PLAYING, autoplay)
        } else if (mediaPlayerServiceConnection != null && mediaPlayerServiceConnection!!.isConnected) {
            outState.putInt(EXTRA_PLAY_POSITION, mediaPlayerServiceConnection!!.currentPosition)
            outState.putBoolean(EXTRA_PLAYING, mediaPlayerServiceConnection!!.isPlaying)
        }
    }

    override fun onStart() {
        super.onStart()
        Log_OC.v(TAG, "onStart")
        val file = file
        if (file != null) {
            // bind to any existing player
            mediaPlayerServiceConnection!!.bind()
            if (MimeTypeUtil.isAudio(file)) {
                binding.mediaController.setMediaPlayer(mediaPlayerServiceConnection)
                binding.mediaController.visibility = View.VISIBLE
                mediaPlayerServiceConnection!!.start(user!!, file, autoplay, savedPlaybackPosition)
                binding.emptyView.emptyListView.visibility = View.GONE
                binding.progress.visibility = View.GONE
            } else if (MimeTypeUtil.isVideo(file)) {
                if (mediaPlayerServiceConnection!!.isConnected) {
                    // always stop player
                    stopAudio()
                }
                if (exoPlayer != null) {
                    playVideo()
                } else {
                    val handler = Handler()
                    Executors.newSingleThreadExecutor().execute {
                        try {
                            nextcloudClient = clientFactory.createNextcloudClient(accountManager.user)
                            handler.post {
                                exoPlayer = createNextcloudExoplayer(this, nextcloudClient!!)
                                exoPlayer!!.addListener(
                                    ExoplayerListener(
                                        this,
                                        binding.exoplayerView,
                                        exoPlayer!!
                                    )
                                )
                                playVideo()
                            }
                        } catch (e: CreationException) {
                            handler.post { Log_OC.e(TAG, "error setting up ExoPlayer", e) }
                        }
                    }
                }
            }
        }
    }

    private fun initWindowInsetsController() {
        windowInsetsController = WindowCompat.getInsetsController(
            window,
            window.decorView
        )
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun applyWindowInsets() {
        val playerView = binding.exoplayerView
        val exoControls = playerView.findViewById<FrameLayout>(R.id.exo_bottom_bar)
        val exoProgress = playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
        val progressBottomMargin = exoProgress.marginBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type
                    .displayCutout()
            )

            binding.materialToolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
            }
            exoControls.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom
            }
            exoProgress.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + progressBottomMargin
            }
            exoControls.updatePadding(left = insets.left, right = insets.right)
            exoProgress.updatePadding(left = insets.left, right = insets.right)
            binding.materialToolbar.updatePadding(left = insets.left, right = insets.right)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupVideoView() {
        initWindowInsetsController()
        val type = WindowInsetsCompat.Type.systemBars()
        binding.exoplayerView.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
            if (visibility == View.VISIBLE) {
                windowInsetsController.show(type)
                supportActionBar!!.show()
            } else if (visibility == View.GONE) {
                windowInsetsController.hide(type)
                supportActionBar!!.hide()
            }
        })
        binding.exoplayerView.player = exoPlayer
    }

    private fun stopAudio() {
        mediaPlayerServiceConnection!!.stop()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.custom_menu_placeholder, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        if (item.itemId == R.id.custom_menu_placeholder_item) {
            val file = file
            if (storageManager != null && file != null) {
                // Update the file
                val updatedFile = storageManager.getFileById(file.fileId)
                setFile(updatedFile)
                val fileNew = getFile()
                fileNew?.let { showFileActions(it) }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showFileActions(file: OCFile) {
        val additionalFilter: MutableList<Int> =
            mutableListOf(
                R.id.action_rename_file,
                R.id.action_sync_file,
                R.id.action_move_or_copy,
                R.id.action_favorite,
                R.id.action_unset_favorite,
                R.id.action_pin_to_homescreen
            )

        if (getFile() != null && getFile().isSharedWithMe && !getFile().canReshare()) {
            additionalFilter.add(R.id.action_send_share_file)
        }
        newInstance(file, false, additionalFilter)
            .setResultListener(supportFragmentManager, this, object : ResultListener {
                override fun onResult(actionId: Int) {
                    onFileActionChosen(actionId)
                }
            })
            .show(supportFragmentManager, "actions")
    }

    private fun onFileActionChosen(itemId: Int) {
        when (itemId) {
            R.id.action_send_share_file -> {
                sendShareFile()
            }

            R.id.action_open_file_with -> {
                openFile()
            }

            R.id.action_remove_file -> {
                val dialog = RemoveFilesDialogFragment.newInstance(file)
                dialog.show(supportFragmentManager, ConfirmationDialogFragment.FTAG_CONFIRMATION)
            }

            R.id.action_see_details -> {
                seeDetails()
            }

            R.id.action_sync_file -> {
                fileOperationsHelper.syncFile(file)
            }

            R.id.action_cancel_sync -> {
                fileOperationsHelper.cancelTransference(file)
            }

            R.id.action_stream_media -> {
                fileOperationsHelper.streamMediaFile(file)
            }

            R.id.action_export_file -> {
                val list = ArrayList<OCFile>()
                list.add(file)
                fileOperationsHelper.exportFiles(
                    list,
                    this,
                    binding.root,
                    backgroundJobManager
                )
            }

            R.id.action_download_file -> {
                requestForDownload(file, null)
            }
        }
    }

    override fun onRemoteOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>?) {
        super.onRemoteOperationFinish(operation, result)
        if (operation is RemoveFileOperation) {
            DisplayUtils.showSnackMessage(this, ErrorMessageAdapter.getErrorCauseMessage(result, operation, resources))

            val removedFile = operation.file
            // check if file is still available, if so do nothing
            val fileAvailable: Boolean = storageManager.fileExists(removedFile.fileId)
            if (!fileAvailable && removedFile == file) {
                finish()
            }
        } else if (operation is SynchronizeFileOperation) {
            onSynchronizeFileOperationFinish(result)
        }
    }

    override fun newTransferenceServiceConnection(): ServiceConnection {
        return PreviewMediaServiceConnection()
    }

    private fun onSynchronizeFileOperationFinish(result: RemoteOperationResult<*>?) {
        result?.let {
            invalidateOptionsMenu()
        }
    }

    private inner class PreviewMediaServiceConnection : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName?, service: IBinder?) {
            if (componentName != null) {
                if (componentName == ComponentName(this@PreviewMediaActivity, FileDownloader::class.java)) {
                    mDownloaderBinder = service as FileDownloaderBinder
                }
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            if (componentName == ComponentName(this@PreviewMediaActivity, FileDownloader::class.java)) {
                Log_OC.d(PreviewImageActivity.TAG, "Download service suddenly disconnected")
                mDownloaderBinder = null
            }
        }
    }

    override fun downloadFile(file: OCFile?, packageName: String?, activityName: String?) {
        requestForDownload(file, OCFileListFragment.DOWNLOAD_SEND, packageName, activityName)
    }

    private fun requestForDownload(
        file: OCFile?,
        downloadBehavior: String? = null,
        packageName: String? = null,
        activityName: String? = null
    ) {
        if (!fileDownloaderBinder.isDownloading(user, file)) {
            val i = Intent(this, FileDownloader::class.java)
            i.putExtra(FileDownloader.EXTRA_USER, user)
            i.putExtra(FileDownloader.EXTRA_FILE, file)
            downloadBehavior?.let { behavior ->
                i.putExtra(OCFileListFragment.DOWNLOAD_BEHAVIOUR, behavior)
                i.putExtra(SendShareDialog.PACKAGE_NAME, packageName)
                i.putExtra(SendShareDialog.ACTIVITY_NAME, activityName)
            }
            startService(i)
        }
    }

    private fun seeDetails() {
        stopPreview(false)
        showDetails(file)
    }

    private fun sendShareFile() {
        stopPreview(false)
        fileOperationsHelper.sendShareFile(file)
    }

    private fun playVideo() {
        setupVideoView()
        // load the video file in the video player
        // when done, VideoHelper#onPrepared() will be called
        if (file.isDown) {
            playVideoUri(file.storageUri)
        } else {
            try {
                LoadStreamUrl(this, user, clientFactory).execute(file.localId)
            } catch (e: Exception) {
                Log_OC.e(TAG, "Loading stream url not possible: $e")
            }
        }
    }

    private fun playVideoUri(uri: Uri) {
        binding.progress.visibility = View.GONE
        exoPlayer!!.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer!!.playWhenReady = autoplay
        exoPlayer!!.prepare()
        if (savedPlaybackPosition >= 0) {
            exoPlayer!!.seekTo(savedPlaybackPosition)
        }

        // only autoplay video once
        autoplay = false
    }

    private class LoadStreamUrl(
        previewMediaActivity: PreviewMediaActivity,
        private val user: User?,
        private val clientFactory: ClientFactory?
    ) : AsyncTask<Long?, Void?, Uri?>() {
        private val previewMediaActivityWeakReference: WeakReference<PreviewMediaActivity> =
            WeakReference(previewMediaActivity)

        override fun doInBackground(vararg fileId: Long?): Uri? {
            val client: OwnCloudClient = try {
                clientFactory!!.create(user)
            } catch (e: CreationException) {
                Log_OC.e(TAG, "Loading stream url not possible: $e")
                return null
            }
            val sfo = StreamMediaFileOperation(fileId[0]!!)
            val result = sfo.execute(client)
            return if (!result.isSuccess) {
                null
            } else Uri.parse(result.data[0] as String)
        }

        override fun onPostExecute(uri: Uri?) {
            val weakReference = previewMediaActivityWeakReference.get()
            if (uri != null) {
                weakReference?.videoUri = uri
                weakReference?.playVideoUri(uri)
            } else {
                weakReference?.emptyListView!!.visibility = View.VISIBLE
                weakReference.setVideoErrorMessage(
                    weakReference.getString(R.string.stream_not_possible_headline),
                    R.string.stream_not_possible_message
                )
            }
        }
    }

    override fun onPause() {
        Log_OC.v(TAG, "onPause")
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        Log_OC.v(TAG, "onResume")
    }

    override fun onDestroy() {
        Log_OC.v(TAG, "onDestroy")
        super.onDestroy()
        exoPlayer?.stop()
        exoPlayer?.release()
    }

    override fun onStop() {
        Log_OC.v(TAG, "onStop")
        val file = file
        if (MimeTypeUtil.isAudio(file) && !mediaPlayerServiceConnection!!.isPlaying) {
            stopAudio()
        } else if (MimeTypeUtil.isVideo(file) && exoPlayer != null && exoPlayer!!.isPlaying) {
            savedPlaybackPosition = exoPlayer!!.currentPosition
            exoPlayer!!.pause()
        }
        mediaPlayerServiceConnection!!.unbind()
        super.onStop()
    }

    override fun showDetails(file: OCFile?) {
        val showDetailsIntent = Intent(this, FileDisplayActivity::class.java)
        showDetailsIntent.action = FileDisplayActivity.ACTION_DETAILS
        showDetailsIntent.putExtra(FileActivity.EXTRA_FILE, file)
        showDetailsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(showDetailsIntent)
        finish()
    }

    override fun showDetails(file: OCFile?, activeTab: Int) {
        showDetails(file)
    }

    override fun onBrowsedDownTo(folder: OCFile?) {
        // TODO Auto-generated method stub
    }

    override fun onTransferStateChanged(file: OCFile?, downloading: Boolean, uploading: Boolean) {
        // TODO Auto-generated method stub
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log_OC.v(TAG, "onConfigurationChanged $this")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log_OC.v(TAG, "onActivityResult $this")
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            savedPlaybackPosition = data!!.getLongExtra(EXTRA_START_POSITION, 0)
            autoplay = data.getBooleanExtra(EXTRA_AUTOPLAY, false)
        }
    }

    /**
     * Opens the previewed file with an external application.
     */
    private fun openFile() {
        stopPreview(true)
        fileOperationsHelper.openFile(file)
    }

    private fun stopPreview(stopAudio: Boolean) {
        val file = file
        if (MimeTypeUtil.isAudio(file) && stopAudio) {
            mediaPlayerServiceConnection!!.pause()
        } else if (MimeTypeUtil.isVideo(file)) {
            savedPlaybackPosition = exoPlayer!!.currentPosition
            exoPlayer!!.stop()
        }
    }

    val position: Long
        get() {
            if (prepared) {
                savedPlaybackPosition = exoPlayer!!.currentPosition
            }
            Log_OC.v(TAG, "getting position: $savedPlaybackPosition")
            return savedPlaybackPosition
        }

    companion object {
        private val TAG = PreviewMediaActivity::class.java.simpleName
        const val EXTRA_FILE = "FILE"
        const val EXTRA_USER = "USER"
        const val EXTRA_AUTOPLAY = "AUTOPLAY"
        const val EXTRA_START_POSITION = "START_POSITION"
        private const val EXTRA_PLAY_POSITION = "PLAY_POSITION"
        private const val EXTRA_PLAYING = "PLAYING"
        private const val FILE = "FILE"
        private const val USER = "USER"
        private const val PLAYBACK_POSITION = "PLAYBACK_POSITION"
        private const val AUTOPLAY = "AUTOPLAY"

        /**
         * Helper method to test if an [OCFile] can be passed to a [PreviewMediaActivity] to be previewed.
         *
         * @param file File to test if can be previewed.
         * @return 'True' if the file can be handled by the activity.
         */
        fun canBePreviewed(file: OCFile?): Boolean {
            return file != null && (MimeTypeUtil.isAudio(file) || MimeTypeUtil.isVideo(file))
        }
    }
}