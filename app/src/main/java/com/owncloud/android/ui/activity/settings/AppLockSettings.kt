/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
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
import com.owncloud.android.ui.activity.settings.util.bindOrRemove
import com.owncloud.android.ui.activity.settings.util.isCredentialCheckSuccessful
import com.owncloud.android.ui.activity.settings.util.isOk
import com.owncloud.android.ui.activity.settings.util.launchExtendedSettingsDialog
import com.owncloud.android.ui.activity.settings.util.registerForResult
import com.owncloud.android.ui.model.ExtendedSettingsActivityDialog
import com.owncloud.android.utils.DeviceCredentialUtils

class AppLockSettings(
    fragment: PreferenceFragmentCompat,
    private val preferences: AppPreferences,
    savedInstanceState: Bundle?
) {

    private val activity = fragment.requireActivity()
    private var lockPreference: Preference? = null
    private var pendingLock = savedInstanceState?.getString(KEY_PENDING_LOCK) ?: LOCK_NONE

    private val lockSelectionLauncher = fragment.registerForResult(::onLockSelectionResult)
    private val passcodeRequestLauncher = fragment.registerForResult(::onPasscodeRequestResult)
    private val passcodeConfirmLauncher = fragment.registerForResult(::onPasscodeConfirmResult)
    private val deviceCredentialsConfirmLauncher = fragment.registerForResult(::onDeviceCredentialsConfirmResult)

    fun setup(category: PreferenceCategory, isLockAvailable: Boolean) {
        lockPreference = category.bindOrRemove(category.findPreference<Preference>(PREFERENCE_LOCK), isLockAvailable) {
            lockSelectionLauncher.launchExtendedSettingsDialog(activity, ExtendedSettingsActivityDialog.AppPasscode)
        }

        updateLockSummary(preferences.lockPreference)
    }

    fun saveState(outState: Bundle) {
        outState.putString(KEY_PENDING_LOCK, pendingLock)
    }

    fun showPasscodeDialogIfEnforceAppProtection() {
        if (!MDMConfig.enforceProtection(activity)) return
        if (preferences.lockPreference != LOCK_NONE) return

        lockSelectionLauncher.launchExtendedSettingsDialog(
            activity,
            ExtendedSettingsActivityDialog.AppPasscode,
            dismissable = false
        )
    }

    private fun onLockSelectionResult(result: ActivityResult) {
        val selectedLock = result.data?.getStringExtra(ExtendedSettingsActivityDialog.AppPasscode.key) ?: return
        val currentLock = preferences.lockPreference ?: LOCK_NONE
        if (currentLock == selectedLock) return

        if (currentLock == LOCK_NONE) {
            enableLock(selectedLock)
            return
        }

        pendingLock = selectedLock
        disableLock(currentLock)
    }

    private fun onPasscodeRequestResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_CANCELED -> showPasscodeDialogIfEnforceAppProtection()
            Activity.RESULT_OK -> result.data?.getStringExtra(PassCodeActivity.KEY_PASSCODE)?.let(::storePasscode)
        }
    }

    private fun onPasscodeConfirmResult(result: ActivityResult) {
        if (!result.isOk) return
        if (result.data?.getBooleanExtra(PassCodeActivity.KEY_CHECK_RESULT, false) != true) return

        onLockRemoved(R.string.pass_code_removed)
    }

    private fun onDeviceCredentialsConfirmResult(result: ActivityResult) {
        if (!result.isOk) return
        if (result.data?.isCredentialCheckSuccessful != true) return

        onLockRemoved(R.string.credentials_disabled)
    }

    private fun enableLock(lock: String) {
        pendingLock = LOCK_NONE

        when (lock) {
            LOCK_PASSCODE -> passcodeRequestLauncher.launch(
                createPassCodeIntent(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)
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
            LOCK_PASSCODE -> passcodeConfirmLauncher.launch(
                createPassCodeIntent(PassCodeActivity.ACTION_CHECK_WITH_RESULT)
            )

            LOCK_DEVICE_CREDENTIALS -> deviceCredentialsConfirmLauncher.launch(
                Intent(activity.applicationContext, RequestCredentialsActivity::class.java)
            )
        }
    }

    private fun createPassCodeIntent(action: String): Intent =
        Intent(activity.applicationContext, PassCodeActivity::class.java).setAction(action)

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
        private const val KEY_PENDING_LOCK = "pending_lock"
    }
}
