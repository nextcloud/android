package com.owncloud.android.services.observer;

import java.io.File;
import java.util.HashSet;

import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.InstantUploadPreference;
import com.owncloud.android.lib.common.utils.Log_OC;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.widget.Toast;

public class InstantUploadFolderObserverService extends Service {
    private InstantUploadFolderObserver fileOb;
    private static final String TAG = "InstantUploadFolderObserverService";
    //private static final int MAX_FO = 1;
    //private List<MyFileObserver> fileOb_list = new ArrayList<MyFileObserver>();

    // TODO Tobi: restart Service on change of setting (crud)

    @Override
    public void onCreate() {
        File sdcard = new File("/mnt/sdcard/WhatsApp/Media");
        Log_OC.d("InstantUploadFolderObserverService", "watching file: " + sdcard.getAbsolutePath());
        fileOb = new InstantUploadFolderObserver(sdcard.getAbsolutePath(), "WhatsApp");
        fileOb.startWatching();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        Log_OC.d("InstantUploadFolderObserverService", "start");
        return Service.START_NOT_STICKY;
        //return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onStart(Intent intent, int startid) {
        Log_OC.d("InstantUploadFolderObserverService", "start");
        fileOb.startWatching();
        /*for (int i = 0; i < fileOb_list.size(); ++i) {
            fileOb_list.get(i).startWatching();
        }*/
        Toast.makeText(this.getApplicationContext(), "start monitoring file modification", Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onDestroy() {
        fileOb.stopWatching();
        /*for (int i = 0; i < fileOb_list.size(); ++i) {
            fileOb_list.get(i).stopWatching();
        }*/
        Toast.makeText(this.getApplicationContext(), "stop monitoring file modification", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }


    public static HashSet<InstantUploadPreference> getAll(){
        SharedPreferences settings = MainApp.getAppContext().
                          getSharedPreferences("InstantUploadPreferences", Context.MODE_PRIVATE);
        String preferencesString = settings.getString("preferences", "");
        Log_OC.d(TAG, preferencesString);

        HashSet<InstantUploadPreference> preferencesSet = new HashSet<InstantUploadPreference>();
        for (String string : preferencesString.replace("[", "").replace("]","").replaceAll("\\s+", "").split(",")) {
            preferencesSet.add(new InstantUploadPreference(MainApp.getAppContext(), string));
        }
        
        return preferencesSet;
    }

    public static void delete(String mInstantUpload) {
        SharedPreferences settings = MainApp.getAppContext().
                          getSharedPreferences("InstantUploadPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        
        String preferencesString = settings.getString("preferences", "");

        HashSet<String> preferencesStringSet = new HashSet<String>();
        for (String string : preferencesString.replace("[", "").replace("]","").replaceAll("\\s+", "").split(",")) {
            preferencesStringSet.add(string);
        }
        
        preferencesStringSet.remove(mInstantUpload);
        SharedPreferences sharedPref = MainApp.getAppContext().
                          getSharedPreferences("instantUploadPref_" + mInstantUpload, Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
        sharedPrefEditor.clear();
        sharedPrefEditor.apply();
        
        editor.putString("preferences", preferencesStringSet.toString());
        editor.apply();
        
        Log_OC.d(TAG, preferencesStringSet.toString());
    }
    
    public static String add(){
        SharedPreferences settings = MainApp.getAppContext().
                          getSharedPreferences("InstantUploadPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        
        String preferencesString = settings.getString("preferences", "");

        HashSet<String> preferencesStringSet = new HashSet<String>();
        for (String string : preferencesString.replace("[", "").replace("]","").replaceAll("\\s+", "").split(",")) {
            preferencesStringSet.add(string);
        }
         
        String newValue = String.valueOf(System.currentTimeMillis());
        preferencesStringSet.add(newValue);
         
        editor.putString("preferences", preferencesStringSet.toString());
        editor.apply();
        
        Log_OC.d(TAG, preferencesStringSet.toString());
        
        return newValue;
    }
    
    public static boolean checkIfLocalPathIsObserved(String localPath){
        for (InstantUploadPreference i : getAll()){
            if (!i.getLocalPath().equals("") && (i.getLocalPath().startsWith(localPath)
                || localPath.startsWith(i.getLocalPath()) 
                || localPath.equalsIgnoreCase(i.getLocalPath()))){
                return true;
            }
        }
        return false;
    }
}
