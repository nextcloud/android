/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
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
package com.owncloud.android.utils;

import android.content.Context;

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import java.util.Set;

/**
 * Utilities to handle jobs
 */

public class JobUtils {

    public static void rescheduleJobsWithNetworkRequirements(Context context) {
        JobManager jobManager = JobManager.create(context);
        Set<JobRequest> jobRequests = jobManager.getAllJobRequests();
        for (JobRequest jobRequest : jobRequests) {
                // check that a job has network requirements
                if (!jobRequest.requiredNetworkType().equals(JobRequest.NetworkType.ANY))  {
                    JobRequest.Builder builder = jobRequest.cancelAndEdit();
                    builder.setExecutionWindow(15L, 25L);
                    builder.build().schedule();
                }
        }
    }

}
