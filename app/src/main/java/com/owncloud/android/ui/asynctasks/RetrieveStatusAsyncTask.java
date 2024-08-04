/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.asynctasks;

import android.os.AsyncTask;

import com.nextcloud.client.account.User;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.common.NextcloudClient;
import com.nextcloud.ui.ChooseAccountDialogFragment;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.users.GetStatusRemoteOperation;
import com.owncloud.android.lib.resources.users.Status;
import com.owncloud.android.lib.resources.users.StatusType;

import java.lang.ref.WeakReference;

import androidx.lifecycle.Lifecycle;

public class RetrieveStatusAsyncTask extends AsyncTask<Void, Void, Status> {
    private final User user;
    private final WeakReference<ChooseAccountDialogFragment> chooseAccountDialogFragment;
    private final ClientFactory clientFactory;

    public RetrieveStatusAsyncTask(User user,
                                   ChooseAccountDialogFragment chooseAccountDialogFragment,
                                   ClientFactory clientFactory) {
        this.user = user;
        this.chooseAccountDialogFragment = new WeakReference<>(chooseAccountDialogFragment);
        this.clientFactory = clientFactory;
    }

    @Override
    protected com.owncloud.android.lib.resources.users.Status doInBackground(Void... voids) {
        try {
            NextcloudClient client = clientFactory.createNextcloudClient(user);
            RemoteOperationResult<com.owncloud.android.lib.resources.users.Status> result =
                new GetStatusRemoteOperation().execute(client);

            if (result.isSuccess()) {
                return result.getResultData();
            } else {
                return new com.owncloud.android.lib.resources.users.Status(StatusType.OFFLINE, "", "", -1);
            }
        } catch (ClientFactory.CreationException | NullPointerException e) {
            return new com.owncloud.android.lib.resources.users.Status(StatusType.OFFLINE, "", "", -1);
        }
    }

    @Override
    protected void onPostExecute(com.owncloud.android.lib.resources.users.Status status) {
        ChooseAccountDialogFragment fragment = chooseAccountDialogFragment.get();

        if (fragment != null && fragment.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            fragment.setStatus(status, fragment.requireContext());
        }
    }
}
