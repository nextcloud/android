/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.MainApp

object NextcloudExoPlayer {
    private const val FIVE_SECONDS_IN_MILLIS = 5000L

    /**
     * Creates an [ExoPlayer] that uses [NextcloudClient] for HTTP connections, thus respecting redirections,
     * IP versions and certificates.
     *
     */
    @OptIn(UnstableApi::class)
    @JvmStatic
    fun createNextcloudExoplayer(context: Context, nextcloudClient: NextcloudClient): ExoPlayer {
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(nextcloudClient.client)
        okHttpDataSourceFactory.setUserAgent(MainApp.getUserAgent())
        val mediaSourceFactory = DefaultMediaSourceFactory(
            DefaultDataSource.Factory(
                context,
                okHttpDataSourceFactory
            )
        )
        return ExoPlayer
            .Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekForwardIncrementMs(FIVE_SECONDS_IN_MILLIS)
            .build()
    }
}
