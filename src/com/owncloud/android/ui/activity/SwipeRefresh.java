package com.owncloud.android.ui.activity;

import android.support.v4.widget.SwipeRefreshLayout;

    public interface SwipeRefresh extends SwipeRefreshLayout.OnRefreshListener {

        public void onRefreshForced(boolean ingnoreTag);

    }

