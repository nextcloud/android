/*
 *   ownCloud Android client application
 *
 *   @author sassman
 *   @author David A. Velasco
 *   @author Andy Scherzinger
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
 *   Copyright (C) 2019 Andy Scherzinger
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
 */

package com.owncloud.android.syncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;

import java.io.IOException;

/**
 * Base synchronization adapter for ownCloud designed to be subclassed for different
 * resource types, like FileSync, ConcatsSync, CalendarSync, etc..
 *
 * Implements the standard {@link AbstractThreadedSyncAdapter}.
 */
abstract class AbstractOwnCloudSyncAdapter extends
        AbstractThreadedSyncAdapter {

    private AccountManager accountManager;
    private Account account;
    private ContentProviderClient contentProviderClient;
    private FileDataStorageManager storageManager;

    private OwnCloudClient client;

    AbstractOwnCloudSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        this.setAccountManager(AccountManager.get(context));
    }

    AbstractOwnCloudSyncAdapter(Context context, boolean autoInitialize,
                                       boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        this.setAccountManager(AccountManager.get(context));
    }

    void initClientForCurrentAccount() throws OperationCanceledException,
            AuthenticatorException, IOException, AccountNotFoundException {
        OwnCloudAccount ocAccount = new OwnCloudAccount(account, getContext());
        client = OwnCloudClientManagerFactory.getDefaultSingleton().
                getClientFor(ocAccount, getContext());
    }

    public AccountManager getAccountManager() {
        return this.accountManager;
    }

    public Account getAccount() {
        return this.account;
    }

    public ContentProviderClient getContentProviderClient() {
        return this.contentProviderClient;
    }

    public FileDataStorageManager getStorageManager() {
        return this.storageManager;
    }

    protected OwnCloudClient getClient() {
        return this.client;
    }

    public void setAccountManager(AccountManager accountManager) {
        this.accountManager = accountManager;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public void setContentProviderClient(ContentProviderClient contentProviderClient) {
        this.contentProviderClient = contentProviderClient;
    }

    public void setStorageManager(FileDataStorageManager storageManager) {
        this.storageManager = storageManager;
    }
}
