/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.network;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import com.nextcloud.client.account.User;
import com.nextcloud.common.NextcloudClient;
import com.nextcloud.common.PlainClient;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.accounts.AccountUtils;

import java.io.IOException;

class ClientFactoryImpl implements ClientFactory {

    private Context context;

    ClientFactoryImpl(Context context) {
        this.context = context;
    }

    @Override
    public OwnCloudClient create(User user) throws CreationException {
        try {
            return OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(user.toOwnCloudAccount(), context);
        } catch (OperationCanceledException|
                 AuthenticatorException|
            IOException e) {
            throw new CreationException(e);
        }
    }

    @Override
    public NextcloudClient createNextcloudClient(User user) throws CreationException {
        try {
            return OwnCloudClientFactory.createNextcloudClient(user.toPlatformAccount(), context);
        } catch (AccountUtils.AccountNotFoundException e) {
            throw new CreationException(e);
        }
    }

    @Override
    public OwnCloudClient create(Account account)
        throws OperationCanceledException, AuthenticatorException, IOException,
        AccountUtils.AccountNotFoundException {
        return OwnCloudClientFactory.createOwnCloudClient(account, context);
    }

    @Override
    public OwnCloudClient create(Account account, Activity currentActivity)
        throws OperationCanceledException, AuthenticatorException, IOException,
        AccountUtils.AccountNotFoundException {
        return OwnCloudClientFactory.createOwnCloudClient(account, context, currentActivity);
    }

    @Override
    public OwnCloudClient create(Uri uri, boolean followRedirects, boolean useNextcloudUserAgent) {
        return OwnCloudClientFactory.createOwnCloudClient(uri, context, followRedirects);
    }

    @Override
    public OwnCloudClient create(Uri uri, boolean followRedirects) {
        return OwnCloudClientFactory.createOwnCloudClient(uri, context, followRedirects);
    }

    @Override
    public PlainClient createPlainClient() {
        return new PlainClient(context);
    }
}
