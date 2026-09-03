/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.player.media3.PlaybackModel
import com.nextcloud.client.player.model.ThumbnailLoader
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
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getSerializableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.PreviewPlaybackFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.MimeTypeUtil
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.launch
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
        private const val ARGUMENT_FILE = "ARGUMENT_FILE"
        private const val ARGUMENT_COLLECTION = "ARGUMENT_COLLECTION"
        private const val ARGUMENT_AUTOPLAY = "ARGUMENT_AUTOPLAY"
        private const val SURFACE_ALPHA_VISIBLE = 1f
        private const val SURFACE_ALPHA_HIDDEN = 0f

        fun newInstance(file: OCFile, collection: PlaybackCollection, autoplay: Boolean = false) =
            PreviewPlaybackFragment().apply {
                arguments = bundleOf(
                    ARGUMENT_FILE to file,
                    ARGUMENT_COLLECTION to collection,
                    ARGUMENT_AUTOPLAY to autoplay
                )
            }
    }

    @Inject
    lateinit var playbackModel: PlaybackModel

    @Inject
    lateinit var playerLauncher: PlayerLauncher

    @Inject
    lateinit var thumbnailLoader: ThumbnailLoader

    private lateinit var binding: PreviewPlaybackFragmentBinding
    private lateinit var file: OCFile
    private lateinit var playbackFile: PlaybackFile
    private var playbackCollection = PlaybackCollection.FOLDER
    private var autoplay: Boolean = false
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

    private fun registerPictureInPictureOnBack() {
        if (!MimeTypeUtil.isVideo(file) || !requireContext().isPictureInPictureAllowed()) {
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
            thumbnailLoader.await(playbackFile, size, size)?.let(binding.thumbnail::setImageBitmap)
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
