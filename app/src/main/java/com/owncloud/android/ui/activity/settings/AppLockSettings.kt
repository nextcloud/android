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
import com.nextcloud.client.preferences.AppLock
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.SnackbarUtil
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.R
import com.owncloud.android.providers.documentstorage.DocumentsStorageProvider
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.RequestCredentialsActivity
import com.owncloud.android.ui.activity.settings.util.bindOrRemove
import com.owncloud.android.ui.activity.settings.util.isCredentialCheckSuccessful
import com.owncloud.android.ui.activity.settings.util.isOk
import com.owncloud.android.ui.activity.settings.util.registerForResult
import com.owncloud.android.ui.activity.settings.util.showIfAbsent
import com.owncloud.android.ui.dialog.AppPassCodeDialog
import com.owncloud.android.utils.DeviceCredentialUtils

class AppLockSettings(
    private val fragment: PreferenceFragmentCompat,
    private val preferences: AppPreferences,
    savedInstanceState: Bundle?
) {

    private val activity = fragment.requireActivity()
    private var lockPreference: Preference? = null
    private var pendingLock = AppLock.fromValue(savedInstanceState?.getString(KEY_PENDING_LOCK))

    private val passcodeRequestLauncher = fragment.registerForResult(::onPasscodeRequestResult)
    private val passcodeConfirmLauncher = fragment.registerForResult(::onPasscodeConfirmResult)
    private val deviceCredentialsConfirmLauncher = fragment.registerForResult(::onDeviceCredentialsConfirmResult)

    init {
        fragment.childFragmentManager.setFragmentResultListener(AppPassCodeDialog.REQUEST_KEY, fragment) { _, result ->
            result.getString(AppPassCodeDialog.RESULT_LOCK)?.let { onLockSelected(AppLock.fromValue(it)) }
        }
    }

    fun setup(category: PreferenceCategory, isLockAvailable: Boolean) {
        lockPreference = category.bindOrRemove(category.findPreference<Preference>(PREFERENCE_LOCK), isLockAvailable) {
            showLockSelectionDialog(dismissable = true)
        }

        updateLockSummary(preferences.lockPreference)
    }

    fun saveState(outState: Bundle) {
        outState.putString(KEY_PENDING_LOCK, pendingLock.value)
    }

    fun showPasscodeDialogIfEnforceAppProtection() {
        if (!MDMConfig.enforceProtection(activity)) return
        if (preferences.lockPreference != AppLock.NONE) return

        showLockSelectionDialog(dismissable = false)
    }

    private fun showLockSelectionDialog(dismissable: Boolean) {
        AppPassCodeDialog.instance(dismissable).showIfAbsent(fragment.childFragmentManager, AppPassCodeDialog.TAG)
    }

    private fun onLockSelected(selectedLock: AppLock) {
        val currentLock = preferences.lockPreference
        if (currentLock == selectedLock) return

        if (currentLock == AppLock.NONE) {
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

    private fun enableLock(lock: AppLock) {
        pendingLock = AppLock.NONE

        when (lock) {
            AppLock.PASSCODE -> passcodeRequestLauncher.launch(
                createPassCodeIntent(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)
            )

            AppLock.DEVICE_CREDENTIALS -> enableDeviceCredentials()

            AppLock.NONE -> Unit
        }
    }

    private fun enableDeviceCredentials() {
        if (!DeviceCredentialUtils.areCredentialsAvailable(activity.applicationContext)) {
            SnackbarUtil.show(activity, R.string.prefs_lock_device_credentials_not_setup)
            return
        }

        SnackbarUtil.show(activity, R.string.prefs_lock_device_credentials_enabled)
        changeLockSetting(AppLock.DEVICE_CREDENTIALS)
    }

    private fun disableLock(lock: AppLock) {
        when (lock) {
            AppLock.PASSCODE -> passcodeConfirmLauncher.launch(
                createPassCodeIntent(PassCodeActivity.ACTION_CHECK_WITH_RESULT)
            )

            AppLock.DEVICE_CREDENTIALS -> deviceCredentialsConfirmLauncher.launch(
                Intent(activity.applicationContext, RequestCredentialsActivity::class.java)
            )

            AppLock.NONE -> Unit
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
        changeLockSetting(AppLock.PASSCODE)

        SnackbarUtil.show(activity, R.string.pass_code_stored)
    }

    private fun onLockRemoved(@StringRes messageRes: Int) {
        changeLockSetting(AppLock.NONE)
        SnackbarUtil.show(activity, messageRes)

        if (pendingLock != AppLock.NONE) {
            enableLock(pendingLock)
        }
    }

    private fun changeLockSetting(lock: AppLock) {
        preferences.lockPreference = lock
        updateLockSummary(lock)

        DocumentsStorageProvider.notifyRootsChanged(activity)
    }

    private fun updateLockSummary(lock: AppLock) {
        lockPreference?.setSummary(lock.summaryResId)
    }

    companion object {
        const val PREFERENCE_LOCK = "lock"
        private const val PASSCODE_LENGTH = 4
        private const val KEY_PENDING_LOCK = "pending_lock"
    }
}
