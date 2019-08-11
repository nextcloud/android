package com.owncloud.android.ui.activities;

public class ActivityListHeader implements ActivityListElement {
    final private String headline;

    public ActivityListHeader(String headline) {
        this.headline = headline;
    }

    public String getHeadline() {
        return headline;
    }

}
