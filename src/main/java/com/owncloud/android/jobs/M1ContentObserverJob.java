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
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.MediaFolder;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.utils.FilesSyncHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class M1ContentObserverJob extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(MainApp.getAppContext().
                getContentResolver());

        List<SyncedFolder> syncedFolderListImages = new ArrayList<>();
        List<SyncedFolder> syncedFolderListVideos = new ArrayList<>();

        for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
            if (MediaFolder.IMAGE == syncedFolder.getType()) {
                syncedFolderListImages.add(syncedFolder);
            } else if (MediaFolder.VIDEO == syncedFolder.getType()) {
                syncedFolderListVideos.add(syncedFolder);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (params.getJobId() == FilesSyncHelper.ContentSyncJobId) {
                if (params.getTriggeredContentAuthorities() != null && params.getTriggeredContentUris() != null
                        && params.getTriggeredContentUris().length > 0) {
                    new JobRequest.Builder(FilesSyncJob.TAG)
                            .setExecutionWindow(1, TimeUnit.SECONDS.toMillis(2))
                            .setBackoffCriteria(TimeUnit.SECONDS.toMillis(5), JobRequest.BackoffPolicy.LINEAR)
                            .setUpdateCurrent(false)
                            .build()
                            .schedule();
                }
            }
        }

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
