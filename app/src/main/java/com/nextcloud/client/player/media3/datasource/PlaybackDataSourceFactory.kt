/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.media3.datasource

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.nextcloud.common.NextcloudClient
import com.nextcloud.repository.ClientRepository
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.FileDataStorageManager
import dagger.Lazy
import javax.inject.Inject

@UnstableApi
class PlaybackDataSourceFactory @Inject constructor(
    private val context: Context,
    private val cache: Lazy<Cache>,
    private val fileDataStorageManager: FileDataStorageManager,
    private val clientRepository: ClientRepository
) : DataSource.Factory {

    override fun createDataSource(): DataSource = CacheDataSource.Factory()
        .setUpstreamDataSourceFactory(createUpstreamDataSourceFactory())
        .setCache(cache.get())
        .createDataSource()

    private fun createUpstreamDataSourceFactory() = DataSource.Factory {
        PlaybackDataSource(
            createDelegate = { nextcloudClient ->
                DefaultDataSource.Factory(context, createHttpDataSourceFactory(nextcloudClient)).createDataSource()
            },
            fileDataStorageManager = fileDataStorageManager,
            clientRepository = clientRepository
        )
    }

    private fun createHttpDataSourceFactory(nextcloudClient: NextcloudClient): HttpDataSource.Factory =
        OkHttpDataSource.Factory(nextcloudClient.client)
            .setUserAgent(MainApp.userAgent)
}
