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
import android.os.Parcelable
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ionos.annotation.IonosCustomization
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Dialog that shows permission options in SDK >= 30
 *
 * Allows choosing "full access" (MANAGE_ALL_FILES) or "read-only media" (READ_EXTERNAL_STORAGE)
 */
@RequiresApi(Build.VERSION_CODES.R)
class StoragePermissionDialogFragment : DialogFragment(), Injectable {

    private var permissionRequired = false

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            permissionRequired = it.getBoolean(ARG_PERMISSION_REQUIRED, false)
        }
    }

    @IonosCustomization()
    override fun onStart() {
        super.onStart()
        dialog?.let {
            val alertDialog = it as AlertDialog

            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE) as MaterialButton
            viewThemeUtils.ionos.material.colorMaterialButtonPrimaryTonal(positiveButton)

            val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE) as MaterialButton
            viewThemeUtils.ionos.material.colorMaterialButtonPrimaryBorderless(negativeButton)

            val neutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL) as MaterialButton
            viewThemeUtils.ionos.material.colorMaterialButtonPrimaryBorderless(neutralButton)
        }
    }

    @IonosCustomization
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = when {
            permissionRequired -> R.string.ionos_file_management_permission
            else -> R.string.ionos_file_management_permission_optional
        }
        val explanationResource = when {
            permissionRequired -> R.string.ionos_file_management_permission_text
            else -> R.string.ionos_file_management_permission_optional_text
        }
        val message = getString(explanationResource, getString(R.string.app_name))

        val dialogBuilder = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_ownCloud_Dialog)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.permission_allow) { _, _ ->
                setResult(Result.FULL_ACCESS)
                dismiss()
            }
            .setNegativeButton(R.string.permission_deny) { _, _ ->
                setResult(Result.CANCEL)
                dismiss()
            }

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(requireContext(), dialogBuilder)

        return dialogBuilder.create()
    }

    private fun setResult(result: Result) {
        parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf(RESULT_KEY to result))
    }

    @Parcelize
    enum class Result : Parcelable {
        CANCEL,
        FULL_ACCESS,
        MEDIA_READ_ONLY
    }

    companion object {
        private const val ARG_PERMISSION_REQUIRED = "ARG_PERMISSION_REQUIRED"
        const val REQUEST_KEY = "REQUEST_KEY_STORAGE_PERMISSION"
        const val RESULT_KEY = "RESULT"

        /**
         * @param permissionRequired Whether the permission is absolutely required by the calling component.
         * This changes the texts to a more strict version.
         */
        fun newInstance(permissionRequired: Boolean): StoragePermissionDialogFragment {
            return StoragePermissionDialogFragment().apply {
                arguments = bundleOf(ARG_PERMISSION_REQUIRED to permissionRequired)
            }
        }
    }
}
