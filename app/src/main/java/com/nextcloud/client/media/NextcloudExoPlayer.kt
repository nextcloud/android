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
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.nextcloud.common.NextcloudClient

object NextcloudExoPlayer {

    /**
     * Creates an [ExoPlayer] that uses [NextcloudClient] for HTTP connections, thus respecting redirections,
     * IP versions and certificates.
     *
     */
    @JvmStatic
    fun createNextcloudExoplayer(context: Context, nextcloudClient: NextcloudClient): ExoPlayer {
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(nextcloudClient.client)
        val mediaSourceFactory = DefaultMediaSourceFactory(
            DefaultDataSource.Factory(
                context,
                okHttpDataSourceFactory
            )
        )
        return ExoPlayer.Builder(context).setMediaSourceFactory(mediaSourceFactory).build()
    }
}
