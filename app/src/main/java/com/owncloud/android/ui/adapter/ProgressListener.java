/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017-2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

import android.widget.ProgressBar;

import com.owncloud.android.db.OCUpload;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;

import java.lang.ref.WeakReference;

/**
 * Progress listener for file transfers.
 */
public class ProgressListener implements OnDatatransferProgressListener {
    private int mLastPercent;
    private OCUpload mUpload;
    private WeakReference<ProgressBar> mProgressBar;

    public ProgressListener(OCUpload upload, ProgressBar progressBar) {
        mUpload = upload;
        mProgressBar = new WeakReference<>(progressBar);
    }

    @Override
    public void onTransferProgress(
            long progressRate,
            long totalTransferredSoFar,
            long totalToTransfer,
            String fileAbsoluteName) {
        int percent = (int) (100.0 * ((double) totalTransferredSoFar) / ((double) totalToTransfer));

        if (percent != mLastPercent) {
            ProgressBar progressBar = mProgressBar.get();
            if (progressBar != null) {
                progressBar.setProgress(percent);
                progressBar.postInvalidate();
            }
        }

        mLastPercent = percent;
    }

    public boolean isWrapping(ProgressBar progressBar) {
        ProgressBar wrappedProgressBar = mProgressBar.get();
        return wrappedProgressBar != null && wrappedProgressBar == progressBar; // on purpose; don't replace with equals
    }

    public OCUpload getUpload() {
        return mUpload;
    }
}
