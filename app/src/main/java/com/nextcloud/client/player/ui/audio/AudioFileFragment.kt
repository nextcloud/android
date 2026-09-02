/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui.audio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.player.media3.PlaybackModel
import com.nextcloud.client.player.model.ThumbnailLoader
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.model.state.PlaybackItemMetadata
import com.nextcloud.client.player.model.state.PlaybackState
import com.nextcloud.client.player.util.PlayerUtil.getPlaybackFile
import com.nextcloud.client.player.util.PlayerUtil.putPlaybackFile
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
        private const val DETAILS_SEPARATOR = ", "

        fun createInstance(file: PlaybackFile) = AudioFileFragment().apply {
            arguments = Bundle().apply { putPlaybackFile(ARGUMENT_FILE, file) }
        }
    }

    @Inject
    lateinit var playbackModel: PlaybackModel

    @Inject
    lateinit var thumbnailLoader: ThumbnailLoader

    private var _binding: PlayerAudioFileFragmentBinding? = null
    private val binding get() = checkNotNull(_binding) { "Binding accessed outside of the view lifecycle" }

    private var fileThumbnailJob: Job? = null
    private var isFileThumbnailLoaded = false
    private var metadata: PlaybackItemMetadata? = null

    private val file by lazy {
        requireNotNull(arguments.getPlaybackFile(ARGUMENT_FILE)) {
            "AudioFileFragment requires a $ARGUMENT_FILE argument"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        PlayerAudioFileFragmentBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.title.isSelected = true
        binding.title.text = file.getNameWithoutExtension()
        binding.fileDetails.text = file.getDetailsText()
        fileThumbnailJob = loadFileThumbnail()
    }

    override fun onStart() {
        super.onStart()
        playbackModel.state?.let(::onPlaybackUpdate)
        playbackModel.addListener(this)
    }

    override fun onStop() {
        playbackModel.removeListener(this)
        super.onStop()
    }

    override fun onDestroyView() {
        fileThumbnailJob = null
        _binding = null
        super.onDestroyView()
    }

    override fun onPlaybackUpdate(state: PlaybackState) {
        val itemState = state.currentItemState?.takeIf { it.file.id == file.id } ?: return
        val newMetadata = itemState.metadata?.takeIf { it != metadata } ?: return
        onMetadataUpdate(newMetadata)
    }

    private fun onMetadataUpdate(metadata: PlaybackItemMetadata) {
        this.metadata = metadata

        if (!isFileThumbnailLoaded && metadata.hasArtwork()) {
            fileThumbnailJob?.cancel()
            loadMetadataArtwork(metadata)
        }

        binding.title.text = metadata.toTitleText()
    }

    private fun PlaybackItemMetadata.hasArtwork(): Boolean = artworkData != null || artworkUri != null

    private fun PlaybackItemMetadata.toTitleText(): CharSequence = when {
        artist.isNullOrEmpty() -> title
        else -> getString(R.string.player_audio_title_with_artist, artist, title)
    }

    private fun loadFileThumbnail(): Job = viewLifecycleOwner.lifecycleScope.launch {
        val context = context ?: return@launch
        val thumbnailSize = context.resources.getDimensionPixelSize(R.dimen.player_album_cover_size)
        val thumbnail = thumbnailLoader.await(context, file, thumbnailSize, thumbnailSize) ?: return@launch
        val albumCover = _binding?.albumCover ?: return@launch
        albumCover.setImageBitmap(thumbnail)
        isFileThumbnailLoaded = true
    }

    private fun loadMetadataArtwork(metadata: PlaybackItemMetadata) {
        val source = metadata.artworkData ?: metadata.artworkUri ?: return
        thumbnailLoader.load(binding.albumCover, source, file.id)
    }

    private fun PlaybackFile.getDetailsText(): String = listOfNotNull(
        contentLength.takeIf { it > 0 }?.let { DisplayUtils.bytesToHumanReadable(it) },
        lastModified.takeIf { it > 0 }?.let(::getLastModifiedText)
    ).joinToString(DETAILS_SEPARATOR)

    private fun getLastModifiedText(lastModified: Long): String {
        val relativeTimestamp = DisplayUtils.getRelativeTimestamp(context, lastModified)
        return getString(R.string.player_last_modified, relativeTimestamp)
    }
}
