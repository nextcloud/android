/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsetsController
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.NotificationWork
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.R
import com.owncloud.android.databinding.NotificationsLayoutBinding
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.notifications.GetNotificationsRemoteOperation
import com.owncloud.android.lib.resources.notifications.models.Notification
import com.owncloud.android.ui.adapter.NotificationListAdapter
import com.owncloud.android.ui.adapter.NotificationListAdapter.NotificationViewHolder
import com.owncloud.android.ui.asynctasks.DeleteAllNotificationsTask
import com.owncloud.android.ui.notifications.NotificationsContract
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.PushUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.util.Optional
import javax.inject.Inject

/**
 * Activity displaying all server side stored notification items.
 */
@Suppress("TooManyFunctions")
class NotificationsActivity :
    AppCompatActivity(),
    NotificationsContract.View,
    Injectable {

    lateinit var binding: NotificationsLayoutBinding

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

    override fun onCreate(savedInstanceState: Bundle?) {
        Log_OC.v(TAG, "onCreate() start")

        super.onCreate(savedInstanceState)

        binding = NotificationsLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar()
        setupStatusBar()
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
        intent?.let {
            it.extras?.let { bundle ->
                setupUser(bundle)
            }
        }
    }

    private fun setupActionBar() {
        setSupportActionBar(findViewById(R.id.toolbar_back_button))
        supportActionBar?.apply {
            setTitle(R.string.drawer_item_notifications)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_arrow_back_foreground)
        }
    }

    private fun setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appearanceLightStatusBars = if (preferences.isDarkModeEnabled) {
                0
            } else {
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            }
            window.insetsController?.setSystemBarsAppearance(
                appearanceLightStatusBars,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (preferences.isDarkModeEnabled) {
                0
            } else {
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
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
            val result = client?.let { getRemoteNotificationOperation.execute(it) }
            if (result?.isSuccess == true && result.resultData != null) {
                runOnUiThread { populateList(result.resultData) }
            } else {
                Log_OC.d(TAG, result?.logMessage)
                // show error
                runOnUiThread {
                    setEmptyContent(
                        getString(R.string.notifications_no_results_headline),
                        result?.logMessage
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
                client = clientFactory.createNextcloudClient(user)
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
        when (itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
            R.id.action_empty_notifications -> {
                DeleteAllNotificationsTask(client, this).execute()
            }
            else -> {
                retval = super.onOptionsItemSelected(item)
            }
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

    override fun onActionCallback(isSuccess: Boolean, notification: Notification, holder: NotificationViewHolder) {
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
