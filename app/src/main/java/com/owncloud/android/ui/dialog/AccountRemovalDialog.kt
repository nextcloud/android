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
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
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
import com.owncloud.android.utils.DisplayUtils.AvatarGenerationListener
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class AccountRemovalDialog : DialogFragment(), AvatarGenerationListener, Injectable {

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private var user: User? = null
    private lateinit var alertDialog: AlertDialog
    private var _binding: AccountRemovalDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = requireArguments().getParcelable(KEY_USER)
    }

    override fun onStart() {
        super.onStart()

        // disable positive button and apply theming
        alertDialog = dialog as AlertDialog
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

        viewThemeUtils.platform.themeRadioButton(binding.radioLocalRemove)
        viewThemeUtils.platform.themeRadioButton(binding.radioRequestDeletion)
        viewThemeUtils.platform.colorTextButtons(
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE),
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        )

        binding.userName.text = UserAccountManager.getDisplayName(user)
        binding.account.text = user?.let { DisplayUtils.convertIdn(it.accountName, false) }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = AccountRemovalDialogBinding.inflate(layoutInflater)

        // start avatar generation
        setAvatar()

        // hide second option when plug-in isn't installed
        if (hasDropAccount()) {
            binding.requestDeletion.visibility = View.VISIBLE
        }

        val builder =
            MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.delete_account)
                .setView(binding.root)
                .setNegativeButton(R.string.common_cancel) { _, _ -> }
                .setPositiveButton(R.string.delete_account) { _, _ -> removeAccount() }

        // allow selection by clicking on list element
        binding.localRemove.setOnClickListener {
            binding.radioLocalRemove.performClick()
        }
        binding.requestDeletion.setOnClickListener {
            binding.radioRequestDeletion.performClick()
        }

        // set listeners for custom radio button list
        binding.radioLocalRemove.setOnClickListener {
            binding.radioRequestDeletion.isChecked = false
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                text = getText(R.string.delete_account)
                isEnabled = true
            }
        }
        binding.radioRequestDeletion.setOnClickListener {
            binding.radioLocalRemove.isChecked = false
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                text = getString(R.string.request_account_deletion_button)
                isEnabled = true
            }
        }

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(requireActivity(), builder)

        return builder.create()
    }

    /**
     * Get value of `drop-account` capability.
     */
    private fun hasDropAccount(): Boolean {
        val capability = FileDataStorageManager(user, context?.contentResolver).getCapability(user)
        return capability.dropAccount.isTrue
    }

    /**
     * Start removal of account. Depending on which option is checked, either a browser will open to request deletion,
     * or the local account will be removed immediately.
     */
    private fun removeAccount() {
        user?.let { user ->
            if (binding.radioRequestDeletion.isChecked) {
                DisplayUtils.startLinkIntent(activity, user.server.uri.toString() + DROP_ACCOUNT_URI)
            } else {
                backgroundJobManager.startAccountRemovalJob(user.accountName, false)
            }
        }
    }

    /**
     * Start avatar generation.
     */
    private fun setAvatar() {
        try {
            val imageView = binding.userIcon
            imageView.tag = user!!.accountName
            DisplayUtils.setAvatar(
                user!!,
                this,
                resources.getDimension(R.dimen.list_item_avatar_icon_radius),
                resources,
                imageView,
                context
            )
        } catch (_: Exception) {
        }
    }

    override fun avatarGenerated(avatarDrawable: Drawable?, callContext: Any?) {
        avatarDrawable?.let {
            binding.userIcon.setImageDrawable(it)
        }
    }

    override fun shouldCallGeneratedCallback(tag: String?, callContext: Any?): Boolean {
        return binding.userIcon.tag == tag
    }

    companion object {
        private const val KEY_USER = "USER"
        private const val DROP_ACCOUNT_URI = "/settings/user/drop_account"

        @JvmStatic
        fun newInstance(user: User) = AccountRemovalDialog().apply {
            arguments = Bundle().apply {
                putParcelable(KEY_USER, user)
            }
        }
    }
}
