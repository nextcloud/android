/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.network;

import android.content.Context;

import com.nextcloud.client.account.UserAccountManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class NetworkModule {

    @Provides
    ConnectivityService connectivityService(Context context,
                                            UserAccountManager accountManager,
                                            ClientFactory clientFactory,
                                            WalledCheckCache walledCheckCache) {
        return new ConnectivityServiceImpl(context,
                                           accountManager,
                                           clientFactory,
                                           new ConnectivityServiceImpl.GetRequestBuilder(),
                                           walledCheckCache
        );
    }

    @Provides
    @Singleton
    ClientFactory clientFactory(Context context) {
        return new ClientFactoryImpl(context);
    }
}
