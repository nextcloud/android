/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.asynctasks;

import android.os.AsyncTask;

import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;

/**
 * Asynchronous task checking if there is space enough to copy all the files chosen to upload into the ownCloud local
 * folder. Maybe an AsyncTask is not strictly necessary, but who really knows.
 */
public class CheckAvailableSpaceTask extends AsyncTask<Boolean, Void, Boolean> {

    private String[] paths;
    private CheckAvailableSpaceListener callback;

    public CheckAvailableSpaceTask(CheckAvailableSpaceListener callback, String... paths) {
        this.paths = paths;
        this.callback = callback;
    }

    /**
     * Updates the UI before trying the movement.
     */
    @Override
    protected void onPreExecute() {
        callback.onCheckAvailableSpaceStart();
    }

    /**
     * Checks the available space.
     *
     * @param params boolean flag if storage calculation should be done.
     * @return 'True' if there is space enough or doesn't have to be calculated
     */
    @Override
    protected Boolean doInBackground(Boolean... params) {
        File localFile;
        long total = 0;
        for (int i = 0; paths != null && i < paths.length; i++) {
            String localPath = paths[i];
            localFile = new File(localPath);
            total += localFile.length();
        }
        return FileStorageUtils.getUsableSpace() >= total;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        callback.onCheckAvailableSpaceFinish(result, paths);
    }

    public interface CheckAvailableSpaceListener {
        void onCheckAvailableSpaceStart();

        void onCheckAvailableSpaceFinish(boolean hasEnoughSpaceAvailable, String... filesToUpload);
    }
}
