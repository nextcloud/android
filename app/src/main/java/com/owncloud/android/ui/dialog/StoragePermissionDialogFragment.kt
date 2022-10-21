/*
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * Copyright (C) 2022 Álvaro Brey Vilas
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.databinding.StoragePermissionDialogBinding
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Dialog that shows permission options in SDK >= 30
 *
 * Allows choosing "full access" (MANAGE_ALL_FILES) or "read-only media" (READ_EXTERNAL_STORAGE)
 */
@RequiresApi(Build.VERSION_CODES.R)
class StoragePermissionDialogFragment :
    DialogFragment(), Injectable {

    private lateinit var binding: StoragePermissionDialogBinding

    private var permissionRequired = false

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            permissionRequired = it.getBoolean(ARG_PERMISSION_REQUIRED, false)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            val alertDialog = it as AlertDialog
            viewThemeUtils.platform.colorTextButtons(alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Inflate the layout for the dialog
        val inflater = requireActivity().layoutInflater
        binding = StoragePermissionDialogBinding.inflate(inflater, null, false)

        val view: View = binding.root
        val explanationResource = when {
            permissionRequired -> R.string.file_management_permission_text
            else -> R.string.file_management_permission_optional_text
        }
        binding.storagePermissionExplanation.text = getString(explanationResource, getString(R.string.app_name))

        // Setup layout
        viewThemeUtils.material.colorMaterialButtonPrimaryFilled(binding.btnFullAccess)
        binding.btnFullAccess.setOnClickListener {
            setResult(Result.FULL_ACCESS)
            dismiss()
        }
        viewThemeUtils.platform.colorTextButtons(binding.btnReadOnly)
        binding.btnReadOnly.setOnClickListener {
            setResult(Result.MEDIA_READ_ONLY)
            dismiss()
        }

        // Build the dialog
        val titleResource = when {
            permissionRequired -> R.string.file_management_permission
            else -> R.string.file_management_permission_optional
        }

        val builder = MaterialAlertDialogBuilder(binding.btnReadOnly.context)
            .setTitle(titleResource)
            .setView(view)
            .setNegativeButton(R.string.common_cancel) { _, _ ->
                setResult(Result.CANCEL)
                dismiss()
            }

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.btnReadOnly.context, builder)

        return builder.create()
    }

    private fun setResult(result: Result) {
        parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf(RESULT_KEY to result))
    }

    @Parcelize
    enum class Result : Parcelable {
        CANCEL, FULL_ACCESS, MEDIA_READ_ONLY
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
