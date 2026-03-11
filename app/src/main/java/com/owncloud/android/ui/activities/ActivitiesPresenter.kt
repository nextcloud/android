/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities;

import com.nextcloud.common.NextcloudClient;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository;
import com.owncloud.android.ui.activities.data.files.FilesRepository;
import com.owncloud.android.ui.activity.BaseActivity;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ActivitiesPresenter implements ActivitiesContract.ActionListener {

    private final ActivitiesContract.View activitiesView;
    private final ActivitiesRepository activitiesRepository;
    private final FilesRepository filesRepository;
    private boolean activityStopped;


    ActivitiesPresenter(@NonNull ActivitiesRepository activitiesRepository,
                        @NonNull FilesRepository filesRepository,
                        @NonNull ActivitiesContract.View activitiesView) {
        this.activitiesRepository = activitiesRepository;
        this.activitiesView = activitiesView;
        this.filesRepository = filesRepository;
    }

    @Override
    public void loadActivities(long lastGiven) {
        if (UNDEFINED == lastGiven) {
            activitiesView.showLoadingMessage();
        } else {
            activitiesView.setProgressIndicatorState(true);
        }
        activitiesRepository.getActivities(lastGiven, new ActivitiesRepository.LoadActivitiesCallback() {
            @Override
            public void onActivitiesLoaded(List<Object> activities, NextcloudClient client, long lastGiven) {

                if (!activityStopped) {
                    activitiesView.setProgressIndicatorState(false);
                    activitiesView.showActivities(activities, client, lastGiven);
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
    public void onStop() {
        activityStopped = true;
    }

    @Override
    public void onResume() {
        activityStopped = false;
    }
}
