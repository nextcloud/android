/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.extensions.setVisibleIf
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.R
import com.owncloud.android.databinding.DialogAppPasscodeBinding
import com.owncloud.android.ui.activity.SettingsActivity
import com.owncloud.android.ui.model.ExtendedSettingsActivityDialog
import com.owncloud.android.utils.DeviceCredentialUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class AppPassCodeDialog :
    DialogFragment(),
    Injectable {

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private lateinit var binding: DialogAppPasscodeBinding

    private var currentSelection = SettingsActivity.LOCK_NONE

    override fun onStart() {
        super.onStart()
        val alertDialog = dialog as AlertDialog

        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE) as? MaterialButton
        positiveButton?.let {
            viewThemeUtils.material.colorMaterialButtonPrimaryTonal(it)
        }
        checkPositiveButtonActiveness()

        val dismissable = arguments?.getBoolean(ARG_DISMISSABLE, true) ?: true
        isCancelable = dismissable
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogAppPasscodeBinding.inflate(layoutInflater)

        currentSelection = preferences.lockPreference ?: SettingsActivity.LOCK_NONE

        val passCodeEnabled = resources.getBoolean(R.bool.passcode_enabled)
        val deviceCredentialsEnabled = resources.getBoolean(R.bool.device_credentials_enabled)
        val enforceProtection = MDMConfig.enforceProtection(requireContext())
        val deviceCredentialsAvailable = DeviceCredentialUtils.areCredentialsAvailable(requireContext())
        val dismissable = arguments?.getBoolean(ARG_DISMISSABLE, true) ?: true

        binding.lockPasscode.setVisibleIf(passCodeEnabled)
        binding.lockDeviceCredentials.setVisibleIf(deviceCredentialsEnabled && deviceCredentialsAvailable)
        binding.lockNone.setVisibleIf(!enforceProtection)

        setupTheme()
        setCurrentSelection()
        setupListener()

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setPositiveButton(R.string.common_ok) { _, _ ->
                applySelection()
                dismiss()
            }

        if (!enforceProtection && dismissable) {
            builder.setNegativeButton(R.string.common_cancel) { _, _ ->
                dismiss()
            }
        }

        builder.setCancelable(dismissable)

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(requireContext(), builder)

        return builder.create()
    }

    private fun setupTheme() {
        viewThemeUtils.platform.apply {
            colorTextView(binding.dialogTitle)
            themeRadioButton(binding.lockPasscode)
            themeRadioButton(binding.lockDeviceCredentials)
            themeRadioButton(binding.lockNone)
        }
    }

    private fun setCurrentSelection() {
        val radioGroup = binding.lockRadioGroup

        when (currentSelection) {
            SettingsActivity.LOCK_PASSCODE -> radioGroup.check(R.id.lock_passcode)
            SettingsActivity.LOCK_DEVICE_CREDENTIALS -> radioGroup.check(R.id.lock_device_credentials)
            SettingsActivity.LOCK_NONE -> radioGroup.check(R.id.lock_none)
        }
    }

    private fun setupListener() {
        binding.lockRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedLock = when (checkedId) {
                R.id.lock_passcode -> SettingsActivity.LOCK_PASSCODE
                R.id.lock_device_credentials -> SettingsActivity.LOCK_DEVICE_CREDENTIALS
                R.id.lock_none -> SettingsActivity.LOCK_NONE
                else -> SettingsActivity.LOCK_NONE
            }

            currentSelection = selectedLock
            checkPositiveButtonActiveness()
        }
    }

    private fun checkPositiveButtonActiveness() {
        val positiveButton = (dialog as? AlertDialog)
            ?.getButton(AlertDialog.BUTTON_POSITIVE) as? MaterialButton
        val enforceProtection = MDMConfig.enforceProtection(requireContext())
        if (enforceProtection) {
            positiveButton?.isEnabled = (binding.lockPasscode.isChecked || binding.lockDeviceCredentials.isChecked)
        }
    }

    private fun applySelection() {
        val selectedLock = currentSelection

        setFragmentResult(
            ExtendedSettingsActivityDialog.AppPasscode.key,
            bundleOf(ExtendedSettingsActivityDialog.AppPasscode.key to selectedLock)
        )
    }

    companion object {
        private const val ARG_DISMISSABLE = "dismissable"

        fun instance(dismissable: Boolean): AppPassCodeDialog = AppPassCodeDialog().apply {
            arguments = bundleOf(ARG_DISMISSABLE to dismissable)
        }
    }
}
