/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015  ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.ui.preview;

import android.accounts.Account;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.VirtualFolderType;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.FileStorageUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * Adapter class that provides Fragment instances
 */
//public class PreviewImagePagerAdapter extends PagerAdapter {
public class PreviewImagePagerAdapter extends FragmentStatePagerAdapter {
    
    private Vector<OCFile> mImageFiles;
    private Account mAccount;
    private Set<Object> mObsoleteFragments;
    private Set<Integer> mObsoletePositions;
    private Set<Integer> mDownloadErrors;
    private FileDataStorageManager mStorageManager;
    
    private Map<Integer, FileFragment> mCachedFragments;

    /**
     * Constructor.
     * 
     * @param fragmentManager   {@link FragmentManager} instance that will handle
     *                          the {@link Fragment}s provided by the adapter.
     * @param parentFolder      Folder where images will be searched for.
     * @param storageManager    Bridge to database.
     */
    public PreviewImagePagerAdapter(FragmentManager fragmentManager, OCFile parentFolder,
                                    Account account, FileDataStorageManager storageManager,
                                    boolean onlyOnDevice) {
        super(fragmentManager);
        
        if (fragmentManager == null) {
            throw new IllegalArgumentException("NULL FragmentManager instance");
        }
        if (parentFolder == null) {
            throw new IllegalArgumentException("NULL parent folder");
        } 
        if (storageManager == null) {
            throw new IllegalArgumentException("NULL storage manager");
        }

        mAccount = account;
        mStorageManager = storageManager;
        mImageFiles = mStorageManager.getFolderImages(parentFolder, onlyOnDevice);
        
        mImageFiles = FileStorageUtils.sortOcFolder(mImageFiles);
        
        mObsoleteFragments = new HashSet<Object>();
        mObsoletePositions = new HashSet<Integer>();
        mDownloadErrors = new HashSet<Integer>();
        //mFragmentManager = fragmentManager;
        mCachedFragments = new HashMap<Integer, FileFragment>();
    }

    /**
     * Constructor.
     *
     * @param fragmentManager {@link FragmentManager} instance that will handle
     *                        the {@link Fragment}s provided by the adapter.
     * @param type            Type of virtual folder, e.g. favorite or photos
     * @param storageManager  Bridge to database.
     */
    public PreviewImagePagerAdapter(FragmentManager fragmentManager, VirtualFolderType type,
                                    Account account, FileDataStorageManager storageManager) {
        super(fragmentManager);

        if (fragmentManager == null) {
            throw new IllegalArgumentException("NULL FragmentManager instance");
        }
        if (type == null) {
            throw new IllegalArgumentException("NULL parent folder");
        }
        if (storageManager == null) {
            throw new IllegalArgumentException("NULL storage manager");
        }

        mAccount = account;
        mStorageManager = storageManager;
        mImageFiles = mStorageManager.getVirtualFolderContent(type, true);

        if (type == VirtualFolderType.PHOTOS) {
            mImageFiles = FileStorageUtils.sortOcFolderDescDateModified(mImageFiles);
        }

        mObsoleteFragments = new HashSet<Object>();
        mObsoletePositions = new HashSet<Integer>();
        mDownloadErrors = new HashSet<Integer>();
        //mFragmentManager = fragmentManager;
        mCachedFragments = new HashMap<Integer, FileFragment>();
    }

    /**
     * Returns the image files handled by the adapter.
     * 
     * @return  A vector with the image files handled by the adapter.
     */
    protected OCFile getFileAt(int position) {
        return mImageFiles.get(position);
    }

    
    public Fragment getItem(int i) {
        OCFile file = mImageFiles.get(i);
        Fragment fragment;
        if (file.isDown()) {
            fragment = PreviewImageFragment.newInstance(file, mObsoletePositions.contains(i));
            
        } else if (mDownloadErrors.contains(i)) {
            fragment = FileDownloadFragment.newInstance(file, mAccount, true);
            ((FileDownloadFragment)fragment).setError(true);
            mDownloadErrors.remove(i);
            
        } else {
            fragment = FileDownloadFragment.newInstance(file, mAccount, mObsoletePositions.contains(i));
        }
        mObsoletePositions.remove(i);
        return fragment;
    }

    public int getFilePosition(OCFile file) {
        return mImageFiles.indexOf(file);
    }
    
    @Override
    public int getCount() {
        return mImageFiles.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mImageFiles.get(position).getFileName();
    }

    
    public void updateFile(int position, OCFile file) {
        FileFragment fragmentToUpdate = mCachedFragments.get(Integer.valueOf(position));
        if (fragmentToUpdate != null) {
            mObsoleteFragments.add(fragmentToUpdate);
        }
        mObsoletePositions.add(Integer.valueOf(position));
        mImageFiles.set(position, file);
    }
    
    
    public void updateWithDownloadError(int position) {
        FileFragment fragmentToUpdate = mCachedFragments.get(Integer.valueOf(position));
        if (fragmentToUpdate != null) {
            mObsoleteFragments.add(fragmentToUpdate);
        }
        mDownloadErrors.add(Integer.valueOf(position));
    }
    
    public void clearErrorAt(int position) {
        FileFragment fragmentToUpdate = mCachedFragments.get(Integer.valueOf(position));
        if (fragmentToUpdate != null) {
            mObsoleteFragments.add(fragmentToUpdate);
        }
        mDownloadErrors.remove(Integer.valueOf(position));
    }
    
    
    @Override
    public int getItemPosition(Object object) {
        if (mObsoleteFragments.contains(object)) {
            mObsoleteFragments.remove(object);
            return POSITION_NONE;
        }
        return super.getItemPosition(object);
    }


    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object fragment = super.instantiateItem(container, position);
        mCachedFragments.put(Integer.valueOf(position), (FileFragment)fragment);
        return fragment;
    }
    
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
       mCachedFragments.remove(Integer.valueOf(position));
       super.destroyItem(container, position, object);
    }


    public boolean pendingErrorAt(int position) {
        return mDownloadErrors.contains(Integer.valueOf(position));
    }

    /**
     * Reset the image zoom to default value for each CachedFragments
     */
    public void resetZoom() {
        Iterator<FileFragment> entries = mCachedFragments.values().iterator();
        while (entries.hasNext()) {
        FileFragment fileFragment = entries.next();
            if (fileFragment instanceof PreviewImageFragment) {
                ((PreviewImageFragment) fileFragment).getImageView().resetZoom();
            }
        }
    }

    /* -*
     * Called when a change in the shown pages is going to start being made.
     * 
     * @param   container   The containing View which is displaying this adapter's page views.
     *- /
    @Override
    public void startUpdate(ViewGroup container) {
        Log_OC.e(TAG, "** startUpdate");
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Log_OC.e(TAG, "** instantiateItem " + position);
        
        if (mFragments.size() > position) {
            Fragment fragment = mFragments.get(position);
            if (fragment != null) {
                Log_OC.e(TAG, "** \t returning cached item");
                return fragment;
            }
        }

        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }

        Fragment fragment = getItem(position);
        if (mSavedState.size() > position) {
            Fragment.SavedState savedState = mSavedState.get(position);
            if (savedState != null) {
                // TODO WATCH OUT:
                // * The Fragment must currently be attached to the FragmentManager.
                // * A new Fragment created using this saved state must be the same class type as the Fragment it was created from.
                // * The saved state can not contain dependencies on other fragments -- that is it can't use putFragment(Bundle, String, Fragment) 
                //   to store a fragment reference                 
                fragment.setInitialSavedState(savedState);
            }
        }
        while (mFragments.size() <= position) {
            mFragments.add(null);
        }
        fragment.setMenuVisibility(false);
        mFragments.set(position, fragment);
        //Log_OC.e(TAG, "** \t adding fragment at position " + position + ", containerId " + container.getId());
        mCurTransaction.add(container.getId(), fragment);

        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        Log_OC.e(TAG, "** destroyItem " + position);
        Fragment fragment = (Fragment)object;
        
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        Log_OC.e(TAG, "** \t removing fragment at position " + position);
        while (mSavedState.size() <= position) {
            mSavedState.add(null);
        }
        mSavedState.set(position, mFragmentManager.saveFragmentInstanceState(fragment));
        mFragments.set(position, null);

        mCurTransaction.remove(fragment);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment)object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem.setMenuVisibility(false);
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true);
            }
            mCurrentPrimaryItem = fragment;
        }
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        Log_OC.e(TAG, "** finishUpdate (start)");
        if (mCurTransaction != null) {
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            mFragmentManager.executePendingTransactions();
        }
        Log_OC.e(TAG, "** finishUpdate (end)");
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((Fragment)object).getView() == view;
    }

    @Override
    public Parcelable saveState() {
        Bundle state = null;
        if (mSavedState.size() > 0) {
            state = new Bundle();
            Fragment.SavedState[] savedStates = new Fragment.SavedState[mSavedState.size()];
            mSavedState.toArray(savedStates);
            state.putParcelableArray("states", savedStates);
        }
        for (int i=0; i<mFragments.size(); i++) {
            Fragment fragment = mFragments.get(i);
            if (fragment != null) {
                if (state == null) {
                    state = new Bundle();
                }
                String key = "f" + i;
                mFragmentManager.putFragment(state, key, fragment);
            }
        }
        return state;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        if (state != null) {
            Bundle bundle = (Bundle)state;
            bundle.setClassLoader(loader);
            Parcelable[] states = bundle.getParcelableArray("states");
            mSavedState.clear();
            mFragments.clear();
            if (states != null) {
                for (int i=0; i<states.length; i++) {
                    mSavedState.add((Fragment.SavedState)states[i]);
                }
            }
            Iterable<String> keys = bundle.keySet();
            for (String key: keys) {
                if (key.startsWith("f")) {
                    int index = Integer.parseInt(key.substring(1));
                    Fragment f = mFragmentManager.getFragment(bundle, key);
                    if (f != null) {
                        while (mFragments.size() <= index) {
                            mFragments.add(null);
                        }
                        f.setMenuVisibility(false);
                        mFragments.set(index, f);
                    } else {
                        Log_OC.w(TAG, "Bad fragment at key " + key);
                    }
                }
            }
        }
    }
    */
}
