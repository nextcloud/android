/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.ExternalStorageAuthDialogBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class ExternalStorageAuthDialogFragment :
    DialogFragment(),
    Injectable {

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private lateinit var binding: ExternalStorageAuthDialogBinding

    private var positiveButton: MaterialButton? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val folder = requireArguments().getParcelableArgument(ARG_FOLDER, OCFile::class.java)

        binding = ExternalStorageAuthDialogBinding.inflate(requireActivity().layoutInflater, null, false)
        viewThemeUtils.material.colorTextInputLayout(binding.usernameContainer)
        viewThemeUtils.material.colorTextInputLayout(binding.passwordContainer)
        binding.username.doAfterTextChanged { updatePositiveButtonState() }
        binding.password.doAfterTextChanged { updatePositiveButtonState() }

        val builder = MaterialAlertDialogBuilder(requireActivity())
            .setView(binding.root)
            .setTitle(R.string.external_storage_auth_dialog_title)
            .setMessage(getString(R.string.external_storage_auth_dialog_message, folder?.fileName))
            .setPositiveButton(R.string.login) { _, _ -> sendCredentials(folder) }
            .setNegativeButton(R.string.common_cancel, null)

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.root.context, builder)
        return builder.create()
    }

    override fun onStart() {
        super.onStart()
        themeButtons()
        updatePositiveButtonState()
    }

    private fun themeButtons() {
        val dialog = dialog as? AlertDialog ?: return

        positiveButton = (dialog.getButton(AlertDialog.BUTTON_POSITIVE) as? MaterialButton)?.also {
            viewThemeUtils.material.colorMaterialButtonPrimaryTonal(it)
        }
        (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) as? MaterialButton)?.let {
            viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(it)
        }
    }

    private fun updatePositiveButtonState() {
        positiveButton?.isEnabled = !binding.username.text.isNullOrEmpty() && !binding.password.text.isNullOrEmpty()
    }

    private fun sendCredentials(folder: OCFile?) {
        val result = bundleOf(
            RESULT_FOLDER to folder,
            RESULT_USERNAME to binding.username.text.toString(),
            RESULT_PASSWORD to binding.password.text.toString()
        )
        parentFragmentManager.setFragmentResult(REQUEST_KEY, result)
    }

    companion object {
        const val TAG = "ExternalStorageAuthDialogFragment"
        const val REQUEST_KEY = "EXTERNAL_STORAGE_AUTH_REQUEST"
        const val RESULT_FOLDER = "FOLDER"
        const val RESULT_USERNAME = "USERNAME"
        const val RESULT_PASSWORD = "PASSWORD"

        private const val ARG_FOLDER = "FOLDER"

        @JvmStatic
        fun newInstance(folder: OCFile): ExternalStorageAuthDialogFragment = ExternalStorageAuthDialogFragment().apply {
            arguments = bundleOf(ARG_FOLDER to folder)
        }
    }
}
