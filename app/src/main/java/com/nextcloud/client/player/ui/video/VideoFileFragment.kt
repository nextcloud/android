/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.player.media3.PlaybackModel
import com.nextcloud.client.player.model.ThumbnailLoader
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.model.state.PlaybackState
import com.nextcloud.client.player.model.state.VideoSize
import com.nextcloud.client.player.util.applyVideoSize
import com.nextcloud.utils.extensions.getSerializableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.PlayerVideoFileFragmentBinding
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.launch
import javax.inject.Inject

class VideoFileFragment :
    Fragment(),
    PlaybackModel.Listener {

    companion object {
        private const val ARGUMENT_FILE = "ARGUMENT_FILE"
        private const val SURFACE_ALPHA_VISIBLE = 1f
        private const val SURFACE_ALPHA_HIDDEN = 0f

        fun createInstance(file: PlaybackFile) = VideoFileFragment().apply {
            arguments = bundleOf(ARGUMENT_FILE to file)
        }
    }

    @Inject
    lateinit var playerModel: PlaybackModel

    @Inject
    lateinit var thumbnailLoader: ThumbnailLoader

    private var _binding: PlayerVideoFileFragmentBinding? = null
    private val binding get() = checkNotNull(_binding) { "Binding accessed outside of the view lifecycle" }

    private var previousVideoSize: VideoSize? = null

    private val file by lazy {
        requireNotNull(arguments.getSerializableArgument(ARGUMENT_FILE, PlaybackFile::class.java)) {
            "VideoFileFragment requires a $ARGUMENT_FILE argument"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        PlayerVideoFileFragmentBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadFileThumbnail()
    }

    override fun onStart() {
        super.onStart()
        render(playerModel.state)
        playerModel.addListener(this)
    }

    override fun onStop() {
        playerModel.removeListener(this)
        super.onStop()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onPlaybackUpdate(state: PlaybackState) {
        render(state)
    }

    private fun loadFileThumbnail() = viewLifecycleOwner.lifecycleScope.launch {
        val thumbnailSize = resources.getDimensionPixelSize(R.dimen.player_album_cover_size)
        val thumbnail = thumbnailLoader.await(requireContext(), file, thumbnailSize, thumbnailSize) ?: return@launch
        binding.thumbnail.setImageBitmap(thumbnail)
    }

    private fun render(state: PlaybackState?) {
        val currentItemState = state?.currentItemState

        if (currentItemState?.file == file) {
            showVideo(currentItemState.videoSize)
            return
        }

        binding.surfaceView.isVisible = false
        if (currentItemState == null) {
            playerModel.setVideoSurfaceView(null)
        }
    }

    private fun showVideo(videoSize: VideoSize?) {
        playerModel.setVideoSurfaceView(binding.surfaceView)
        binding.surfaceView.isVisible = true
        binding.surfaceView.alpha = if (videoSize == null) SURFACE_ALPHA_HIDDEN else SURFACE_ALPHA_VISIBLE

        if (videoSize == null || previousVideoSize == videoSize) return

        previousVideoSize = videoSize
        binding.surfaceView.applyVideoSize(videoSize)
    }
}
