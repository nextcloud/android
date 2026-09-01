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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.player.media3.PlaybackModel
import com.nextcloud.client.player.model.ThumbnailLoader
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.model.file.toPlaybackFile
import com.nextcloud.client.player.model.file.PlaybackFileType
import com.nextcloud.client.player.model.state.PlaybackState
import com.nextcloud.client.player.model.state.PlayerState
import com.nextcloud.client.player.model.state.VideoSize
import com.nextcloud.client.player.ui.PlayerActivity
import com.nextcloud.client.player.ui.PlayerLauncher
import com.nextcloud.client.player.util.applyVideoSize
import com.nextcloud.client.player.util.isPictureInPictureAllowed
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getSerializableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.PreviewPlaybackFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.fragment.SearchType
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
        private const val ARGUMENT_SEARCH_TYPE = "ARGUMENT_SEARCH_TYPE"
        private const val ARGUMENT_AUTOPLAY = "ARGUMENT_AUTOPLAY"

        fun newInstance(file: OCFile, searchType: SearchType?, autoplay: Boolean = false) =
            PreviewPlaybackFragment().apply {
                arguments = bundleOf(
                    ARGUMENT_FILE to file,
                    ARGUMENT_SEARCH_TYPE to searchType,
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
    private var searchType: SearchType? = null
    private var autoplay: Boolean = false
    private var previousVideoSize: VideoSize? = null

    private var pictureInPictureCallback: OnBackPressedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
        file = arguments.getParcelableArgument(ARGUMENT_FILE, OCFile::class.java)
            ?: throw IllegalArgumentException("bundle is not containing a file")
        playbackFile = file.toPlaybackFile()
        searchType = arguments.getSerializableArgument(ARGUMENT_SEARCH_TYPE, SearchType::class.java)
        autoplay = arguments?.getBoolean(ARGUMENT_AUTOPLAY) == true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = PreviewPlaybackFragmentBinding.inflate(inflater, container, false)
        loadThumbnail()
        registerPictureInPictureOnBack()
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
            val resumePlayback = playbackModel.state?.currentItemState?.playerState == PlayerState.PLAYING
            startActivity(
                PlayerActivity.createPictureInPictureIntent(
                    requireContext(),
                    PlaybackFileType.VIDEO,
                    resumePlayback
                )
            )
            requireActivity().finish()
        }
    }

    override fun onResume() {
        super.onResume()
        playbackModel.onPictureInPictureClose?.invoke()
        pictureInPictureCallback?.isEnabled = true
        preparePlayback()
        playbackModel.addListener(this)
        binding.playerControlView.onStart()
        render(playbackModel.state)
    }

    override fun onPause() {
        pictureInPictureCallback?.isEnabled = false
        binding.playerControlView.onStop()
        playbackModel.removeListener(this)
        if (isCurrentItem(playbackModel.state)) {
            playbackModel.pause()
            playbackModel.setVideoSurfaceView(null)
        }
        super.onPause()
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
            playerLauncher.prepare(this, file, searchType, autoplay)
        }
    }

    private fun isCurrentItem(state: PlaybackState?): Boolean = state?.currentItemState?.file?.id == playbackFile.id

    private fun loadThumbnail() {
        viewLifecycleOwner.lifecycleScope.launch {
            val context = context ?: return@launch
            val size = context.resources.getDimension(R.dimen.player_album_cover_size).toInt()
            thumbnailLoader.await(context, playbackFile, size, size)?.let(binding.thumbnail::setImageBitmap)
        }
    }

    private fun render(state: PlaybackState?) {
        if (!isCurrentItem(state)) {
            binding.surfaceView.visibility = View.GONE
            return
        }
        showVideo(state?.currentItemState?.videoSize)
    }

    private fun showVideo(videoSize: VideoSize?) {
        playbackModel.setVideoSurfaceView(binding.surfaceView)
        binding.surfaceView.visibility = View.VISIBLE
        binding.surfaceView.alpha = if (videoSize != null) 1f else 0f

        if (videoSize != null && previousVideoSize != videoSize) {
            previousVideoSize = videoSize
            binding.surfaceView.applyVideoSize(videoSize)
        }
    }
}
