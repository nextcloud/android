package com.owncloud.android.services.observer;

import java.io.File;

import com.owncloud.android.lib.common.utils.Log_OC;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

public class InstantUploadFolderObserverService extends Service {
    private InstantUploadFolderObserver fileOb;
    //private static final int MAX_FO = 1;
    //private List<MyFileObserver> fileOb_list = new ArrayList<MyFileObserver>();
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
}
