/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.ui.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.player.model.PlaybackModel
import com.nextcloud.client.player.model.ThumbnailLoader
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.model.state.PlaybackState
import com.nextcloud.client.player.model.state.VideoSize
import com.nextcloud.client.player.util.getDisplayHeight
import com.nextcloud.client.player.util.getDisplayWidth
import com.owncloud.android.R
import com.owncloud.android.databinding.PlayerVideoFileFragmentBinding
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.jvm.optionals.getOrNull

class VideoFileFragment :
    Fragment(),
    PlaybackModel.Listener {

    companion object {
        private const val ARGUMENT_FILE = "ARGUMENT_FILE"

        fun createInstance(file: PlaybackFile) = VideoFileFragment().apply {
            arguments = bundleOf(ARGUMENT_FILE to file)
        }
    }

    @Inject
    lateinit var playerModel: PlaybackModel

    @Inject
    lateinit var thumbnailLoader: ThumbnailLoader

    private lateinit var file: PlaybackFile

    private lateinit var binding: PlayerVideoFileFragmentBinding

    private var previousVideoSize: VideoSize? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
        this.file = arguments?.getSerializable(ARGUMENT_FILE) as PlaybackFile
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = PlayerVideoFileFragmentBinding.inflate(inflater, container, false)
        loadFileThumbnail()
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        render(playerModel.state.getOrNull())
        playerModel.addListener(this)
    }

    override fun onStop() {
        playerModel.removeListener(this)
        super.onStop()
    }

    override fun onPlaybackUpdate(state: PlaybackState) {
        render(state)
    }

    private fun loadFileThumbnail() {
        viewLifecycleOwner.lifecycleScope.launch {
            val context = context ?: return@launch
            val thumbnailSize = context.resources.getDimension(R.dimen.player_album_cover_size)
            val thumbnail = thumbnailLoader.await(context, file, thumbnailSize.toInt(), thumbnailSize.toInt())
            thumbnail?.let(binding.thumbnail::setImageBitmap)
        }
    }

    private fun render(state: PlaybackState?) {
        val currentItemState = state?.currentItemState
        if (currentItemState?.file == file) {
            showVideo(currentItemState.videoSize)
        } else {
            binding.surfaceView.visibility = View.GONE
            if (currentItemState == null) {
                playerModel.setVideoSurfaceView(null)
            }
        }
    }

    private fun showVideo(videoSize: VideoSize?) {
        playerModel.setVideoSurfaceView(binding.surfaceView)
        binding.surfaceView.visibility = View.VISIBLE
        binding.surfaceView.alpha = if (videoSize != null) 1f else 0f

        if (videoSize != null && previousVideoSize != videoSize) {
            previousVideoSize = videoSize
            setVideoSize(videoSize.width, videoSize.height)
        }
    }

    private fun setVideoSize(videoWidth: Int, videoHeight: Int) {
        val screenWidth = requireContext().getDisplayWidth()
        val screenHeight = requireContext().getDisplayHeight()
        val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()
        val videoProportion = videoWidth.toFloat() / videoHeight.toFloat()

        val layoutParams = binding.surfaceView.layoutParams
        if (screenProportion < videoProportion) {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.height = (screenWidth.toFloat() / videoProportion).toInt()
        } else {
            layoutParams.width = (videoProportion * screenHeight.toFloat()).toInt()
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        binding.surfaceView.layoutParams = layoutParams
    }
}
