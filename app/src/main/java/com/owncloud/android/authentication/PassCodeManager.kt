/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.authentication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.view.View
import android.view.WindowManager
import androidx.annotation.VisibleForTesting
import com.nextcloud.client.core.Clock
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.MainApp
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.RequestCredentialsActivity
import com.owncloud.android.ui.activity.SettingsActivity
import com.owncloud.android.utils.DeviceCredentialUtils
import kotlin.math.abs

@Suppress("TooManyFunctions")
class PassCodeManager(private val preferences: AppPreferences, private val clock: Clock) {
    companion object {
        private val exemptOfPasscodeActivities = setOf(
            PassCodeActivity::class.java,
            RequestCredentialsActivity::class.java
        )
        const val PASSCODE_ACTIVITY = 9999

        /**
         * Keeping a "low" positive value is the easiest way to prevent
         * the pass code being requested on screen rotations.
         */
        private const val PASS_CODE_TIMEOUT = 5000

        private const val TAG = "PassCodeManager"
    }

    private var visibleActivitiesCounter = 0
    private var lastResumedActivity: Activity? = null

    private fun isExemptActivity(activity: Activity): Boolean {
        return exemptOfPasscodeActivities.contains(activity.javaClass)
    }

    fun onActivityResumed(activity: Activity): Boolean {
        var askedForPin = false
        val timestamp = preferences.lockTimestamp
        setSecureFlag(activity)

        if (!isExemptActivity(activity)) {
            val passcodeRequested = passCodeShouldBeRequested(timestamp)
            val credentialsRequested = deviceCredentialsShouldBeRequested(timestamp, activity)
            val shouldHideView = passcodeRequested || credentialsRequested
            toggleActivityVisibility(shouldHideView, activity)
            askedForPin = shouldHideView

            if (passcodeRequested) {
                requestPasscode(activity)
            } else if (credentialsRequested) {
                requestCredentials(activity)
            }
            if (askedForPin) {
                preferences.lockTimestamp = 0
            }
        }

        if (!askedForPin && preferences.lockTimestamp != 0L) {
            updateLockTimestamp()
        }

        if (!isExemptActivity(activity)) {
            addVisibleActivity(activity) // keep it AFTER passCodeShouldBeRequested was checked
        }

        return askedForPin
    }

    /**
     * Used to hide root view while transitioning to passcode activity
     */
    private fun toggleActivityVisibility(
        hide: Boolean,
        activity: Activity
    ) {
        if (hide) {
            getActivityRootView(activity)?.visibility = View.GONE
        } else {
            getActivityRootView(activity)?.visibility = View.VISIBLE
        }
    }

    private fun addVisibleActivity(activity: Activity) {
        // don't count the same activity twice
        if (lastResumedActivity != activity) {
            visibleActivitiesCounter++
            lastResumedActivity = activity
        }
    }

    private fun removeVisibleActivity() {
        visibleActivitiesCounter--
        lastResumedActivity = null
    }

    private fun setSecureFlag(activity: Activity) {
        val window = activity.window
        if (window != null) {
            if (isPassCodeEnabled() || deviceCredentialsAreEnabled(activity)) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    private fun requestPasscode(activity: Activity) {
        val i = Intent(MainApp.getAppContext(), PassCodeActivity::class.java)
        i.action = PassCodeActivity.ACTION_CHECK
        i.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        activity.startActivityForResult(i, PASSCODE_ACTIVITY)
    }

    private fun requestCredentials(activity: Activity) {
        val i = Intent(MainApp.getAppContext(), RequestCredentialsActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        activity.startActivityForResult(i, PASSCODE_ACTIVITY)
    }

    fun onActivityStopped(activity: Activity) {
        if (visibleActivitiesCounter > 0 && !isExemptActivity(activity)) {
            removeVisibleActivity()
        }
        val powerMgr = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        if ((isPassCodeEnabled() || deviceCredentialsAreEnabled(activity)) && !powerMgr.isScreenOn) {
            activity.moveTaskToBack(true)
        }
    }

    fun updateLockTimestamp() {
        preferences.lockTimestamp = clock.millisSinceBoot
    }

    /**
     * `true` if the time elapsed since last unlock is longer than [PASS_CODE_TIMEOUT] and no activities are visible
     */
    private fun shouldBeLocked(timestamp: Long) =
        abs(clock.millisSinceBoot - timestamp) > PASS_CODE_TIMEOUT &&
            visibleActivitiesCounter <= 0

    @VisibleForTesting
    fun passCodeShouldBeRequested(timestamp: Long): Boolean {
        return shouldBeLocked(timestamp) && isPassCodeEnabled()
    }

    private fun isPassCodeEnabled(): Boolean = SettingsActivity.LOCK_PASSCODE == preferences.lockPreference

    private fun deviceCredentialsShouldBeRequested(timestamp: Long, activity: Activity): Boolean {
        return shouldBeLocked(timestamp) && deviceCredentialsAreEnabled(activity)
    }

    private fun deviceCredentialsAreEnabled(activity: Activity): Boolean {
        return SettingsActivity.LOCK_DEVICE_CREDENTIALS == preferences.lockPreference ||
            (preferences.isFingerprintUnlockEnabled && DeviceCredentialUtils.areCredentialsAvailable(activity))
    }

    private fun getActivityRootView(activity: Activity): View? {
        return activity.window?.findViewById(android.R.id.content)
            ?: activity.window?.decorView?.findViewById(android.R.id.content)
    }
}
