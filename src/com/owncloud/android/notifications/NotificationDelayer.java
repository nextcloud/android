package com.owncloud.android.notifications;

import java.util.Random;

import android.app.NotificationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

public class NotificationDelayer {
    
    public static void cancelWithDelay(
            final NotificationManager notificationManager,
            final int notificationId,
            long delayInMillis) {
    
        HandlerThread thread = new HandlerThread(
                "NotificationDelayerThread_" + (new Random(System.currentTimeMillis())).nextInt(),
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        
        Handler handler = new Handler(thread.getLooper()); 
        handler.postDelayed(new Runnable() { 
             public void run() { 
                 notificationManager.cancel(notificationId);
                 ((HandlerThread)Thread.currentThread()).getLooper().quit();
             } 
        }, delayInMillis); 
    
    }
    

}
