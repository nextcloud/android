/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.R
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class AccountRemovalConfirmationDialog : DialogFragment(), Injectable {
    @JvmField
    @Inject
    var backgroundJobManager: BackgroundJobManager? = null

    @JvmField
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null

    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        user = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable(KEY_USER, User::class.java)
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable(KEY_USER)
        }
    }

    override fun onStart() {
        super.onStart()

        val alertDialog = dialog as AlertDialog?

        if (alertDialog != null) {
            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE) as MaterialButton
            viewThemeUtils?.material?.colorMaterialButtonPrimaryTonal(positiveButton)

            val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE) as MaterialButton
            viewThemeUtils?.material?.colorMaterialButtonPrimaryBorderless(negativeButton)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.delete_account)
            .setMessage(resources.getString(R.string.delete_account_warning, user!!.accountName))
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton(R.string.common_ok) { _: DialogInterface?, _: Int ->
                backgroundJobManager?.startAccountRemovalJob(
                    user!!.accountName,
                    false
                )
            }
            .setNegativeButton(R.string.common_cancel, null)

        viewThemeUtils?.dialog?.colorMaterialAlertDialogBackground(requireActivity(), builder)

        return builder.create()
    }

    companion object {

        private const val KEY_USER = "USER"

        @JvmStatic
        fun newInstance(user: User?): AccountRemovalConfirmationDialog {
            val bundle = Bundle()
            bundle.putParcelable(KEY_USER, user)
            val dialog = AccountRemovalConfirmationDialog()
            dialog.arguments = bundle
            return dialog
        }
    }

}
