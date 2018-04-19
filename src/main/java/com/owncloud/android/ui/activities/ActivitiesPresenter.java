package com.owncloud.android.ui.activities;

import android.support.annotation.NonNull;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.ui.activities.data.ActivitiesRepository;

import java.util.List;

public class ActivitiesPresenter implements ActivitiesContract.ActionListener {

    private final ActivitiesContract.View mActivitiesView;
    private final ActivitiesRepository mActivitiesRepository;


    public ActivitiesPresenter(@NonNull ActivitiesRepository activitiesRepository,
                               @NonNull ActivitiesContract.View activitiesView) {
        mActivitiesRepository = activitiesRepository;
        mActivitiesView = activitiesView;
    }

    @Override
    public void loadActivites(String pageUrl) {
        mActivitiesView.setProgressIndicatorState(true);
        mActivitiesRepository.getActivities(pageUrl, new ActivitiesRepository.LoadActivitiesCallback() {
            @Override
            public void onActivitiesLoaded(List<Object> activities, OwnCloudClient client, boolean clear) {
                mActivitiesView.setProgressIndicatorState(false);
                mActivitiesView.showActivites(activities, client, clear);
            }

            @Override
            public void onActivitiesLoadedError(String error) {
                mActivitiesView.setProgressIndicatorState(false);
                mActivitiesView.showActivitiesLoadError(error);
            }
        });


    }

    @Override
    public void openActivity() {

    }

}