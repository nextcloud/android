/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud GmbH
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

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

/**
 * Job creator for android-job
 */

public class NCJobCreator implements JobCreator {
    @Override
    public Job create(String tag) {
        switch (tag) {
            case ContactsBackupJob.TAG:
                return new ContactsBackupJob();
            case ContactsImportJob.TAG:
                return new ContactsImportJob();
            case AccountRemovalJob.TAG:
                return new AccountRemovalJob();
            case FilesSyncJob.TAG:
                return new FilesSyncJob();
            case OfflineSyncJob.TAG:
                return new OfflineSyncJob();
            default:
                return null;
        }
    }
}
