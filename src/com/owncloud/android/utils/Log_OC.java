package com.owncloud.android.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.os.Environment;
import android.util.Log;


public class Log_OC {
    

    private static boolean isEnabled = false;
    private static File logFile;
    private static File folder;
    private static BufferedWriter buf;  
    
    public static void i(String TAG, String message){
        // Printing the message to LogCat console
        int a = Log.i(TAG, message);
        // Write the log message to the file
        appendLog(TAG+" : "+message);
    }

    public static void d(String TAG, String message){
        Log.d(TAG, message);
        appendLog(TAG + " : " + message);
    }
    public static void d(String TAG, String message, Exception e) {
        Log.d(TAG, message, e);
        appendLog(TAG + " : " + message + " Exception : "+ e.getStackTrace());
    }
    public static void e(String TAG, String message){
        Log.e(TAG, message);
        appendLog(TAG + " : " + message);
    }
    
    public static void e(String TAG, String message, Throwable e) {
        Log.e(TAG, message, e);
        appendLog(TAG+" : " + message +" Exception : " + e.getStackTrace());
    }
    
    public static void v(String TAG, String message){
        Log.v(TAG, message);
        appendLog(TAG+" : "+ message);
    }
    
    public static void w(String TAG, String message) {
        Log.w(TAG,message); 
        appendLog(TAG+" : "+ message);
    }
    
    public static void wtf(String TAG, String message) {
        Log.wtf(TAG,message); 
        appendLog(TAG+" : "+ message);
    }
    
    public static void startLogging(String logPath) {
        folder = new File(logPath);
        logFile = new File(folder + File.separator + "log.txt");
        
        boolean isFileCreated = false;
        
        if (!folder.exists()) {
            folder.mkdirs();
            isFileCreated = true;
            Log.d("LOG_OC", "Log file created");
        }

        try { 
            logFile.createNewFile();
            buf = new BufferedWriter(new FileWriter(logFile, true));
            isEnabled = true;
            if (isFileCreated) {
                appendPhoneInfo();
            }
        }catch (IOException e){ 
            e.printStackTrace(); 
        } 
    }
    
    public static void stopLogging() {
        if (logFile != null) {
            isEnabled = false;
            try {
                buf = new BufferedWriter(new FileWriter(logFile, false));
                buf.append("");
                buf.close();
            } catch (IOException e) {
                e.printStackTrace();
            } 
        }
    }
    
    private static void appendPhoneInfo() {
        appendLog("Model : " + android.os.Build.MODEL);
        appendLog("Brand : " + android.os.Build.BRAND);
        appendLog("Product : " + android.os.Build.PRODUCT);
        appendLog("Device : " + android.os.Build.DEVICE);
        appendLog("Version-Codename : " + android.os.Build.VERSION.CODENAME);
        appendLog("Version-Release : " + android.os.Build.VERSION.RELEASE);
    }
    
    private static void appendLog(String text) { 
        if (isEnabled) {
            String logPath = Environment.getExternalStorageDirectory()+File.separator+"owncloud"+File.separator+"log";
            startLogging(logPath);
            String timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());

            try {
               buf = new BufferedWriter(new FileWriter(logFile, true));
               buf.write(timeStamp + " -> " +text); 
               buf.newLine();
               buf.close();
           } catch (IOException e) { 
               e.printStackTrace(); 
        } 
    }
}

    
   

  

   
   
}
