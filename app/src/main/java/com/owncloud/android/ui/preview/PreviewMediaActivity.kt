/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Parneet Singh <gurayaparneet@gmail.com>
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2016 David A. Velasco  <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.preview

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.OptIn
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
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import com.google.common.util.concurrent.MoreExecutors
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.media.BackgroundPlayerService
import com.nextcloud.client.media.ExoplayerListener
import com.nextcloud.client.media.NextcloudExoPlayer.createNextcloudExoplayer
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.common.NextcloudClient
import com.nextcloud.ui.fileactions.FileActionsBottomSheet.Companion.newInstance
import com.nextcloud.ui.fileactions.FileActionsBottomSheet.ResultListener
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.logFileSize
import com.nextcloud.utils.extensions.statusBarHeight
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityPreviewMediaBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.StreamMediaFileOperation
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.DownloadType
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
@Suppress("TooManyFunctions")
class PreviewMediaActivity :
    FileActivity(),
    FileFragment.ContainerActivity,
    OnRemoteOperationListener,
    SendShareDialog.SendShareDialogDownloader,
    Injectable {

    private var user: User? = null
    private var savedPlaybackPosition: Long = 0
    private var autoplay = true
    private var streamUri: Uri? = null

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    private lateinit var binding: ActivityPreviewMediaBinding
    private var emptyListView: ViewGroup? = null
    private var videoPlayer: ExoPlayer? = null
    private var videoMediaSession: MediaSession? = null
    private var audioMediaController: MediaController? = null
    private var nextcloudClient: NextcloudClient? = null
    private lateinit var windowInsetsController: WindowInsetsControllerCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            setTheme(R.style.Theme_ownCloud_Toolbar)
        }

        binding = ActivityPreviewMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.materialToolbar)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        applyWindowInsets()
        initArguments(savedInstanceState)
        showMediaTypeViews()
        configureSystemBars()
        emptyListView = binding.emptyView.emptyListView
        showProgressLayout()
        addMarginForEmptyView()
        if (file == null) {
            return
        }
        if (MimeTypeUtil.isAudio(file)) {
            setGenericThumbnail()
            initializeAudioPlayer()
        }
    }

    private fun addMarginForEmptyView() {
        val layoutParams = emptyListView?.layoutParams ?: return
        val statusBarHeight = statusBarHeight().toFloat()
        val marginTop = DisplayUtils.convertDpToPixel(statusBarHeight, this)
        when (layoutParams) {
            is LinearLayout.LayoutParams -> layoutParams.setMargins(0, marginTop, 0, 0)
            is FrameLayout.LayoutParams -> layoutParams.setMargins(0, marginTop, 0, 0)
            else -> {
                Log_OC.e(TAG, "Unsupported LayoutParams type: ${layoutParams::class.java.simpleName}")
                return
            }
        }
        emptyListView?.layoutParams = layoutParams
    }

    private fun initArguments(savedInstanceState: Bundle?) {
        intent?.let {
            initWithIntent(it)
        }

        if (savedInstanceState == null) {
            checkNotNull(file) { "Instanced with a NULL OCFile" }
            checkNotNull(user) { "Instanced with a NULL ownCloud Account" }
        } else {
            initWithBundle(savedInstanceState)
        }
    }

    private fun initWithIntent(intent: Intent) {
        file = intent.getParcelableArgument(FILE, OCFile::class.java)
        user = intent.getParcelableArgument(USER, User::class.java)
        savedPlaybackPosition = intent.getLongExtra(PLAYBACK_POSITION, 0L)
        autoplay = intent.getBooleanExtra(AUTOPLAY, true)
    }

    private fun initWithBundle(bundle: Bundle) {
        file = bundle.getParcelableArgument(EXTRA_FILE, OCFile::class.java)
        user = bundle.getParcelableArgument(EXTRA_USER, User::class.java)
        savedPlaybackPosition = bundle.getInt(EXTRA_PLAY_POSITION).toLong()
        autoplay = bundle.getBoolean(EXTRA_PLAYING)
    }

    private fun showMediaTypeViews() {
        if (file == null) {
            return
        }

        val isFileVideo = MimeTypeUtil.isVideo(file)

        binding.exoplayerView.visibility = if (isFileVideo) View.VISIBLE else View.GONE
        binding.imagePreview.visibility = if (isFileVideo) View.GONE else View.VISIBLE

        if (isFileVideo) {
            binding.root.setBackgroundColor(resources.getColor(R.color.black, null))
        }
    }

    private fun configureSystemBars() {
        updateActionBarTitleAndHomeButton(file)

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            viewThemeUtils.files.themeActionBar(this, it)
        }

        viewThemeUtils.platform.themeStatusBar(
            this
        )
    }

    private fun showProgressLayout() {
        binding.progress.visibility = View.VISIBLE
        binding.audioControllerView.visibility = View.GONE
        binding.emptyView.emptyListView.visibility = View.GONE
    }

    private fun hideProgressLayout() {
        binding.progress.visibility = View.GONE
        binding.audioControllerView.visibility = View.VISIBLE
        binding.emptyView.emptyListView.visibility = View.VISIBLE
    }

    private fun setVideoErrorMessage(headline: String, @StringRes message: Int) {
        binding.emptyView.run {
            emptyListViewHeadline.text = headline
            emptyListViewText.setText(message)
            emptyListIcon.setImageResource(R.drawable.file_movie)
            emptyListViewText.visibility = View.VISIBLE
            emptyListIcon.visibility = View.VISIBLE

            hideProgressLayout()
        }
    }

    private fun setGenericThumbnail() {
        binding.imagePreview.setImageDrawable(genericThumbnail())
    }

    private fun genericThumbnail(): Drawable? {
        val result = AppCompatResources.getDrawable(this, R.drawable.logo)
        result?.let {
            if (!resources.getBoolean(R.bool.is_branded_client)) {
                DrawableCompat.setTint(it, resources.getColor(R.color.primary, this.theme))
            }
        }

        return result
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        file.logFileSize(TAG)
        outState.let { bundle ->
            bundle.putParcelable(EXTRA_FILE, file)
            bundle.putParcelable(EXTRA_USER, user)
            saveMediaInstanceState(bundle)
        }
    }

    private fun saveMediaInstanceState(bundle: Bundle) {
        bundle.run {
            if (MimeTypeUtil.isVideo(file)) {
                videoPlayer?.let {
                    savedPlaybackPosition = it.currentPosition
                    autoplay = it.playWhenReady
                }
            } else {
                audioMediaController?.let {
                    savedPlaybackPosition = it.currentPosition
                    autoplay = it.playWhenReady
                }
            }
            putLong(EXTRA_PLAY_POSITION, savedPlaybackPosition)
            putBoolean(EXTRA_PLAYING, autoplay)
        }
    }

    override fun onStart() {
        super.onStart()

        Log_OC.v(TAG, "onStart")

        if (MimeTypeUtil.isVideo(file)) {
            initializeVideoPlayer()
        }
    }

    private fun initializeVideoPlayer() {
        val handler = Handler(Looper.getMainLooper())
        Executors.newSingleThreadExecutor().execute {
            try {
                nextcloudClient = clientFactory.createNextcloudClient(accountManager.user)

                nextcloudClient?.let { client ->
                    handler.post {
                        videoPlayer = createNextcloudExoplayer(this, client)
                        videoMediaSession = MediaSession.Builder(this,videoPlayer as Player).build()

                        videoPlayer?.let { player ->
                            player.addListener(
                                ExoplayerListener(
                                    this,
                                    binding.exoplayerView,
                                    player
                                )
                            )

                            playVideo()
                        }
                    }
                }
            } catch (e: CreationException) {
                handler.post { Log_OC.e(TAG, "error setting up ExoPlayer", e) }
            }
        }
    }

    private fun releaseVideoPlayer() {
        videoPlayer?.let {
            savedPlaybackPosition = it.currentPosition
            autoplay = it.playWhenReady
            it.release()
            videoMediaSession?.release()
        }
        videoMediaSession = null
        videoPlayer = null
    }

    private fun initializeAudioPlayer() {
        val sessionToken = SessionToken(this, ComponentName(this, BackgroundPlayerService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                try {
                    audioMediaController = controllerFuture.get()
                    playAudio()
                    binding.audioControllerView.setMediaPlayer(audioMediaController)
                } catch (e: Exception) {
                    println("exception raised while getting the media controller ${e.message}")
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun playAudio() {
        if (file.isDown) {
            prepareAudioPlayer(file.storageUri)
        } else {
            try {
                LoadStreamUrl(this, user, clientFactory).execute(file.localId)
            } catch (e: Exception) {
                Log_OC.e(TAG, "Loading stream url not possible: $e")
            }
        }
    }

    private fun prepareAudioPlayer(uri: Uri) {
        audioMediaController?.let { audioPlayer ->
            audioPlayer.addListener(object : Player.Listener {

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if(playbackState == Player.STATE_READY){
                        hideProgressLayout()
                    }
                }

                override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                    val artworkBitmap = mediaMetadata.artworkData?.let { bytes: ByteArray ->
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                    if (artworkBitmap != null) {
                        binding.imagePreview.setImageBitmap(artworkBitmap)
                    }
                }
            })
            audioPlayer.setMediaItem(MediaItem.fromUri(uri))
            audioPlayer.playWhenReady = autoplay
            audioPlayer.seekTo(savedPlaybackPosition)
            audioPlayer.prepare()
        }
    }

    private fun releaseAudioPlayer() {
        audioMediaController?.let { audioPlayer ->
            audioPlayer.stop()
            audioPlayer.release()
        }
        audioMediaController = null
    }

    private fun initWindowInsetsController() {
        windowInsetsController = WindowCompat.getInsetsController(
            window,
            window.decorView
        ).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    @OptIn(markerClass = [UnstableApi::class])
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

    @OptIn(UnstableApi::class)
    private fun setupVideoView() {
        initWindowInsetsController()
        val type = WindowInsetsCompat.Type.systemBars()
        binding.exoplayerView.let {
            it.setShowNextButton(false)
            it.setShowPreviousButton(false)
            it.setControllerVisibilityListener(
                PlayerView.ControllerVisibilityListener { visibility ->
                    if (visibility == View.VISIBLE) {
                        windowInsetsController.show(type)
                        supportActionBar!!.show()
                    } else if (visibility == View.GONE) {
                        windowInsetsController.hide(type)
                        supportActionBar!!.hide()
                    }
                }
            )
            it.player = videoPlayer
        }
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
            .setResultListener(
                supportFragmentManager,
                this,
                object : ResultListener {
                    override fun onResult(actionId: Int) {
                        onFileActionChosen(actionId)
                    }
                }
            )
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
                videoPlayer?.pause()
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
            val errorMessage = ErrorMessageAdapter.getErrorCauseMessage(result, operation, resources)
            DisplayUtils.showSnackMessage(this, errorMessage)

            val removedFile = operation.file
            val fileAvailable: Boolean = storageManager.fileExists(removedFile.fileId)
            if (!fileAvailable && removedFile == file) {
                releaseAudioPlayer()
                finish()
            }
        } else if (operation is SynchronizeFileOperation) {
            onSynchronizeFileOperationFinish(result)
        }
    }

    private fun onSynchronizeFileOperationFinish(result: RemoteOperationResult<*>?) {
        result?.let {
            invalidateOptionsMenu()
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
        if (FileDownloadHelper.instance().isDownloading(user, file)) {
            return
        }

        user?.let { user ->
            file?.let { file ->
                FileDownloadHelper.instance().downloadFile(
                    user,
                    file,
                    downloadBehavior ?: "",
                    DownloadType.DOWNLOAD,
                    packageName ?: "",
                    activityName ?: ""
                )
            }
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

    @Suppress("TooGenericExceptionCaught")
    private fun playVideo() {
        setupVideoView()

        if (file.isDown) {
            prepareVideoPlayer(file.storageUri)
        } else {
            try {
                LoadStreamUrl(this, user, clientFactory).execute(file.localId)
            } catch (e: Exception) {
                Log_OC.e(TAG, "Loading stream url not possible: $e")
            }
        }
    }

    private fun prepareVideoPlayer(uri: Uri) {
        binding.progress.visibility = View.GONE
        val videoMediaItem = MediaItem.fromUri(uri)
        videoPlayer?.run {
            setMediaItem(videoMediaItem)
            playWhenReady = autoplay
            seekTo(savedPlaybackPosition)
            prepare()
        }
    }

    private class LoadStreamUrl(
        previewMediaActivity: PreviewMediaActivity,
        private val user: User?,
        private val clientFactory: ClientFactory?
    ) : AsyncTask<Long?, Void?, Uri?>() {
        private val previewMediaActivityWeakReference: WeakReference<PreviewMediaActivity> =
            WeakReference(previewMediaActivity)

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg fileId: Long?): Uri? {
            val client: OwnCloudClient? = try {
                clientFactory?.create(user)
            } catch (e: CreationException) {
                Log_OC.e(TAG, "Loading stream url not possible: $e")
                return null
            }

            val sfo = StreamMediaFileOperation(fileId[0]!!)
            val result = sfo.execute(client)

            return if (!result.isSuccess) {
                null
            } else {
                Uri.parse(result.data[0] as String)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(uri: Uri?) {
            val weakReference = previewMediaActivityWeakReference.get()
            weakReference?.apply {
                if (uri != null) {
                    streamUri = uri
                    if (MimeTypeUtil.isVideo(file)) {
                        prepareVideoPlayer(uri)
                    } else if (MimeTypeUtil.isAudio(file)) {
                        prepareAudioPlayer(uri)
                    }
                } else {
                    emptyListView?.visibility = View.VISIBLE
                    setVideoErrorMessage(
                        weakReference.getString(R.string.stream_not_possible_headline),
                        R.string.stream_not_possible_message
                    )
                }
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
        super.onDestroy()

        Log_OC.v(TAG, "onDestroy")
    }

    override fun onStop() {
        Log_OC.v(TAG, "onStop")

        releaseVideoPlayer()
        super.onStop()
    }

    override fun showDetails(file: OCFile?) {
        val intent = Intent(this, FileDisplayActivity::class.java).apply {
            action = FileDisplayActivity.ACTION_DETAILS
            putExtra(FileActivity.EXTRA_FILE, file)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        startActivity(intent)
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

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log_OC.v(TAG, "onActivityResult $this")
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            savedPlaybackPosition = data?.getLongExtra(EXTRA_START_POSITION, 0) ?: 0
            autoplay = data?.getBooleanExtra(EXTRA_AUTOPLAY, false) ?: false
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
        if (MimeTypeUtil.isAudio(file) && stopAudio) {
            audioMediaController?.pause()
        } else if (MimeTypeUtil.isVideo(file)) {
            releaseVideoPlayer()
        }
    }

    companion object {
        private val TAG = PreviewMediaActivity::class.java.simpleName

        const val MEDIA_CONTROL_READY_RECEIVER: String = "MEDIA_CONTROL_READY_RECEIVER"
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
