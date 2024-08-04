/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

import com.owncloud.android.ui.fragment.FeatureWebFragment;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class FeaturesWebViewAdapter extends FragmentStateAdapter {
    private String[] mWebUrls;

    public FeaturesWebViewAdapter(FragmentActivity fragmentActivity, String... webUrls) {
        super(fragmentActivity);
        mWebUrls = webUrls;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return FeatureWebFragment.newInstance(mWebUrls[position]);
    }

    @Override
    public int getItemCount() {
        return mWebUrls.length;
    }
}
