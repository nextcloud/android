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
import com.owncloud.android.R
import com.owncloud.android.databinding.NotificationsLayoutBinding
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.notifications.DeleteAllNotificationsRemoteOperation
import com.owncloud.android.lib.resources.notifications.GetNotificationsRemoteOperation
import com.owncloud.android.lib.resources.notifications.models.Notification
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

/**
 * Activity displaying all server side stored notification items.
 */
@Suppress("TooManyFunctions")
class NotificationsFragment :
    Fragment(),
    NotificationsContract.View,
    Injectable {

    private var binding: NotificationsLayoutBinding? = null

    private var adapter: NotificationListAdapter? = null
    private var snackbar: Snackbar? = null
    private var client: NextcloudClient? = null
    private var optionalUser: Optional<User>? = null

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var preferences: AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = NotificationsLayoutBinding.inflate(inflater, container, false)
        val binding = binding!!
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log_OC.v(TAG, "onViewCreated() start")

        setupMenu()
        initUser()
        setupContainingList()
        setupPushWarning()
        setupContent()

        if (optionalUser?.isPresent == false) {
            showError()
        }
    }

    private fun initUser() {
        optionalUser = Optional.of(accountManager.user)
        arguments?.let { bundle ->
            setupUser(bundle)
        }
    }

    private fun setupContainingList() {
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

    private fun setupUser(bundle: Bundle) {
        val accountName = bundle.getString(NotificationWork.KEY_NOTIFICATION_ACCOUNT)

        if (accountName != null && optionalUser?.isPresent == true) {
            val user = optionalUser?.get()
            if (user?.accountName.equals(accountName, ignoreCase = true)) {
                accountManager.setCurrentOwnCloudAccount(accountName)
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

    @Suppress("NestedBlockDepth")
    private fun setupPushWarning() {
        if (!resources.getBoolean(R.bool.show_push_warning)) {
            return
        }

        if (snackbar != null) {
            if (snackbar?.isShown == false) {
                snackbar?.show()
            }
        } else {
            val pushUrl = resources.getString(R.string.push_server_url)

            if (pushUrl.isEmpty() && BuildHelper.isFlavourGPlay()) {
                // branded client without push server
                return
            }

            if (pushUrl.isEmpty()) {
                snackbar = binding?.emptyList?.emptyListView?.let {
                    Snackbar.make(
                        it,
                        R.string.push_notifications_not_implemented,
                        Snackbar.LENGTH_INDEFINITE
                    )
                }
            } else {
                val arbitraryDataProvider = ArbitraryDataProviderImpl(requireActivity())
                val accountName: String = if (optionalUser?.isPresent == true) {
                    optionalUser?.get()?.accountName ?: ""
                } else {
                    ""
                }
                val usesOldLogin = arbitraryDataProvider.getBooleanValue(
                    accountName,
                    UserAccountManager.ACCOUNT_USES_STANDARD_PASSWORD
                )

                if (usesOldLogin) {
                    snackbar = binding?.emptyList?.emptyListView?.let {
                        Snackbar.make(
                            it,
                            R.string.push_notifications_old_login,
                            Snackbar.LENGTH_INDEFINITE
                        )
                    }
                } else {
                    val pushValue = arbitraryDataProvider.getValue(accountName, PushUtils.KEY_PUSH)
                    if (pushValue.isEmpty()) {
                        snackbar = binding?.emptyList?.emptyListView?.let {
                            Snackbar.make(
                                it,
                                R.string.push_notifications_temp_error,
                                Snackbar.LENGTH_INDEFINITE
                            )
                        }
                    }
                }
            }

            if (snackbar != null && snackbar?.isShown == false) {
                snackbar?.show()
            }
        }
    }

    private fun setupContent() {
        binding?.run {
            emptyList.emptyListIcon.setImageResource(R.drawable.ic_notification)
            setLoadingMessageEmpty()
            val layoutManager = LinearLayoutManager(requireContext())
            list.layoutManager = layoutManager
            fetchAndSetData()
        }

    }

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

    private fun fetchAndSetData() {
        lifecycleScope.launch(Dispatchers.IO) {
            initializeAdapter()
            val getRemoteNotificationOperation = GetNotificationsRemoteOperation()
            val result = client?.let { getRemoteNotificationOperation.execute(it) }
            withContext(Dispatchers.Main) {
                if (result?.isSuccess == true && result.resultData != null) {
                    populateList(result.resultData ?: listOf())
                } else {
                    Log_OC.d(TAG, result?.logMessage)
                    setEmptyContent(
                        getString(R.string.notifications_no_results_headline),
                        result?.getLogMessage(requireContext())
                    )
                }
                hideRefreshLayoutLoader()
            }
        }
    }

    private fun initializeClient() {
        if (client == null && optionalUser?.isPresent == true) {
            try {
                val user = optionalUser?.get()
                client = clientFactory.createNextcloudClient(user)
            } catch (e: ClientFactory.CreationException) {
                Log_OC.e(TAG, "Error initializing client", e)
            }
        }
    }

    private fun initializeAdapter() {
        initializeClient()
        if (adapter == null) {
            adapter = NotificationListAdapter(client, this, viewThemeUtils)
            binding?.list?.adapter = adapter
        }
    }

    private fun hideRefreshLayoutLoader() {
        binding?.swipeContainingList?.isRefreshing = false
        binding?.swipeContainingEmpty?.isRefreshing = false
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.activity_notifications, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.action_empty_notifications -> {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val result = DeleteAllNotificationsRemoteOperation().execute(client!!)
                            withContext(Dispatchers.Main) {
                                onRemovedAllNotifications(result.isSuccess)
                            }
                        }

                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
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
            emptyList.emptyListView.visibility = View.VISIBLE
            emptyList.emptyListViewHeadline.text = headline
            emptyList.emptyListViewText.text = message
            emptyList.emptyListIcon.setImageResource(R.drawable.ic_notification)
            emptyList.emptyListViewText.visibility = View.VISIBLE
            emptyList.emptyListIcon.visibility = View.VISIBLE
        }
    }

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

    override fun onActionCallback(isSuccess: Boolean, notification: Notification, holder: NotificationListAdapter.NotificationViewHolder) {
        if (isSuccess) {
            adapter?.removeNotification(holder)
        } else {
            adapter?.setButtons(holder, notification)
            DisplayUtils.showSnackMessage(requireActivity(), getString(R.string.notification_action_failed))
        }
    }

    companion object {
        private val TAG = NotificationsFragment::class.java.simpleName
    }
}