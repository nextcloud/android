/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.EditBoxDialogBinding
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.KeyboardUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

/**
 * Dialog to rename a public share.
 */
class RenamePublicShareDialogFragment :
    DialogFragment(),
    DialogInterface.OnClickListener,
    Injectable {
    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var keyboardUtils: KeyboardUtils

    private lateinit var binding: EditBoxDialogBinding
    private var publicShare: OCShare? = null

    override fun onStart() {
        super.onStart()

        val alertDialog = dialog as AlertDialog? ?: return

        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE) as? MaterialButton
        val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE) as? MaterialButton

        positiveButton?.let {
            viewThemeUtils.material.colorMaterialButtonPrimaryTonal(positiveButton)
        }

        negativeButton?.let {
            viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(negativeButton)
        }
    }

    override fun onResume() {
        super.onResume()
        keyboardUtils.showKeyboardForEditText(requireDialog().window, binding.userInput)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        publicShare = requireArguments().getParcelableArgument(ARG_PUBLIC_SHARE, OCShare::class.java)

        val inflater = requireActivity().layoutInflater
        binding = EditBoxDialogBinding.inflate(inflater, null, false)
        val view: View = binding.root

        viewThemeUtils.material.colorTextInputLayout(binding.userInputContainer)
        binding.userInput.setText(publicShare?.label)

        val builder = MaterialAlertDialogBuilder(view.context)
            .setView(view)
            .setPositiveButton(R.string.file_rename, this)
            .setNegativeButton(R.string.common_cancel, this)
            .setTitle(R.string.public_share_name)

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.userInput.context, builder)

        return builder.create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        when (which) {
            AlertDialog.BUTTON_POSITIVE -> {
                var newName = ""
                if (binding.userInput.text != null) {
                    newName = binding.userInput.text.toString().trim()
                }

                if (TextUtils.isEmpty(newName)) {
                    DisplayUtils.showSnackMessage(requireActivity(), R.string.label_empty)
                    return
                }

                (requireActivity() as ComponentsGetter).fileOperationsHelper.setLabelToPublicShare(
                    publicShare,
                    newName
                )
            }
        }
    }

    companion object {
        private const val ARG_PUBLIC_SHARE = "PUBLIC_SHARE"

        fun newInstance(share: OCShare?): RenamePublicShareDialogFragment {
            val bundle = Bundle().apply {
                putParcelable(ARG_PUBLIC_SHARE, share)
            }

            return RenamePublicShareDialogFragment().apply {
                arguments = bundle
            }
        }
    }
}
