package com.owncloud.android.ui.activity;

import android.support.v4.widget.SwipeRefreshLayout;

    public interface OnEnforceableRefreshListener extends SwipeRefreshLayout.OnRefreshListener {

        public void onRefresh(boolean enforced);

    }

