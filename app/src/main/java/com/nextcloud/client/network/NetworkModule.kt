/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.network

import android.content.Context
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.operations.GetMethod
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class NetworkModule {

    @Provides
    @Singleton
    fun connectivityService(
        context: Context,
        accountManager: UserAccountManager,
        clientFactory: ClientFactory,
        walledCheckCache: WalledCheckCache
    ): ConnectivityService = ConnectivityServiceImpl(
        context,
        accountManager,
        clientFactory,
        { GetMethod(it, false) },
        walledCheckCache
    )

    @Provides
    @Singleton
    fun clientFactory(context: Context?): ClientFactory = ClientFactoryImpl(context)
}
