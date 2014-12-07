package com.owncloud.android.datamodel;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.services.observer.InstantUploadFolderObserverService;

public class InstantUploadPreference {
    private String id;
    private boolean onlyWiFi = false;
    private boolean onlyCharging = false;
    private String localPath;
    private String remotePath;
    private SharedPreferences sharedPref;
    private Context context;

    private static final String TAG = "InstantUploadPreferences";


    public InstantUploadPreference(Context context, String id){
        this.id = id;
        this.context = context;
        sharedPref = context.getSharedPreferences("instantUploadPref_" + id, Context.MODE_PRIVATE);

        try {
            localPath =     sharedPref.getString("iu_local", "");
            Log_OC.d(TAG, "read local: " + localPath);
        } catch (Exception e){
            Log_OC.e(TAG, "local: " + e.toString());
        }

        try {
            remotePath =    sharedPref.getString("iu_remote", "");
            Log_OC.d(TAG, "read remote: " + remotePath);
        } catch (Exception e){
            Log_OC.e(TAG, "remote: " + e.toString());
        }

        try {
            onlyWiFi =      sharedPref.getBoolean("iu_wifi", false);
            Log_OC.d(TAG, "read onlyWiFi: " + onlyWiFi);
        } catch (Exception e){
            Log_OC.e(TAG, "wifi: " + e.toString());
        }

        try {
            onlyCharging =  sharedPref.getBoolean("iu_charging", false);
            Log_OC.d(TAG, "read onlyCharging: " + onlyCharging);
        } catch (Exception e){
            Log_OC.e(TAG, "charging: " + e.toString());
        }
    }

    public boolean saveAll(){
        // TODO Fehler abfangen: falscher lokaler Pfad, falscher Syntax

        if (InstantUploadFolderObserverService.checkIfLocalPathIsObserved(localPath)){
            Toast.makeText(context, "Local path is already observed", Toast.LENGTH_LONG).show();
            return false;
        }

        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString("iu_local", localPath)
        .putString("iu_remote", remotePath)
        .putBoolean("iu_wifi", onlyWiFi)
        .putBoolean("iu_charging", onlyCharging);
        editor.apply();

        Log_OC.d(TAG, "put local: " + localPath);
        Log_OC.d(TAG, "put remote: " + remotePath);
        Log_OC.d(TAG, "put wifi: " + onlyWiFi);
        Log_OC.d(TAG, "put charging: " + onlyCharging);

        return true;
    }

    public String toString(){
        return localPath;
    }

    public String getId(){
        return id;
    }

    public boolean isOnlyWiFi() {
        return onlyWiFi;
    }

    public void setOnlyWiFi(boolean onlyWiFi) {
        this.onlyWiFi = onlyWiFi;
    }

    public boolean isOnlyCharging() {
        return onlyCharging;
    }

    public void setOnlyCharging(boolean onlyCharging) {
        this.onlyCharging = onlyCharging;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }
}
