/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.android.appReview

import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.nextcloud.appReview.AppReviewShownModel
import com.nextcloud.appReview.InAppReviewHelper
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.extensions.getFormattedStringDate
import com.nextcloud.utils.extensions.isCurrentYear
import com.owncloud.android.lib.common.utils.Log_OC

// Reference: https://developer.android.com/guide/playcore/in-app-review
/**
 * This class responsible to handle & manage in-app review related methods
 */
class InAppReviewHelperImpl(val appPreferences: AppPreferences) : InAppReviewHelper {

    override fun resetAndIncrementAppRestartCounter() {
        val appReviewShownModel = appPreferences.inAppReviewData
        val currentTimeMills = System.currentTimeMillis()

        if (appReviewShownModel != null) {
            if (currentTimeMills.isCurrentYear(appReviewShownModel.firstShowYear)) {
                appReviewShownModel.appRestartCount += 1
                appPreferences.setInAppReviewData(appReviewShownModel)
            } else {
                resetReviewShownModel()
            }
        } else {
            resetReviewShownModel()
        }
    }

    private fun resetReviewShownModel() {
        val appReviewShownModel = AppReviewShownModel(
            System.currentTimeMillis().getFormattedStringDate(YEAR_FORMAT),
            1,
            0,
            null
        )
        appPreferences.setInAppReviewData(appReviewShownModel)
    }

    override fun showInAppReview(activity: AppCompatActivity) {
        val appReviewShownModel = appPreferences.inAppReviewData
        val currentTimeMills = System.currentTimeMillis()

        appReviewShownModel?.let {
            if (it.appRestartCount >= MIN_APP_RESTARTS_REQ &&
                currentTimeMills.isCurrentYear(it.firstShowYear) &&
                it.reviewShownCount < MAX_DISPLAY_PER_YEAR
            ) {
                doAppReview(activity)
            } else {
                Log_OC.d(
                    TAG,
                    "Yearly limit has been reached or minimum app restarts are not completed: $appReviewShownModel"
                )
            }
        }
    }

    private fun doAppReview(activity: AppCompatActivity) {
        val manager = ReviewManagerFactory.create(activity)
        val request: Task<ReviewInfo> = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // We can get the ReviewInfo object
                val reviewInfo: ReviewInfo = task.result!!
                launchAppReviewFlow(manager, activity, reviewInfo)
            } else {
                // There was some problem, log or handle the error code.
                Log_OC.e(TAG, "Failed to get ReviewInfo: ${task.exception?.message}")
            }
        }
    }

    private fun launchAppReviewFlow(manager: ReviewManager, activity: AppCompatActivity, reviewInfo: ReviewInfo) {
        val flow = manager.launchReviewFlow(activity, reviewInfo)
        flow.addOnCompleteListener { _ ->
            // The flow has finished. The API does not indicate whether the user
            // reviewed or not, or even whether the review dialog was shown. Thus, no
            // matter the result, we continue our app flow.
            // Scenarios in which the flow won't shown:
            // 1. Showing dialog to frequently
            // 2. If quota is reached can be checked in official documentation
            // 3. Flow won't be shown if user has already reviewed the app. User has to delete the review from play store to show the review dialog again
            // Link for more info: https://stackoverflow.com/a/63342266
            Log_OC.d(TAG, "App Review flow is completed")
        }

        // on successful showing review dialog increment the count and capture the date
        val appReviewShownModel = appPreferences.inAppReviewData
        appReviewShownModel?.let {
            it.appRestartCount = 0
            it.reviewShownCount += 1
            it.lastReviewShownDate = System.currentTimeMillis().getFormattedStringDate(DATE_TIME_FORMAT)
            appPreferences.setInAppReviewData(it)
        }
    }

    companion object {
        private val TAG = InAppReviewHelperImpl::class.java.simpleName
        const val YEAR_FORMAT = "yyyy"
        const val DATE_TIME_FORMAT = "dd-MM-yyyy HH:mm:ss"
        const val MIN_APP_RESTARTS_REQ = 10 // minimum app restarts required to ask the review
        const val MAX_DISPLAY_PER_YEAR = 15 // maximum times to ask review in a year
    }
}
