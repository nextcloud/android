/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.nextcloud.client.player.media3.common.PlayerFactory
import javax.inject.Inject

private const val FIVE_SECONDS_IN_MILLIS = 5000L

@UnstableApi
class ExoPlayerFactory @Inject constructor(
    private val context: Context,
    private val dataSourceFactory: DataSource.Factory
) : PlayerFactory {

    override fun create(): Player = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .setAudioAttributes(AudioAttributes.DEFAULT, true)
        .setHandleAudioBecomingNoisy(true)
        .setSeekForwardIncrementMs(FIVE_SECONDS_IN_MILLIS)
        .build()
}
