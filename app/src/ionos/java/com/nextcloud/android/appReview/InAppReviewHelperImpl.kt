/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.android.appReview

import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.appReview.InAppReviewHelper
import com.nextcloud.client.preferences.AppPreferences

class InAppReviewHelperImpl(appPreferences: AppPreferences) :
    InAppReviewHelper {
    override fun resetAndIncrementAppRestartCounter() {
    }

    override fun showInAppReview(activity: AppCompatActivity) {
    }
}
