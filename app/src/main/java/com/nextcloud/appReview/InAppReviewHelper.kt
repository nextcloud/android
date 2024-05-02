/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.appReview

import androidx.appcompat.app.AppCompatActivity

interface InAppReviewHelper {

    /**
     * method to be called from Application onCreate() method to work properly
     * since we have to capture the app restarts Application is the best place to do that
     * this method will do the following:
     * 1. Reset the @see AppReviewModel with the current year (yyyy),
     * if the app is launched first time or if the year has changed.
     * 2. If the year is same then it will only increment the appRestartCount
     */
    fun resetAndIncrementAppRestartCounter()

    /**
     * method to be called from Activity onResume() method
     * this method will check the following conditions:
     * 1. if the minimum app restarts happened
     * 2. if the year is current
     * 3. if maximum review dialog is shown or not
     * once all the conditions satisfies it will trigger In-App Review manager to show the flow
     */
    fun showInAppReview(activity: AppCompatActivity)
}
