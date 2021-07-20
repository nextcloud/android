/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
        } catch (ClientFactory.CreationException e) {
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
