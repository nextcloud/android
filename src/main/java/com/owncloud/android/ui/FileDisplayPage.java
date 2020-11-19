package com.owncloud.android.ui;

import android.os.Bundle;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.lib.resources.files.SearchRemoteOperation;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.events.SearchEvent;
import com.owncloud.android.ui.fragment.GalleryFragment;
import com.owncloud.android.ui.fragment.MoreFragment;
import com.owncloud.android.ui.fragment.OCFileListFragment;

import org.parceler.Parcels;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

public class FileDisplayPage {

    public OCFileListFragment homeFragment = new OCFileListFragment();
    public GalleryFragment photoFragment = new GalleryFragment(true);
    public OCFileListFragment favFragment = new OCFileListFragment();
    public MoreFragment moreFragment = new MoreFragment();
    public Fragment currentFragment;

    public FileDisplayPage() {
        Bundle args = new Bundle();
        args.putBoolean(OCFileListFragment.ARG_ALLOW_CONTEXTUAL_ACTIONS, true);
        homeFragment.setArguments(args);

        SearchEvent favSearchEvent = new SearchEvent("", SearchRemoteOperation.SearchType.FAVORITE_SEARCH);
        Bundle favBundle = new Bundle();
        favBundle.putParcelable(OCFileListFragment.SEARCH_EVENT, Parcels.wrap(favSearchEvent));
        favFragment.setArguments(favBundle);

        SearchEvent photoSearchEvent = new SearchEvent("image/%", SearchRemoteOperation.SearchType.PHOTO_SEARCH);
        Bundle photoBundle = new Bundle();
        photoBundle.putParcelable(OCFileListFragment.SEARCH_EVENT, Parcels.wrap(photoSearchEvent));
        photoFragment.setArguments(photoBundle);
    }

    public void show(FragmentActivity activity, Fragment fragment) {
        currentFragment = fragment;
        if (isShow(fragment)) {
            return;
        }
        if (!(fragment instanceof MoreFragment)) {
            MainApp.showOnlyFilesOnDevice(false);
        }
        hideAllWithout(activity, fragment);
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        if (fragment.isAdded()) {
            MainApp.showOnlyFilesOnDevice(false);
            transaction.show(fragment);
        } else {
            transaction.add(R.id.left_fragment_container, fragment, FileDisplayActivity.TAG_LIST_OF_FILES);
        }
        transaction.commit();
    }

    public void hideAllWithout(FragmentActivity activity, Fragment fragment) {
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        if (homeFragment != fragment && isShow(homeFragment)) {
            transaction.hide(homeFragment);
        }
        if (favFragment != fragment && isShow(favFragment)) {
            transaction.hide(favFragment);
        }
        if (photoFragment != fragment && isShow(photoFragment)) {
            transaction.hide(photoFragment);
        }
        if (moreFragment != fragment && isShow(moreFragment)) {
            transaction.hide(moreFragment);
        }
        transaction.commit();
    }

    public boolean isShow(Fragment fragment) {
        return fragment.isAdded() && !fragment.isHidden();
    }
}
