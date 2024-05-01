/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.onboarding

import android.app.Activity
import android.content.Context
import com.owncloud.android.features.FeatureItem

interface OnboardingService {
    val whatsNew: Array<FeatureItem>
    val isFirstRun: Boolean
    fun launchActivityIfNeeded(activity: Activity)
    fun launchFirstRunIfNeeded(activity: Activity): Boolean
    fun shouldShowWhatsNew(callingContext: Context): Boolean
}
