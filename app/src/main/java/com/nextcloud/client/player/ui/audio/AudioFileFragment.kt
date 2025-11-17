/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.ui.audio

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
import com.nextcloud.client.player.model.state.PlaybackItemMetadata
import com.nextcloud.client.player.model.state.PlaybackState
import com.owncloud.android.R
import com.owncloud.android.databinding.PlayerAudioFileFragmentBinding
import com.owncloud.android.utils.DisplayUtils
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

open class AudioFileFragment :
    Fragment(),
    PlaybackModel.Listener {

    companion object {
        private const val ARGUMENT_FILE = "ARGUMENT_FILE"

        fun createInstance(file: PlaybackFile) = AudioFileFragment().apply {
            arguments = bundleOf(ARGUMENT_FILE to file)
        }
    }

    @Inject
    lateinit var playbackModel: PlaybackModel

    @Inject
    lateinit var thumbnailLoader: ThumbnailLoader

    private lateinit var binding: PlayerAudioFileFragmentBinding
    private lateinit var loadFileThumbnailJob: Job
    private var isFileThumbnailLoaded = false
    private var metadata: PlaybackItemMetadata? = null
    private val file by lazy { arguments?.getSerializable(ARGUMENT_FILE) as PlaybackFile }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = PlayerAudioFileFragmentBinding.inflate(inflater, container, false)
        binding.title.isSelected = true
        binding.title.text = file.getNameWithoutExtension()
        binding.fileDetails.text = file.getDetailsText()
        loadFileThumbnailJob = loadFileThumbnail()
        return binding.getRoot()
    }

    override fun onStart() {
        super.onStart()
        playbackModel.state.ifPresent(::onPlaybackUpdate)
        playbackModel.addListener(this)
    }

    override fun onStop() {
        playbackModel.removeListener(this)
        super.onStop()
    }

    override fun onPlaybackUpdate(state: PlaybackState) {
        state.currentItemState?.let {
            if (it.file.id == file.id && it.metadata != null && it.metadata != metadata) {
                onMetadataUpdate(it.metadata)
            }
        }
    }

    private fun onMetadataUpdate(metadata: PlaybackItemMetadata) {
        this.metadata = metadata
        if (!isFileThumbnailLoaded && (metadata.artworkData != null || metadata.artworkUri != null)) {
            loadFileThumbnailJob.takeIf { it.isActive }?.cancel()
            loadMetadataArtwork(metadata)
        }
        binding.title.text = if (metadata.artist.isNullOrEmpty()) {
            metadata.title
        } else {
            "${metadata.artist} • ${metadata.title}"
        }
    }

    private fun loadFileThumbnail(): Job = viewLifecycleOwner.lifecycleScope.launch {
        val thumbnailSize = resources.getDimension(R.dimen.player_album_cover_size).toInt()
        val thumbnail = thumbnailLoader.await(requireContext(), file, thumbnailSize, thumbnailSize)
        if (thumbnail != null) {
            binding.albumCover.setImageBitmap(thumbnail)
            isFileThumbnailLoaded = true
        }
    }

    private fun loadMetadataArtwork(metadata: PlaybackItemMetadata) {
        val source = metadata.artworkData ?: metadata.artworkUri ?: return
        thumbnailLoader.load(binding.albumCover, source, file.id)
    }

    private fun PlaybackFile.getDetailsText(): String {
        val size = if (contentLength > 0) DisplayUtils.bytesToHumanReadable(contentLength) else ""
        val date = if (lastModified > 0) getLastModifiedText(lastModified) else ""
        return if (size.isNotEmpty() && date.isNotEmpty()) "$size, $date" else size + date
    }

    private fun getLastModifiedText(lastModified: Long): String {
        val relativeTimestamp = DisplayUtils.getRelativeTimestamp(context, lastModified)
        return getString(R.string.player_last_modified, relativeTimestamp)
    }
}
