/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Chawki Chouib <chouibc@gmail.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2016-2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.activity

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.accounts.OperationCanceledException
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.collect.Sets
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.onboarding.FirstRunActivity
import com.nextcloud.model.WorkerState
import com.nextcloud.model.WorkerState.DownloadStarted
import com.nextcloud.model.WorkerStateLiveData
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.mdm.MDMConfig.multiAccountSupport
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.services.OperationsService.OperationsServiceBinder
import com.owncloud.android.ui.adapter.UserListAdapter
import com.owncloud.android.ui.adapter.UserListItem
import com.owncloud.android.ui.dialog.AccountRemovalDialog.Companion.newInstance
import com.owncloud.android.ui.events.AccountRemovedEvent
import com.owncloud.android.ui.helpers.FileOperationsHelper
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * An Activity that allows the user to manage accounts.
 */
class ManageAccountsActivity :
    FileActivity(),
    UserListAdapter.Listener,
    AccountManagerCallback<Boolean?>,
    ComponentsGetter,
    UserListAdapter.ClickListener {

    private var recyclerView: RecyclerView? = null
    private val handler = Handler()
    private var accountName: String? = null
    private var userListAdapter: UserListAdapter? = null
    private var originalUsers: Set<String>? = null
    private var originalCurrentUser: String? = null

    private var multipleAccountsSupported = false

    private var workerAccountName: String? = null
    private var workerCurrentDownload: DownloadFileOperation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.accounts_layout)

        setupToolbar()
        setupActionBar()
        setupUsers()

        @Suppress("DEPRECATION")
        arbitraryDataProvider = ArbitraryDataProviderImpl(this)
        multipleAccountsSupported = multiAccountSupport(this)

        setupUserList()
        handleBackPress()
    }

    private fun setupUsers() {
        val users = accountManager.allUsers
        originalUsers = toAccountNames(users)

        user.ifPresent {
            originalCurrentUser = user.get().accountName
        }
    }

    private fun setupActionBar() {
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
            viewThemeUtils.files.themeActionBar(this, it, R.string.prefs_manage_accounts)
        }
    }

    private fun setupUserList() {
        userListAdapter = UserListAdapter(
            this,
            accountManager,
            userListItems,
            this,
            multipleAccountsSupported,
            true,
            true,
            viewThemeUtils
        )

        recyclerView = findViewById(R.id.account_list)
        recyclerView?.setAdapter(userListAdapter)
        recyclerView?.setLayoutManager(LinearLayoutManager(this))
        observeWorkerState()
    }

    @Suppress("ReturnCount")
    @Deprecated("Use ActivityResultLauncher")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != KEY_DELETE_CODE || data == null) {
            return
        }

        val bundle = data.extras
        if (bundle == null || !bundle.containsKey(UserInfoActivity.KEY_ACCOUNT)) {
            return
        }

        val account = bundle.getParcelableArgument(UserInfoActivity.KEY_ACCOUNT, Account::class.java) ?: return
        val user = accountManager.getUser(account.name).orElseThrow { RuntimeException() }
        accountName = account.name
        performAccountRemoval(user)
    }

    private fun handleBackPress() {
        onBackPressedDispatcher.addCallback(
            this,
            onBackPressedCallback
        )
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val resultIntent = Intent()

            if (accountManager.allUsers.isNotEmpty()) {
                resultIntent.putExtra(KEY_ACCOUNT_LIST_CHANGED, hasAccountListChanged())
                resultIntent.putExtra(KEY_CURRENT_ACCOUNT_CHANGED, hasCurrentAccountChanged())
                setResult(RESULT_OK, resultIntent)
            } else {
                val intent = Intent(this@ManageAccountsActivity, AuthenticatorActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
            }

            finish()
        }
    }

    /**
     * checks the set of actual accounts against the set of original accounts when the activity has been started.
     *
     * @return true if account list has changed, false if not
     */
    private fun hasAccountListChanged(): Boolean {
        val users = accountManager.allUsers
        val newList: MutableList<User> = ArrayList()
        for (user in users) {
            val pendingForRemoval = arbitraryDataProvider.getBooleanValue(user, PENDING_FOR_REMOVAL)

            if (!pendingForRemoval) {
                newList.add(user)
            }
        }
        val actualAccounts = toAccountNames(newList)
        return originalUsers != actualAccounts
    }

    /**
     * checks actual current account against current accounts when the activity has been started.
     *
     * @return true if account list has changed, false if not
     */
    private fun hasCurrentAccountChanged(): Boolean {
        val user = userAccountManager.user
        return if (user.isAnonymous) {
            true
        } else {
            user.accountName != originalCurrentUser
        }
    }

    private val userListItems: List<UserListItem>
        get() {
            val users = accountManager.allUsers
            val userListItems: MutableList<UserListItem> =
                ArrayList(users.size)
            for (user in users) {
                val pendingForRemoval =
                    arbitraryDataProvider.getBooleanValue(user, PENDING_FOR_REMOVAL)
                userListItems.add(UserListItem(user, !pendingForRemoval))
            }

            if (multiAccountSupport(this)) {
                userListItems.add(UserListItem())
            }

            return userListItems
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var result = true

        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
        } else {
            result = super.onOptionsItemSelected(item)
        }

        return result
    }

    override fun showFirstRunActivity() {
        val intent = Intent(applicationContext, FirstRunActivity::class.java).apply {
            putExtra(FirstRunActivity.EXTRA_ALLOW_CLOSE, true)
        }
        startActivity(intent)
    }

    @Suppress("TooGenericExceptionCaught")
    @SuppressLint("NotifyDataSetChanged")
    override fun startAccountCreation() {
        val am = AccountManager.get(applicationContext)
        am.addAccount(
            MainApp.getAccountType(this),
            null,
            null,
            null,
            this,
            { future: AccountManagerFuture<Bundle>? ->
                if (future != null) {
                    try {
                        val result = future.result
                        val name = result.getString(AccountManager.KEY_ACCOUNT_NAME)
                        accountManager.setCurrentOwnCloudAccount(name)
                        userListAdapter = UserListAdapter(
                            this,
                            accountManager,
                            userListItems,
                            this,
                            multipleAccountsSupported,
                            false,
                            true,
                            viewThemeUtils
                        )
                        recyclerView?.adapter = userListAdapter
                        runOnUiThread { userListAdapter?.notifyDataSetChanged() }
                    } catch (e: OperationCanceledException) {
                        Log_OC.d(TAG, "Account creation canceled")
                    } catch (e: Exception) {
                        Log_OC.e(TAG, "Account creation finished in exception: ", e)
                    }
                }
            },
            handler
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onAccountRemovedEvent(event: AccountRemovedEvent) {
        val userListItemArray = userListItems
        userListAdapter?.clear()
        userListAdapter?.addAll(userListItemArray)
        userListAdapter?.notifyDataSetChanged()
    }

    override fun run(future: AccountManagerFuture<Boolean?>) {
        if (!future.isDone) {
            return
        }

        // after remove account
        accountName?.let {
            val user = accountManager.getUser(it)

            if (!user.isPresent) {
                fileUploadHelper.cancel(it)
                FileDownloadHelper.instance().cancelAllDownloadsForAccount(workerAccountName, workerCurrentDownload)
            }
        }

        val currentUser = userAccountManager.user
        if (currentUser.isAnonymous) {
            var accountName = ""
            val users = accountManager.allUsers
            if (users.size > 0) {
                accountName = users[0].accountName
            }
            accountManager.setCurrentOwnCloudAccount(accountName)
        }

        val userListItemArray = userListItems
        if (userListItemArray.size > SINGLE_ACCOUNT) {
            userListAdapter = UserListAdapter(
                this,
                accountManager,
                userListItemArray,
                this,
                multipleAccountsSupported,
                false,
                true,
                viewThemeUtils
            )
            recyclerView?.adapter = userListAdapter
        } else {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun getHandler(): Handler = handler

    override fun getOperationsServiceBinder(): OperationsServiceBinder? = null

    override fun getStorageManager(): FileDataStorageManager = super.getStorageManager()

    override fun getFileOperationsHelper(): FileOperationsHelper? = null

    @Suppress("DEPRECATION")
    @SuppressLint("NotifyDataSetChanged")
    private fun performAccountRemoval(user: User) {
        val itemCount = userListAdapter?.itemCount ?: 0

        // disable account in recycler view
        for (i in 0 until itemCount) {
            val item = userListAdapter?.getItem(i)

            if (item != null && item.user.accountName.equals(user.accountName, ignoreCase = true)) {
                item.isEnabled = false
                break
            }

            userListAdapter?.notifyDataSetChanged()
        }

        // store pending account removal
        val arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(this)
        arbitraryDataProvider.storeOrUpdateKeyValue(user.accountName, PENDING_FOR_REMOVAL, true.toString())

        FileDownloadHelper.instance().cancelAllDownloadsForAccount(workerAccountName, workerCurrentDownload)
        fileUploadHelper.cancel(user.accountName)
        backgroundJobManager.startAccountRemovalJob(user.accountName, false)

        // immediately select a new account
        val users = accountManager.allUsers

        var newAccountName = ""
        for (u in users) {
            if (!u.accountName.equals(u.accountName, ignoreCase = true)) {
                newAccountName = u.accountName
                break
            }
        }

        if (newAccountName.isEmpty()) {
            Log_OC.d(TAG, "new account set to null")
            accountManager.resetOwnCloudAccount()
        } else {
            Log_OC.d(TAG, "new account set to: $newAccountName")
            accountManager.setCurrentOwnCloudAccount(newAccountName)
        }

        // only one to be (deleted) account remaining
        if (users.size < MIN_MULTI_ACCOUNT_SIZE) {
            val resultIntent = Intent()
            resultIntent.putExtra(KEY_ACCOUNT_LIST_CHANGED, true)
            resultIntent.putExtra(KEY_CURRENT_ACCOUNT_CHANGED, true)
            setResult(RESULT_OK, resultIntent)
            onBackPressedDispatcher.onBackPressed()
        }
    }

    @Suppress("DEPRECATION")
    private fun openAccount(user: User) {
        val intent = Intent(this, UserInfoActivity::class.java).apply {
            putExtra(UserInfoActivity.KEY_ACCOUNT, user)

            val oca = user.toOwnCloudAccount()
            putExtra(UserListAdapter.KEY_DISPLAY_NAME, oca.displayName)
        }

        startActivityForResult(intent, UserListAdapter.KEY_USER_INFO_REQUEST_CODE)
    }

    @Suppress("DEPRECATION")
    @VisibleForTesting
    fun showUser(user: User, userInfo: UserInfo?) {
        val intent = Intent(this, UserInfoActivity::class.java).apply {
            val oca = user.toOwnCloudAccount()
            putExtra(UserInfoActivity.KEY_ACCOUNT, user)
            putExtra(UserListAdapter.KEY_DISPLAY_NAME, oca.displayName)
            putExtra(UserInfoActivity.KEY_USER_DATA, userInfo)
        }

        startActivityForResult(intent, UserListAdapter.KEY_USER_INFO_REQUEST_CODE)
    }

    override fun onOptionItemClicked(user: User, view: View) {
        if (view.id == R.id.account_menu) {
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.item_account, popup.menu)

            if (accountManager.user == user) {
                popup.menu.findItem(R.id.action_open_account).setVisible(false)
            }

            popup.setOnMenuItemClickListener { item: MenuItem ->
                val itemId = item.itemId
                when (itemId) {
                    R.id.action_open_account -> {
                        accountClicked(user.hashCode())
                    }
                    R.id.action_delete_account -> {
                        openAccountRemovalDialog(user, supportFragmentManager)
                    }
                    else -> {
                        openAccount(user)
                    }
                }
                true
            }

            popup.show()
        } else {
            openAccount(user)
        }
    }

    private fun observeWorkerState() {
        WorkerStateLiveData.instance().observe(
            this
        ) { state: WorkerState? ->
            if (state is DownloadStarted) {
                Log_OC.d(TAG, "Download worker started")
                workerAccountName = state.user?.accountName
                workerCurrentDownload = state.currentDownload
            }
        }
    }

    override fun onAccountClicked(user: User) {
        openAccount(user)
    }

    companion object {
        private val TAG: String = ManageAccountsActivity::class.java.simpleName

        const val KEY_ACCOUNT_LIST_CHANGED: String = "ACCOUNT_LIST_CHANGED"
        const val KEY_CURRENT_ACCOUNT_CHANGED: String = "CURRENT_ACCOUNT_CHANGED"
        const val PENDING_FOR_REMOVAL: String = UserAccountManager.PENDING_FOR_REMOVAL

        private const val KEY_DELETE_CODE = 101
        private const val SINGLE_ACCOUNT = 1
        private const val MIN_MULTI_ACCOUNT_SIZE = 2

        private fun toAccountNames(users: Collection<User>): Set<String> {
            val accountNames: MutableSet<String> = Sets.newHashSetWithExpectedSize(users.size)
            for (user in users) {
                accountNames.add(user.accountName)
            }
            return accountNames
        }

        fun openAccountRemovalDialog(user: User, fragmentManager: FragmentManager) {
            val dialog = newInstance(user)
            dialog.show(fragmentManager, "dialog")
        }
    }
}
