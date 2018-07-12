/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.jobs;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.utils.FilesSyncHelper;
import com.owncloud.android.utils.PowerUtils;

/*
    Job that triggers new FilesSyncJob in case new photo or video were detected
    and starts a job to find new media folders
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class NContentObserverJob extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (params.getJobId() == FilesSyncHelper.ContentSyncJobId && params.getTriggeredContentAuthorities()
                    != null && params.getTriggeredContentUris() != null
                    && params.getTriggeredContentUris().length > 0) {

                SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(getContentResolver());
                if (!PowerUtils.isPowerSaveMode(getApplicationContext()) && syncedFolderProvider
                        .countEnabledSyncedFolders() > 0) {
                    PersistableBundleCompat persistableBundleCompat = new PersistableBundleCompat();
                    persistableBundleCompat.putBoolean(FilesSyncJob.SKIP_CUSTOM, true);

                    new JobRequest.Builder(FilesSyncJob.TAG)
                            .startNow()
                            .setExtras(persistableBundleCompat)
                            .setUpdateCurrent(false)
                            .build()
                            .schedule();
                }

                new JobRequest.Builder(MediaFoldersDetectionJob.TAG)
                        .startNow()
                        .setUpdateCurrent(false)
                        .build()
                        .schedule();

            }

            FilesSyncHelper.scheduleJobOnN();
        }

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
