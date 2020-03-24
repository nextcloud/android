/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud GmbH
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.jobs;

import android.content.Context;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.core.Clock;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.datamodel.UploadsStorageManager;

import org.greenrobot.eventbus.EventBus;

import androidx.annotation.NonNull;

/**
 * Job creator for android-job
 */

public class NCJobCreator implements JobCreator {

    private final Context context;
    private final UserAccountManager accountManager;
    private final AppPreferences preferences;
    private final UploadsStorageManager uploadsStorageManager;
    private final ConnectivityService connectivityService;
    private final PowerManagementService powerManagementService;
    private final Clock clock;
    private final EventBus eventBus;

    public NCJobCreator(
        Context context,
        UserAccountManager accountManager,
        AppPreferences preferences,
        UploadsStorageManager uploadsStorageManager,
        ConnectivityService connectivityServices,
        PowerManagementService powerManagementService,
        Clock clock,
        EventBus eventBus
    ) {
        this.context = context;
        this.accountManager = accountManager;
        this.preferences = preferences;
        this.uploadsStorageManager = uploadsStorageManager;
        this.connectivityService = connectivityServices;
        this.powerManagementService = powerManagementService;
        this.clock = clock;
        this.eventBus = eventBus;
    }

    @Override
    public Job create(@NonNull String tag) {
        switch (tag) {
            case ContactsBackupJob.TAG:
                return new ContactsBackupJob(accountManager);
            case ContactsImportJob.TAG:
                return new ContactsImportJob();
            case AccountRemovalJob.TAG:
                return new AccountRemovalJob(uploadsStorageManager, accountManager, clock, eventBus);
            case FilesSyncJob.TAG:
                return new FilesSyncJob(accountManager,
                                        preferences,
                                        uploadsStorageManager,
                                        connectivityService,
                                        powerManagementService,
                                        clock);
            case OfflineSyncJob.TAG:
                return new OfflineSyncJob(accountManager, connectivityService, powerManagementService);
            case NotificationJob.TAG:
                return new NotificationJob(context, accountManager);
            case MediaFoldersDetectionJob.TAG:
                return new MediaFoldersDetectionJob(accountManager, clock);
            default:
                return null;
        }
    }
}
