package com.nmc.android.app_review


import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.app_review.InAppReviewHelperImpl
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class InAppReviewModule {

    @Provides
    @Singleton
    internal fun providesInAppReviewHelper(appPreferences: AppPreferences): InAppReviewHelper {
        return InAppReviewHelperImpl(appPreferences)
    }
}
