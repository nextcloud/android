/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2021 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.databinding.MultipleAccountsBinding
import com.owncloud.android.ui.adapter.UserListAdapter
import com.owncloud.android.ui.adapter.UserListItem
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class MultipleAccountsDialog : DialogFragment(), Injectable, UserListAdapter.ClickListener {
    @JvmField
    @Inject
    var accountManager: UserAccountManager? = null

    @JvmField
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null
    var highlightCurrentlyActiveAccount = true

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        val binding = MultipleAccountsBinding.inflate(inflater, null, false)

        val builder = MaterialAlertDialogBuilder(binding.root.context)
        val adapter = UserListAdapter(
            requireActivity(),
            accountManager,
            accountListItems,
            this,
            false,
            highlightCurrentlyActiveAccount,
            false,
            viewThemeUtils
        )

        binding.list.setHasFixedSize(true)
        binding.list.layoutManager = LinearLayoutManager(requireActivity())
        binding.list.adapter = adapter

        builder.setView(binding.root).setTitle(R.string.common_choose_account)

        viewThemeUtils?.dialog?.colorMaterialAlertDialogBackground(requireContext(), builder)

        return builder.create()
    }

    private val accountListItems: List<UserListItem>
        /**
         * creates the account list items list including the add-account action in case
         * multiaccount_support is enabled.
         *
         * @return list of account list items
         */
        get() {
            val users = accountManager?.allUsers ?: listOf()

            val adapterUserList: MutableList<UserListItem> = ArrayList(users.size)
            for (user in users) {
                adapterUserList.add(UserListItem(user))
            }
            return adapterUserList
        }

    override fun onOptionItemClicked(user: User, view: View) {
        // By default, access account if option is clicked
        onAccountClicked(user)
    }

    override fun onAccountClicked(user: User) {
        val parentActivity = activity as AccountChooserInterface?
        parentActivity?.onAccountChosen(user)
        dismiss()
    }
}
