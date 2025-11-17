/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
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
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.FileDataStorageManager
import javax.inject.Inject

@UnstableApi
class DefaultDataSourceFactory @Inject constructor(
    private val context: Context,
    private val cache: Cache,
    private val fileDataStorageManager: FileDataStorageManager,
    private val clientFactory: ClientFactory,
    private val accountManager: UserAccountManager
) : DataSource.Factory {

    override fun createDataSource(): DataSource = CacheDataSource.Factory()
        .setUpstreamDataSourceFactory(createUpstreamDataSourceFactory())
        .setCache(cache)
        .createDataSource()

    private fun createUpstreamDataSourceFactory() = DataSource.Factory {
        DefaultDataSource(
            delegate = DefaultDataSource.Factory(context, createHttpDataSourceFactory()).createDataSource(),
            fileDataStorageManager = fileDataStorageManager,
            ownCloudClient = clientFactory.create(accountManager.user)
        )
    }

    private fun createHttpDataSourceFactory(): HttpDataSource.Factory {
        val client = clientFactory.createNextcloudClient(accountManager.user).client
        return OkHttpDataSource.Factory(client)
            .setUserAgent(MainApp.getUserAgent())
    }
}
