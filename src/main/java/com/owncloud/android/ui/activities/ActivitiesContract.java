package com.owncloud.android.ui.activities;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.ui.activity.BaseActivity;

import java.util.List;

public interface ActivitiesContract {

    interface View {
        void showActivites(List<Object> activities, OwnCloudClient client, String nextPageUrl);
        void showActivitiesLoadError(String error);
        void showActivityDetailUI(OCFile ocFile);
        void showActivityDetailUIIsNull();
        void showActivityDetailError(String error);
        void showLoadingMessage();
        void showEmptyContent(String headline, String message);
        void setProgressIndicatorState(boolean isActive);
    }

    interface ActionListener {
        void loadActivities(String pageUrl);
        void openActivity(String fileUrl, BaseActivity baseActivity, boolean isSharingSupported);
    }
}
