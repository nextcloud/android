package com.owncloud.android.ui.activities;

import com.owncloud.android.lib.common.OwnCloudClient;

import java.util.List;

public interface ActivitiesContract {

    interface View {
        void showActivites(List<Object> activities, OwnCloudClient client, boolean clear);
        void showActivitiesLoadError(String error);
        void showActivityDetailUI();
        void showActivityDetailUIIsNull();
        void showLoadingMessage();
        void showEmptyContent(String headline, String message);
        void setProgressIndicatorState(boolean isActive);
    }

    interface ActionListener {
        void loadActivites(String pageUrl);
        void openActivity();
    }
}
