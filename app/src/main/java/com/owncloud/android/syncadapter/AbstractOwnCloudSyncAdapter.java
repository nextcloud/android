/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2011-2012 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-FileCopyrightText: 2011 Sven Aßmann <sven.assmann@lubico.biz>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.syncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;

import java.io.IOException;

/**
 * Base synchronization adapter for Nextcloud designed to be subclassed for different
 * resource types, like FileSync, ContactsSync, CalendarSync, etc.
 * Implements the standard {@link AbstractThreadedSyncAdapter}.
 */
abstract class AbstractOwnCloudSyncAdapter extends
        AbstractThreadedSyncAdapter {

    private AccountManager accountManager;
    private Account account;
    private ContentProviderClient contentProviderClient;
    private FileDataStorageManager storageManager;
    private final UserAccountManager userAccountManager;
    private OwnCloudClient client;

    AbstractOwnCloudSyncAdapter(Context context, boolean autoInitialize, UserAccountManager userAccountManager) {
        super(context, autoInitialize);
        this.setAccountManager(AccountManager.get(context));
        this.userAccountManager = userAccountManager;
    }

    AbstractOwnCloudSyncAdapter(Context context,
                                boolean autoInitialize,
                                boolean allowParallelSyncs,
                                UserAccountManager userAccountManager) {
        super(context, autoInitialize, allowParallelSyncs);
        this.setAccountManager(AccountManager.get(context));
        this.userAccountManager = userAccountManager;
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

    public User getUser() {
        Account account = getAccount();
        String accountName = account != null ? account.name : null;
        User user = userAccountManager.getUser(accountName);
        if (user == null) {
            user = userAccountManager.getAnonymousUser();
        }
        return user;
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
