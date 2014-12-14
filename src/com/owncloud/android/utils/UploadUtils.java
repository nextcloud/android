package com.owncloud.android.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.os.BatteryManager;

import com.owncloud.android.db.UploadDbObject;


public class UploadUtils {

    public static boolean isCharging(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    public static boolean isConnectedViaWiFi(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI
                && cm.getActiveNetworkInfo().getState() == State.CONNECTED;
    }
    
    /**
     * Returns true when user is able to cancel this upload. That is, when
     * upload is currently in progress or scheduled for upload.
     */
    static public  boolean userCanCancelUpload(UploadDbObject uploadFile) {
        switch (uploadFile.getUploadStatus()) {
        case UPLOAD_IN_PROGRESS:
        case UPLOAD_LATER:
        case UPLOAD_FAILED_RETRY:
            return true;
        default:
            return false;
        }
    }

    /**
     * Returns true when user can choose to retry this upload. That is, when
     * user cancelled upload before or when upload has failed.
     */
    static public boolean userCanRetryUpload(UploadDbObject uploadFile) {
        switch (uploadFile.getUploadStatus()) {
        case UPLOAD_CANCELLED:
        case UPLOAD_FAILED_RETRY://automatically retried. no need for user option.
        case UPLOAD_FAILED_GIVE_UP: //TODO this case needs to be handled as described by
            // https://github.com/owncloud/android/issues/765#issuecomment-66490312
        case UPLOAD_LATER: //upload is already schedule but allow user to increase priority
        case UPLOAD_SUCCEEDED: // if user wants let him to re-upload (maybe
                               // remote file was deleted...)
            return true;
        default:
            return false;
        }
    }
}
