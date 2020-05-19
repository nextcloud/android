package com.nextcloud.ui

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.ui.adapter.UserListAdapter
import com.owncloud.android.ui.adapter.UserListItem
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.DisplayUtils.AvatarGenerationListener
import kotlinx.android.synthetic.main.account_item.*
import kotlinx.android.synthetic.main.fragment_choose_account.*
import java.util.ArrayList

private const val ARG_CURRENT_USER_PARAM = "currentUser"

class ChooseAccountFragment : DialogFragment(), AvatarGenerationListener, UserListAdapter.ClickListener {
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
            account_menu.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable
                .ic_check_circle)?.let { icon -> DrawableCompat.wrap(icon) })
            val tintedCheck = ContextCompat.getDrawable(requireContext(), R.drawable.account_circle_white)?.let { DrawableCompat.wrap(it) }

            // Creating adapter for accounts list
            val adapter = UserListAdapter(activity as BaseActivity,
                accountManager,
                getAccountListItems(),
                tintedCheck,
                this,
                false, false)
            accounts_list.setHasFixedSize(true)
            accounts_list.layoutManager = LinearLayoutManager(activity)
            accounts_list.adapter = adapter

            /*
            // Creating listeners for quick-actions
            user_layout.setOnClickListener {
                dismiss()
            }
            */
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
            ChooseAccountFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CURRENT_USER_PARAM, user)
                }
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_choose_account, container, false)
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
        // Todo : Implement the onOptionItemClicked
    }
}
