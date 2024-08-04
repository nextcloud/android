/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
@file:Suppress("DEPRECATION")

package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class IndeterminateProgressDialog : DialogFragment(), Injectable {

    @JvmField
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null

    /**
     * {@inheritDoc}
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // / create indeterminate progress dialog
        val progressDialog = ProgressDialog(requireActivity(), R.style.ProgressDialogTheme)
        progressDialog.isIndeterminate = true
        progressDialog.setOnShowListener {
            val v = progressDialog.findViewById<ProgressBar>(android.R.id.progress)
            viewThemeUtils?.platform?.tintDrawable(requireContext(), v.indeterminateDrawable)
        }

        // / set message
        val messageId = requireArguments().getInt(ARG_MESSAGE_ID, R.string.placeholder_sentence)
        progressDialog.setMessage(getString(messageId))

        // / set cancellation behavior
        val cancelable = requireArguments().getBoolean(ARG_CANCELABLE, false)
        if (!cancelable) {
            progressDialog.setCancelable(false)
            // disable the back button
            val keyListener =
                DialogInterface.OnKeyListener { _, keyCode, _ -> keyCode == KeyEvent.KEYCODE_BACK }
            progressDialog.setOnKeyListener(keyListener)
        }
        return progressDialog
    }

    companion object {
        private val ARG_MESSAGE_ID = IndeterminateProgressDialog::class.java.canonicalName?.plus(".ARG_MESSAGE_ID")
        private val ARG_CANCELABLE = IndeterminateProgressDialog::class.java.canonicalName?.plus(".ARG_CANCELABLE")

        /**
         * Public factory method to get dialog instances.
         *
         * @param messageId     Resource id for a message to show in the dialog.
         * @param cancelable    If 'true', the dialog can be cancelled by the user input (BACK button, touch outside...)
         * @return New dialog instance, ready to show.
         */
        @JvmStatic
        fun newInstance(messageId: Int, cancelable: Boolean): IndeterminateProgressDialog {
            val fragment = IndeterminateProgressDialog()
            fragment.setStyle(STYLE_NO_FRAME, R.style.ownCloud_AlertDialog)
            val args = Bundle()
            args.putInt(ARG_MESSAGE_ID, messageId)
            args.putBoolean(ARG_CANCELABLE, cancelable)
            fragment.arguments = args
            return fragment
        }
    }
}
