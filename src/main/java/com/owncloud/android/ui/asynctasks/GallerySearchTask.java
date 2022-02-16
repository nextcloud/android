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
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.ui.adapter.OCFileListAdapter;
import com.owncloud.android.ui.fragment.ExtendedListFragment;
import com.owncloud.android.ui.fragment.GalleryFragment;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeTypeUtil;

import java.lang.ref.WeakReference;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GallerySearchTask extends AsyncTask<Void, Void, GallerySearchTask.Result> {

    private final User user;
    private final WeakReference<GalleryFragment> photoFragmentWeakReference;
    private final FileDataStorageManager storageManager;
    private final int limit;
    private final long startDate;
    private final long endDate;

    private String remotePath;
    private List<OCFile> mediaObject;
    private boolean isImageHideClicked;
    private boolean isVideoHideClicked;
    private List<OCFile> imageList;
    private List<OCFile> videoList;

    public GallerySearchTask(GalleryFragment photoFragment,
                             User user,
                             FileDataStorageManager storageManager,
                             long startDate,
                             long endDate,
                             int limit,String remotePath, List<OCFile> mediaObject,
                             boolean isImageHideClicked, boolean isVideoHideClicked) {
        this.user = user;
        this.photoFragmentWeakReference = new WeakReference<>(photoFragment);
        this.storageManager = storageManager;
        this.startDate = startDate;
        this.endDate = endDate;
        this.limit = limit;

        this.remotePath = remotePath;
        this.mediaObject = mediaObject;
        this.isImageHideClicked = isImageHideClicked;
        this.isVideoHideClicked = isVideoHideClicked;
        videoList = new ArrayList<>();
        imageList = new ArrayList<>();
    }

    @Override
    protected GallerySearchTask.Result doInBackground(Void... voids) {
        if (photoFragmentWeakReference.get() == null) {
            return new Result(false, false, -1);
        }
        GalleryFragment photoFragment = photoFragmentWeakReference.get();

        if (isCancelled()) {
            return new Result(false, false, -1);
        } else {
            OCCapability ocCapability = storageManager.getCapability(user.getAccountName());

            SearchRemoteOperation searchRemoteOperation = new SearchRemoteOperation("",
                                                                                    SearchRemoteOperation.SearchType.GALLERY_SEARCH,
                                                                                    false,
                                                                                    ocCapability);


            searchRemoteOperation.setLimit(limit);
            searchRemoteOperation.setStartDate(startDate);
            searchRemoteOperation.setEndDate(endDate);

            if (photoFragment.getContext() != null) {
                Log_OC.d(this,
                         "Start gallery search with " + new Date(startDate * 1000L) +
                             " - " + new Date(endDate * 1000L) +
                             " with limit: " + limit);

                RemoteOperationResult result = searchRemoteOperation.execute(user.toPlatformAccount(),
                                                                             photoFragment.getContext());

                if (result.isSuccess() && result.getData() != null && !isCancelled()) {
                    if (result.getData() == null || result.getData().size() == 0) {
                        photoFragment.setEmptyListMessage(ExtendedListFragment.SearchType.GALLERY_SEARCH);
                    }
                }

                boolean emptySearch = parseMedia(startDate, endDate, Objects.requireNonNull(result.getData()));
                long lastTimeStamp = findLastTimestamp(result.getData());

                photoFragment.getAdapter().showAllGalleryItems(storageManager,remotePath,
                                                               mediaObject,isVideoHideClicked,isImageHideClicked,
                                                               imageList,videoList,photoFragment);

                return new Result(result.isSuccess(), emptySearch, lastTimeStamp);
            } else {
                return new Result(false, false, -1);
            }
        }
    }

    @Override
    protected void onPostExecute(GallerySearchTask.Result result) {
        /*if (photoFragmentWeakReference.get() != null) {
            GalleryFragment photoFragment = photoFragmentWeakReference.get();

            if (result.isSuccess() && result.getData() != null && !isCancelled()) {
                if (result.getData() == null || result.getData().size() == 0) {
                    photoFragment.setSearchDidNotFindNewPhotos(true);
                } else {
                    OCFileListAdapter adapter = photoFragment.getAdapter();
                   // mediaObject.clear();
                    if (result.getData().size() < limit) {
                        // stop loading spinner
                        photoFragment.setSearchDidNotFindNewPhotos(true);
                    }

                    for (Object c : result.getData()) {
                        if (c instanceof RemoteFile) {
                            if (((RemoteFile) c).getRemotePath().contains(remotePath))
                             {
                                 mediaObject.add(c);
                             }
                        }
                    }
                    if(mediaObject.size()<limit)
                    {
                        photoFragment.setSearchDidNotFindNewPhotos(true);
                    }


                   setAdapterWithHideShowImage(mediaObject, adapter,isVideoHideClicked,isImageHideClicked,imageList,
                                               videoList,storageManager,photoFragment);

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
        } */
        if (photoFragmentWeakReference.get() != null) {
            GalleryFragment photoFragment = photoFragmentWeakReference.get();

            photoFragment.setLoading(false);
            photoFragment.searchCompleted(result.emptySearch, result.lastTimestamp);

            if (!result.success) {
                photoFragment.setEmptyListMessage(ExtendedListFragment.SearchType.GALLERY_SEARCH);
            }
        }
    }


    //Set Image/Video List According to Selection of Hide/Show Image/Video
    public static void setAdapterWithHideShowImage(List<Object> mediaObject, OCFileListAdapter adapter,
                                                  boolean isVideoHideClicked, boolean isImageHideClicked,
                                                   List<Object> imageList, List<Object> videoList ,
                                                   FileDataStorageManager storageManager,
                                                   GalleryFragment photoFragment ) {

        if (isVideoHideClicked) {
            imageList.clear();
            for (Object s : mediaObject) {
                if (s instanceof RemoteFile) {

                    if (MimeTypeUtil.isImage(((RemoteFile) s).getMimeType()) && !imageList.contains(s)) {
                        imageList.add(s);
                    }
                }
            }
            if(!imageList.isEmpty())
            {
                updateAndNotifyAdapter(imageList, adapter, storageManager);
            }
            else
            {
                photoFragment.setEmptyListMessage(ExtendedListFragment.SearchType.GALLERY_SEARCH);
            }

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
            if(!videoList.isEmpty())
            {
                updateAndNotifyAdapter(videoList, adapter, storageManager);
            }
            else
            {
                photoFragment.setEmptyListMessage(ExtendedListFragment.SearchType.GALLERY_SEARCH);
            }
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

    private long findLastTimestamp(ArrayList<RemoteFile> remoteFiles) {
        int lastPosition = remoteFiles.size() - 1;

        if (lastPosition < 0) {
            return -1;
        }

        RemoteFile lastFile = remoteFiles.get(lastPosition);
        return lastFile.getModifiedTimestamp() / 1000;
    }

    private boolean parseMedia(long startDate, long endDate, List<Object> remoteFiles) {
        // retrieve all between startDate and endDate
        Map<String, OCFile> localFilesMap = RefreshFolderOperation.prefillLocalFilesMap(null,
                                                                                        storageManager.getGalleryItems(startDate * 1000L,
                                                                                                                       endDate * 1000L));
        List<OCFile> filesToAdd = new ArrayList<>();
        List<OCFile> filesToUpdate = new ArrayList<>();

        OCFile localFile;
        for (Object file : remoteFiles) {
            OCFile ocFile = FileStorageUtils.fillOCFile((RemoteFile) file);

            localFile = localFilesMap.remove(ocFile.getRemotePath());

            if (localFile == null) {
                // add new file
                filesToAdd.add(ocFile);
            } else if (!localFile.getEtag().equals(ocFile.getEtag())) {
                // update file
                ocFile.setLastSyncDateForData(System.currentTimeMillis());
                filesToUpdate.add(ocFile);
            }
        }

        // add new files
        for (OCFile file : filesToAdd) {
            storageManager.saveFile(file);
        }

        // update existing files
        for (OCFile file : filesToUpdate) {
            storageManager.saveFile(file);
        }

        // existing files to remove
        for (OCFile file : localFilesMap.values()) {
            storageManager.removeFile(file, true, true);
        }

        Log_OC.d(this, "Gallery search result:" +
            " new: " + filesToAdd.size() +
            " updated: " + filesToUpdate.size() +
            " deleted: " + localFilesMap.values().size());

        return didNotFindNewResults(filesToAdd, filesToUpdate, localFilesMap.values());
    }

    private boolean didNotFindNewResults(List<OCFile> filesToAdd,
                                         List<OCFile> filesToUpdate,
                                         Collection<OCFile> filesToRemove) {
        return filesToAdd.isEmpty() && filesToUpdate.isEmpty() && filesToRemove.isEmpty();
    }


    public static class Result {
        public boolean success;
        public boolean emptySearch;
        public long lastTimestamp;

        public Result(boolean success, boolean emptySearch, long lastTimestamp) {
            this.success = success;
            this.emptySearch = emptySearch;
            this.lastTimestamp = lastTimestamp;
        }
    }

}
