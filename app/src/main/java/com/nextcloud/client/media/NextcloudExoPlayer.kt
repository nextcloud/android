/*
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * Copyright (C) 2022 Álvaro Brey Vilas
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.media

import android.content.Context
import androidx.annotation.OptIn
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
            .setSeekForwardIncrementMs(FIVE_SECONDS_IN_MILLIS)
            .build()
    }
}
