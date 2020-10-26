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

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.lib.resources.users.PredefinedStatus
import com.owncloud.android.lib.resources.users.Status
import com.owncloud.android.ui.StatusDrawable
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.ui.adapter.UserListAdapter
import com.owncloud.android.ui.adapter.UserListItem
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.DisplayUtils.AvatarGenerationListener
import kotlinx.android.synthetic.main.account_item.*
import java.util.ArrayList
import javax.inject.Inject

private const val ARG_CURRENT_USER_PARAM = "currentUser"

class SetStatusDialogFragment : DialogFragment(),
    AvatarGenerationListener,
    UserListAdapter.ClickListener,
    Injectable {
    private lateinit var dialogView: View
    private var currentUser: User? = null
    private lateinit var accountManager: UserAccountManager
    private lateinit var predefinedStatus: ArrayList<PredefinedStatus>
    @Inject
    lateinit var arbitraryDataProvider: ArbitraryDataProvider

    @Inject
    lateinit var clientFactory: ClientFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentUser = it.getParcelable(ARG_CURRENT_USER_PARAM)

            val json = arbitraryDataProvider.getValue(currentUser, ArbitraryDataProvider.PREDEFINED_STATUS)

            if (!json.isEmpty()) {
                val myType = object : TypeToken<ArrayList<PredefinedStatus>>() {}.type
                predefinedStatus = Gson().fromJson(json, myType)
            }

            val size = predefinedStatus.size
        }
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_set_status, null)
        return MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        accountManager = (activity as BaseActivity).userAccountManager

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
            SetStatusDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CURRENT_USER_PARAM, user)
                }
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return dialogView
    }

    override fun shouldCallGeneratedCallback(tag: String?, callContext: Any?): Boolean {
        return (callContext as ImageView).tag.toString() == tag
    }

    override fun avatarGenerated(avatarDrawable: Drawable?, callContext: Any?) {
        if (user_icon != null) {
            user_icon.setImageDrawable(avatarDrawable)
        }
    }

    override fun onAccountClicked(user: User?) {
        (activity as DrawerActivity).accountClicked(user.hashCode())
    }

    override fun onOptionItemClicked(user: User?, view: View?) {
        // Un-needed for this context
    }

    fun setStatus(newStatus: Status) {
        val size = DisplayUtils.convertDpToPixel(9f, context)
        ticker.background = null
        ticker.setImageDrawable(StatusDrawable(newStatus, size.toFloat(), context))
        ticker.visibility = View.VISIBLE

        if (newStatus.message.isNullOrBlank()) {
            status.text = ""
            status.visibility = View.GONE
        } else {
            status.text = newStatus.message
            status.visibility = View.VISIBLE
        }

        view?.invalidate()
    }
}
