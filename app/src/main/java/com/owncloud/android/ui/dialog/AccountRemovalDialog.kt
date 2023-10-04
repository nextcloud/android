/*
 * Nextcloud Android client application
 *
 * @author ZetaTom
 * @author Tobias Kaminsky
 * Copyright (C) 2023 ZetaTom
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
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.R
import com.owncloud.android.databinding.AccountRemovalDialogBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class AccountRemovalDialog : DialogFragment(), Injectable {

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private var user: User? = null
    private var _binding: AccountRemovalDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = requireArguments().getParcelable(KEY_USER)
    }

    override fun onStart() {
        super.onStart()
        val alertDialog = dialog as AlertDialog
        viewThemeUtils.platform.themeRadioButton(binding.radioLocalRemove)
        viewThemeUtils.platform.themeRadioButton(binding.radioRequestDeletion)
        viewThemeUtils.platform.colorTextButtons(
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE), alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
        )

        binding.userName.text = UserAccountManager.getDisplayName(user)
        binding.account.text = user?.let { DisplayUtils.convertIdn(it.accountName, false) }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = AccountRemovalDialogBinding.inflate(layoutInflater)



        val builder = MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.delete_account)
            .setView(binding.root)
            .setNegativeButton(R.string.common_cancel) { _, _ -> }
            .setPositiveButton("Continue") { _, _ ->
            }

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(requireActivity(), builder)

        binding.radioLocalRemove.setOnClickListener {
            binding.radioRequestDeletion.isChecked = false
            (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).text = "Remove Account"
        }
        binding.radioRequestDeletion.setOnClickListener {
            binding.radioLocalRemove.isChecked = false
            (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).text = "Request Deletion"
        }

        return builder.create()
    }

    private fun hasDropAccount() {
        val capability = FileDataStorageManager(user, context?.contentResolver).getCapability(user)
    }

    companion object {
        private const val KEY_USER = "USER"

        @JvmStatic
        fun newInstance(user: User) = AccountRemovalDialog().apply {
            arguments = Bundle().apply {
                putParcelable(KEY_USER, user)
            }
        }
    }
}