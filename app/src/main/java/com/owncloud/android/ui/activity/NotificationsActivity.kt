/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * @author Mario Danic
 * @author Chris Narkiewicz
 * Copyright (C) 2017 Andy Scherzinger
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.NotificationWork
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.java.util.Optional
import com.owncloud.android.R
import com.owncloud.android.databinding.NotificationsLayoutBinding
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.notifications.GetNotificationsRemoteOperation
import com.owncloud.android.lib.resources.notifications.models.Notification
import com.owncloud.android.ui.adapter.NotificationListAdapter
import com.owncloud.android.ui.adapter.NotificationListAdapter.NotificationViewHolder
import com.owncloud.android.ui.asynctasks.DeleteAllNotificationsTask
import com.owncloud.android.ui.notifications.NotificationsContract
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.PushUtils

/**
 * Activity displaying all server side stored notification items.
 */
class NotificationsActivity : DrawerActivity(), NotificationsContract.View {

    private lateinit var binding: NotificationsLayoutBinding

    private var adapter: NotificationListAdapter? = null
    private var snackbar: Snackbar? = null
    private var client: OwnCloudClient? = null
    private var optionalUser: Optional<User>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log_OC.v(TAG, "onCreate() start")

        super.onCreate(savedInstanceState)

        binding = NotificationsLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        optionalUser = user

        intent?.let {
            it.extras?.let { bundle ->
                setupUser(bundle)
            }
        }

        setupToolbar()
        updateActionBarTitleAndHomeButtonByString(getString(R.string.drawer_item_notifications))
        setupDrawer(R.id.nav_notifications)

        if (optionalUser?.isPresent == false) {
            showError()
        }

        setupContainingList()
        setupPushWarning()
        setupContent()
    }

    private fun setupContainingList() {
        viewThemeUtils.androidx.themeSwipeRefreshLayout(binding.swipeContainingList)
        viewThemeUtils.androidx.themeSwipeRefreshLayout(binding.swipeContainingEmpty)
        binding.swipeContainingList.setOnRefreshListener {
            setLoadingMessage()
            binding.swipeContainingList.isRefreshing = true
            fetchAndSetData()
        }
        binding.swipeContainingEmpty.setOnRefreshListener {
            setLoadingMessageEmpty()
            fetchAndSetData()
        }
    }

    private fun setupUser(bundle: Bundle) {
        val accountName = bundle.getString(NotificationWork.KEY_NOTIFICATION_ACCOUNT)

        if (accountName != null && optionalUser?.isPresent == true) {
            val user = optionalUser?.get()
            if (user?.accountName.equals(accountName, ignoreCase = true)) {
                accountManager.setCurrentOwnCloudAccount(accountName)
                setUser(userAccountManager.user)
                optionalUser = getUser()
            }
        }
    }

    private fun showError() {
        runOnUiThread {
            setEmptyContent(
                getString(R.string.notifications_no_results_headline),
                getString(R.string.account_not_found)
            )
        }
        return
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
            if (pushUrl.isEmpty()) {
                snackbar = Snackbar.make(
                    binding.emptyList.emptyListView,
                    R.string.push_notifications_not_implemented,
                    Snackbar.LENGTH_INDEFINITE
                )
            } else {
                val arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(this)
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
                    snackbar = Snackbar.make(
                        binding.emptyList.emptyListView,
                        R.string.push_notifications_old_login,
                        Snackbar.LENGTH_INDEFINITE
                    )
                } else {
                    val pushValue = arbitraryDataProvider.getValue(accountName, PushUtils.KEY_PUSH)
                    if (pushValue.isEmpty()) {
                        snackbar = Snackbar.make(
                            binding.emptyList.emptyListView,
                            R.string.push_notifications_temp_error,
                            Snackbar.LENGTH_INDEFINITE
                        )
                    }
                }
            }

            if (snackbar != null && snackbar?.isShown == false) {
                snackbar?.show()
            }
        }
    }

    override fun openDrawer() {
        super.openDrawer()
        if (snackbar != null && snackbar?.isShown == true) {
            snackbar?.dismiss()
        }
    }

    override fun closeDrawer() {
        super.closeDrawer()
        setupPushWarning()
    }

    /**
     * sets up the UI elements and loads all notification items.
     */
    private fun setupContent() {
        binding.emptyList.emptyListIcon.setImageResource(R.drawable.ic_notification)
        setLoadingMessageEmpty()
        val layoutManager = LinearLayoutManager(this)
        binding.list.layoutManager = layoutManager
        fetchAndSetData()
    }

    @VisibleForTesting
    fun populateList(notifications: List<Notification>?) {
        initializeAdapter()
        adapter?.setNotificationItems(notifications)
        binding.loadingContent.visibility = View.GONE

        if (notifications?.isNotEmpty() == true) {
            binding.swipeContainingEmpty.visibility = View.GONE
            binding.swipeContainingList.visibility = View.VISIBLE
        } else {
            setEmptyContent(
                getString(R.string.notifications_no_results_headline),
                getString(R.string.notifications_no_results_message)
            )
            binding.swipeContainingList.visibility = View.GONE
            binding.swipeContainingEmpty.visibility = View.VISIBLE
        }
    }

    private fun fetchAndSetData() {
        val t = Thread {
            initializeAdapter()
            val getRemoteNotificationOperation = GetNotificationsRemoteOperation()
            val result = getRemoteNotificationOperation.execute(client)
            if (result.isSuccess && result.resultData != null) {
                runOnUiThread { populateList(result.resultData) }
            } else {
                Log_OC.d(TAG, result.logMessage)
                // show error
                runOnUiThread {
                    setEmptyContent(
                        getString(R.string.notifications_no_results_headline),
                        result.logMessage
                    )
                }
            }
            hideRefreshLayoutLoader()
        }
        t.start()
    }

    private fun initializeClient() {
        if (client == null && optionalUser?.isPresent == true) {
            try {
                val user = optionalUser?.get()
                client = clientFactory.create(user)
            } catch (e: CreationException) {
                Log_OC.e(TAG, "Error initializing client", e)
            }
        }
    }

    private fun initializeAdapter() {
        initializeClient()
        if (adapter == null) {
            adapter = NotificationListAdapter(client, this, viewThemeUtils)
            binding.list.adapter = adapter
        }
    }

    private fun hideRefreshLayoutLoader() {
        runOnUiThread {
            binding.swipeContainingList.isRefreshing = false
            binding.swipeContainingEmpty.isRefreshing = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_notifications, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var retval = true
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            if (isDrawerOpen) {
                closeDrawer()
            } else {
                openDrawer()
            }
        } else if (itemId == R.id.action_empty_notifications) {
            DeleteAllNotificationsTask(client, this).execute()
        } else {
            retval = super.onOptionsItemSelected(item)
        }
        return retval
    }

    private fun setLoadingMessage() {
        binding.swipeContainingEmpty.visibility = View.GONE
    }

    @VisibleForTesting
    fun setLoadingMessageEmpty() {
        binding.swipeContainingList.visibility = View.GONE
        binding.emptyList.emptyListView.visibility = View.GONE
        binding.loadingContent.visibility = View.VISIBLE
    }

    @VisibleForTesting
    fun setEmptyContent(headline: String?, message: String?) {
        binding.swipeContainingList.visibility = View.GONE
        binding.loadingContent.visibility = View.GONE
        binding.swipeContainingEmpty.visibility = View.VISIBLE
        binding.emptyList.emptyListView.visibility = View.VISIBLE
        binding.emptyList.emptyListViewHeadline.text = headline
        binding.emptyList.emptyListViewText.text = message
        binding.emptyList.emptyListIcon.setImageResource(R.drawable.ic_notification)
        binding.emptyList.emptyListViewText.visibility = View.VISIBLE
        binding.emptyList.emptyListIcon.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        setDrawerMenuItemChecked(R.id.nav_notifications)
    }

    override fun onRemovedNotification(isSuccess: Boolean) {
        if (!isSuccess) {
            DisplayUtils.showSnackMessage(this, getString(R.string.remove_notification_failed))
            fetchAndSetData()
        }
    }

    override fun removeNotification(holder: NotificationViewHolder) {
        adapter?.removeNotification(holder)
        if (adapter?.itemCount == 0) {
            setEmptyContent(
                getString(R.string.notifications_no_results_headline),
                getString(R.string.notifications_no_results_message)
            )
            binding.swipeContainingList.visibility = View.GONE
            binding.loadingContent.visibility = View.GONE
            binding.swipeContainingEmpty.visibility = View.VISIBLE
        }
    }

    override fun onRemovedAllNotifications(isSuccess: Boolean) {
        if (isSuccess) {
            adapter?.removeAllNotifications()
            setEmptyContent(
                getString(R.string.notifications_no_results_headline),
                getString(R.string.notifications_no_results_message)
            )
            binding.loadingContent.visibility = View.GONE
            binding.swipeContainingList.visibility = View.GONE
            binding.swipeContainingEmpty.visibility = View.VISIBLE
        } else {
            DisplayUtils.showSnackMessage(this, getString(R.string.clear_notifications_failed))
        }
    }

    override fun onActionCallback(
        isSuccess: Boolean,
        notification: Notification,
        holder: NotificationViewHolder
    ) {
        if (isSuccess) {
            adapter?.removeNotification(holder)
        } else {
            adapter?.setButtons(holder, notification)
            DisplayUtils.showSnackMessage(this, getString(R.string.notification_action_failed))
        }
    }

    companion object {
        private val TAG = NotificationsActivity::class.java.simpleName
    }
}
