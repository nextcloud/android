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
