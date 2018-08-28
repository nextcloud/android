package com.owncloud.android.ui.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.owncloud.android.features.FeatureItem;
import com.owncloud.android.ui.fragment.FeatureFragment;

public class FeaturesViewAdapter extends FragmentPagerAdapter {

    private FeatureItem[] mFeatures;

    public FeaturesViewAdapter(FragmentManager fm, FeatureItem... features) {
        super(fm);
        mFeatures = features;
    }

    @Override
    public Fragment getItem(int position) {
        return FeatureFragment.newInstance(mFeatures[position]);
    }

    @Override
    public int getCount() {
        return mFeatures.length;
    }
}
