/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.PermissionUtil.REQUEST_CODE_MANAGE_ALL_FILES
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.R)
class StoragePermissionDialogFragment : DialogFragment(), Injectable {

    private var showStrictText = false

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            showStrictText = it.getBoolean(ARG_SHOW_STRICT_TEXT, false)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            val alertDialog = it as AlertDialog

            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE) as MaterialButton
            viewThemeUtils.material.colorMaterialButtonPrimaryTonal(positiveButton)

            val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE) as MaterialButton
            viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(negativeButton)

            val neutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL) as MaterialButton
            viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(neutralButton)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = when {
            showStrictText -> R.string.file_management_permission
            else -> R.string.file_management_permission_optional
        }
        val explanationResource = when {
            showStrictText -> R.string.file_management_permission_text
            else -> R.string.file_management_permission_optional_text
        }
        val message = getString(explanationResource, getString(R.string.app_name))

        val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.storage_permission_full_access) { _, _ ->
                requestManageAllFiles()
                dismiss()
            }
            .setNegativeButton(R.string.storage_permission_media_read_only) { _, _ ->
                requestRequiredStoragePermissions()
                dismiss()
            }
            .setNeutralButton(R.string.storage_permission_dont_ask_again) { _, _ ->
                appPreferences.dontAskStoragePermissionAgain = true
                dismiss()
            }

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(requireContext(), dialogBuilder)

        return dialogBuilder.create()
    }

    @Suppress("DEPRECATION")
    private fun requestManageAllFiles() {
        activity?.let {
            val intent = PermissionUtil.getManageAllFilesIntent(it)
            it.startActivityForResult(intent, REQUEST_CODE_MANAGE_ALL_FILES)
        }
    }

    private fun requestRequiredStoragePermissions() {
        if (appPreferences.dontAskStoragePermissionAgain) {
            return
        }

        activity?.let {
            PermissionUtil.requestRequiredStoragePermissions(it)
        }
    }

    companion object {
        private const val ARG_SHOW_STRICT_TEXT = "ARG_SHOW_STRICT_TEXT"

        /**
         * @param showStrictText Whether the permission is absolutely required by the calling component.
         * This changes the texts to a more strict version.
         */
        fun newInstance(showStrictText: Boolean): StoragePermissionDialogFragment {
            return StoragePermissionDialogFragment().apply {
                arguments = bundleOf(ARG_SHOW_STRICT_TEXT to showStrictText)
            }
        }
    }
}
