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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.media.ExoplayerListener
import com.nextcloud.client.media.NextcloudExoPlayer.createNextcloudExoplayer
import com.nextcloud.client.media.PlayerService
import com.nextcloud.client.media.PlayerServiceConnection
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.common.NextcloudClient
import com.nextcloud.ui.fileactions.FileActionsBottomSheet.Companion.newInstance
import com.nextcloud.ui.fileactions.FileActionsBottomSheet.ResultListener
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.logFileSize
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityPreviewMediaBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
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
import com.owncloud.android.utils.BitmapUtils
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

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            setTheme(R.style.Theme_ownCloud_Toolbar)
        }

        binding = ActivityPreviewMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.materialToolbar)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        applyWindowInsets()
        initArguments(savedInstanceState)
        mediaPlayerServiceConnection = PlayerServiceConnection(this)
        showMediaTypeViews()
        configureSystemBars()
        emptyListView = binding.emptyView.emptyListView
        showProgressLayout()
    }

    private fun registerMediaControlReceiver() {
        val filter = IntentFilter(MEDIA_CONTROL_READY_RECEIVER)
        LocalBroadcastManager.getInstance(this).registerReceiver(mediaControlReceiver, filter)
    }

    private val mediaControlReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.getBooleanExtra(PlayerService.IS_MEDIA_CONTROL_LAYOUT_READY, false).run {
                if (this) {
                    hideProgressLayout()
                    mediaPlayerServiceConnection?.bind()
                    setupAudioPlayerServiceConnection()
                } else {
                    showProgressLayout()
                }
            }
        }
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

        if (MimeTypeUtil.isAudio(file)) {
            preparePreviewForAudioFile()
        }
    }

    private fun preparePreviewForAudioFile() {
        registerMediaControlReceiver()

        if (file.isDown) {
            return
        }

        requestForDownload(file)
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
        } else {
            extractAndSetCoverArt(file)
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
        binding.mediaController.visibility = View.GONE
        binding.emptyView.emptyListView.visibility = View.GONE
    }

    private fun hideProgressLayout() {
        binding.progress.visibility = View.GONE
        binding.mediaController.visibility = View.VISIBLE
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

    /**
     * tries to read the cover art from the audio file and sets it as cover art.
     *
     * @param file audio file with potential cover art
     */
    @Suppress("TooGenericExceptionCaught", "NestedBlockDepth")
    private fun extractAndSetCoverArt(file: OCFile) {
        if (!MimeTypeUtil.isAudio(file)) {
            return
        }

        val bitmap = if (file.storagePath == null) {
            getAudioThumbnail(file)
        } else {
            getThumbnail(file.storagePath) ?: getAudioThumbnail(file)
        }

        if (bitmap != null) {
            binding.imagePreview.setImageBitmap(bitmap)
        } else {
            setGenericThumbnail()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun getThumbnail(storagePath: String?): Bitmap? {
        return try {
            MediaMetadataRetriever().run {
                setDataSource(storagePath)
                BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture?.size ?: 0)
            }
        } catch (t: Throwable) {
            BitmapUtils.drawableToBitmap(genericThumbnail())
        }
    }

    private fun getAudioThumbnail(file: OCFile): Bitmap? {
        return ThumbnailsCacheManager.getBitmapFromDiskCache(
            ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.remoteId
        )
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
            if (MimeTypeUtil.isVideo(file) && exoPlayer != null) {
                exoPlayer?.let {
                    savedPlaybackPosition = it.currentPosition
                    autoplay = it.isPlaying
                }
                putLong(EXTRA_PLAY_POSITION, savedPlaybackPosition)
                putBoolean(EXTRA_PLAYING, autoplay)
            } else if (mediaPlayerServiceConnection != null && mediaPlayerServiceConnection!!.isConnected) {
                putInt(EXTRA_PLAY_POSITION, mediaPlayerServiceConnection!!.currentPosition)
                putBoolean(EXTRA_PLAYING, mediaPlayerServiceConnection!!.isPlaying)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        Log_OC.v(TAG, "onStart")

        if (file == null) {
            return
        }

        mediaPlayerServiceConnection?.bind()

        if (MimeTypeUtil.isAudio(file)) {
            setupAudioPlayerServiceConnection()
        } else if (MimeTypeUtil.isVideo(file)) {
            if (mediaPlayerServiceConnection?.isConnected == true) {
                stopAudio()
            }

            if (exoPlayer != null) {
                playVideo()
            } else {
                initNextcloudExoPlayer()
            }
        }
    }

    private fun setupAudioPlayerServiceConnection() {
        binding.mediaController.run {
            setMediaPlayer(mediaPlayerServiceConnection)
            visibility = View.VISIBLE
        }

        user?.let {
            mediaPlayerServiceConnection?.start(it, file, autoplay, savedPlaybackPosition)
        }

        binding.emptyView.emptyListView.visibility = View.GONE
        binding.progress.visibility = View.GONE
    }

    private fun initNextcloudExoPlayer() {
        val handler = Handler(Looper.getMainLooper())
        Executors.newSingleThreadExecutor().execute {
            try {
                nextcloudClient = clientFactory.createNextcloudClient(accountManager.user)

                nextcloudClient?.let { client ->
                    handler.post {
                        exoPlayer = createNextcloudExoplayer(this, client)

                        exoPlayer?.let { player ->
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
            it.player = exoPlayer
        }
    }

    private fun stopAudio() {
        mediaPlayerServiceConnection?.stop()
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

        exoPlayer?.run {
            setMediaItem(MediaItem.fromUri(uri))
            playWhenReady = autoplay
            prepare()

            if (savedPlaybackPosition >= 0) {
                seekTo(savedPlaybackPosition)
            }
        }

        autoplay = false
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
                    videoUri = uri
                    playVideoUri(uri)
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
        Log_OC.v(TAG, "onDestroy")

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mediaControlReceiver)

        super.onDestroy()
        exoPlayer?.run {
            stop()
            release()
        }
    }

    override fun onStop() {
        Log_OC.v(TAG, "onStop")

        val file = file
        if (MimeTypeUtil.isAudio(file) && mediaPlayerServiceConnection?.isPlaying == false) {
            stopAudio()
        } else if (MimeTypeUtil.isVideo(file) && exoPlayer != null && exoPlayer?.isPlaying == true) {
            savedPlaybackPosition = exoPlayer?.currentPosition ?: 0L
            exoPlayer?.pause()
        }

        mediaPlayerServiceConnection?.unbind()

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
            mediaPlayerServiceConnection?.pause()
        } else if (MimeTypeUtil.isVideo(file)) {
            savedPlaybackPosition = exoPlayer?.currentPosition ?: 0
            exoPlayer?.stop()
        }
    }

    val position: Long
        get() {
            if (prepared) {
                savedPlaybackPosition = exoPlayer?.currentPosition ?: 0
            }
            Log_OC.v(TAG, "getting position: $savedPlaybackPosition")
            return savedPlaybackPosition
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
