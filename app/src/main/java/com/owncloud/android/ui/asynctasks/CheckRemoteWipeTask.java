/*
 *
 *  * Nextcloud Android client application
 *  *
 *  * @author Tobias Kaminsky
 *  * Copyright (C) 2019 Tobias Kaminsky
 *  * Copyright (C) 2019 Nextcloud GmbH
 *  *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 *
 */

package com.owncloud.android.ui.asynctasks;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.os.AsyncTask;

import com.nextcloud.client.jobs.BackgroundJobManager;
import com.owncloud.android.lib.common.accounts.AccountTypeUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.users.CheckRemoteWipeRemoteOperation;
import com.owncloud.android.ui.activity.FileActivity;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class CheckRemoteWipeTask extends AsyncTask<Void, Void, Boolean> {
    private BackgroundJobManager backgroundJobManager;
    private Account account;
    private WeakReference<FileActivity> fileActivityWeakReference;

    public CheckRemoteWipeTask(BackgroundJobManager backgroundJobManager,
                               Account account,
                               WeakReference<FileActivity> fileActivityWeakReference) {
        this.backgroundJobManager = backgroundJobManager;
        this.account = account;
        this.fileActivityWeakReference = fileActivityWeakReference;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        final FileActivity fileActivity = fileActivityWeakReference.get();

        if (fileActivity == null) {
            Log_OC.e(this, "Check for remote wipe: no context available");
            return Boolean.FALSE;
        }

        String password;
        try {
            AccountManager am = AccountManager.get(fileActivity);
            password = am.blockingGetAuthToken(account, AccountTypeUtils.getAuthTokenTypePass(account.type), false);
        } catch (AuthenticatorException | IOException | OperationCanceledException e) {
            return Boolean.FALSE;
        }

        RemoteOperationResult<Void> checkWipeResult = new CheckRemoteWipeRemoteOperation(password).executeNextcloudClient(account, fileActivity);

        if (checkWipeResult.isSuccess()) {
            backgroundJobManager.startAccountRemovalJob(account.name, true);
        } else {
            Log_OC.e(this, "Check for remote wipe not needed -> update credentials");
            fileActivity.performCredentialsUpdate(account, fileActivity);
        }

        return Boolean.TRUE;
    }
}
