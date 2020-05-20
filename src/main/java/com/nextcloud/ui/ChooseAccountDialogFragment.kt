/*
 * Nextcloud Android client application
 *
 * @author Infomaniak Network SA
 * Copyright (C) 2020 Infomaniak Network SA
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextcloud.ui

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.owncloud.android.R
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.ui.adapter.UserListAdapter
import com.owncloud.android.ui.adapter.UserListItem
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.DisplayUtils.AvatarGenerationListener
import kotlinx.android.synthetic.main.account_item.*
import kotlinx.android.synthetic.main.dialog_choose_account.*
import java.util.ArrayList

private const val ARG_CURRENT_USER_PARAM = "currentUser"

class ChooseAccountDialogFragment : DialogFragment(), AvatarGenerationListener, UserListAdapter.ClickListener {
    private var currentUser: User? = null
    private lateinit var accountManager: UserAccountManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentUser = it.getParcelable(ARG_CURRENT_USER_PARAM)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        accountManager = (activity as BaseActivity).userAccountManager
        currentUser?.let { user ->

            // Defining user picture
            user_icon.tag = user.accountName
            DisplayUtils.setAvatar(user, this, resources.getDimension(R.dimen.list_item_avatar_icon_radius), resources, user_icon, context)

            // Defining user texts, accounts, etc.
            user_name.text = user.toOwnCloudAccount().displayName
            ticker.visibility = View.GONE
            account.text = user.accountName

            // Defining user right indicator
            account_menu.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_check_circle))

            // Creating adapter for accounts list
            val adapter = UserListAdapter(activity as BaseActivity,
                accountManager,
                getAccountListItems(),
                null,
                this,
                false, false)
            accounts_list.adapter = adapter

            // Creating listeners for quick-actions
            current_account.setOnClickListener {
                dismiss()
            }
            add_account.setOnClickListener {
                (activity as DrawerActivity).openAddAccount()
            }
            manage_accounts.setOnClickListener {
                (activity as DrawerActivity).openManageAccounts()
            }
        }
    }

    private fun getAccountListItems(): List<UserListItem>? {
        val users = accountManager.allUsers
        val adapterUserList: MutableList<UserListItem> = ArrayList(users.size)
        // Remove the current account from the adapter to display only other accounts
        for (user in users) {
            if (user != currentUser) {
                adapterUserList.add(UserListItem(user))
            }
        }
        return adapterUserList
    }

    /**
     * Fragment creator
     */
    companion object {
        @JvmStatic
        fun newInstance(user: User) =
            ChooseAccountDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CURRENT_USER_PARAM, user)
                }
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_choose_account, container, false)
    }

    override fun shouldCallGeneratedCallback(tag: String?, callContext: Any?): Boolean {
        return (callContext as ImageView).tag.toString() == tag
    }

    override fun avatarGenerated(avatarDrawable: Drawable?, callContext: Any?) {
        user_icon.setImageDrawable(avatarDrawable)
    }

    override fun onAccountClicked(user: User?) {
        (activity as DrawerActivity).accountClicked(user.hashCode())
    }

    override fun onOptionItemClicked(user: User?, view: View?) {
        // Un-needed for this context
    }
}
