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

import android.os.AsyncTask;

import com.nextcloud.client.account.User;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.SearchRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.ui.fragment.GalleryFragment;
import com.owncloud.android.utils.FileStorageUtils;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GallerySearchTask extends AsyncTask<Void, Void, GallerySearchTask.Result> {

    private final User user;
    private final WeakReference<GalleryFragment> photoFragmentWeakReference;
    private final FileDataStorageManager storageManager;
    private final int limit;
    private final long endDate;

    public GallerySearchTask(GalleryFragment photoFragment,
                             User user,
                             FileDataStorageManager storageManager,
                             long endDate,
                             int limit) {
        this.user = user;
        this.photoFragmentWeakReference = new WeakReference<>(photoFragment);
        this.storageManager = storageManager;
        this.endDate = endDate;
        this.limit = limit;
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
            searchRemoteOperation.setEndDate(endDate);

            //workaround to keep SearchRemoteOperation functioning correctly even if we don't actively use startDate
            searchRemoteOperation.setStartDate(0L);

            if (photoFragment.getContext() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                Log_OC.d(this,
                         "Start gallery search since "
                             + dateFormat.format(new Date(endDate * 1000L))
                             + " with limit: "
                             + limit);
                RemoteOperationResult result = searchRemoteOperation.execute(user, photoFragment.getContext());

                if (result.isSuccess()) {
                    long lastTimeStamp = findLastTimestamp(result.getData());

                    //query the local storage based on the lastTimeStamp retrieved, not by 1970-01-01
                    boolean emptySearch = parseMedia(lastTimeStamp, this.endDate, result.getData());
                    return new Result(result.isSuccess(), emptySearch, lastTimeStamp);
                } else {
                    return new Result(false, false, -1);
                }
            } else {
                return new Result(false, false, -1);
            }
        }
    }

    @Override
    protected void onPostExecute(GallerySearchTask.Result result) {
        if (photoFragmentWeakReference.get() != null) {
            GalleryFragment photoFragment = photoFragmentWeakReference.get();
            photoFragment.searchCompleted(result.emptySearch, result.lastTimestamp);
        }
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

        List<OCFile> localFiles = storageManager.getGalleryItems(startDate * 1000L, endDate * 1000L);

        if (BuildConfig.DEBUG) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Log_OC.d(this,
                     "parseMedia - start: "
                         + dateFormat.format(new Date(startDate * 1000L))
                         + " - "
                         + dateFormat.format(new Date(endDate * 1000L)));

            for (OCFile localFile : localFiles) {
                Log_OC.d(this,
                         "local file: modified: "
                             + dateFormat.format(new Date(localFile.getModificationTimestamp()))
                             + " path: "
                             + localFile.getRemotePath());
            }
        }

        Map<String, OCFile> localFilesMap = RefreshFolderOperation.prefillLocalFilesMap(null, localFiles);

        long filesAdded = 0, filesUpdated = 0, filesDeleted = 0, unchangedFiles = 0;

        for (Object file : remoteFiles) {
            OCFile ocFile = FileStorageUtils.fillOCFile((RemoteFile) file);

            if (BuildConfig.DEBUG) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                Log_OC.d(this,
                         "remote file: modified: "
                             + dateFormat.format(new Date(ocFile.getModificationTimestamp()))
                             + " path: "
                             + ocFile.getRemotePath());
            }

            OCFile localFile = localFilesMap.remove(ocFile.getRemotePath());

            if (localFile == null) {
                // add new file
                storageManager.saveFile(ocFile);
                filesAdded++;
            } else if (!localFile.getEtag().equals(ocFile.getEtag())) {
                // update file
                ocFile.setLastSyncDateForData(System.currentTimeMillis());
                storageManager.saveFile(ocFile);
                filesUpdated++;
            } else {
                unchangedFiles++;
            }
        }

        // existing files to remove
        filesDeleted = localFilesMap.values().size();

        for (OCFile file : localFilesMap.values()) {
            if (BuildConfig.DEBUG) {
                Log_OC.d(this, "Gallery Sync: File deleted " + file.getRemotePath());
            }

            storageManager.removeFile(file, true, true);
        }

        if (BuildConfig.DEBUG) {
            Log_OC.d(this, "Gallery search result:" +
                " new: " + filesAdded +
                " updated: " + filesUpdated +
                " deleted: " + filesDeleted +
                " unchanged: " + unchangedFiles);
        }
        final long totalFiles = filesAdded + filesUpdated + filesDeleted + unchangedFiles;
        return totalFiles <= 0;
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

