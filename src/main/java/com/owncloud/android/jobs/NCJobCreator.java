/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud GmbH
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
import android.text.TextUtils;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.core.Clock;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.owncloud.android.datamodel.UploadsStorageManager;

import org.greenrobot.eventbus.EventBus;

import androidx.annotation.NonNull;

/**
 * Job creator for android-job
 */

public class NCJobCreator implements JobCreator {

    private final UserAccountManager accountManager;
    private final UploadsStorageManager uploadsStorageManager;
    private final Clock clock;
    private final EventBus eventBus;
    private final BackgroundJobManager backgroundJobManager;

    public NCJobCreator(
        UserAccountManager accountManager,
        UploadsStorageManager uploadsStorageManager,
        Clock clock,
        EventBus eventBus,
        BackgroundJobManager backgroundJobManager
    ) {
        this.accountManager = accountManager;
        this.uploadsStorageManager = uploadsStorageManager;
        this.clock = clock;
        this.eventBus = eventBus;
        this.backgroundJobManager = backgroundJobManager;
    }

    @Override
    public Job create(@NonNull String tag) {
        if (TextUtils.equals(tag, AccountRemovalJob.TAG)) {
            return new AccountRemovalJob(uploadsStorageManager,
                                         accountManager,
                                         backgroundJobManager,
                                         clock,
                                         eventBus);
        } else {
            return null;
        }
    }
}
