/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.media3.session

import android.content.Context
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import com.nextcloud.client.player.media3.datasource.PlaybackDataSourceFactory
import com.nextcloud.client.player.media3.resumption.PlaybackResumptionConfigStore
import com.owncloud.android.R
import javax.inject.Inject

private const val SEEK_FORWARD_INCREMENT_IN_MILLISECONDS = 5000L

@UnstableApi
class MediaSessionFactory @Inject constructor(
    private val context: Context,
    private val dataSourceFactory: PlaybackDataSourceFactory,
    private val sessionCallback: MediaSessionCallback,
    private val resumptionConfigStore: PlaybackResumptionConfigStore,
    private val bitmapLoader: MediaSessionBitmapLoader
) {

    private val resumptionPlayerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let { resumptionConfigStore.updateCurrentFileId(it.mediaId) }
        }
    }

    fun create(): MediaSession {
        val player = createPlayer()
        player.addListener(resumptionPlayerListener)
        return MediaSession
            .Builder(context, player)
            .setBitmapLoader(bitmapLoader)
            .setCallback(sessionCallback)
            .setCustomLayout(createCustomLayout())
            .build()
    }

    private fun createPlayer(): Player = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .setAudioAttributes(AudioAttributes.DEFAULT, true)
        .setHandleAudioBecomingNoisy(true)
        .setSeekForwardIncrementMs(SEEK_FORWARD_INCREMENT_IN_MILLISECONDS)
        .build()

    private fun createCustomLayout(): List<CommandButton> = listOf(
        CommandButton
            .Builder()
            .setDisplayName(context.getString(R.string.player_media_controls_close_action_title))
            .setIconResId(R.drawable.player_ic_close)
            .setSessionCommand(SessionCommand(MediaSessionCallback.CLOSE_ACTION, Bundle.EMPTY))
            .build()
    )
}
