/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.dialog.account

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.headers.AccountCustomHeaders
import com.nextcloud.utils.headers.CustomHeadersParseResult
import com.nextcloud.utils.headers.CustomHeadersParser
import com.owncloud.android.R
import com.owncloud.android.databinding.CustomHeadersDialogBinding
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

/**
 * Edits the custom headers of an existing account, for example after a token rotation or when the server was moved
 * behind a pre-authentication proxy.
 */
class CustomHeadersDialogFragment :
    DialogFragment(),
    Injectable {

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private lateinit var binding: CustomHeadersDialogBinding
    private lateinit var user: User

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        user = requireNotNull(arguments.getParcelableArgument(ARG_USER, User::class.java))
        binding = CustomHeadersDialogBinding.inflate(requireActivity().layoutInflater, null, false)
        viewThemeUtils.material.colorTextInputLayout(binding.customHeadersContainer)

        if (savedInstanceState == null) {
            val stored = AccountCustomHeaders.read(requireContext(), user.toPlatformAccount())
            binding.customHeadersInput.setText(CustomHeadersParser.format(stored))
        }
        binding.customHeadersInput.doAfterTextChanged { validate() }

        val builder = MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.custom_headers_edit)
            .setMessage(R.string.custom_headers_edit_message)
            .setView(binding.root)
            .setPositiveButton(R.string.common_save) { _, _ -> save() }
            .setNegativeButton(R.string.common_cancel, null)
        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.customHeadersContainer.context, builder)
        return builder.create()
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? AlertDialog ?: return
        (dialog.getButton(AlertDialog.BUTTON_POSITIVE) as? MaterialButton)?.let {
            viewThemeUtils.material.colorMaterialButtonPrimaryTonal(it)
        }
        (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) as? MaterialButton)?.let {
            viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(it)
        }
        validate()
    }

    private fun validate() {
        val error = when (val parsed = parse()) {
            is CustomHeadersParseResult.Invalid -> getString(R.string.custom_headers_invalid, parsed.lineNumber)

            is CustomHeadersParseResult.Valid -> getString(R.string.custom_headers_https_only)
                .takeIf { parsed.headers.isNotEmpty() && !isHttps() }
        }
        binding.customHeadersContainer.error = error
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = error == null
    }

    private fun save() {
        val parsed = parse() as? CustomHeadersParseResult.Valid ?: return
        if (parsed.headers.isNotEmpty() && !isHttps()) {
            return
        }
        AccountCustomHeaders.replace(requireContext(), user.toPlatformAccount(), parsed.headers)
    }

    private fun parse(): CustomHeadersParseResult =
        CustomHeadersParser.parse(binding.customHeadersInput.text?.toString().orEmpty())

    private fun isHttps(): Boolean = user.server.uri.scheme.equals(HTTPS, ignoreCase = true)

    companion object {
        const val TAG = "CustomHeadersDialogFragment"
        private const val ARG_USER = "user"
        private const val HTTPS = "https"

        @JvmStatic
        fun newInstance(user: User): CustomHeadersDialogFragment =
            CustomHeadersDialogFragment().apply { arguments = bundleOf(ARG_USER to user) }
    }
}
