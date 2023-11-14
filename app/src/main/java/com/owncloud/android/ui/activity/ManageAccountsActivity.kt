/*
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * @author Chris Narkiewicz  <hello@ezaquarii.com>
 * @author Chawki Chouib  <chouibc@gmail.com>
 * Copyright (C) 2016 ownCloud Inc.
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2020 Chawki Chouib  <chouibc@gmail.com>
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.activity

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.accounts.OperationCanceledException
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.MenuItem
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.common.collect.Sets
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.onboarding.FirstRunActivity
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.databinding.AccountsLayoutBinding
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.files.services.FileDownloader
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.services.OperationsService.OperationsServiceBinder
import com.owncloud.android.ui.adapter.UserListAdapter
import com.owncloud.android.ui.adapter.UserListItem
import com.owncloud.android.ui.dialog.AccountRemovalConfirmationDialog.Companion.newInstance
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

    private val handler = Handler(Looper.getMainLooper())
    private var accountName: String? = null
    private var userListAdapter: UserListAdapter? = null
    private var downloadServiceConnection: ServiceConnection? = null
    private var uploadServiceConnection: ServiceConnection? = null
    private var originalUsers: Set<String>? = null
    private var originalCurrentUser: String? = null
    private var arbitraryDataProvider: ArbitraryDataProvider? = null
    private var multipleAccountsSupported = false

    private lateinit var binding: AccountsLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = AccountsLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupActionBar()
        setupUsers()

        arbitraryDataProvider = ArbitraryDataProviderImpl(this)
        multipleAccountsSupported = resources.getBoolean(R.bool.multiaccount_support)

        setupAccountList()
        initializeComponentGetters()
    }

    private fun setupActionBar() {
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
            viewThemeUtils.files.themeActionBar(this, it, R.string.prefs_manage_accounts)
        }
    }

    private fun setupUsers() {
        val users = accountManager.allUsers
        originalUsers = toAccountNames(users)
        val currentUser = user
        if (currentUser.isPresent) {
            originalCurrentUser = currentUser.get().accountName
        }
    }

    private fun setupAccountList() {
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
        binding.accountList.adapter = userListAdapter
        binding.accountList.layoutManager = LinearLayoutManager(this)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == KEY_DELETE_CODE && data != null) {
            val bundle = data.extras
            if (bundle != null && bundle.containsKey(UserInfoActivity.KEY_ACCOUNT)) {
                val account = bundle.getParcelable<Account>(UserInfoActivity.KEY_ACCOUNT)
                if (account != null) {
                    val user = accountManager.getUser(account.name).orElseThrow { RuntimeException() }
                    accountName = account.name
                    performAccountRemoval(user)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val resultIntent = Intent()
        if (accountManager.allUsers.size > 0) {
            resultIntent.putExtra(KEY_ACCOUNT_LIST_CHANGED, hasAccountListChanged())
            resultIntent.putExtra(KEY_CURRENT_ACCOUNT_CHANGED, hasCurrentAccountChanged())
            setResult(RESULT_OK, resultIntent)
            super.onBackPressed()
        } else {
            val intent = Intent(applicationContext, AuthenticatorActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
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
            val pendingForRemoval = arbitraryDataProvider?.getBooleanValue(user, PENDING_FOR_REMOVAL)
            if (pendingForRemoval == false) {
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

    /**
     * Initialize ComponentsGetters.
     */
    private fun initializeComponentGetters() {
        downloadServiceConnection = newTransferenceServiceConnection()
        downloadServiceConnection?.let {
            bindService(
                Intent(this, FileDownloader::class.java),
                it,
                BIND_AUTO_CREATE
            )
        }

        uploadServiceConnection = newTransferenceServiceConnection()
        uploadServiceConnection?.let {
            bindService(
                Intent(this, FileUploader::class.java),
                it,
                BIND_AUTO_CREATE
            )
        }
    }

    private val userListItems: List<UserListItem>
        get() {
            val users = accountManager.allUsers
            val userListItems: MutableList<UserListItem> = ArrayList(users.size)
            for (user in users) {
                val pendingForRemoval = arbitraryDataProvider?.getBooleanValue(user, PENDING_FOR_REMOVAL)
                pendingForRemoval?.let {
                    userListItems.add(UserListItem(user, !it))
                }
            }
            if (resources.getBoolean(R.bool.multiaccount_support)) {
                userListItems.add(UserListItem())
            }
            return userListItems
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var retval = true
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        } else {
            retval = super.onOptionsItemSelected(item)
        }
        return retval
    }

    override fun showFirstRunActivity() {
        val firstRunIntent = Intent(applicationContext, FirstRunActivity::class.java)
        firstRunIntent.putExtra(FirstRunActivity.EXTRA_ALLOW_CLOSE, true)
        startActivity(firstRunIntent)
    }

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
                        binding.accountList.adapter = userListAdapter
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
        if (future.isDone) {
            // after remove account
            val user = accountManager.getUser(accountName)
            if (!user.isPresent) {
                // Cancel transfers of the removed account
                mUploaderBinder?.cancel(accountName)
                mDownloaderBinder?.cancel(accountName)
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

                binding.accountList.adapter = userListAdapter
            } else {
                onBackPressed()
            }
        }
    }

    override fun onDestroy() {
        if (downloadServiceConnection != null) {
            unbindService(downloadServiceConnection!!)
            downloadServiceConnection = null
        }
        if (uploadServiceConnection != null) {
            unbindService(uploadServiceConnection!!)
            uploadServiceConnection = null
        }
        super.onDestroy()
    }

    override fun getHandler(): Handler {
        return handler
    }

    override fun getFileUploaderBinder(): FileUploaderBinder {
        return mUploaderBinder
    }

    override fun getOperationsServiceBinder(): OperationsServiceBinder? {
        return null
    }

    override fun getStorageManager(): FileDataStorageManager {
        return super.getStorageManager()
    }

    override fun getFileOperationsHelper(): FileOperationsHelper? {
        return null
    }

    override fun newTransferenceServiceConnection(): ServiceConnection {
        return ManageAccountsServiceConnection()
    }

    private fun performAccountRemoval(user: User) {
        disableAccountInList(user)
        storePendingAccountRemoval(user)
        cancelTransfers(user)

        val users = accountManager.allUsers
        selectNewAccount(users)

        // only one to be (deleted) account remaining
        if (users.size < MIN_MULTI_ACCOUNT_SIZE) {
            val resultIntent = Intent()
            resultIntent.putExtra(KEY_ACCOUNT_LIST_CHANGED, true)
            resultIntent.putExtra(KEY_CURRENT_ACCOUNT_CHANGED, true)
            setResult(RESULT_OK, resultIntent)
            super.onBackPressed()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun disableAccountInList(user: User) {
        for (i in 0 until userListAdapter!!.itemCount) {
            val item = userListAdapter?.getItem(i)
            if (item != null && item.user.accountName.equals(user.accountName, ignoreCase = true)) {
                item.isEnabled = false
                break
            }
            userListAdapter?.notifyDataSetChanged()
        }
    }

    private fun storePendingAccountRemoval(user: User) {
        val arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(this)
        arbitraryDataProvider.storeOrUpdateKeyValue(user.accountName, PENDING_FOR_REMOVAL, true.toString())
    }

    private fun cancelTransfers(user: User) {
        mUploaderBinder?.cancel(user)
        mDownloaderBinder?.cancel(user.accountName)
        backgroundJobManager.startAccountRemovalJob(user.accountName, false)
    }

    private fun selectNewAccount(users: MutableList<User>) {
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
    }

    private fun openAccount(user: User) {
        val intent = Intent(this, UserInfoActivity::class.java)
        intent.putExtra(UserInfoActivity.KEY_ACCOUNT, user)
        val oca = user.toOwnCloudAccount()
        intent.putExtra(UserListAdapter.KEY_DISPLAY_NAME, oca.displayName)
        startActivityForResult(intent, UserListAdapter.KEY_USER_INFO_REQUEST_CODE)
    }

    @VisibleForTesting
    fun showUser(user: User, userInfo: UserInfo?) {
        val intent = Intent(this, UserInfoActivity::class.java)
        val oca = user.toOwnCloudAccount()
        intent.putExtra(UserInfoActivity.KEY_ACCOUNT, user)
        intent.putExtra(UserListAdapter.KEY_DISPLAY_NAME, oca.displayName)
        intent.putExtra(UserInfoActivity.KEY_USER_DATA, userInfo)
        startActivityForResult(intent, UserListAdapter.KEY_USER_INFO_REQUEST_CODE)
    }

    override fun onOptionItemClicked(user: User, view: View) {
        if (view.id == R.id.account_menu) {
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.item_account, popup.menu)
            if (accountManager.user == user) {
                popup.menu.findItem(R.id.action_open_account).isVisible = false
            }
            popup.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.action_open_account -> {
                        accountClicked(user.hashCode())
                    }

                    R.id.action_delete_account -> {
                        openAccountRemovalConfirmationDialog(user, supportFragmentManager)
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

    override fun onAccountClicked(user: User) {
        openAccount(user)
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private inner class ManageAccountsServiceConnection : ServiceConnection {
        override fun onServiceConnected(component: ComponentName, service: IBinder) {
            if (component == ComponentName(this@ManageAccountsActivity, FileDownloader::class.java)) {
                mDownloaderBinder = service as FileDownloaderBinder
            } else if (component == ComponentName(this@ManageAccountsActivity, FileUploader::class.java)) {
                Log_OC.d(TAG, "Upload service connected")
                mUploaderBinder = service as FileUploaderBinder
            }
        }

        override fun onServiceDisconnected(component: ComponentName) {
            if (component == ComponentName(this@ManageAccountsActivity, FileDownloader::class.java)) {
                Log_OC.d(TAG, "Download service suddenly disconnected")
                mDownloaderBinder = null
            } else if (component == ComponentName(this@ManageAccountsActivity, FileUploader::class.java)) {
                Log_OC.d(TAG, "Upload service suddenly disconnected")
                mUploaderBinder = null
            }
        }
    }

    companion object {
        private val TAG = ManageAccountsActivity::class.java.simpleName

        const val KEY_ACCOUNT_LIST_CHANGED = "ACCOUNT_LIST_CHANGED"
        const val KEY_CURRENT_ACCOUNT_CHANGED = "CURRENT_ACCOUNT_CHANGED"
        const val PENDING_FOR_REMOVAL = UserAccountManager.PENDING_FOR_REMOVAL
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

        fun openAccountRemovalConfirmationDialog(user: User?, fragmentManager: FragmentManager?) {
            user?.let {
                val dialog = newInstance(user)
                fragmentManager?.let {
                    dialog.show(it, "dialog")
                }
            }
        }
    }
}
