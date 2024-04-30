/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import com.nextcloud.client.account.UserAccountManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class PreferencesModule {

    @Provides
    @Singleton
    public SharedPreferences sharedPreferences(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Provides
    @Singleton
    public AppPreferences appPreferences(Context context,
                                         SharedPreferences sharedPreferences,
                                         UserAccountManager userAccountManager) {
        return new AppPreferencesImpl(context, sharedPreferences, userAccountManager);
    }
}
