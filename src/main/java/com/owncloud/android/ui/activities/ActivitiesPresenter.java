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

    private final ActivitiesContract.View mActivitiesView;
    private final ActivitiesRepository mActivitiesRepository;
    private final FilesRepository mFilesRepository;


    public ActivitiesPresenter(@NonNull ActivitiesRepository activitiesRepository,
                               @NonNull FilesRepository filesRepository,
                               @NonNull ActivitiesContract.View activitiesView) {
        mActivitiesRepository = activitiesRepository;
        mActivitiesView = activitiesView;
        mFilesRepository = filesRepository;
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
    public void openActivity(String fileUrl, BaseActivity baseActivity, boolean isSharingSupported) {
        mActivitiesView.setProgressIndicatorState(true);
        mFilesRepository.readRemoteFile(fileUrl, baseActivity, isSharingSupported,
                new FilesRepository.ReadRemoteFileCallback() {
                    @Override
                    public void onFileLoaded(@Nullable OCFile ocFile) {
                        mActivitiesView.setProgressIndicatorState(false);
                        if (ocFile != null) {
                            mActivitiesView.showActivityDetailUI(ocFile);
                        } else {
                            mActivitiesView.showActivityDetailUIIsNull();
                        }
                    }

                    @Override
                    public void onFileLoadError(String error) {
                        mActivitiesView.setProgressIndicatorState(false);
                        mActivitiesView.showActivityDetailError(error);
                    }
                });
    }

}