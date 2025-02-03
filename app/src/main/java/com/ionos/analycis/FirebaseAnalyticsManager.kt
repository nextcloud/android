/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.analycis

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject

class FirebaseAnalyticsManager @Inject constructor(
    private val context: Context,
) : AnalyticsManager {

    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(context) }

    override fun setEnabled(enabled: Boolean) {
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
    }
}
