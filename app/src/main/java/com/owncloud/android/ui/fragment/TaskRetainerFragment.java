/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2016 David A. Velasco <dvelasco@owncloud.com>
 * SPDX-License-Identifier: GPL-2.0-only
 */
package com.owncloud.android.ui.fragment;

import android.content.Context;
import android.os.Bundle;

import com.owncloud.android.ui.activity.ReceiveExternalFilesActivity;
import com.owncloud.android.ui.asynctasks.CopyAndUploadContentUrisTask;

import androidx.fragment.app.Fragment;

/**
 * Fragment retaining a background task across configuration changes.
 */
public class TaskRetainerFragment extends Fragment {

    public static final String FTAG_TASK_RETAINER_FRAGMENT = "TASK_RETAINER_FRAGMENT";

    private CopyAndUploadContentUrisTask mTask;

    /**
     * Updates the listener of the retained task whenever the parent
     * Activity is attached.
     *
     * Since its done in main thread, and provided the AsyncTask only accesses
     * the listener in the main thread (should so), no sync problem should occur.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mTask != null) {
            if (context instanceof ReceiveExternalFilesActivity) {
                mTask.setListener((CopyAndUploadContentUrisTask.OnCopyTmpFilesTaskListener) context);
            } else {
                mTask.setListener(null);
            }
        }
    }

    /**
     * Only called once, since the instance is retained across configuration changes
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);    // the key point
    }

    /**
     * Sets the task to retain across configuration changes
     *
     * @param task  Task to retain
     */
    public void setTask(CopyAndUploadContentUrisTask task) {
        if (mTask != null) {
            mTask.setListener(null);
        }
        mTask = task;
        Context context = getContext();
        if (mTask != null && context != null) {
            if (context instanceof ReceiveExternalFilesActivity) {
                task.setListener((CopyAndUploadContentUrisTask.OnCopyTmpFilesTaskListener) context);
            } else {
                task.setListener(null);
            }
        }
    }
}
