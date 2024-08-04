/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

import com.owncloud.android.features.FeatureItem;
import com.owncloud.android.ui.fragment.FeatureFragment;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class FeaturesViewAdapter extends FragmentStateAdapter {

    private final FeatureItem[] mFeatures;

    public FeaturesViewAdapter(FragmentActivity fragmentActivity, FeatureItem... features) {
        super(fragmentActivity);
        mFeatures = features;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return FeatureFragment.newInstance(mFeatures[position]);
    }

    @Override
    public int getItemCount() {
        return mFeatures.length;
    }
}
