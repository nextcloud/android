/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */

package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.databinding.LoadingDialogBinding
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class IndeterminateProgressDialog :
    DialogFragment(),
    Injectable {

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val cancelable = requireArguments().getBoolean(ARG_CANCELABLE, false)
        isCancelable = cancelable

        val messageId = requireArguments().getInt(ARG_MESSAGE_ID, R.string.placeholder_sentence)

        val binding = LoadingDialogBinding.inflate(layoutInflater).apply {
            loadingText.setText(messageId)
            loadingBar.indeterminateDrawable?.let { drawable ->
                viewThemeUtils.platform.tintDrawable(requireContext(), drawable)
            }
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setCancelable(cancelable)

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(requireContext(), builder)

        return builder.create()
    }

    companion object {
        private const val ARG_MESSAGE_ID = "message_id"
        private const val ARG_CANCELABLE = "cancelable"

        @JvmStatic
        fun newInstance(messageId: Int, cancelable: Boolean): IndeterminateProgressDialog =
            IndeterminateProgressDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_MESSAGE_ID, messageId)
                    putBoolean(ARG_CANCELABLE, cancelable)
                }
            }
    }
}
