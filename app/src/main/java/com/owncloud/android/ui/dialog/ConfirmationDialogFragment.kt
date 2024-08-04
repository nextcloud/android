/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2012 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.dialog

//noinspection SuspiciousImport
import android.R
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.di.Injectable
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

open class ConfirmationDialogFragment : DialogFragment(), Injectable {

    @JvmField
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null

    private var mListener: ConfirmationDialogFragmentListener? = null

    override fun onStart() {
        super.onStart()

        val alertDialog = dialog as AlertDialog?

        if (alertDialog != null) {
            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE) as MaterialButton?
            if (positiveButton != null) {
                viewThemeUtils?.material?.colorMaterialButtonPrimaryTonal(positiveButton)
            }

            val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE) as MaterialButton?
            if (negativeButton != null) {
                viewThemeUtils?.material?.colorMaterialButtonPrimaryBorderless(negativeButton)
            }

            val neutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL) as MaterialButton?
            if (neutralButton != null) {
                viewThemeUtils?.material?.colorMaterialButtonPrimaryBorderless(neutralButton)
            }
        }
    }

    fun setOnConfirmationListener(listener: ConfirmationDialogFragmentListener?) {
        mListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val messageArguments = requireArguments().getStringArray(ARG_MESSAGE_ARGUMENTS) ?: arrayOf<String>()
        val titleId = requireArguments().getInt(ARG_TITLE_ID, -1)
        val messageId = requireArguments().getInt(ARG_MESSAGE_RESOURCE_ID, -1)
        val positiveButtonTextId = requireArguments().getInt(ARG_POSITIVE_BTN_RES, -1)
        val negativeButtonTextId = requireArguments().getInt(ARG_NEGATIVE_BTN_RES, -1)
        val neutralButtonTextId = requireArguments().getInt(ARG_NEUTRAL_BTN_RES, -1)

        @Suppress("SpreadOperator")
        val message = getString(messageId, *messageArguments)

        val builder = MaterialAlertDialogBuilder(requireActivity())
            .setIcon(com.owncloud.android.R.drawable.ic_warning)
            .setIconAttribute(R.attr.alertDialogIcon)
            .setMessage(message)

        if (titleId == 0) {
            builder.setTitle(R.string.dialog_alert_title)
        } else if (titleId != -1) {
            builder.setTitle(titleId)
        }

        if (positiveButtonTextId != -1) {
            builder.setPositiveButton(positiveButtonTextId) { dialog: DialogInterface, _: Int ->
                mListener?.onConfirmation(tag)
                dialog.dismiss()
            }
        }
        if (negativeButtonTextId != -1) {
            builder.setNegativeButton(negativeButtonTextId) { dialog: DialogInterface, _: Int ->
                mListener?.onCancel(tag)
                dialog.dismiss()
            }
        }
        if (neutralButtonTextId != -1) {
            builder.setNeutralButton(neutralButtonTextId) { dialog: DialogInterface, _: Int ->
                mListener?.onNeutral(tag)
                dialog.dismiss()
            }
        }

        viewThemeUtils?.dialog?.colorMaterialAlertDialogBackground(requireActivity(), builder)

        return builder.create()
    }

    interface ConfirmationDialogFragmentListener {
        fun onConfirmation(callerTag: String?)
        fun onNeutral(callerTag: String?)
        fun onCancel(callerTag: String?)
    }

    companion object {
        const val ARG_MESSAGE_RESOURCE_ID = "resource_id"
        const val ARG_MESSAGE_ARGUMENTS = "string_array"
        const val ARG_TITLE_ID = "title_id"
        const val ARG_POSITIVE_BTN_RES = "positive_btn_res"
        const val ARG_NEUTRAL_BTN_RES = "neutral_btn_res"
        const val ARG_NEGATIVE_BTN_RES = "negative_btn_res"
        const val FTAG_CONFIRMATION = "CONFIRMATION_FRAGMENT"

        /**
         * Public factory method to create new ConfirmationDialogFragment instances.
         *
         * @param messageResId         Resource id for a message to show in the dialog.
         * @param messageArguments     Arguments to complete the message, if it's a format string. May be null.
         * @param titleResId           Resource id for a text to show in the title. 0 for default alert title, -1 for no
         * title.
         * @param positiveButtonTextId Resource id for the text of the positive button. -1 for no positive button.
         * @param neutralButtonTextId  Resource id for the text of the neutral button. -1 for no neutral button.
         * @param negativeButtonTextId Resource id for the text of the negative button. -1 for no negative button.
         * @return Dialog ready to show.
         */
        @JvmStatic
        fun newInstance(
            messageResId: Int,
            messageArguments: Array<String?>?,
            titleResId: Int,
            positiveButtonTextId: Int,
            negativeButtonTextId: Int,
            neutralButtonTextId: Int
        ): ConfirmationDialogFragment {
            check(messageResId != -1) { "Calling confirmation dialog without message resource" }
            val frag = ConfirmationDialogFragment()
            val args = Bundle()
            args.putInt(ARG_MESSAGE_RESOURCE_ID, messageResId)
            args.putStringArray(ARG_MESSAGE_ARGUMENTS, messageArguments)
            args.putInt(ARG_TITLE_ID, titleResId)
            args.putInt(ARG_POSITIVE_BTN_RES, positiveButtonTextId)
            args.putInt(ARG_NEGATIVE_BTN_RES, negativeButtonTextId)
            args.putInt(ARG_NEUTRAL_BTN_RES, neutralButtonTextId)
            frag.arguments = args
            return frag
        }
    }
}
