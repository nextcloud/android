/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2021 Tobias Kaminsky
 * Copyright (C) 2021 Nextcloud GmbH
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

import com.nextcloud.android.lib.resources.profile.GetHoverCardRemoteOperation;
import com.nextcloud.android.lib.resources.profile.HoverCard;
import com.nextcloud.client.account.User;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.common.NextcloudClient;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.ui.fragment.ProfileBottomSheetDialog;
import com.owncloud.android.utils.DisplayUtils;

import java.lang.ref.WeakReference;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

public class RetrieveHoverCardAsyncTask extends AsyncTask<Void, Void, HoverCard> {
    private final User user;
    private final String userId;
    private final WeakReference<FragmentActivity> activityWeakReference;
    private final ClientFactory clientFactory;

    public RetrieveHoverCardAsyncTask(User user,
                                      String userId,
                                      FragmentActivity activity,
                                      ClientFactory clientFactory) {
        this.user = user;
        this.userId = userId;
        this.activityWeakReference = new WeakReference<>(activity);
        this.clientFactory = clientFactory;
    }

    @Override
    protected HoverCard doInBackground(Void... voids) {
        try {
            NextcloudClient client = clientFactory.createNextcloudClient(user);
            RemoteOperationResult<HoverCard> result = new GetHoverCardRemoteOperation(userId).execute(client);

            if (result.isSuccess()) {
                return result.getResultData();
            } else {
                return null;
            }
        } catch (ClientFactory.CreationException | NullPointerException e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(HoverCard hoverCard) {
        FragmentActivity activity = this.activityWeakReference.get();

        if (activity != null && activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            if (hoverCard.getActions().size() > 0) {
                new ProfileBottomSheetDialog(activity, user, hoverCard).show();
            } else {
                DisplayUtils.showSnackMessage(activity, R.string.no_actions);
            }
        }
    }
}
