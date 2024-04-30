/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

import com.owncloud.android.ui.fragment.FeatureWebFragment;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class FeaturesWebViewAdapter extends FragmentPagerAdapter {
    private String[] mWebUrls;

    public FeaturesWebViewAdapter(FragmentManager fm, String... webUrls) {
        super(fm);
        mWebUrls = webUrls;
    }

    @Override
    public Fragment getItem(int position) {
        return FeatureWebFragment.newInstance(mWebUrls[position]);
    }

    @Override
    public int getCount() {
        return mWebUrls.length;
    }
}
