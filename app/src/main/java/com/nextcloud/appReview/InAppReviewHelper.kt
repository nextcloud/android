/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2023 Tobias Kaminsky
 * Copyright (C) 2023 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
