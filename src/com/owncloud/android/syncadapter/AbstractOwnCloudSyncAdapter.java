/**
 *   ownCloud Android client application
 *
 *   @author sassman
 *   @author David A. Velasco
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.syncadapter;

import java.io.IOException;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;

/**
 * Base synchronization adapter for ownCloud designed to be subclassed for different
 * resource types, like FileSync, ConcatsSync, CalendarSync, etc..
 * 
 * Implements the standard {@link AbstractThreadedSyncAdapter}.
 */
public abstract class AbstractOwnCloudSyncAdapter extends
        AbstractThreadedSyncAdapter {

    private AccountManager accountManager;
    private Account account;
    private ContentProviderClient mContentProviderClient;
    private FileDataStorageManager mStoreManager;

    private OwnCloudClient mClient = null;

    public AbstractOwnCloudSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        this.setAccountManager(AccountManager.get(context));
    }

    public AbstractOwnCloudSyncAdapter(Context context, boolean autoInitialize,
                                       boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        this.setAccountManager(AccountManager.get(context));
    }

    public AccountManager getAccountManager() {
        return accountManager;
    }

    public void setAccountManager(AccountManager accountManager) {
        this.accountManager = accountManager;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public ContentProviderClient getContentProviderClient() {
        return mContentProviderClient;
    }

    public void setContentProviderClient(ContentProviderClient contentProvider) {
        this.mContentProviderClient = contentProvider;
    }

    public void setStorageManager(FileDataStorageManager storage_manager) {
        mStoreManager = storage_manager;
    }

    public FileDataStorageManager getStorageManager() {
        return mStoreManager;
    }

    protected void initClientForCurrentAccount() throws OperationCanceledException,
            AuthenticatorException, IOException, AccountNotFoundException {
        AccountUtils.constructFullURLForAccount(getContext(), account);
        OwnCloudAccount ocAccount = new OwnCloudAccount(account, getContext());
        mClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                getClientFor(ocAccount, getContext());
    }
    
    protected OwnCloudClient getClient() {
        return mClient;
    }
    
}