/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.categories

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.operations.e2e.E2ECertificateRenewalResult
import com.owncloud.android.operations.e2e.E2ECertificateRenewalService
import com.owncloud.android.operations.e2e.E2EDeletionService
import com.owncloud.android.ui.activity.RequestCredentialsActivity
import com.owncloud.android.ui.activity.SetupEncryptionActivity
import com.owncloud.android.ui.activity.settings.util.bindOrRemove
import com.owncloud.android.ui.activity.settings.util.isCredentialCheckSuccessful
import com.owncloud.android.ui.activity.settings.util.isOk
import com.owncloud.android.ui.activity.settings.util.registerForResult
import com.owncloud.android.ui.activity.settings.util.setOnClickAction
import com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.utils.ClipboardUtil
import com.owncloud.android.utils.DeviceCredentialUtils
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.theme.CapabilityUtils
import java.text.DateFormat

@Suppress("TooManyFunctions")
class E2ESettingsCategory(
    private val fragment: PreferenceFragmentCompat,
    private val user: User,
    private val arbitraryDataProvider: ArbitraryDataProvider,
    private val connectivityService: ConnectivityService,
    clientFactory: ClientFactory
) : SettingsCategory {

    private val activity = fragment.requireActivity()
    private val deletionService = E2EDeletionService(clientFactory)
    private val certificateRenewalService = E2ECertificateRenewalService(clientFactory, arbitraryDataProvider)

    private val encryptionSetupLauncher = fragment.registerForResult(::onEncryptionSetupResult)
    private val mnemonicLauncher = fragment.registerForResult { if (it.isOk) showMnemonic(it.data) }

    override fun setup() {
        val category = fragment.findPreference<PreferenceCategory>(MORE_CATEGORY_KEY) ?: return
        val isSetUp = FileOperationsHelper.isEndToEndEncryptionSetup(activity, user)
        val keysExist = CapabilityUtils.getCapability(activity).endToEndEncryptionKeysExist
        val isDebugActionAvailable = BuildConfig.DEBUG && isSetUp

        category.bindOrRemove(
            fragment.findPreference<Preference>(SETUP_E2E_KEY),
            !isSetUp && !keysExist.isTrue && !keysExist.isUnknown
        ) { runIfConnected(::startEncryptionSetup) }

        category.bindOrRemove(
            fragment.findPreference<Preference>(SETUP_E2E_KEYS_EXIST_KEY),
            keysExist.isTrue && !isSetUp
        ) { startEncryptionSetup() }

        setupMnemonicPreference(category)

        category.bindOrRemove(fragment.findPreference<Preference>(REMOVE_E2E_KEY), isSetUp) {
            showRemoveE2EDialog(category)
        }

        category.bindOrRemove(
            fragment.findPreference<Preference>(REMOVE_E2E_FILES_AND_KEYS_KEY),
            isDebugActionAvailable
        ) { showRemoveE2EKeysAndFilesDialog(category) }

        category.bindOrRemove(
            fragment.findPreference<Preference>(RENEW_CERTIFICATE_KEY),
            isDebugActionAvailable,
            ::renewCertificate
        )?.let(::updateCertificateSummary)
    }

    fun showMnemonic(data: Intent?) {
        if (data == null) {
            SnackbarUtil.show(activity, R.string.error_retrieving_mnemonic)
            return
        }

        if (!data.isCredentialCheckSuccessful) return

        showMnemonicDialog(readMnemonic())
    }

    private fun setupMnemonicPreference(category: PreferenceCategory) {
        val mnemonicPreference = fragment.findPreference<Preference>(MNEMONIC_KEY) ?: return

        if (readMnemonic().isEmpty()) {
            category.removePreference(mnemonicPreference)
            return
        }

        if (!DeviceCredentialUtils.areCredentialsAvailable(activity)) {
            mnemonicPreference.isEnabled = false
            mnemonicPreference.setSummary(R.string.prefs_e2e_no_device_credentials)
            return
        }

        mnemonicPreference.setOnClickAction {
            val intent = Intent(activity.applicationContext, RequestCredentialsActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

            mnemonicLauncher.launch(intent)
        }
    }

    private fun startEncryptionSetup() {
        val intent = Intent(activity.applicationContext, SetupEncryptionActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            .putExtra(EXTRA_USER, user)

        encryptionSetupLauncher.launch(intent)
    }

    private fun onEncryptionSetupResult(result: ActivityResult) {
        if (result.data?.getBooleanExtra(SetupEncryptionDialogFragment.SUCCESS, false) != true) return

        activity.recreate()
    }

    private fun runIfConnected(action: () -> Unit) {
        if (!connectivityService.connectivity.isConnected) {
            SnackbarUtil.show(activity, R.string.e2e_offline)
            return
        }

        action()
    }

    private fun showRemoveE2EDialog(category: PreferenceCategory) {
        MaterialAlertDialogBuilder(activity, R.style.FallbackTheming_Dialog)
            .setTitle(R.string.prefs_e2e_mnemonic)
            .setMessage(R.string.remove_e2e_message)
            .setCancelable(true)
            .setNegativeButton(R.string.common_cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.confirm_removal) { dialog, _ ->
                removeE2E(category, REMOVE_E2E_KEY, MNEMONIC_KEY)
                dialog.dismiss()
            }
            .show()
    }

    private fun showRemoveE2EKeysAndFilesDialog(category: PreferenceCategory) {
        deletionService.showRemoveE2EKeysAndFilesAlertDialog(activity, user) { success ->
            if (success) {
                removeE2E(category, REMOVE_E2E_FILES_AND_KEYS_KEY, MNEMONIC_KEY, REMOVE_E2E_KEY)
            }
        }
    }

    private fun removeE2E(category: PreferenceCategory, vararg preferenceKeys: String) {
        EncryptionUtils.removeE2E(arbitraryDataProvider, user)

        preferenceKeys.forEach(category::removePreferenceRecursively)
    }

    private fun renewCertificate(preference: Preference) = runIfConnected {
        certificateRenewalService.showRenewCertificateDialog(activity, user) { result ->
            onCertificateRenewalResult(preference, result)
        }
    }

    private fun onCertificateRenewalResult(preference: Preference, result: E2ECertificateRenewalResult) {
        when (result) {
            E2ECertificateRenewalResult.Success -> {
                updateCertificateSummary(preference)
                SnackbarUtil.show(activity, R.string.renew_e2e_certificate_success)
            }

            is E2ECertificateRenewalResult.Failure -> SnackbarUtil.show(activity, result.messageId)
        }
    }

    private fun updateCertificateSummary(preference: Preference) {
        val validity = certificateRenewalService.getCertificateValidity(user)
        if (validity == null) {
            preference.setSummary(R.string.renew_e2e_certificate_summary)
            return
        }

        val validUntil = DateFormat.getDateInstance().format(validity.notAfter)
        preference.summary = activity.getString(R.string.renew_e2e_certificate_valid_until, validUntil)
    }

    private fun showMnemonicDialog(mnemonic: String) {
        MaterialAlertDialogBuilder(activity, R.style.FallbackTheming_Dialog)
            .setTitle(R.string.prefs_e2e_mnemonic)
            .setMessage(mnemonic)
            .setPositiveButton(R.string.common_ok) { dialog, _ -> dialog.dismiss() }
            .setNegativeButton(R.string.common_cancel) { dialog, _ -> dialog.dismiss() }
            .setNeutralButton(R.string.common_copy) { _, _ ->
                ClipboardUtil.copyToClipboard(activity, mnemonic, false)
            }
            .show()
    }

    private fun readMnemonic(): String =
        arbitraryDataProvider.getValue(user.accountName, EncryptionUtils.MNEMONIC).trim { it <= ' ' }

    companion object {
        private const val MORE_CATEGORY_KEY = "more"
        private const val SETUP_E2E_KEY = "setup_e2e"
        private const val SETUP_E2E_KEYS_EXIST_KEY = "setup_e2e_keys_exist"
        private const val MNEMONIC_KEY = "mnemonic"
        private const val REMOVE_E2E_KEY = "remove_e2e"
        private const val REMOVE_E2E_FILES_AND_KEYS_KEY = "remove_e2e_files_and_keys"
        private const val RENEW_CERTIFICATE_KEY = "renew_e2e_certificate"
        private const val EXTRA_USER = "EXTRA_USER"
    }
}
