package com.nextcloud.client.preferences

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.nextcloud.client.account.UserAccountManager
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class PreferencesModule {
    @Provides
    @Singleton
    fun sharedPreferences(context: Context?): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @Singleton
    fun appPreferences(
        context: Context,
        sharedPreferences: SharedPreferences,
        userAccountManager: UserAccountManager
    ): AppPreferences {
        return AppPreferencesImpl(context, sharedPreferences, userAccountManager)
    }
}