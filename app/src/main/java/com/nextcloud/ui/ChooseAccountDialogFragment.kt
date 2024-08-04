/*
 * Nextcloud Android client application
 *
 * @author Infomaniak Network SA
 * Copyright (C) 2020 Infomaniak Network SA
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.DialogChooseAccountBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.resources.users.Status
import com.owncloud.android.ui.StatusDrawable
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.ui.adapter.UserListAdapter
import com.owncloud.android.ui.adapter.UserListItem
import com.owncloud.android.ui.asynctasks.RetrieveStatusAsyncTask
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.DisplayUtils.AvatarGenerationListener
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

private const val ARG_CURRENT_USER_PARAM = "currentUser"

private const val STATUS_SIZE_IN_DP = 9f

class ChooseAccountDialogFragment :
    DialogFragment(),
    AvatarGenerationListener,
    UserListAdapter.ClickListener,
    Injectable {
    private lateinit var dialogView: View
    private var currentUser: User? = null
    private lateinit var accountManager: UserAccountManager
    private var currentStatus: Status? = null

    private var _binding: DialogChooseAccountBinding? = null
    val binding get() = _binding!!

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentUser = it.getParcelableArgument(ARG_CURRENT_USER_PARAM, User::class.java)
        }
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogChooseAccountBinding.inflate(layoutInflater)
        dialogView = binding.root

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.statusView.context, builder)

        return builder.create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        accountManager = (activity as BaseActivity).userAccountManager
        currentUser?.let { user ->

            // Defining user picture
            binding.currentAccount.userIcon.tag = user.accountName
            DisplayUtils.setAvatar(
                user,
                this,
                resources.getDimension(R.dimen.list_item_avatar_icon_radius),
                resources,
                binding.currentAccount.userIcon,
                context
            )

            // Defining user texts, accounts, etc.
            binding.currentAccount.userName.text = user.toOwnCloudAccount().displayName
            binding.currentAccount.ticker.visibility = View.GONE
            binding.currentAccount.account.text = user.accountName

            // Defining user right indicator
            val icon = viewThemeUtils.platform.tintPrimaryDrawable(requireContext(), R.drawable.ic_check_circle)
            binding.currentAccount.accountMenu.setImageDrawable(icon)

            // Creating adapter for accounts list
            val adapter = UserListAdapter(
                activity as BaseActivity,
                accountManager,
                getAccountListItems(),
                this,
                false,
                false,
                true,
                viewThemeUtils
            )

            // hide "add account" when no multi account
            if (!resources.getBoolean(R.bool.multiaccount_support)) {
                binding.addAccount.visibility = View.GONE
            }

            binding.accountsList.adapter = adapter

            // Creating listeners for quick-actions
            binding.currentAccount.root.setOnClickListener {
                dismiss()
            }
            binding.addAccount.setOnClickListener {
                (activity as DrawerActivity).openAddAccount()
            }
            binding.manageAccounts.setOnClickListener {
                (activity as DrawerActivity).openManageAccounts()
            }

            binding.setStatus.setOnClickListener {
                val setStatusDialog = SetStatusDialogFragment.newInstance(accountManager.user, currentStatus)
                setStatusDialog.show((activity as DrawerActivity).supportFragmentManager, "fragment_set_status")

                dismiss()
            }

            val capability = FileDataStorageManager(user, context?.contentResolver)
                .getCapability(user)

            if (capability.userStatus.isTrue) {
                binding.statusView.visibility = View.VISIBLE
            }

            RetrieveStatusAsyncTask(user, this, clientFactory).execute()
        }

        themeViews()
    }

    private fun themeViews() {
        viewThemeUtils.platform.themeDialogDivider(binding.separatorLine)
        viewThemeUtils.platform.themeDialog(binding.root)

        viewThemeUtils.material.colorMaterialTextButton(binding.setStatus)
        viewThemeUtils.dialog.colorDialogMenuText(binding.setStatus)
        viewThemeUtils.material.colorMaterialTextButton(binding.addAccount)
        viewThemeUtils.dialog.colorDialogMenuText(binding.addAccount)
        viewThemeUtils.material.colorMaterialTextButton(binding.manageAccounts)
        viewThemeUtils.dialog.colorDialogMenuText(binding.manageAccounts)
    }

    private fun getAccountListItems(): List<UserListItem> {
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
        fun newInstance(user: User) = ChooseAccountDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_CURRENT_USER_PARAM, user)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return dialogView
    }

    override fun shouldCallGeneratedCallback(tag: String?, callContext: Any?): Boolean {
        return (callContext as ImageView).tag.toString() == tag
    }

    override fun avatarGenerated(avatarDrawable: Drawable?, callContext: Any?) {
        if (_binding != null) {
            binding.currentAccount.userIcon.setImageDrawable(avatarDrawable)
        }
    }

    override fun onAccountClicked(user: User?) {
        (activity as DrawerActivity).accountClicked(user.hashCode())
    }

    override fun onOptionItemClicked(user: User?, view: View?) {
        // Un-needed for this context
    }

    fun setStatus(newStatus: Status, context: Context) {
        currentStatus = newStatus

        val size = DisplayUtils.convertDpToPixel(STATUS_SIZE_IN_DP, context)
        binding.currentAccount.ticker.background = null
        binding.currentAccount.ticker.setImageDrawable(StatusDrawable(newStatus, size.toFloat(), context))
        binding.currentAccount.ticker.visibility = View.VISIBLE

        binding.currentAccount.status.let {
            if (newStatus.message.isNullOrBlank()) {
                it.text = getString(R.string.empty)
                it.visibility = View.GONE
            } else {
                it.text = newStatus.message
                it.visibility = View.VISIBLE
            }
        }

        view?.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}
