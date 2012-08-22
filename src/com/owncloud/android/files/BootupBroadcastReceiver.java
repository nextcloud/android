package com.owncloud.android.files;

import com.owncloud.android.files.services.FileObserverService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootupBroadcastReceiver extends BroadcastReceiver {

    private static String TAG = "BootupBroadcastReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.wtf(TAG, "Incorrect action sent " + intent.getAction());
            return;
        }
        Log.d(TAG, "Starting file observer service...");
        Intent i = new Intent(context, FileObserverService.class);
        i.putExtra(FileObserverService.KEY_FILE_CMD,
                   FileObserverService.CMD_INIT_OBSERVED_LIST);
        context.startService(i);
        Log.d(TAG, "DONE");
    }

}
