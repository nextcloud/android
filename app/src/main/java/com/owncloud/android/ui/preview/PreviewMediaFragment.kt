/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2023 Parneet Singh <gurayaparneet@gmail.com>
 * SPDX-FileCopyrightText: 2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2013 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.preview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.download.FileDownloadHelper.Companion.instance
import com.nextcloud.client.media.ExoplayerListener
import com.nextcloud.client.media.NextcloudExoPlayer.createNextcloudExoplayer
import com.nextcloud.client.media.PlayerServiceConnection
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.common.NextcloudClient
import com.nextcloud.ui.fileactions.FileActionsBottomSheet.Companion.newInstance
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.logFileSize
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentPreviewMediaBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.files.StreamMediaFileOperation
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.MimeTypeUtil
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * This fragment shows a preview of a downloaded media file (audio or video).
 *
 *
 * Trying to get an instance with NULL [OCFile] or ownCloud [User] values will produce an
 * [IllegalStateException].
 *
 *
 * By now, if the [OCFile] passed is not downloaded, an [IllegalStateException] is generated on
 * instantiation too.
 */

/**
 * Creates an empty fragment for previews.
 *
 *
 * MUST BE KEPT: the system uses it when tries to reinstantiate a fragment automatically (for instance, when the
 * device is turned a aside).
 *
 *
 * DO NOT CALL IT: an [OCFile] and [User] must be provided for a successful construction
 */

@Suppress("NestedBlockDepth", "ComplexMethod", "LongMethod", "TooManyFunctions")
class PreviewMediaFragment : FileFragment(), OnTouchListener, Injectable {
    private var user: User? = null
    private var savedPlaybackPosition: Long = 0

    private var autoplay = true
    private var isLivePhoto = false
    private val prepared = false
    private var mediaPlayerServiceConnection: PlayerServiceConnection? = null

    private var videoUri: Uri? = null

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    lateinit var binding: FragmentPreviewMediaBinding
    private var emptyListView: ViewGroup? = null
    private var exoPlayer: ExoPlayer? = null
    private var nextcloudClient: NextcloudClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            initArguments(it)
        }

        mediaPlayerServiceConnection = PlayerServiceConnection(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        Log_OC.v(TAG, "onCreateView")

        binding = FragmentPreviewMediaBinding.inflate(inflater, container, false)
        emptyListView = binding.emptyView.emptyListView
        setLoadingView()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log_OC.v(TAG, "onActivityCreated")

        checkArgumentsAfterViewCreation(savedInstanceState)

        if (file != null) {
            prepareExoPlayerView()
        }

        toggleDrawerLockMode(containerActivity, DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        addMenuHost()
    }

    private fun checkArgumentsAfterViewCreation(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            checkNotNull(file) { "Instanced with a NULL OCFile" }
            checkNotNull(user) { "Instanced with a NULL ownCloud Account" }
        } else {
            file = savedInstanceState.getParcelableArgument(EXTRA_FILE, OCFile::class.java)
            user = savedInstanceState.getParcelableArgument(EXTRA_USER, User::class.java)
            savedPlaybackPosition = savedInstanceState.getInt(EXTRA_PLAY_POSITION).toLong()
            autoplay = savedInstanceState.getBoolean(EXTRA_PLAYING)
        }
    }

    private fun initArguments(bundle: Bundle) {
        file.logFileSize(TAG)
        file = bundle.getParcelableArgument(FILE, OCFile::class.java)
        user = bundle.getParcelableArgument(USER, User::class.java)

        savedPlaybackPosition = bundle.getLong(PLAYBACK_POSITION)
        autoplay = bundle.getBoolean(AUTOPLAY)
        isLivePhoto = bundle.getBoolean(IS_LIVE_PHOTO)
    }

    private fun setLoadingView() {
        binding.progress.visibility = View.VISIBLE
        binding.emptyView.emptyListView.visibility = View.GONE
    }

    private fun setVideoErrorMessage(headline: String, @StringRes message: Int = R.string.stream_not_possible_message) {
        binding.emptyView.run {
            emptyListViewHeadline.text = headline
            emptyListViewText.setText(message)
            emptyListIcon.setImageResource(R.drawable.file_movie)
            emptyListViewText.visibility = View.VISIBLE
            emptyListIcon.visibility = View.VISIBLE
            emptyListView.visibility = View.VISIBLE
        }

        binding.progress.visibility = View.GONE
    }

    /**
     * tries to read the cover art from the audio file and sets it as cover art.
     *
     * @param file audio file with potential cover art
     */

    @Suppress("TooGenericExceptionCaught")
    private fun extractAndSetCoverArt(file: OCFile) {
        if (!MimeTypeUtil.isAudio(file)) return

        if (file.storagePath == null) {
            setThumbnailForAudio(file)
        } else {
            try {
                val mmr = MediaMetadataRetriever().apply {
                    setDataSource(file.storagePath)
                }

                val data = mmr.embeddedPicture
                if (data != null) {
                    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                    binding.imagePreview.setImageBitmap(bitmap) // associated cover art in bitmap
                } else {
                    setThumbnailForAudio(file)
                }
            } catch (t: Throwable) {
                setGenericThumbnail()
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
        AppCompatResources.getDrawable(requireContext(), R.drawable.logo)?.let { logo ->
            if (!resources.getBoolean(R.bool.is_branded_client)) {
                // only colour logo of non-branded client
                DrawableCompat.setTint(logo, resources.getColor(R.color.primary, requireContext().theme))
            }
            binding.imagePreview.setImageDrawable(logo)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        file.logFileSize(TAG)
        toggleDrawerLockMode(containerActivity, DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        outState.run {
            putParcelable(EXTRA_FILE, file)
            putParcelable(EXTRA_USER, user)

            if (MimeTypeUtil.isVideo(file) && exoPlayer != null) {
                savedPlaybackPosition = exoPlayer?.currentPosition ?: 0L
                autoplay = exoPlayer?.isPlaying ?: false
                putLong(EXTRA_PLAY_POSITION, savedPlaybackPosition)
                putBoolean(EXTRA_PLAYING, autoplay)
            } else if (mediaPlayerServiceConnection != null && mediaPlayerServiceConnection?.isConnected == true) {
                putInt(EXTRA_PLAY_POSITION, mediaPlayerServiceConnection?.currentPosition ?: 0)
                putBoolean(EXTRA_PLAYING, mediaPlayerServiceConnection?.isPlaying ?: false)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        prepareMedia()
    }

    private fun prepareMedia() {
        if (file == null || !isAdded) {
            Log_OC.d(TAG, "File is null or fragment not attached to a context.")
            return
        }

        mediaPlayerServiceConnection?.bind()

        if (MimeTypeUtil.isAudio(file)) {
            prepareForAudio()
        } else if (MimeTypeUtil.isVideo(file)) {
            prepareForVideo(context ?: MainApp.getAppContext())
        }
    }

    @Suppress("DEPRECATION", "TooGenericExceptionCaught")
    private fun prepareForVideo(context: Context) {
        if (mediaPlayerServiceConnection?.isConnected == true) {
            // always stop player
            stopAudio()
        }
        if (exoPlayer != null) {
            playVideo()
        } else {
            val handler = Handler(Looper.getMainLooper())
            Executors.newSingleThreadExecutor().execute {
                try {
                    nextcloudClient = clientFactory.createNextcloudClient(accountManager.user)
                    handler.post {
                        nextcloudClient?.let { client ->
                            createExoPlayer(context, client)
                            playVideo()
                        }
                    }
                } catch (e: CreationException) {
                    handler.post { Log_OC.e(TAG, "error setting up ExoPlayer", e) }
                }
            }
        }
    }

    private fun createExoPlayer(context: Context, client: NextcloudClient) {
        exoPlayer = createNextcloudExoplayer(context, client)
        exoPlayer?.let {
            val listener = ExoplayerListener(context, binding.exoplayerView, it) { goBackToLivePhoto() }
            it.addListener(listener)
        }
    }

    private fun prepareForAudio() {
        binding.mediaController.setMediaPlayer(null)
        binding.mediaController.visibility = View.VISIBLE
        mediaPlayerServiceConnection?.start(user!!, file, autoplay, savedPlaybackPosition)
        binding.emptyView.emptyListView.visibility = View.GONE
        binding.progress.visibility = View.GONE
    }

    private fun goBackToLivePhoto() {
        if (!isLivePhoto) {
            return
        }

        showActionBar()
        requireActivity().supportFragmentManager.popBackStack()
    }

    private fun prepareExoPlayerView() {
        if (MimeTypeUtil.isVideo(file)) {
            binding.exoplayerView.visibility = View.VISIBLE
            binding.imagePreview.visibility = View.GONE
        } else {
            binding.exoplayerView.visibility = View.GONE
            binding.imagePreview.visibility = View.VISIBLE
            extractAndSetCoverArt(file)
        }
    }

    private fun showActionBar() {
        val currentActivity: Activity = requireActivity()
        if (currentActivity is PreviewImageActivity) {
            currentActivity.toggleActionBarVisibility(false)
        }
    }

    @OptIn(UnstableApi::class)
    private fun setupVideoView() {
        binding.exoplayerView.run {
            setShowNextButton(false)
            setShowPreviousButton(false)
            player = exoPlayer
            setFullscreenButtonClickListener { startFullScreenVideo() }
        }
    }

    private fun stopAudio() {
        mediaPlayerServiceConnection?.stop()
    }

    private fun addMenuHost() {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menu.removeItem(R.id.action_search)
                    menuInflater.inflate(R.menu.custom_menu_placeholder, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.custom_menu_placeholder_item -> {
                            if (containerActivity.storageManager == null || file == null) return false

                            val updatedFile = containerActivity.storageManager.getFileById(file.fileId)
                            file = updatedFile
                            file?.let { newFile ->
                                showFileActions(newFile)
                            }

                            true
                        }

                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun showFileActions(file: OCFile) {
        val additionalFilter: MutableList<Int> = ArrayList(
            listOf(
                R.id.action_rename_file,
                R.id.action_sync_file,
                R.id.action_move_or_copy,
                R.id.action_favorite,
                R.id.action_unset_favorite,
                R.id.action_pin_to_homescreen
            )
        )

        if (getFile() != null && getFile().isSharedWithMe && !getFile().canReshare()) {
            additionalFilter.add(R.id.action_send_share_file)
        }

        newInstance(file, false, additionalFilter)
            .setResultListener(childFragmentManager, this) { itemId: Int -> this.onFileActionChosen(itemId) }
            .show(childFragmentManager, "actions")
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
                dialog.show(requireFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION)
            }

            R.id.action_see_details -> {
                seeDetails()
            }

            R.id.action_sync_file -> {
                containerActivity.fileOperationsHelper.syncFile(file)
            }

            R.id.action_cancel_sync -> {
                containerActivity.fileOperationsHelper.cancelTransference(file)
            }

            R.id.action_stream_media -> {
                containerActivity.fileOperationsHelper.streamMediaFile(file)
            }

            R.id.action_export_file -> {
                val list = ArrayList<OCFile>()
                list.add(file)
                containerActivity.fileOperationsHelper.exportFiles(
                    list,
                    context,
                    view,
                    backgroundJobManager
                )
            }

            R.id.action_download_file -> {
                instance().downloadFileIfNotStartedBefore(user!!, file)
            }
        }
    }

    /**
     * Update the file of the fragment with file value
     *
     * @param file Replaces the held file with a new one
     */
    fun updateFile(file: OCFile?) {
        setFile(file)
    }

    private fun seeDetails() {
        stopPreview(false)
        containerActivity.showDetails(file)
    }

    private fun sendShareFile() {
        stopPreview(false)
        containerActivity.fileOperationsHelper.sendShareFile(file)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun playVideo() {
        setupVideoView()
        // load the video file in the video player
        // when done, VideoHelper#onPrepared() will be called
        if (file.isDown) {
            playVideoUri(file.storageUri)
        } else {
            try {
                LoadStreamUrl(this, user, clientFactory).execute(
                    file.localId
                )
            } catch (e: Exception) {
                Log_OC.e(TAG, "Loading stream url not possible: $e")
            }
        }
    }

    private fun playVideoUri(uri: Uri) {
        binding.progress.visibility = View.GONE

        exoPlayer?.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer?.playWhenReady = autoplay
        exoPlayer?.prepare()

        if (savedPlaybackPosition >= 0) {
            exoPlayer?.seekTo(savedPlaybackPosition)
        }

        // only autoplay video once
        autoplay = false
    }

    @Suppress("DEPRECATION", "ReturnCount")
    private class LoadStreamUrl(
        previewMediaFragment: PreviewMediaFragment,
        private val user: User?,
        private val clientFactory: ClientFactory?
    ) : AsyncTask<Long?, Void?, Uri?>() {
        private val previewMediaFragmentWeakReference = WeakReference(previewMediaFragment)

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg fileId: Long?): Uri? {
            val client: OwnCloudClient?
            try {
                client = clientFactory?.create(user)
            } catch (e: CreationException) {
                Log_OC.e(TAG, "Loading stream url not possible: $e")
                return null
            }

            val sfo = fileId[0]?.let { StreamMediaFileOperation(it) }
            val result = sfo?.execute(client)

            if (result?.isSuccess == false) {
                return null
            }

            return Uri.parse(result?.data?.get(0) as String)
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(uri: Uri?) {
            val previewMediaFragment = previewMediaFragmentWeakReference.get()
            val context = previewMediaFragment?.context

            if (previewMediaFragment?.binding == null || context == null) {
                Log_OC.e(TAG, "Error streaming file: no previewMediaFragment!")
                return
            }

            previewMediaFragment.run {
                if (uri != null) {
                    videoUri = uri
                    playVideoUri(uri)
                } else {
                    emptyListView?.visibility = View.VISIBLE
                    setVideoErrorMessage(getString(R.string.stream_not_possible_headline))
                }
            }
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
        toggleDrawerLockMode(containerActivity, DrawerLayout.LOCK_MODE_UNLOCKED)

        super.onStop()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && v == binding.exoplayerView) {
            // added a margin on the left to avoid interfering with gesture to open navigation drawer
            if (event.x / Resources.getSystem().displayMetrics.density > MIN_DENSITY_RATIO) {
                startFullScreenVideo()
            }
            return true
        }
        return false
    }

    private fun startFullScreenVideo() {
        activity?.let { activity ->
            nextcloudClient?.let { client ->
                exoPlayer?.let { player ->
                    PreviewVideoFullscreenDialog(activity, client, player, binding.exoplayerView).show()
                }
            }
        }
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
            savedPlaybackPosition = data?.getLongExtra(EXTRA_START_POSITION, 0) ?: 0L
            autoplay = data?.getBooleanExtra(EXTRA_AUTOPLAY, false) ?: false
        }
    }

    /**
     * Opens the previewed file with an external application.
     */
    private fun openFile() {
        stopPreview(true)
        containerActivity.fileOperationsHelper.openFile(file)
    }

    private fun stopPreview(stopAudio: Boolean) {
        if (stopAudio && mediaPlayerServiceConnection != null) {
            mediaPlayerServiceConnection?.stop()
        } else if (exoPlayer != null) {
            savedPlaybackPosition = exoPlayer?.currentPosition ?: 0L
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

    private fun toggleDrawerLockMode(containerActivity: ContainerActivity, lockMode: Int) {
        (containerActivity as DrawerActivity).setDrawerLockMode(lockMode)
    }

    override fun onDetach() {
        exoPlayer?.let {
            it.stop()
            it.release()
        }

        super.onDetach()
    }

    companion object {
        private val TAG: String = PreviewMediaFragment::class.java.simpleName

        const val EXTRA_FILE: String = "FILE"
        const val EXTRA_USER: String = "USER"
        const val EXTRA_AUTOPLAY: String = "AUTOPLAY"
        const val EXTRA_START_POSITION: String = "START_POSITION"

        private const val EXTRA_PLAY_POSITION = "PLAY_POSITION"
        private const val EXTRA_PLAYING = "PLAYING"
        private const val MIN_DENSITY_RATIO = 24.0

        private const val FILE = "FILE"
        private const val USER = "USER"
        private const val PLAYBACK_POSITION = "PLAYBACK_POSITION"
        private const val AUTOPLAY = "AUTOPLAY"
        private const val IS_LIVE_PHOTO = "IS_LIVE_PHOTO"

        /**
         * Creates a fragment to preview a file.
         *
         *
         * When 'fileToDetail' or 'user' are null
         *
         * @param fileToDetail An [OCFile] to preview in the fragment
         * @param user         Currently active user
         */
        @JvmStatic
        fun newInstance(
            fileToDetail: OCFile?,
            user: User?,
            startPlaybackPosition: Long,
            autoplay: Boolean,
            isLivePhoto: Boolean
        ): PreviewMediaFragment {
            val previewMediaFragment = PreviewMediaFragment()

            val bundle = Bundle().apply {
                putParcelable(FILE, fileToDetail)
                putParcelable(USER, user)
                putLong(PLAYBACK_POSITION, startPlaybackPosition)
                putBoolean(AUTOPLAY, autoplay)
                putBoolean(IS_LIVE_PHOTO, isLivePhoto)
            }

            previewMediaFragment.arguments = bundle

            return previewMediaFragment
        }

        /**
         * Helper method to test if an [OCFile] can be passed to a [PreviewMediaFragment] to be previewed.
         *
         * @param file File to test if can be previewed.
         * @return 'True' if the file can be handled by the fragment.
         */
        @JvmStatic
        fun canBePreviewed(file: OCFile?): Boolean {
            return file != null && (MimeTypeUtil.isAudio(file) || MimeTypeUtil.isVideo(file))
        }
    }
}
