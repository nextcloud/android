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
import android.os.AsyncTask;

import com.nextcloud.client.jobs.BackgroundJobManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.helpers.CredentialsUpdateHelper;

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

        if (new CredentialsUpdateHelper(fileActivity).isRemoteWipeRequested(account)) {
            backgroundJobManager.startAccountRemovalJob(account.name, true);
        } else {
            Log_OC.e(this, "Check for remote wipe not needed -> update credentials");
            fileActivity.performCredentialsUpdate(account);
        }

        return Boolean.TRUE;
    }
}
