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

/**
 * Dialog that shows permission options in SDK >= 30
 *
 * Allows choosing "full access" (MANAGE_ALL_FILES) or "read-only media" (READ_EXTERNAL_STORAGE)
 */
@RequiresApi(Build.VERSION_CODES.R)
class StoragePermissionDialogFragment :
    DialogFragment(),
    Injectable {

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var preferences: AppPreferences

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
        val title = R.string.file_management_permission_optional
        val explanationResource = R.string.file_management_permission_optional_text
        val message = getString(explanationResource, getString(R.string.app_name))

        val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.storage_permission_full_access) { _, _ ->
                val intent = PermissionUtil.getManageAllFilesIntent(requireActivity())
                requireActivity().startActivityForResult(intent, REQUEST_CODE_MANAGE_ALL_FILES)
                dismiss()
            }
            .setNegativeButton(R.string.storage_permission_media_read_only) { _, _ ->
                PermissionUtil.requestStoragePermission(
                    activity = this.requireActivity(),
                    readOnly = true,
                    viewThemeUtils = viewThemeUtils
                )
                dismiss()
            }
            .setNeutralButton(R.string.storage_permission_dont_ask) { _, _ ->
                preferences.isStoragePermissionRequested = true
                dismiss()
            }

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(requireContext(), dialogBuilder)

        return dialogBuilder.create()
    }
}
