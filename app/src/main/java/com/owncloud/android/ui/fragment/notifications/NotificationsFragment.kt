/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.VisibleForTesting
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.NotificationWork
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.common.NextcloudClient
import com.nextcloud.utils.BuildHelper
import com.nextcloud.utils.GlideHelper
import com.nextcloud.utils.extensions.getTypedActivity
import com.owncloud.android.R
import com.owncloud.android.databinding.NotificationsLayoutBinding
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.notifications.DeleteAllNotificationsRemoteOperation
import com.owncloud.android.lib.resources.notifications.DeleteNotificationRemoteOperation
import com.owncloud.android.lib.resources.notifications.GetNotificationsRemoteOperation
import com.owncloud.android.lib.resources.notifications.models.Action
import com.owncloud.android.lib.resources.notifications.models.Notification
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.adapter.NotificationListAdapter
import com.owncloud.android.ui.asynctasks.NotificationExecuteActionTask
import com.owncloud.android.ui.fragment.notifications.model.NotificationsUIState
import com.owncloud.android.ui.notifications.NotificationsContract
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.PushUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Optional
import javax.inject.Inject

@Suppress("TooManyFunctions", "ReturnCount")
class NotificationsFragment :
    Fragment(),
    NotificationsContract.View,
    NotificationsAdapterItemClick,
    Injectable {

    private var binding: NotificationsLayoutBinding? = null
    private var adapter: NotificationListAdapter? = null
    private var optionalUser: Optional<User>? = null

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var preferences: AppPreferences

    private var client: NextcloudClient? = null

    private var state: NotificationsUIState = NotificationsUIState.Loading
        set(value) {
            field = value
            renderState(value)
        }

    // region Lifecycle
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = NotificationsLayoutBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val initForTesting = activity?.intent?.getBooleanExtra(EXTRA_INIT_FOR_TESTING, false)
        if (initForTesting == true) return

        lifecycleScope.launch {
            val baseActivity = getTypedActivity(BaseActivity::class.java)
            val client = baseActivity?.clientRepository?.getNextcloudClient() ?: run {
                state = NotificationsUIState.Error(getString(R.string.account_not_found))
                return@launch
            }
            this@NotificationsFragment.client = client

            withContext(Dispatchers.Main) {
                setupMenu(client)
                initUser()
                setupSwipeRefresh(client)
                setupPushWarning()
                setupContent(client)
                if (optionalUser?.isPresent == false) {
                    state = NotificationsUIState.Error(getString(R.string.account_not_found))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
    // endregion

    // region State rendering

    private fun renderState(state: NotificationsUIState) {
        binding?.run {
            itemSwipeRefreshLayout.visibility = View.GONE
            shimmerAndEmptySwipeRefreshLayout.visibility = View.GONE

            shimmerLayout.visibility = View.GONE
            emptyList.root.visibility = View.GONE

            emptyList.emptyListViewHeadline.text = ""
            emptyList.emptyListViewText.text = ""
            emptyList.emptyListViewText.visibility = View.GONE

            when (state) {
                is NotificationsUIState.Loading -> renderLoading()

                is NotificationsUIState.Loaded -> renderLoaded(state.items)

                is NotificationsUIState.Empty -> renderEmpty(
                    headline = getString(R.string.notifications_no_results_headline),
                    message = getString(R.string.notifications_no_results_message)
                )

                is NotificationsUIState.Error -> renderEmpty(
                    headline = getString(R.string.notifications_no_results_headline),
                    message = state.message
                )
            }
        }
    }

    private fun NotificationsLayoutBinding.renderEmpty(headline: String, message: String?) {
        shimmerAndEmptySwipeRefreshLayout.visibility = View.VISIBLE

        shimmerLayout.visibility = View.GONE

        emptyList.run {
            root.visibility = View.VISIBLE
            emptyListIcon.visibility = View.VISIBLE

            emptyListViewHeadline.text = headline
            emptyListIcon.setImageResource(R.drawable.ic_notification)
            emptyListViewText.apply {
                text = message ?: ""
                visibility = if (message.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun renderLoading() {
        binding?.shimmerAndEmptySwipeRefreshLayout?.visibility = View.VISIBLE
        binding?.shimmerLayout?.visibility = View.VISIBLE
        binding?.emptyList?.root?.visibility = View.GONE
    }

    private fun renderLoaded(items: List<Notification>) {
        initializeAdapter()
        adapter?.setNotificationItems(items)
        binding?.itemSwipeRefreshLayout?.visibility = View.VISIBLE
    }
    // endregion

    // region Setup
    private fun initUser() {
        optionalUser = Optional.of(accountManager.user)
        val accountName = activity?.intent?.getStringExtra(NotificationWork.KEY_NOTIFICATION_ACCOUNT)
        if (optionalUser?.get()?.accountName.equals(accountName, ignoreCase = true)) {
            accountManager.setCurrentOwnCloudAccount(accountName)
        }
    }

    private fun setupSwipeRefresh(client: NextcloudClient) {
        binding?.run {
            viewThemeUtils.androidx.themeSwipeRefreshLayout(itemSwipeRefreshLayout)
            viewThemeUtils.androidx.themeSwipeRefreshLayout(shimmerAndEmptySwipeRefreshLayout)
            itemSwipeRefreshLayout.setOnRefreshListener {
                state = NotificationsUIState.Loading
                itemSwipeRefreshLayout.isRefreshing = true
                fetchAndSetData(client)
            }
            shimmerAndEmptySwipeRefreshLayout.setOnRefreshListener {
                state = NotificationsUIState.Loading
                fetchAndSetData(client)
            }
        }
    }

    private fun setupContent(client: NextcloudClient) {
        binding?.run {
            emptyList.emptyListIcon.setImageResource(R.drawable.ic_notification)
            list.layoutManager = LinearLayoutManager(requireContext())
            fetchAndSetData(client)
        }
    }

    private fun setupMenu(client: NextcloudClient) {
        (requireActivity() as MenuHost).addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.activity_notifications, menu)
                }

                override fun onMenuItemSelected(item: MenuItem): Boolean {
                    if (item.itemId != R.id.action_empty_notifications) return false
                    lifecycleScope.launch(Dispatchers.IO) {
                        val result = DeleteAllNotificationsRemoteOperation().execute(client)
                        withContext(Dispatchers.Main) { onRemovedAllNotifications(result.isSuccess) }
                    }
                    return true
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun setupPushWarning() {
        if (!resources.getBoolean(R.bool.show_push_warning)) return

        val pushUrl = resources.getString(R.string.push_server_url)
        if (pushUrl.isEmpty() && BuildHelper.isFlavourGPlay()) return

        val messageRes = when {
            pushUrl.isEmpty() -> R.string.push_notifications_not_implemented
            isUsingOldLogin() -> R.string.push_notifications_old_login
            isPushValueEmpty() -> R.string.push_notifications_temp_error
            else -> return
        }

        DisplayUtils.showSnackMessage(this, messageRes)
    }

    private fun isUsingOldLogin(): Boolean {
        val accountName = optionalUser?.orElse(null)?.accountName ?: return false
        return ArbitraryDataProviderImpl(requireActivity())
            .getBooleanValue(accountName, UserAccountManager.ACCOUNT_USES_STANDARD_PASSWORD)
    }

    private fun isPushValueEmpty(): Boolean {
        val accountName = optionalUser?.orElse(null)?.accountName ?: return true
        return ArbitraryDataProviderImpl(requireActivity()).getValue(accountName, PushUtils.KEY_PUSH).isEmpty()
    }
    // endregion

    // region Data loading
    private fun fetchAndSetData(client: NextcloudClient) {
        lifecycleScope.launch(Dispatchers.IO) {
            initializeAdapter()
            val result = GetNotificationsRemoteOperation().execute(client)
            withContext(Dispatchers.Main) {
                state = when {
                    result?.isSuccess == true && result.resultData != null -> {
                        val items = result.resultData ?: emptyList()
                        if (items.isEmpty()) {
                            NotificationsUIState.Empty
                        } else {
                            NotificationsUIState.Loaded(items)
                        }
                    }

                    else -> {
                        try {
                            Log_OC.d(TAG, result?.logMessage)
                            NotificationsUIState.Error(
                                result?.getLogMessage(requireContext())
                                    ?: getString(R.string.notifications_no_results_message)
                            )
                        } catch (_: Exception) {
                            NotificationsUIState.Error(getString(R.string.notifications_no_results_message))
                        }
                    }
                }
                hideRefreshLayoutLoader()
            }
        }
    }

    private fun initializeAdapter() {
        if (adapter == null) {
            adapter = NotificationListAdapter(this@NotificationsFragment, viewThemeUtils, this)
            binding?.list?.adapter = adapter
        }
    }

    private fun hideRefreshLayoutLoader() {
        binding?.itemSwipeRefreshLayout?.isRefreshing = false
        binding?.shimmerAndEmptySwipeRefreshLayout?.isRefreshing = false
    }
    // endregion

    @VisibleForTesting
    fun initForTesting(state: NotificationsUIState) {
        adapter = NotificationListAdapter(this@NotificationsFragment, viewThemeUtils, this)
        binding?.list?.adapter = adapter
        binding?.list?.layoutManager = LinearLayoutManager(requireContext())
        this.state = state
    }

    // region Callbacks
    override fun onRemovedNotification(isSuccess: Boolean, client: NextcloudClient) {
        if (!isSuccess) {
            DisplayUtils.showSnackMessage(requireActivity(), getString(R.string.remove_notification_failed))
            fetchAndSetData(client)
        }
    }

    override fun removeNotification(holder: NotificationListAdapter.NotificationViewHolder) {
        adapter?.removeNotification(holder)
        if (adapter?.itemCount == 0) {
            state = NotificationsUIState.Empty
        }
    }

    override fun onRemovedAllNotifications(isSuccess: Boolean) {
        if (isSuccess) {
            adapter?.removeAllNotifications()
            state = NotificationsUIState.Empty
        } else {
            DisplayUtils.showSnackMessage(requireActivity(), getString(R.string.clear_notifications_failed))
        }
    }

    override fun onActionCallback(
        isSuccess: Boolean,
        notification: Notification,
        holder: NotificationListAdapter.NotificationViewHolder
    ) {
        if (isSuccess) {
            adapter?.removeNotification(holder)
        } else {
            adapter?.bindButtons(holder, notification)
            DisplayUtils.showSnackMessage(requireActivity(), getString(R.string.notification_action_failed))
        }
    }

    override fun onBindIcon(imageView: ImageView, url: String) {
        GlideHelper.loadIntoImageView(
            requireContext(),
            client,
            url,
            imageView,
            R.drawable.ic_notification,
            false
        )
    }

    override fun deleteNotification(id: Int) {
        val client = client ?: run {
            Log_OC.e(TAG, "client not initialized")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val result = DeleteNotificationRemoteOperation(id).execute(client)
            withContext(Dispatchers.Main) {
                onRemovedNotification(result?.isSuccess == true, client)
            }
        }
    }

    override fun onActionClick(
        holder: NotificationListAdapter.NotificationViewHolder,
        action: Action,
        notification: Notification
    ) {
        val client = client ?: run {
            Log_OC.e(TAG, "client not initialized")
            return
        }

        NotificationExecuteActionTask(client, holder, notification, this).execute(action)
    }
    // endregion

    companion object {
        private val TAG = NotificationsFragment::class.java.simpleName
        const val EXTRA_INIT_FOR_TESTING = "init_for_testing"
    }
}
