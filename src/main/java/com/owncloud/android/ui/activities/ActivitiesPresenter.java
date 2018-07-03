/**
 * Nextcloud Android client application
 *
 * Copyright (C) 2018 Edvard Holst
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activities;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository;
import com.owncloud.android.ui.activities.data.files.FilesRepository;
import com.owncloud.android.ui.activity.BaseActivity;

import java.util.List;

public class ActivitiesPresenter implements ActivitiesContract.ActionListener {

    private final ActivitiesContract.View activitiesView;
    private final ActivitiesRepository activitiesRepository;
    private final FilesRepository filesRepository;
    private boolean activityStopped = false;


    ActivitiesPresenter(@NonNull ActivitiesRepository activitiesRepository,
                        @NonNull FilesRepository filesRepository,
                        @NonNull ActivitiesContract.View activitiesView) {
        this.activitiesRepository = activitiesRepository;
        this.activitiesView = activitiesView;
        this.filesRepository = filesRepository;
    }

    @Override
    public void loadActivities(String pageUrl) {
        activitiesView.setProgressIndicatorState(true);
        activitiesRepository.getActivities(pageUrl, new ActivitiesRepository.LoadActivitiesCallback() {
            @Override
            public void onActivitiesLoaded(List<Object> activities, OwnCloudClient client,
                                           String nextPageUrl) {

                if (!activityStopped) {
                    activitiesView.setProgressIndicatorState(false);
                    activitiesView.showActivities(activities, client, nextPageUrl);
                }
            }

            @Override
            public void onActivitiesLoadedError(String error) {
                if (!activityStopped) {
                    activitiesView.setProgressIndicatorState(false);
                    activitiesView.showActivitiesLoadError(error);
                }
            }
        });
    }

    @Override
    public void openActivity(String fileUrl, BaseActivity baseActivity) {
        activitiesView.setProgressIndicatorState(true);
        filesRepository.readRemoteFile(fileUrl, baseActivity, new FilesRepository.ReadRemoteFileCallback() {
                    @Override
                    public void onFileLoaded(@Nullable OCFile ocFile) {
                        activitiesView.setProgressIndicatorState(false);
                        if (ocFile != null) {
                            activitiesView.showActivityDetailUI(ocFile);
                        } else {
                            activitiesView.showActivityDetailUIIsNull();
                        }
                    }

                    @Override
                    public void onFileLoadError(String error) {
                        activitiesView.setProgressIndicatorState(false);
                        activitiesView.showActivityDetailError(error);
                    }
                });
    }

    @Override
    public void stopLoadingActivity() {
        activityStopped = true;
    }
}
