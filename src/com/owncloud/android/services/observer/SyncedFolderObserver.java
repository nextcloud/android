package com.owncloud.android.services.observer;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.FileObserver;
import android.os.PersistableBundle;
import android.util.Log;

import com.owncloud.android.MainApp;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.services.SyncedFolderJobService;
import com.owncloud.android.utils.RecursiveFileObserver;

import java.io.File;
import java.util.Date;

class SyncedFolderObserver extends RecursiveFileObserver {

    private Context context;

    private static final int MY_BACKGROUND_JOB = 0;
    public static final String TAG = "SyncedFolderObserver";
    private String remoteFolder;


    public SyncedFolderObserver(String path, String remoteFolder) {
        super(path, FileObserver.CREATE + FileObserver.MOVED_TO);

        context = MainApp.getAppContext();
        this.remoteFolder = remoteFolder;
        Log_OC.d("SyncedFolderObserver", "Started watching: " + path);
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onEvent(int event, String path) {
        PersistableBundle bundle = new PersistableBundle();
        bundle.putString("filePath", path);
        bundle.putString("remoteFolder", remoteFolder);
        bundle.putLong("dateTaken", new Date().getTime());

        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo job = new JobInfo.Builder(
                MY_BACKGROUND_JOB,
                new ComponentName(context, SyncedFolderJobService.class))
                .setRequiresCharging(false)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setExtras(bundle)
                .build();

        Integer result = js.schedule(job);
        if (result <= 0) {
            Log_OC.d(TAG, "Job failed to start: " + result);
        }

        Log.d(TAG, "Event: " + event + " Path: " + path);
    }
}
