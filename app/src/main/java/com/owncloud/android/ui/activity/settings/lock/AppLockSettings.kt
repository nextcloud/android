/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.lock

import android.app.Activity
import android.content.Intent
import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceCategory
import android.preference.PreferenceManager
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.SnackbarUtil
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.R
import com.owncloud.android.providers.documentstorage.DocumentsStorageProvider
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.RequestCredentialsActivity
import com.owncloud.android.ui.activity.SettingsActivity.Companion.LOCK_DEVICE_CREDENTIALS
import com.owncloud.android.ui.activity.SettingsActivity.Companion.LOCK_NONE
import com.owncloud.android.ui.activity.SettingsActivity.Companion.LOCK_PASSCODE
import com.owncloud.android.ui.activity.SettingsActivity.Companion.PREFERENCE_LOCK
import com.owncloud.android.ui.activity.settings.SettingsRequestCode
import com.owncloud.android.ui.activity.settings.util.bindOrRemove
import com.owncloud.android.ui.activity.settings.util.isCredentialCheckSuccessful
import com.owncloud.android.ui.activity.settings.util.startExtendedSettingsDialog
import com.owncloud.android.ui.model.ExtendedSettingsActivityDialog
import com.owncloud.android.utils.DeviceCredentialUtils

class AppLockSettings(private val activity: PreferenceActivity, private val preferences: AppPreferences) {

    private var lockPreference: Preference? = null
    private var pendingLock = LOCK_NONE

    fun setup(category: PreferenceCategory, isLockAvailable: Boolean) {
        lockPreference = category.bindOrRemove(activity.findPreference(PREFERENCE_LOCK), isLockAvailable) {
            activity.startExtendedSettingsDialog(ExtendedSettingsActivityDialog.AppPasscode)
        }

        updateLockSummary(preferences.lockPreference)
    }

    fun showPasscodeDialogIfEnforceAppProtection() {
        if (!MDMConfig.enforceProtection(activity)) return
        if (preferences.lockPreference != LOCK_NONE) return

        activity.startExtendedSettingsDialog(ExtendedSettingsActivityDialog.AppPasscode, dismissable = false)
    }

    fun onLockSelected(selectedLock: String) {
        val currentLock = preferences.lockPreference ?: LOCK_NONE
        if (currentLock == selectedLock) return

        if (currentLock == LOCK_NONE) {
            enableLock(selectedLock)
            return
        }

        pendingLock = selectedLock
        disableLock(currentLock)
    }

    fun onPasscodeRequestResult(resultCode: Int, data: Intent?) {
        when (resultCode) {
            Activity.RESULT_CANCELED -> showPasscodeDialogIfEnforceAppProtection()
            Activity.RESULT_OK -> data?.getStringExtra(PassCodeActivity.KEY_PASSCODE)?.let(::storePasscode)
        }
    }

    fun onPasscodeConfirmResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) return
        if (data?.getBooleanExtra(PassCodeActivity.KEY_CHECK_RESULT, false) != true) return

        onLockRemoved(R.string.pass_code_removed)
    }

    fun onDeviceCredentialsConfirmResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) return
        if (data?.isCredentialCheckSuccessful != true) return

        onLockRemoved(R.string.credentials_disabled)
    }

    private fun enableLock(lock: String) {
        pendingLock = LOCK_NONE

        when (lock) {
            LOCK_PASSCODE -> startPassCodeActivity(
                PassCodeActivity.ACTION_REQUEST_WITH_RESULT,
                SettingsRequestCode.REQUEST_PASSCODE
            )

            LOCK_DEVICE_CREDENTIALS -> enableDeviceCredentials()
        }
    }

    private fun enableDeviceCredentials() {
        if (!DeviceCredentialUtils.areCredentialsAvailable(activity.applicationContext)) {
            SnackbarUtil.show(activity, R.string.prefs_lock_device_credentials_not_setup)
            return
        }

        SnackbarUtil.show(activity, R.string.prefs_lock_device_credentials_enabled)
        changeLockSetting(LOCK_DEVICE_CREDENTIALS)
    }

    private fun disableLock(lock: String) {
        when (lock) {
            LOCK_PASSCODE -> startPassCodeActivity(
                PassCodeActivity.ACTION_CHECK_WITH_RESULT,
                SettingsRequestCode.CONFIRM_PASSCODE
            )

            LOCK_DEVICE_CREDENTIALS -> activity.startActivityForResult(
                Intent(activity.applicationContext, RequestCredentialsActivity::class.java),
                SettingsRequestCode.CONFIRM_DEVICE_CREDENTIALS
            )
        }
    }

    private fun startPassCodeActivity(action: String, requestCode: Int) {
        val intent = Intent(activity.applicationContext, PassCodeActivity::class.java).setAction(action)

        activity.startActivityForResult(intent, requestCode)
    }

    private fun storePasscode(passcode: String) {
        if (passcode.length != PASSCODE_LENGTH) return

        PreferenceManager.getDefaultSharedPreferences(activity.applicationContext).edit {
            passcode.forEachIndexed { index, digit ->
                putString("${PassCodeActivity.PREFERENCE_PASSCODE_D}${index + 1}", digit.toString())
            }
        }
        changeLockSetting(LOCK_PASSCODE)

        SnackbarUtil.show(activity, R.string.pass_code_stored)
    }

    private fun onLockRemoved(@StringRes messageRes: Int) {
        changeLockSetting(LOCK_NONE)
        SnackbarUtil.show(activity, messageRes)

        if (pendingLock != LOCK_NONE) {
            enableLock(pendingLock)
        }
    }

    private fun changeLockSetting(value: String) {
        preferences.lockPreference = value
        updateLockSummary(value)

        DocumentsStorageProvider.notifyRootsChanged(activity)
    }

    private fun updateLockSummary(lockValue: String?) {
        val summaryResId = when (lockValue) {
            LOCK_PASSCODE -> R.string.prefs_lock_using_passcode
            LOCK_DEVICE_CREDENTIALS -> R.string.prefs_lock_using_device_credentials
            else -> R.string.prefs_lock_none
        }

        lockPreference?.setSummary(summaryResId)
    }

    companion object {
        private const val PASSCODE_LENGTH = 4
    }
}
