/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import android.app.Activity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.owncloud.android.lib.common.utils.Log_OC

object GooglePlayUtils {
    private const val PLAY_SERVICES_RESOLUTION_REQUEST = 9000
    private const val TAG = "GooglePlayUtils"

    @JvmStatic
    fun checkPlayServices(activity: Activity): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(activity)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)?.show()
            } else {
                Log_OC.i(TAG, "This device is not supported.")
                activity.finish()
            }
            return false
        }
        return true
    }
}
