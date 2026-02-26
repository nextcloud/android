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
import com.nextcloud.client.preferences.DarkMode
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.DialogThemeSelectionBinding
import com.owncloud.android.ui.model.ExtendedSettingsActivityDialog
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class ThemeSelectionDialog :
    DialogFragment(),
    Injectable {

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private lateinit var binding: DialogThemeSelectionBinding

    private var selectedMode: DarkMode = DarkMode.SYSTEM

    override fun onStart() {
        super.onStart()
        val alertDialog = dialog as AlertDialog

        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE) as? MaterialButton
        positiveButton?.let {
            viewThemeUtils.material.colorMaterialButtonPrimaryTonal(positiveButton)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogThemeSelectionBinding.inflate(layoutInflater)

        selectedMode = preferences.getDarkThemeMode() ?: DarkMode.SYSTEM
        val radioGroup = binding.themeRadioGroup

        viewThemeUtils.platform.run {
            colorTextView(binding.dialogTitle)
            themeRadioButton(binding.themeDark)
            themeRadioButton(binding.themeLight)
            themeRadioButton(binding.themeSystem)
        }

        when (selectedMode) {
            DarkMode.LIGHT -> radioGroup.check(R.id.theme_light)
            DarkMode.DARK -> radioGroup.check(R.id.theme_dark)
            DarkMode.SYSTEM -> radioGroup.check(R.id.theme_system)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedMode = when (checkedId) {
                R.id.theme_light -> DarkMode.LIGHT
                R.id.theme_dark -> DarkMode.DARK
                R.id.theme_system -> DarkMode.SYSTEM
                else -> DarkMode.SYSTEM
            }
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setPositiveButton(R.string.common_ok) { _, _ ->
                applyTheme(selectedMode)
                dismiss()
            }

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(requireContext(), builder)

        return builder.create()
    }

    private fun applyTheme(mode: DarkMode) {
        preferences.setDarkThemeMode(mode)
        MainApp.setAppTheme(mode)

        setFragmentResult(
            ExtendedSettingsActivityDialog.ThemeSelection.key,
            bundleOf(ExtendedSettingsActivityDialog.ThemeSelection.key to mode.name)
        )
    }
}
