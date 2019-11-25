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
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.SearchRemoteOperation;
import com.owncloud.android.ui.activity.ToolbarActivity;
import com.owncloud.android.ui.adapter.OCFileListAdapter;
import com.owncloud.android.ui.fragment.ExtendedListFragment;
import com.owncloud.android.ui.fragment.PhotoFragment;

import java.lang.ref.WeakReference;

public class PhotoSearchTask extends AsyncTask<Void, Void, RemoteOperationResult> {

    private int columnCount;
    private User user;
    private WeakReference<PhotoFragment> photoFragmentWeakReference;
    private SearchRemoteOperation searchRemoteOperation;
    private FileDataStorageManager storageManager;

    public PhotoSearchTask(int columnsCount,
                           PhotoFragment photoFragment,
                           User user,
                           SearchRemoteOperation searchRemoteOperation,
                           FileDataStorageManager storageManager) {
        this.columnCount = columnsCount;
        this.user = user;
        this.photoFragmentWeakReference = new WeakReference<>(photoFragment);
        this.searchRemoteOperation = searchRemoteOperation;
        this.storageManager = storageManager;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (photoFragmentWeakReference.get() == null) {
            return;
        }
        PhotoFragment photoFragment = photoFragmentWeakReference.get();
        photoFragment.setPhotoSearchQueryRunning(true);
    }

    @Override
    protected RemoteOperationResult doInBackground(Void... voids) {
        if (photoFragmentWeakReference.get() == null) {
            return new RemoteOperationResult(new Exception("Photo fragment is null"));
        }
        PhotoFragment photoFragment = photoFragmentWeakReference.get();
        OCFileListAdapter adapter = photoFragment.getAdapter();

        if (isCancelled()) {
            return new RemoteOperationResult(new Exception("Cancelled"));
        } else {
            int limit = 15 * columnCount;

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
            PhotoFragment photoFragment = photoFragmentWeakReference.get();

            if (result.isSuccess() && result.getData() != null && !isCancelled()) {
                if (result.getData() == null || result.getData().size() == 0) {
                    photoFragment.setSearchDidNotFindNewPhotos(true);
                } else {
                    OCFileListAdapter adapter = photoFragment.getAdapter();

                    adapter.setData(result.getData(),
                                    ExtendedListFragment.SearchType.PHOTO_SEARCH,
                                    storageManager,
                                    null,
                                    false);
                    adapter.notifyDataSetChanged();
                    Log_OC.d(this, "Search: count: " + result.getData().size() + " total: " + adapter.getFiles().size());
                }
            }

            final ToolbarActivity fileDisplayActivity = (ToolbarActivity) photoFragment.getActivity();

            if (fileDisplayActivity != null) {
                fileDisplayActivity.setIndeterminate(false);
            }

            if (!result.isSuccess() && !isCancelled()) {
                photoFragment.setEmptyListMessage(ExtendedListFragment.SearchType.PHOTO_SEARCH);
            }

            photoFragment.setPhotoSearchQueryRunning(false);
        }
    }
}
