/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.lang.ref.WeakReference;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

public class RetrieveHoverCardAsyncTask extends AsyncTask<Void, Void, HoverCard> {
    private final User user;
    private final String userId;
    private final WeakReference<FragmentActivity> activityWeakReference;
    private final ClientFactory clientFactory;
    private final ViewThemeUtils viewThemeUtils;

    public RetrieveHoverCardAsyncTask(User user,
                                      String userId,
                                      FragmentActivity activity,
                                      ClientFactory clientFactory,
                                      ViewThemeUtils viewThemeUtils) {
        this.user = user;
        this.userId = userId;
        this.activityWeakReference = new WeakReference<>(activity);
        this.clientFactory = clientFactory;
        this.viewThemeUtils = viewThemeUtils;
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
                new ProfileBottomSheetDialog(activity,
                                             user,
                                             hoverCard,
                                             viewThemeUtils)
                    .show();
            } else {
                DisplayUtils.showSnackMessage(activity, R.string.no_actions);
            }
        }
    }
}
