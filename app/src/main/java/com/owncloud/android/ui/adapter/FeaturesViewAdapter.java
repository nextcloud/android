package com.owncloud.android.ui.adapter;

import com.owncloud.android.features.FeatureItem;
import com.owncloud.android.ui.fragment.FeatureFragment;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

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
