/*
 *
 *  * Nextcloud Android client application
 *  *
 *  * @author Tobias Kaminsky
 *  * Copyright (C) 2019 Tobias Kaminsky
 *  * Copyright (C) 2019 Nextcloud GmbH
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui.asynctasks;

import android.accounts.Account;
import android.os.AsyncTask;

import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.owncloud.android.jobs.AccountRemovalJob;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.users.CheckRemoteWipeRemoteOperation;
import com.owncloud.android.ui.activity.FileActivity;

import java.lang.ref.WeakReference;

public class CheckRemoteWipeTask extends AsyncTask<Void, Void, Boolean> {
    private Account account;
    private WeakReference<FileActivity> fileActivityWeakReference;

    public CheckRemoteWipeTask(Account account, WeakReference<FileActivity> fileActivityWeakReference) {
        this.account = account;
        this.fileActivityWeakReference = fileActivityWeakReference;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        FileActivity fileActivity = fileActivityWeakReference.get();

        if (fileActivity == null) {
            Log_OC.e(this, "Check for remote wipe: no context available");
            return Boolean.FALSE;
        }

        RemoteOperationResult checkWipeResult = new CheckRemoteWipeRemoteOperation().execute(account, fileActivity);

        if (checkWipeResult.isSuccess()) {
            // schedule job
            PersistableBundleCompat bundle = new PersistableBundleCompat();
            bundle.putString(AccountRemovalJob.ACCOUNT, account.name);
            bundle.putBoolean(AccountRemovalJob.REMOTE_WIPE, true);

            new JobRequest.Builder(AccountRemovalJob.TAG)
                .startNow()
                .setExtras(bundle)
                .setUpdateCurrent(false)
                .build()
                .schedule();
        } else {
            Log_OC.e(this, "Check for remote wipe not needed -> update credentials");

            fileActivity.performCredentialsUpdate(account, fileActivity);

        }

        return Boolean.TRUE;
    }
}
