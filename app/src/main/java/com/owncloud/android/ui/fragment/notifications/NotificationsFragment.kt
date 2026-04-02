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
import androidx.annotation.VisibleForTesting
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.NotificationWork
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.common.NextcloudClient
import com.nextcloud.utils.BuildHelper
import com.nextcloud.utils.extensions.getTypedActivity
import com.owncloud.android.R
import com.owncloud.android.databinding.NotificationsLayoutBinding
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.notifications.DeleteAllNotificationsRemoteOperation
import com.owncloud.android.lib.resources.notifications.GetNotificationsRemoteOperation
import com.owncloud.android.lib.resources.notifications.models.Notification
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.adapter.NotificationListAdapter
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
    Injectable {

    private var binding: NotificationsLayoutBinding? = null
    private var adapter: NotificationListAdapter? = null
    private var snackbar: Snackbar? = null
    private var client: NextcloudClient? = null
    private var optionalUser: Optional<User>? = null

    @Inject lateinit var viewThemeUtils: ViewThemeUtils

    @Inject lateinit var accountManager: UserAccountManager

    @Inject lateinit var clientFactory: ClientFactory

    @Inject lateinit var preferences: AppPreferences

    // region Lifecycle
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = NotificationsLayoutBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        initUser()
        setupSwipeRefresh()
        setupPushWarning()
        setupContent()
        if (optionalUser?.isPresent == false) showError()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
    // endregion

    // region Setup
    private fun initUser() {
        optionalUser = Optional.of(accountManager.user)
        arguments?.getString(NotificationWork.KEY_NOTIFICATION_ACCOUNT)?.let { accountName ->
            if (optionalUser?.get()?.accountName.equals(accountName, ignoreCase = true)) {
                accountManager.setCurrentOwnCloudAccount(accountName)
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding?.run {
            viewThemeUtils.androidx.themeSwipeRefreshLayout(swipeContainingList)
            viewThemeUtils.androidx.themeSwipeRefreshLayout(swipeContainingEmpty)
            swipeContainingList.setOnRefreshListener {
                setLoadingMessage()
                swipeContainingList.isRefreshing = true
                fetchAndSetData()
            }
            swipeContainingEmpty.setOnRefreshListener {
                setLoadingMessageEmpty()
                fetchAndSetData()
            }
        }
    }

    private fun setupContent() {
        binding?.run {
            emptyList.emptyListIcon.setImageResource(R.drawable.ic_notification)
            setLoadingMessageEmpty()
            list.layoutManager = LinearLayoutManager(requireContext())
            fetchAndSetData()
        }
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.activity_notifications, menu)
                }

                override fun onMenuItemSelected(item: MenuItem): Boolean {
                    if (item.itemId != R.id.action_empty_notifications) return false
                    lifecycleScope.launch(Dispatchers.IO) {
                        val result = DeleteAllNotificationsRemoteOperation().execute(client!!)
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

        if (snackbar?.isShown == false) {
            snackbar?.show()
            return
        }

        val pushUrl = resources.getString(R.string.push_server_url)
        if (pushUrl.isEmpty() && BuildHelper.isFlavourGPlay()) return

        val messageRes = when {
            pushUrl.isEmpty() -> R.string.push_notifications_not_implemented
            isUsingOldLogin() -> R.string.push_notifications_old_login
            isPushValueEmpty() -> R.string.push_notifications_temp_error
            else -> return
        }

        snackbar = binding?.emptyList?.emptyListView?.let {
            Snackbar.make(it, messageRes, Snackbar.LENGTH_INDEFINITE).also { s -> s.show() }
        }
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
    private fun fetchAndSetData() {
        lifecycleScope.launch(Dispatchers.IO) {
            initializeAdapter()
            val result = client?.let { GetNotificationsRemoteOperation().execute(it) }
            withContext(Dispatchers.Main) {
                if (result?.isSuccess == true && result.resultData != null) {
                    populateList(result.resultData ?: listOf())
                } else {
                    try {
                        Log_OC.d(TAG, result?.logMessage)
                        setEmptyContent(
                            getString(R.string.notifications_no_results_headline),
                            result?.getLogMessage(requireContext())
                        )
                    } catch (_: Exception) {
                    }
                }
                hideRefreshLayoutLoader()
            }
        }
    }

    private fun initializeAdapter() {
        lifecycleScope.launch {
            val baseActivity = getTypedActivity(BaseActivity::class.java)
            client = baseActivity?.clientRepository?.getNextcloudClient()

            withContext(Dispatchers.Main) {
                if (adapter == null) {
                    adapter = NotificationListAdapter(client, this@NotificationsFragment, viewThemeUtils)
                    binding?.list?.adapter = adapter
                }
            }
        }
    }

    private fun hideRefreshLayoutLoader() {
        binding?.swipeContainingList?.isRefreshing = false
        binding?.swipeContainingEmpty?.isRefreshing = false
    }
    // endregion

    // region View state
    @VisibleForTesting
    fun populateList(notifications: List<Notification>) {
        initializeAdapter()
        adapter?.setNotificationItems(notifications)
        binding?.run {
            loadingContent.visibility = View.GONE
            if (notifications.isNotEmpty()) {
                swipeContainingEmpty.visibility = View.GONE
                swipeContainingList.visibility = View.VISIBLE
            } else {
                setEmptyContent(
                    getString(R.string.notifications_no_results_headline),
                    getString(R.string.notifications_no_results_message)
                )
                swipeContainingList.visibility = View.GONE
                swipeContainingEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun setLoadingMessage() {
        binding?.swipeContainingEmpty?.visibility = View.GONE
    }

    @VisibleForTesting
    fun setLoadingMessageEmpty() {
        binding?.run {
            swipeContainingList.visibility = View.GONE
            emptyList.emptyListView.visibility = View.GONE
            loadingContent.visibility = View.VISIBLE
        }
    }

    @VisibleForTesting
    fun setEmptyContent(headline: String?, message: String?) {
        binding?.run {
            swipeContainingList.visibility = View.GONE
            loadingContent.visibility = View.GONE
            swipeContainingEmpty.visibility = View.VISIBLE

            emptyList.run {
                emptyListView.visibility = View.VISIBLE
                emptyListViewHeadline.text = headline
                emptyListViewText.text = message
                emptyListIcon.setImageResource(R.drawable.ic_notification)
                emptyListViewText.visibility = View.VISIBLE
                emptyListIcon.visibility = View.VISIBLE
            }
        }
    }

    private fun showError() {
        requireActivity().runOnUiThread {
            setEmptyContent(
                getString(R.string.notifications_no_results_headline),
                getString(R.string.account_not_found)
            )
        }
    }
    // endregion

    // region callbacks
    override fun onRemovedNotification(isSuccess: Boolean) {
        if (!isSuccess) {
            DisplayUtils.showSnackMessage(requireActivity(), getString(R.string.remove_notification_failed))
            fetchAndSetData()
        }
    }

    override fun removeNotification(holder: NotificationListAdapter.NotificationViewHolder) {
        adapter?.removeNotification(holder)
        if (adapter?.itemCount == 0) {
            setEmptyContent(
                getString(R.string.notifications_no_results_headline),
                getString(R.string.notifications_no_results_message)
            )
            binding?.run {
                swipeContainingList.visibility = View.GONE
                loadingContent.visibility = View.GONE
                swipeContainingEmpty.visibility = View.VISIBLE
            }
        }
    }

    override fun onRemovedAllNotifications(isSuccess: Boolean) {
        if (isSuccess) {
            adapter?.removeAllNotifications()
            setEmptyContent(
                getString(R.string.notifications_no_results_headline),
                getString(R.string.notifications_no_results_message)
            )
            binding?.run {
                loadingContent.visibility = View.GONE
                swipeContainingList.visibility = View.GONE
                swipeContainingEmpty.visibility = View.VISIBLE
            }
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
    // endregion

    companion object {
        private val TAG = NotificationsFragment::class.java.simpleName
    }
}
