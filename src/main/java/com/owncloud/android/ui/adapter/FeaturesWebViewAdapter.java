package com.owncloud.android.ui.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.owncloud.android.ui.fragment.FeatureWebFragment;

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
