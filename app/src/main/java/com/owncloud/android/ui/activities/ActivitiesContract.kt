/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities;

import com.nextcloud.common.NextcloudClient;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.BaseActivity;

import java.util.List;

public interface ActivitiesContract {

    interface View {
        void showActivities(List<Object> activities, NextcloudClient client, long lastGiven);
        void showActivitiesLoadError(String error);
        void showActivityDetailUI(OCFile ocFile);
        void showActivityDetailUIIsNull();
        void showActivityDetailError(String error);
        void showLoadingMessage();
        void showEmptyContent(String headline, String message);
        void setProgressIndicatorState(boolean isActive);
    }

    interface ActionListener {
        int UNDEFINED = -1;

        void loadActivities(long lastGiven);

        void openActivity(String fileUrl, BaseActivity baseActivity);

        void onStop();

        void onResume();
    }
}
