/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.asynctasks;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import com.nextcloud.client.account.User;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.SearchRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.ui.adapter.OCFileListAdapter;
import com.owncloud.android.ui.fragment.ExtendedListFragment;
import com.owncloud.android.ui.fragment.GalleryFragment;
import com.owncloud.android.utils.MimeTypeUtil;

import java.lang.ref.WeakReference;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class GallerySearchTask extends AsyncTask<Void, Void, RemoteOperationResult> {

    private int columnCount;
    private User user;
    private WeakReference<GalleryFragment> photoFragmentWeakReference;
    private SearchRemoteOperation searchRemoteOperation;
    private FileDataStorageManager storageManager;
    private String remotePath;
    private int limit;
    private List<Object> mediaObject;
    private boolean isImageHideClicked;
    private boolean isVideoHideClicked;
    private List<Object> imageList;
    private List<Object> videoList;

    public GallerySearchTask(int columnsCount,
                             GalleryFragment photoFragment,
                             User user,
                             SearchRemoteOperation searchRemoteOperation,
                             FileDataStorageManager storageManager,
                             String remotePath, List<Object> mediaObject,
                             boolean isImageHideClicked, boolean isVideoHideClicked) {
        this.columnCount = columnsCount;
        this.user = user;
        this.photoFragmentWeakReference = new WeakReference<>(photoFragment);
        this.searchRemoteOperation = searchRemoteOperation;
        this.storageManager = storageManager;
        this.remotePath = remotePath;
        this.mediaObject = mediaObject;
        this.isImageHideClicked = isImageHideClicked;
        this.isVideoHideClicked = isVideoHideClicked;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (photoFragmentWeakReference.get() == null) {
            return;
        }
        GalleryFragment photoFragment = photoFragmentWeakReference.get();
        photoFragment.setPhotoSearchQueryRunning(true);
        imageList = new ArrayList<>();
        videoList = new ArrayList<>();
    }

    @Override
    protected RemoteOperationResult doInBackground(Void... voids) {
        if (photoFragmentWeakReference.get() == null) {
            return new RemoteOperationResult(new Exception("Photo fragment is null"));
        }
        GalleryFragment photoFragment = photoFragmentWeakReference.get();
        OCFileListAdapter adapter = photoFragment.getAdapter();

        if (isCancelled()) {
            return new RemoteOperationResult(new Exception("Cancelled"));
        } else {
            limit = 15 * columnCount;

            long timestamp = -1;
            if (adapter.getLastTimestamp() > 0) {
                timestamp = adapter.getLastTimestamp();
            }

            searchRemoteOperation.setLimit(limit);
            searchRemoteOperation.setTimestamp(timestamp);

            if (photoFragment.getContext() != null) {
                return searchRemoteOperation.execute(user.toPlatformAccount(), photoFragment.getContext());
            } else {
                return new RemoteOperationResult(new IllegalStateException("No context available"));
            }
        }
    }

    @Override
    protected void onPostExecute(RemoteOperationResult result) {
        if (photoFragmentWeakReference.get() != null) {
            GalleryFragment photoFragment = photoFragmentWeakReference.get();

            if (result.isSuccess() && result.getData() != null && !isCancelled()) {
                if (result.getData() == null || result.getData().size() == 0) {
                    photoFragment.setSearchDidNotFindNewPhotos(true);
                } else {
                    OCFileListAdapter adapter = photoFragment.getAdapter();
                    mediaObject.clear();
                    if (result.getData().size() < limit) {
                        // stop loading spinner
                        photoFragment.setSearchDidNotFindNewPhotos(true);
                    }

                    for (Object c : result.getData()) {
                        if (c instanceof RemoteFile) {
                            if (((RemoteFile) c).getRemotePath().contains(remotePath)) {
                                mediaObject.add(c);
                            }
                        }
                    }


                   setAdapterWithHideShowImage(mediaObject, adapter,isVideoHideClicked,isImageHideClicked,imageList,
                                               videoList,storageManager);

                    Log_OC.d(this, "Search: count: " + result.getData().size() + " total: " + adapter.getFiles().size());
                }
            }

            photoFragment.setLoading(false);
            if (mediaObject.isEmpty()) {
                photoFragment.setEmptyListMessage(ExtendedListFragment.SearchType.GALLERY_SEARCH);
            }

            if (!result.isSuccess() && !isCancelled()) {
                photoFragment.setEmptyListMessage(ExtendedListFragment.SearchType.GALLERY_SEARCH);
            }

            photoFragment.setPhotoSearchQueryRunning(false);
        }
    }


    //Set Image/Video List According to Selection of Hide/Show Image/Video
    public static void setAdapterWithHideShowImage(List<Object> mediaObject, OCFileListAdapter adapter,
                                                  boolean isVideoHideClicked, boolean isImageHideClicked,
                                                   List<Object> imageList, List<Object> videoList ,
                                                   FileDataStorageManager storageManager ) {

        if (isVideoHideClicked) {
            imageList.clear();
            for (Object s : mediaObject) {
                if (s instanceof RemoteFile) {

                    if (MimeTypeUtil.isImage(((RemoteFile) s).getMimeType()) && !imageList.contains(s)) {
                        imageList.add(s);
                    }
                }
            }
            updateAndNotifyAdapter(imageList, adapter, storageManager);
        }
        else if (isImageHideClicked) {
            videoList.clear();
            for (Object s : mediaObject) {
                if (s instanceof RemoteFile) {
                    if (MimeTypeUtil.isVideo(((RemoteFile) s).getMimeType()) && !videoList.contains(s)) {
                        videoList.add(s);
                    }
                }
            }
            updateAndNotifyAdapter(videoList, adapter, storageManager);
        }
        else
        {
            updateAndNotifyAdapter(mediaObject, adapter, storageManager);
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private static void updateAndNotifyAdapter(List<Object> mediaObject, OCFileListAdapter adapter, FileDataStorageManager storageManager) {
        adapter.setData(mediaObject,
                        ExtendedListFragment.SearchType.GALLERY_SEARCH,
                        storageManager,
                        null,
                        true);
        adapter.notifyDataSetChanged();
    }

}
