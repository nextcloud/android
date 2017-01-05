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
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.services.SyncedFolderJobService;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.RecursiveFileObserver;

import java.io.File;
import java.util.Date;

class SyncedFolderObserver extends RecursiveFileObserver {

    private Context context;

    public static final String TAG = "SyncedFolderObserver";
    private SyncedFolder syncedFolder;


    public SyncedFolderObserver(SyncedFolder syncedFolder) {
        super(syncedFolder.getLocalPath(), FileObserver.CREATE + FileObserver.MOVED_TO);

        context = MainApp.getAppContext();
        this.syncedFolder = syncedFolder;
        Log_OC.d("SyncedFolderObserver", "Started watching: " + syncedFolder.getLocalPath());
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onEvent(int event, String path) {
        Log.d(TAG, "Event: " + event + " Path: " + path);

        File temp = new File(path);

        // do not upload "null"-files, test if file exists and is a real file
        if (!temp.getName().equalsIgnoreCase("null") && temp.isFile() && !temp.getName().endsWith(".tmp")) {
            PersistableBundle bundle = new PersistableBundle();
            // TODO extract
            bundle.putString(SyncedFolderJobService.LOCAL_PATH, path);
            bundle.putString(SyncedFolderJobService.REMOTE_PATH, FileStorageUtils.getInstantUploadFilePath(
                                                                 syncedFolder.getRemotePath(), temp.getName(),
                                                                 new Date().getTime(),
                                                                 syncedFolder.getSubfolderByDate()));
            bundle.putString(SyncedFolderJobService.ACCOUNT, syncedFolder.getAccount());
            bundle.putInt(SyncedFolderJobService.UPLOAD_BEHAVIOUR, syncedFolder.getUploadAction());

            JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            Long date = new Date().getTime();
            JobInfo job = new JobInfo.Builder(
                    date.intValue(),
                    new ComponentName(context, SyncedFolderJobService.class))
                    .setRequiresCharging(syncedFolder.getChargingOnly())
                    .setMinimumLatency(10000)
                    .setRequiredNetworkType(syncedFolder.getWifiOnly() ? JobInfo.NETWORK_TYPE_UNMETERED : JobInfo.NETWORK_TYPE_ANY)
                    .setExtras(bundle)
                    .setPersisted(true)
                    .build();

            Integer result = js.schedule(job);
            if (result <= 0) {
                Log_OC.d(TAG, "Job failed to start: " + result);
            }
        }
    }
}
