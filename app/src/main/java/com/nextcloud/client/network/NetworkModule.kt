/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.network;

import android.content.Context;

import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.operations.GetMethod;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class NetworkModule {

    // todo: check 429, remove manual instance...
    @Provides
    @Singleton
    ConnectivityService connectivityService(Context context,
                                            UserAccountManager accountManager,
                                            ClientFactory clientFactory,
                                            WalledCheckCache walledCheckCache) {
        var instance = ConnectivityServiceImpl.Companion.getInstance();
        if (instance != null) {
            return instance;
        }

        instance = new ConnectivityServiceImpl(context,
                                               accountManager,
                                               clientFactory,
                                               url -> new GetMethod(url, false),
                                               walledCheckCache
        );
        ConnectivityServiceImpl.Companion.setInstance(instance);
        return instance;
    }

    @Provides
    @Singleton
    ClientFactory clientFactory(Context context) {
        return new ClientFactoryImpl(context);
    }
}
