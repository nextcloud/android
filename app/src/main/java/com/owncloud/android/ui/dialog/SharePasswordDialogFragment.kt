/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2017-2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.PasswordDialogBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.KeyboardUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

/**
 * Dialog to input the password for sharing a file/folder.
 *
 *
 * Triggers the share when the password is introduced.
 */
class SharePasswordDialogFragment : DialogFragment(), Injectable {
    @JvmField
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null

    @JvmField
    @Inject
    var keyboardUtils: KeyboardUtils? = null

    private var binding: PasswordDialogBinding? = null
    private var file: OCFile? = null
    private var share: OCShare? = null
    private var createShare = false
    private var askForPassword = false

    override fun onStart() {
        super.onStart()

        val alertDialog = dialog as AlertDialog?

        if (alertDialog != null) {
            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE) as MaterialButton?
            if (positiveButton != null) {
                viewThemeUtils?.material?.colorMaterialButtonPrimaryTonal(positiveButton)
                positiveButton.setOnClickListener {
                    val sharePassword = binding?.sharePassword?.text

                    if (sharePassword != null) {
                        val password = sharePassword.toString()
                        if (!askForPassword && TextUtils.isEmpty(password)) {
                            DisplayUtils.showSnackMessage(binding?.root, R.string.share_link_empty_password)
                            return@setOnClickListener
                        }
                        if (share == null) {
                            setPassword(createShare, file, password)
                        } else {
                            setPassword(share!!, password)
                        }
                    }

                    alertDialog.dismiss()
                }
            }

            val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE) as MaterialButton?
            if (negativeButton != null) {
                viewThemeUtils?.material?.colorMaterialButtonPrimaryBorderless(negativeButton)
            }

            val neutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL) as MaterialButton?
            if (neutralButton != null) {
                val warningColorId = ContextCompat.getColor(requireContext(), R.color.highlight_textColor_Warning)
                viewThemeUtils?.platform?.colorTextButtons(warningColorId, neutralButton)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        keyboardUtils?.showKeyboardForEditText(requireDialog().window, binding!!.sharePassword)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        file = requireArguments().getParcelableArgument(ARG_FILE, OCFile::class.java)
        share = requireArguments().getParcelableArgument(ARG_SHARE, OCShare::class.java)

        createShare = requireArguments().getBoolean(ARG_CREATE_SHARE, false)
        askForPassword = requireArguments().getBoolean(ARG_ASK_FOR_PASSWORD, false)

        // Inflate the layout for the dialog
        val inflater = requireActivity().layoutInflater
        binding = PasswordDialogBinding.inflate(inflater, null, false)

        // Setup layout
        binding?.sharePassword?.setText(R.string.empty)
        viewThemeUtils?.material?.colorTextInputLayout(binding!!.sharePasswordContainer)

        val neutralButtonTextId: Int
        val title: Int
        if (askForPassword) {
            title = R.string.share_link_optional_password_title
            neutralButtonTextId = R.string.common_skip
        } else {
            title = R.string.share_link_password_title
            neutralButtonTextId = R.string.common_cancel
        }

        // Build the dialog
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setView(binding!!.root)
            .setPositiveButton(R.string.common_ok, null)
            .setNegativeButton(R.string.common_delete) { _: DialogInterface?, _: Int -> callSetPassword() }
            .setNeutralButton(neutralButtonTextId) { _: DialogInterface?, _: Int ->
                if (askForPassword) {
                    callSetPassword()
                }
            }
            .setTitle(title)

        viewThemeUtils?.dialog?.colorMaterialAlertDialogBackground(requireContext(), builder)

        return builder.create()
    }

    private fun callSetPassword() {
        if (share == null) {
            setPassword(createShare, file, null)
        } else {
            setPassword(share!!, null)
        }
    }

    private fun setPassword(createShare: Boolean, file: OCFile?, password: String?) {
        val fileOperationsHelper = (requireActivity() as FileActivity).fileOperationsHelper ?: return
        if (createShare) {
            fileOperationsHelper.shareFileViaPublicShare(file, password)
        } else {
            fileOperationsHelper.setPasswordToShare(share, password)
        }
    }

    private fun setPassword(share: OCShare, password: String?) {
        val fileOperationsHelper = (requireActivity() as FileActivity).fileOperationsHelper ?: return
        fileOperationsHelper.setPasswordToShare(share, password)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val ARG_FILE = "FILE"
        private const val ARG_SHARE = "SHARE"
        private const val ARG_CREATE_SHARE = "CREATE_SHARE"
        private const val ARG_ASK_FOR_PASSWORD = "ASK_FOR_PASSWORD"
        const val PASSWORD_FRAGMENT = "PASSWORD_FRAGMENT"

        /**
         * Public factory method to create new SharePasswordDialogFragment instances.
         *
         * @param file        OCFile bound to the public share that which
         * password will be set or updated
         * @param createShare When 'true', the request for password will be
         * followed by the creation of a new public link
         * when 'false', a public share is assumed to exist, and the password is bound to it.
         * @return Dialog ready to show.
         */
        @JvmStatic
        fun newInstance(file: OCFile?, createShare: Boolean, askForPassword: Boolean): SharePasswordDialogFragment {
            val frag = SharePasswordDialogFragment()
            val args = Bundle()
            args.putParcelable(ARG_FILE, file)
            args.putBoolean(ARG_CREATE_SHARE, createShare)
            args.putBoolean(ARG_ASK_FOR_PASSWORD, askForPassword)
            frag.arguments = args
            return frag
        }

        /**
         * Public factory method to create new SharePasswordDialogFragment instances.
         *
         * @param share OCFile bound to the public share that which password will be set or updated
         * @return Dialog ready to show.
         */
        @JvmStatic
        fun newInstance(share: OCShare?, askForPassword: Boolean): SharePasswordDialogFragment {
            val frag = SharePasswordDialogFragment()
            val args = Bundle()
            args.putParcelable(ARG_SHARE, share)
            args.putBoolean(ARG_ASK_FOR_PASSWORD, askForPassword)
            frag.arguments = args
            return frag
        }

        /**
         * Public factory method to create new SharePasswordDialogFragment instances.
         *
         * @param share OCFile bound to the public share that which password will be set or updated
         * @return Dialog ready to show.
         */
        fun newInstance(share: OCShare?): SharePasswordDialogFragment {
            val frag = SharePasswordDialogFragment()
            val args = Bundle()
            args.putParcelable(ARG_SHARE, share)
            frag.arguments = args
            return frag
        }
    }
}
