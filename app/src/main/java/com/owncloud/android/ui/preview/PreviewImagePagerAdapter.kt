/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper_ozturk@proton.me>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2013 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND AGPL-3.0-or-later
 */
package com.owncloud.android.ui.preview;

import android.util.SparseArray;

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
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.util.UnstableApi;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Adapter class that provides Fragment instances
 */
public class PreviewImagePagerAdapter extends FragmentStateAdapter {

    private OCFile selectedFile;
    private List<OCFile> mImageFiles;
    private final User user;
    private final Set<Object> mObsoleteFragments;
    private final Set<Integer> mObsoletePositions;
    private final Set<Integer> mDownloadErrors;
    private final FileDataStorageManager mStorageManager;
    private final SparseArray<FileFragment> mCachedFragments;

    /**
     * Constructor
     *
     * @param fragmentActivity {@link FragmentActivity} instance that will handle the {@link Fragment}s provided by the
     *                        adapter.
     * @param parentFolder    Folder where images will be searched for.
     * @param storageManager  Bridge to database.
     */
    public PreviewImagePagerAdapter(FragmentActivity fragmentActivity,
                                    OCFile selectedFile,
                                    OCFile parentFolder,
                                    User user,
                                    FileDataStorageManager storageManager,
                                    boolean onlyOnDevice,
                                    AppPreferences preferences) {
        super(fragmentActivity);

        if (parentFolder == null) {
            throw new IllegalArgumentException("NULL parent folder");
        }
        if (storageManager == null) {
            throw new IllegalArgumentException("NULL storage manager");
        }

        this.user = user;
        this.selectedFile = selectedFile;
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
     * @param fragmentActivity {@link FragmentActivity} instance that will handle the {@link Fragment}s provided by the
     *                        adapter.
     * @param type            Type of virtual folder, e.g. favorite or photos
     * @param storageManager  Bridge to database.
     */
    public PreviewImagePagerAdapter(FragmentActivity fragmentActivity,
                                    VirtualFolderType type,
                                    User user,
                                    FileDataStorageManager storageManager) {
        super(fragmentActivity);

        if (type == null) {
            throw new IllegalArgumentException("NULL parent folder");
        }
        if (type == VirtualFolderType.NONE) {
            throw new IllegalArgumentException("NONE virtual folder type");
        }
        if (storageManager == null) {
            throw new IllegalArgumentException("NULL storage manager");
        }

        this.user = user;
        mStorageManager = storageManager;

        if (type == VirtualFolderType.GALLERY) {
            mImageFiles = mStorageManager.getAllGalleryItems();
            mImageFiles = FileStorageUtils.sortOcFolderDescDateModifiedWithoutFavoritesFirst(mImageFiles);
        } else {
            mImageFiles = mStorageManager.getVirtualFolderContent(type, true);
        }

        mObsoleteFragments = new HashSet<>();
        mObsoletePositions = new HashSet<>();
        mDownloadErrors = new HashSet<>();
        mCachedFragments = new SparseArray<>();
    }

    public void delete(int position) {
        if (position < 0 || position >= mImageFiles.size()) {
            return;
        }

        FileFragment fragmentToDelete = mCachedFragments.get(position);
        if (fragmentToDelete == null) {
            return;
        }

        mObsoleteFragments.add(fragmentToDelete);
        mObsoletePositions.add(position);

        mImageFiles.remove(position);
        mDownloadErrors.remove(position);
        mCachedFragments.remove(position);

        notifyItemRemoved(position);
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

    @Override
    public long getItemId(int position) {
        return mImageFiles.get(position).hashCode();
    }

    @Override
    public boolean containsItem(long itemId) {
        return super.containsItem(itemId);
    }

    private void addVideoOfLivePhoto(OCFile file) {
        file.livePhotoVideo = selectedFile;
    }

    @NonNull
    @OptIn(markerClass = UnstableApi.class)
    public Fragment getItem(int i) {
        OCFile file = getFileAt(i);
        Fragment fragment;

        if (file == null) {
            fragment = PreviewImageErrorFragment.newInstance();
        } else if (file.isDown()) {
            fragment = PreviewImageFragment.newInstance(file, mObsoletePositions.contains(i), false);
        } else {
            addVideoOfLivePhoto(file);

            if (mDownloadErrors.remove(i)) {
                fragment = FileDownloadFragment.newInstance(file, user, true);
                ((FileDownloadFragment) fragment).setError(true);
            } else {
                if (file.isEncrypted()) {
                    fragment = FileDownloadFragment.newInstance(file, user, mObsoletePositions.contains(i));
                } else if (PreviewMediaFragment.canBePreviewed(file)) {
                    fragment = PreviewMediaFragment.newInstance(file, user, 0, false, file.livePhotoVideo != null);
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

    public boolean pendingErrorAt(int position) {
        return mDownloadErrors.contains(position);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return getItem(position);
    }

    @Override
    public int getItemCount() {
        return mImageFiles.size();
    }
}
