/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2016 Andy Scherzinger
 * Copyright (C) 2016 Nextcloud
 * Copyright (C) 2016 ownCloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
