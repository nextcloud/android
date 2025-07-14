/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.appReview

import com.nextcloud.android.appReview.InAppReviewHelperImpl
import com.nextcloud.client.preferences.AppPreferences
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class InAppReviewModule {

    @Provides
    @Singleton
    internal fun providesInAppReviewHelper(appPreferences: AppPreferences): InAppReviewHelper =
        InAppReviewHelperImpl(appPreferences)
}
