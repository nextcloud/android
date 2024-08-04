/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022-2023 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
    }

    var canAskPin = true
    private var askPinWhenDeviceLocked = false

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
            getActivityRootView(activity)?.visibility = if (shouldHideView) View.GONE else View.VISIBLE
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

        if (!askedForPin && preferences.lockTimestamp != 0L || askPinWhenDeviceLocked) {
            updateLockTimestamp()
            askPinWhenDeviceLocked = false
        }

        return askedForPin
    }

    private fun setSecureFlag(activity: Activity) {
        activity.window?.let { window ->
            if (isPassCodeEnabled() || deviceCredentialsAreEnabled(activity)) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    private fun requestPasscode(activity: Activity) {
        val i = Intent(MainApp.getAppContext(), PassCodeActivity::class.java).apply {
            action = PassCodeActivity.ACTION_CHECK
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        activity.startActivityForResult(i, PASSCODE_ACTIVITY)
    }

    private fun requestCredentials(activity: Activity) {
        val i = Intent(MainApp.getAppContext(), RequestCredentialsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        activity.startActivityForResult(i, PASSCODE_ACTIVITY)
    }

    fun onActivityStopped(activity: Activity) {
        val powerMgr = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        if ((isPassCodeEnabled() || deviceCredentialsAreEnabled(activity)) && !powerMgr.isInteractive) {
            askPinWhenDeviceLocked = true
        }
    }

    fun updateLockTimestamp() {
        preferences.lockTimestamp = clock.millisSinceBoot
        canAskPin = false
    }

    /**
     * `true` if the time elapsed since last unlock is longer than [PASS_CODE_TIMEOUT] and no activities are visible
     */
    private fun shouldBeLocked(timestamp: Long): Boolean {
        return (abs(clock.millisSinceBoot - timestamp) > PASS_CODE_TIMEOUT && canAskPin) || askPinWhenDeviceLocked
    }

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
