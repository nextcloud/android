package com.owncloud.android.services.observer;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.FileObserver;
import android.util.Log;

import com.owncloud.android.MainApp;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.RecursiveFileObserver;

import java.io.File;

public class SyncedFolderObserver extends RecursiveFileObserver {

    File dirToWatch;
    String remoteFolder;
    Context context;

    public static final int MY_BACKGROUND_JOB = 0;


    public SyncedFolderObserver(String path, String remoteFolder) {
        super(path, FileObserver.CREATE + FileObserver.MOVED_TO);

        context = MainApp.getAppContext();
        dirToWatch = new File(path);
        this.remoteFolder = remoteFolder;
        Log_OC.d("SyncedFolderObserver", "Started watching: "+ path);
    }



    @Override
    public void onEvent(int event, String path) {
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
//        JobInfo job = new JobInfo.Builder(
//                MY_BACKGROUND_JOB,
//                new ComponentName(context, MyJobService.class))
//                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
//                .setRequiresCharging(true)
//                .build();
//        js.schedule(job);

        Log.d("SyncedFolder", "Event: " + event + " Path: " + path);
    }
}
