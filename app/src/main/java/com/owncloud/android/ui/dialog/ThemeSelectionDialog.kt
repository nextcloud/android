/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.nextcloud.client.preferences.DarkMode
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.nextcloud.client.di.Injectable
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class ThemeSelectionDialog : DialogFragment(), Injectable  {

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    override fun onStart() {
        super.onStart()
        colorButtons()
        colorRadioButtons()
    }

    private fun colorButtons() {
        val dialog = dialog

        if (dialog is AlertDialog) {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE) as? MaterialButton
            positiveButton?.let {
                viewThemeUtils.material.colorMaterialButtonPrimaryTonal(it)
            }
        }
    }

    private fun colorRadioButtons() {
        val dialog = dialog
        if (dialog is AlertDialog) {
            val listView = dialog.listView ?: return

            for (i in 0 until listView.childCount) {
                val row = listView.getChildAt(i)

                val radioButton = findRadioButtonInView(row)
                radioButton?.let {
                    viewThemeUtils.platform.themeRadioButton(it)
                }
            }
        }
    }

    private fun findRadioButtonInView(view: View): RadioButton? {
        if (view is RadioButton) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findRadioButtonInView(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        preferences = AppPreferencesImpl.fromContext(context)

        val themeEntries = arrayOf(
            getString(R.string.prefs_value_theme_light),
            getString(R.string.prefs_value_theme_dark),
            getString(R.string.prefs_value_theme_system)
        )

        val themeValues = arrayOf(
            DarkMode.LIGHT.name,
            DarkMode.DARK.name,
            DarkMode.SYSTEM.name
        )

        val currentTheme = preferences.getDarkThemeMode()?.name ?: DarkMode.SYSTEM.name
        val selectedIndex = themeValues.indexOf(currentTheme)

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.prefs_theme_title)
            .setSingleChoiceItems(themeEntries, selectedIndex) { dialog, which ->
                if (which >= 0 && which < themeValues.size) {
                    val selectedValue = themeValues[which]
                    val mode = DarkMode.valueOf(selectedValue)

                    preferences.setDarkThemeMode(mode)
                    MainApp.setAppTheme(mode)

                    setFragmentResult(
                        RESULT_KEY,
                        bundleOf(RESULT_KEY to selectedValue)
                    )

                    dialog.dismiss()
                }
            }
            .setPositiveButton(R.string.common_cancel) { _, _ ->
                dismiss()
            }

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(requireContext(), builder)

        return builder.create()
    }

    companion object {
        const val RESULT_KEY = "theme_selection_result"

        fun newInstance(): ThemeSelectionDialog {
            return ThemeSelectionDialog()
        }
    }
}
