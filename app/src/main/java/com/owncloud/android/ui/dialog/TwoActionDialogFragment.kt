/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2023 Alper Ozturk
 * Copyright (C) 2023 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.di.Injectable
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class TwoActionDialogFragment(val listener: TwoActionDialogActionListener) : DialogFragment(), Injectable {

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    interface TwoActionDialogActionListener {
        fun positiveAction()
        fun negativeAction()
    }

    companion object {
        const val titleArgument = "titleArgument"
        const val messageArgument = "messageArgument"
        const val negativeButtonArgument = "negativeButtonArgument"
        const val positiveButtonArgument = "positiveButtonArgument"

        fun newInstance(
            titleId: Int,
            messageId: Int?,
            negativeButtonTextId: Int,
            positiveButtonTextId: Int,
            listener: TwoActionDialogActionListener
        ): TwoActionDialogFragment {
            return TwoActionDialogFragment(listener).apply {
                arguments = Bundle().apply {
                    putInt(titleArgument, titleId)
                    messageId?.let { putInt(messageArgument, it) }
                    putInt(positiveButtonArgument, positiveButtonTextId)
                    putInt(negativeButtonArgument, negativeButtonTextId)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = requireContext().getString(requireArguments().getInt(titleArgument))
        val messageId: Int? = arguments?.getInt(messageArgument)
        val positiveButtonText = requireContext().getString(requireArguments().getInt(positiveButtonArgument))
        val negativeButtonText = requireContext().getString(requireArguments().getInt(negativeButtonArgument))

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setNegativeButton(negativeButtonText) { dialog, _ ->
                dialog.dismiss()
                listener.negativeAction()
            }
            .setPositiveButton(positiveButtonText) { dialog, _ ->
                dialog.dismiss()
                listener.positiveAction()
            }

        messageId?.let {
            builder.setMessage(requireContext().getString(messageId))
        }

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(requireContext(), builder)

        return builder.create()
    }

    override fun onStart() {
        super.onStart()

        val alertDialog = dialog as AlertDialog?
        alertDialog?.let {
            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE) as MaterialButton?
            if (positiveButton != null) {
                viewThemeUtils.material.colorMaterialButtonPrimaryTonal(positiveButton)
            }

            val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE) as MaterialButton?
            if (negativeButton != null) {
                viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(negativeButton)
            }
        }
    }
}
