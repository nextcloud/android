package com.owncloud.android.services;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

/**
 * Created by mdjanic on 21/02/2017.
 */

public class NCJobCreator implements JobCreator {
    @Override
    public Job create(String tag) {
        switch (tag) {
            case AutoUploadJob.TAG:
                return new AutoUploadJob();
            default:
                return null;
        }
    }
}
