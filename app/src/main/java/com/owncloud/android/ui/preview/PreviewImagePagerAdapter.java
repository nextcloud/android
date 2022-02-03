/*
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

import android.util.SparseArray;
import android.view.ViewGroup;

import com.nextcloud.client.account.User;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.VirtualFolderType;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.FileStorageUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

/**
 * Adapter class that provides Fragment instances
 */
public class PreviewImagePagerAdapter extends FragmentStatePagerAdapter {

    private List<OCFile> mImageFiles;
    private User user;
    private Set<Object> mObsoleteFragments;
    private Set<Integer> mObsoletePositions;
    private Set<Integer> mDownloadErrors;
    private FileDataStorageManager mStorageManager;
    private SparseArray<FileFragment> mCachedFragments;

    /**
     * Constructor
     *
     * @param fragmentManager   {@link FragmentManager} instance that will handle
     *                          the {@link Fragment}s provided by the adapter.
     * @param parentFolder      Folder where images will be searched for.
     * @param storageManager    Bridge to database.
     */
    public PreviewImagePagerAdapter(FragmentManager fragmentManager,
                                    OCFile parentFolder,
                                    User user,
                                    FileDataStorageManager storageManager,
                                    boolean onlyOnDevice,
                                    AppPreferences preferences) {
        super(fragmentManager);
        if (parentFolder == null) {
            throw new IllegalArgumentException("NULL parent folder");
        }
        if (storageManager == null) {
            throw new IllegalArgumentException("NULL storage manager");
        }

        this.user = user;
        mStorageManager = storageManager;
        mImageFiles = mStorageManager.getFolderImages(parentFolder, onlyOnDevice);

        FileSortOrder sortOrder = preferences.getSortOrderByFolder(parentFolder);
        mImageFiles = sortOrder.sortCloudFiles(mImageFiles);

        mObsoleteFragments = new HashSet<>();
        mObsoletePositions = new HashSet<>();
        mDownloadErrors = new HashSet<>();
        mCachedFragments = new SparseArray<>();
    }

    /**
     * Constructor
     *
     * @param fragmentManager {@link FragmentManager} instance that will handle
     *                        the {@link Fragment}s provided by the adapter.
     * @param type            Type of virtual folder, e.g. favorite or photos
     * @param storageManager  Bridge to database.
     */
    public PreviewImagePagerAdapter(FragmentManager fragmentManager,
                                    VirtualFolderType type,
                                    User user,
                                    FileDataStorageManager storageManager) {
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

        this.user = user;
        mStorageManager = storageManager;
        mImageFiles = mStorageManager.getVirtualFolderContent(type, true);

        if (type == VirtualFolderType.GALLERY) {
            mImageFiles = FileStorageUtils.sortOcFolderDescDateModifiedWithoutFavoritesFirst(mImageFiles);
        }

        mObsoleteFragments = new HashSet<>();
        mObsoletePositions = new HashSet<>();
        mDownloadErrors = new HashSet<>();
        mCachedFragments = new SparseArray<>();
    }

    /**
     * Returns the image files handled by the adapter.
     *
     * @return OCFile desired image or null if position is not in adapter
     */
    @Nullable
    public OCFile getFileAt(int position) {
        try {
            return mImageFiles.get(position);
        } catch (IndexOutOfBoundsException exception) {
            return null;
        }
    }


    @NonNull
    public Fragment getItem(int i) {
        OCFile file = getFileAt(i);
        Fragment fragment;

        if (file == null) {
            fragment = PreviewImageErrorFragment.newInstance();

        } else if (file.isDown()) {
            fragment = PreviewImageFragment.newInstance(file, mObsoletePositions.contains(i), false);
        } else {
            if (mDownloadErrors.remove(i)) {
                fragment = FileDownloadFragment.newInstance(file, user, true);
                ((FileDownloadFragment) fragment).setError(true);
            } else {
                if (file.isEncrypted()) {
                    fragment = FileDownloadFragment.newInstance(file, user, mObsoletePositions.contains(i));
                } else {
                    fragment = PreviewImageFragment.newInstance(file, mObsoletePositions.contains(i), true);
                }
            }
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
        OCFile file = getFileAt(position);

        if (file != null) {
            return file.getFileName();
        } else {
            return "";
        }
    }


    public void updateFile(int position, OCFile file) {
        FileFragment fragmentToUpdate = mCachedFragments.get(position);
        if (fragmentToUpdate != null) {
            mObsoleteFragments.add(fragmentToUpdate);
        }
        mObsoletePositions.add(position);
        mImageFiles.set(position, file);
    }


    public void updateWithDownloadError(int position) {
        FileFragment fragmentToUpdate = mCachedFragments.get(position);
        if (fragmentToUpdate != null) {
            mObsoleteFragments.add(fragmentToUpdate);
        }
        mDownloadErrors.add(position);
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        if (mObsoleteFragments.remove(object)) {
            return POSITION_NONE;
        }
        return super.getItemPosition(object);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Object fragment = super.instantiateItem(container, position);
        mCachedFragments.put(position, (FileFragment) fragment);
        return fragment;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        mCachedFragments.remove(position);
       super.destroyItem(container, position, object);
    }


    public boolean pendingErrorAt(int position) {
        return mDownloadErrors.contains(position);
    }
}
