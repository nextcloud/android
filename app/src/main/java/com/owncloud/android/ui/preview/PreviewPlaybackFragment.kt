/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.player.media3.PlaybackModel
import com.nextcloud.client.player.model.PlayerThumbnailLoader
import com.nextcloud.client.player.model.file.PlaybackCollection
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.util.PlayerUtil.toPlaybackFile
import com.nextcloud.client.player.model.state.PlaybackState
import com.nextcloud.client.player.model.state.VideoSize
import com.nextcloud.client.player.ui.MediaNavigator
import com.nextcloud.client.player.ui.PlayerLauncher
import com.nextcloud.client.player.util.PlayerUtil.applyVideoSize
import com.nextcloud.client.player.util.PlayerUtil.isPictureInPictureAllowed
import com.nextcloud.client.player.util.PlayerUtil.ownsPlayback
import com.nextcloud.ui.fileactions.FileAction
import com.nextcloud.ui.fileactions.FileActionsBottomSheet
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getSerializableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.PreviewPlaybackFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.FetchRemoteFileOperation
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Plays an audio or video page of the preview pager in place, so that swiping between images and media keeps the
 * user on the same screen. Playback itself is owned by the shared [PlaybackModel], the same one that drives
 * [com.nextcloud.client.player.ui.PlayerActivity], notification and background playback.
 */
class PreviewPlaybackFragment :
    Fragment(),
    PlaybackModel.Listener {

    companion object {
        private val TAG = PreviewPlaybackFragment::class.java.simpleName
        private const val ARGUMENT_FILE = "ARGUMENT_FILE"
        private const val ARGUMENT_COLLECTION = "ARGUMENT_COLLECTION"
        private const val ARGUMENT_AUTOPLAY = "ARGUMENT_AUTOPLAY"
        private const val ARGUMENT_PICTURE_IN_PICTURE_ON_BACK = "ARGUMENT_PICTURE_IN_PICTURE_ON_BACK"
        private const val SURFACE_ALPHA_VISIBLE = 1f
        private const val SURFACE_ALPHA_HIDDEN = 0f

        @Suppress("LongParameterList")
        fun newInstance(
            file: OCFile,
            collection: PlaybackCollection,
            autoplay: Boolean = false,
            pictureInPictureOnBack: Boolean = true
        ) = PreviewPlaybackFragment().apply {
            arguments = bundleOf(
                ARGUMENT_FILE to file,
                ARGUMENT_COLLECTION to collection,
                ARGUMENT_AUTOPLAY to autoplay,
                ARGUMENT_PICTURE_IN_PICTURE_ON_BACK to pictureInPictureOnBack
            )
        }
    }

    @Inject
    lateinit var playbackModel: PlaybackModel

    @Inject
    lateinit var playerLauncher: PlayerLauncher

    @Inject
    lateinit var playerThumbnailLoader: PlayerThumbnailLoader

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private lateinit var binding: PreviewPlaybackFragmentBinding
    private lateinit var file: OCFile
    private lateinit var playbackFile: PlaybackFile
    private var playbackCollection = PlaybackCollection.FOLDER
    private var autoplay: Boolean = false
    private var pictureInPictureOnBack: Boolean = true
    private var wasCurrentItem = false
    private var renderedVideoSize: VideoSize? = null

    private var pictureInPictureCallback: OnBackPressedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
        file = arguments.getParcelableArgument(ARGUMENT_FILE, OCFile::class.java)
            ?: throw IllegalArgumentException("bundle is not containing a file")
        playbackFile = file.toPlaybackFile()
        playbackCollection = arguments.getSerializableArgument(ARGUMENT_COLLECTION, PlaybackCollection::class.java)
            ?: PlaybackCollection.FOLDER
        autoplay = arguments?.getBoolean(ARGUMENT_AUTOPLAY) == true
        pictureInPictureOnBack = arguments?.getBoolean(ARGUMENT_PICTURE_IN_PICTURE_ON_BACK) != false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = PreviewPlaybackFragmentBinding.inflate(inflater, container, false)
        loadThumbnail()
        registerPictureInPictureOnBack()
        binding.playerControlView.navigator = activity as? MediaNavigator
        updatePlayerControlsVisibility()
        binding.root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (ownsPlayback(binding.surfaceView)) render(playbackModel.state)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.custom_menu_placeholder, menu)
                    val item = menu.findItem(R.id.custom_menu_placeholder_item)
                    item.icon?.let {
                        item.setIcon(
                            viewThemeUtils.platform.colorDrawable(
                                it,
                                ContextCompat.getColor(requireContext(), R.color.white)
                            )
                        )
                    }
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
                    R.id.custom_menu_placeholder_item -> {
                        onOverflowClick()
                        true
                    }

                    else -> false
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun onOverflowClick(isManualClick: Boolean = false) {
        val storageManager = previewActivity()?.storageManager ?: return
        val updatedFile = storageManager.getFileById(file.fileId)

        // check for albums file for album file both local and remoteId will be same configured at operation level
        if (!isManualClick && updatedFile != null && updatedFile.localId.toString() == updatedFile.remoteId) {
            fetchFileMetaDataIfAbsent(updatedFile)
            return
        }

        updatedFile?.let { actionsFile ->
            val additionalFilter = FileAction.getFilePreviewActions(actionsFile)
            FileActionsBottomSheet.newInstance(actionsFile, false, additionalFilter)
                .setResultListener(childFragmentManager, viewLifecycleOwner) { itemId: Int ->
                    onFileActionChosen(itemId)
                }
                .show(childFragmentManager, "actions")
        }
    }

    private fun fetchFileMetaDataIfAbsent(ocFile: OCFile) {
        val previewActivity = previewActivity() ?: return
        val context = context ?: return

        previewActivity.showLoadingDialog(getString(R.string.wait_a_moment))
        lifecycleScope.launch(Dispatchers.IO) {
            val operation = FetchRemoteFileOperation(
                context,
                accountManager.user,
                ocFile,
                removeFileFromDb = true,
                storageManager = previewActivity.storageManager
            )
            val result = operation.execute(context)

            withContext(Dispatchers.Main) {
                previewActivity.dismissLoadingDialog()

                if (result?.isSuccess == true && result.resultData != null) {
                    file = result.resultData as OCFile
                    onOverflowClick(isManualClick = true)
                } else {
                    Log_OC.d(TAG, result?.logMessage)
                    DisplayUtils.showSnackMessage(binding.root, result.getLogMessage(context))
                }
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun onFileActionChosen(itemId: Int) {
        val previewActivity = previewActivity() ?: return
        val fileOperationsHelper = previewActivity.fileOperationsHelper

        when (itemId) {
            R.id.action_see_details -> previewActivity.showDetails(file)

            R.id.action_download_file -> previewActivity.requestForDownload(file)

            R.id.action_export_file -> fileOperationsHelper.exportFiles(
                arrayListOf(file),
                context,
                view,
                backgroundJobManager
            )

            R.id.action_send_share_file -> if (file.isSharedWithMe && !file.canReshare()) {
                Snackbar.make(requireView(), R.string.resharing_is_not_allowed, Snackbar.LENGTH_LONG).show()
            } else {
                fileOperationsHelper.sendShareFile(file)
            }

            R.id.action_send_file -> fileOperationsHelper.sendShareFile(file, true)

            R.id.action_open_file_with -> fileOperationsHelper.openFile(file)

            R.id.action_stream_media -> {
                playbackModel.pause()
                fileOperationsHelper.streamMediaFile(file)
            }

            R.id.action_remove_file -> {
                playbackModel.pause()
                RemoveFilesDialogFragment.newInstance(
                    file
                ).show(parentFragmentManager, ConfirmationDialogFragment.FTAG_CONFIRMATION)
            }

            R.id.action_add_to_album -> fileOperationsHelper.addFileToAlbum(listOf(file))

            R.id.action_lock_file -> fileOperationsHelper.toggleFileLock(file, true)

            R.id.action_unlock_file -> fileOperationsHelper.toggleFileLock(file, false)
        }
    }

    private fun registerPictureInPictureOnBack() {
        if (!pictureInPictureOnBack || !MimeTypeUtil.isVideo(file)) {
            return
        }

        if (!requireContext().isPictureInPictureAllowed()) {
            return
        }

        pictureInPictureCallback = requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            enabled = false
        ) {
            if (previewActivity()?.enterPictureInPicture() == true) {
                return@addCallback
            }

            isEnabled = false
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onStart() {
        super.onStart()
        playbackModel.addListener(this)
        render(playbackModel.state)
    }

    override fun onResume() {
        super.onResume()
        playbackModel.onPictureInPictureClose?.invoke()
        pictureInPictureCallback?.isEnabled = true
        preparePlayback()
        binding.playerControlView.onStart()
        render(playbackModel.state)
    }

    override fun onPause() {
        pictureInPictureCallback?.isEnabled = false
        binding.playerControlView.onStop()
        if (!isInPictureInPictureMode() && isCurrentItem(playbackModel.state)) {
            playbackModel.pause()
            playbackModel.setVideoSurfaceView(null)
        }
        super.onPause()
    }

    override fun onStop() {
        playbackModel.removeListener(this)
        super.onStop()
    }

    override fun onDestroyView() {
        playbackModel.clearVideoSurfaceView(binding.surfaceView)
        binding.playerControlView.navigator = null
        pictureInPictureCallback?.remove()
        pictureInPictureCallback = null
        super.onDestroyView()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        render(playbackModel.state)
    }

    override fun onPlaybackUpdate(state: PlaybackState) {
        render(state)
    }

    /**
     * Reuses the queue the player already holds when it contains this page, so that swiping between media pages
     * does not rebuild it.
     */
    private fun preparePlayback() {
        val state = playbackModel.state
        if (state != null && state.currentFiles.any { it.id == playbackFile.id }) {
            playbackModel.switchToFile(playbackFile)
            if (autoplay) {
                playbackModel.play()
            }
        } else {
            playerLauncher.prepare(this, file, playbackCollection, autoplay)
        }
    }

    private fun isCurrentItem(state: PlaybackState?): Boolean = state?.currentItemState?.file?.id == playbackFile.id

    private fun loadThumbnail() {
        viewLifecycleOwner.lifecycleScope.launch {
            val context = context ?: return@launch
            val size = context.resources.getDimension(R.dimen.player_album_cover_size).toInt()
            playerThumbnailLoader.await(playbackFile, size, size)?.let(binding.thumbnail::setImageBitmap)
        }
    }

    private fun render(state: PlaybackState?) {
        updatePlayerControlsVisibility()

        if (isCurrentItem(state)) {
            wasCurrentItem = true
            showVideo(state?.currentItemState?.videoSize)
            return
        }

        val playbackMovedOn = wasCurrentItem && ownsPlayback(binding.surfaceView)
        wasCurrentItem = false
        renderedVideoSize = null
        binding.surfaceView.visibility = View.GONE
        if (playbackMovedOn) {
            showPageOfCurrentItem(state)
        }
    }

    private fun showPageOfCurrentItem(state: PlaybackState?) {
        val previewActivity = previewActivity() ?: return
        val localId = state?.currentItemState?.file?.id?.toLongOrNull()
        if (localId != null && previewActivity.showFilePage(localId)) return

        if (isInPictureInPictureMode()) {
            previewActivity.finishKeepingPlayback()
        }
    }

    private fun previewActivity(): PreviewImageActivity? = activity as? PreviewImageActivity

    private fun isInPictureInPictureMode(): Boolean = activity?.isInPictureInPictureMode == true

    private fun updatePlayerControlsVisibility() {
        binding.playerControlView.isVisible = !isInPictureInPictureMode()
    }

    private fun showVideo(videoSize: VideoSize?) {
        if (ownsPlayback(binding.surfaceView)) {
            playbackModel.setVideoSurfaceView(binding.surfaceView)
        }

        val size = videoSize ?: renderedVideoSize
        renderedVideoSize = size

        binding.surfaceView.visibility = View.VISIBLE
        binding.surfaceView.alpha = if (size != null) SURFACE_ALPHA_VISIBLE else SURFACE_ALPHA_HIDDEN

        if (size != null) {
            binding.surfaceView.applyVideoSize(size)
        }
    }
}
