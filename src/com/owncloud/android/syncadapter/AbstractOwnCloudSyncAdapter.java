/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
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
import java.net.UnknownHostException;
import java.util.Date;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.protocol.HttpContext;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.datamodel.DataStorageManager;
import com.owncloud.android.network.OwnCloudClientUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import eu.alefzero.webdav.WebdavClient;

/**
 * Base SyncAdapter for OwnCloud Designed to be subclassed for the concrete
 * SyncAdapter, like ConcatsSync, CalendarSync, FileSync etc..
 * 
 * @author sassman
 * 
 */
public abstract class AbstractOwnCloudSyncAdapter extends
        AbstractThreadedSyncAdapter {

    private AccountManager accountManager;
    private Account account;
    private ContentProviderClient contentProvider;
    private Date lastUpdated;
    private DataStorageManager mStoreManager;

    private WebdavClient mClient = null;

    public AbstractOwnCloudSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
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

    public ContentProviderClient getContentProvider() {
        return contentProvider;
    }

    public void setContentProvider(ContentProviderClient contentProvider) {
        this.contentProvider = contentProvider;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setStorageManager(DataStorageManager storage_manager) {
        mStoreManager = storage_manager;
    }

    public DataStorageManager getStorageManager() {
        return mStoreManager;
    }

    protected ConnectionKeepAliveStrategy getKeepAliveStrategy() {
        return new ConnectionKeepAliveStrategy() {
            public long getKeepAliveDuration(HttpResponse response,
                    HttpContext context) {
                // Change keep alive straategy basing on response: ie
                // forbidden/not found/etc
                // should have keep alive 0
                // default return: 5s
                int statusCode = response.getStatusLine().getStatusCode();

                // HTTP 400, 500 Errors as well as HTTP 118 - Connection timed
                // out
                if ((statusCode >= 400 && statusCode <= 418)
                        || (statusCode >= 421 && statusCode <= 426)
                        || (statusCode >= 500 && statusCode <= 510)
                        || statusCode == 118) {
                    return 0;
                }

                return 5 * 1000;
            }
        };
    }

    protected HttpResponse fireRawRequest(HttpRequest query)
            throws ClientProtocolException, OperationCanceledException,
            AuthenticatorException, IOException {
        /*
         * BasicHttpContext httpContext = new BasicHttpContext(); BasicScheme
         * basicAuth = new BasicScheme();
         * httpContext.setAttribute("preemptive-auth", basicAuth);
         * 
         * HttpResponse response = getClient().execute(mHost, query,
         * httpContext);
         */
        return null;
    }

    protected void initClientForCurrentAccount() throws UnknownHostException {
        if (AccountUtils.constructFullURLForAccount(getContext(), account) == null) {
            throw new UnknownHostException();
        }
        mClient = OwnCloudClientUtils.createOwnCloudClient(account, getContext());
    }
    
    protected WebdavClient getClient() {
        return mClient;
    }
}