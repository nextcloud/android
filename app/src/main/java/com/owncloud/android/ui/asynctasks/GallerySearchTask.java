/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // Sort by folder-date (YYYY/MM or YYYY/MM/DD in path) first, then by timestamp.
        // Files with folder-dates come first (newest folder → newest files).
        Collections.sort(localFiles, (a, b) -> {
            String pa = a.getRemotePath() == null ? "" : a.getRemotePath();
            String pb = b.getRemotePath() == null ? "" : b.getRemotePath();

            int[] da = extractYmdFromPath(pa);
            int[] db = extractYmdFromPath(pb);

            if (da != null && db != null) {
                // compare folder date descending (newest folder first)
                if (da[0] != db[0]) return Integer.compare(db[0], da[0]); // year
                if (da[1] != db[1]) return Integer.compare(db[1], da[1]); // month
                if (da[2] != db[2]) return Integer.compare(db[2], da[2]); // day (0 if absent)
                // same folder -> newest file first
                return Long.compare(b.getModificationTimestamp(), a.getModificationTimestamp());
            } else if (da != null) {
                return -1; // a has folder-date => comes before b
            } else if (db != null) {
                return 1;  // b has folder-date => comes before a
            } else {
                // neither has folder-date => newest first by timestamp
                return Long.compare(b.getModificationTimestamp(), a.getModificationTimestamp());
            }
        });

        Map<String, OCFile> localFilesMap = RefreshFolderOperation.prefillLocalFilesMap(null, localFiles);

        long filesAdded = 0, filesUpdated = 0, unchangedFiles = 0;

        for (Object file : remoteFiles) {
            if (!(file instanceof RemoteFile remoteFile)) {
                Log_OC.d(this, "object file is not remote file");
                continue;
            }

            final OCFile existingFile = storageManager.getFileByDecryptedRemotePath(remoteFile.getRemotePath());

            // add missing values from local storage to prevent override with null values
            if (existingFile != null) {
                final var imageDimension = existingFile.getImageDimension();
                if (imageDimension != null) {
                    remoteFile.setImageDimension(existingFile.getImageDimension());
                }
                remoteFile.setLocalId(existingFile.getLocalId());
                remoteFile.setCreationTimestamp(existingFile.getCreationTimestamp());
                remoteFile.setUploadTimestamp(existingFile.getUploadTimestamp());
            }

            OCFile ocFile = FileStorageUtils.fillOCFile(remoteFile);

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
        long filesDeleted = localFilesMap.size();

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

        return filesAdded <= 0 && filesUpdated <= 0 && filesDeleted <= 0;
    }

    /**
     * Extract YYYY/MM or YYYY/MM/DD from a file path.
     * @return int[]{year, month, day} where day=0 if only YYYY/MM present, or null if no match.
     */
    private static final Pattern FOLDER_DATE_PATTERN = Pattern.compile("/(\\d{4})/(\\d{1,2})(?:/(\\d{1,2}))?/");

    private static int[] extractYmdFromPath(String path) {
        Matcher m = FOLDER_DATE_PATTERN.matcher(path);
        if (m.find()) {
            int y = Integer.parseInt(m.group(1));
            int mo = Integer.parseInt(m.group(2));
            int d = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
            return new int[]{y, mo, d};
        }
        return null;
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

