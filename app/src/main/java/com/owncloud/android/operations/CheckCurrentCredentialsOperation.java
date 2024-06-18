/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2016 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations;

import com.nextcloud.client.account.User;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;
import com.owncloud.android.operations.common.SyncOperation;

import java.util.ArrayList;

/**
 * Checks validity of currently stored credentials for a given OC account
 */
public class CheckCurrentCredentialsOperation extends SyncOperation {

    private final User user;

    public CheckCurrentCredentialsOperation(User user, FileDataStorageManager storageManager) {
        super(storageManager);
        this.user = user;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        boolean validAccount = user.nameEquals(getStorageManager().getUser().getAccountName());
        if (!validAccount) {
            result = new RemoteOperationResult(new IllegalStateException(
                "Account to validate is not the account connected to!")
            );
        } else {
            RemoteOperation check = new ExistenceCheckRemoteOperation(OCFile.ROOT_PATH, false);
            result = check.execute(client);
            ArrayList<Object> data = new ArrayList<>();
            data.add(user.toPlatformAccount());
            result.setData(data);
        }

        return result;
    }

}
