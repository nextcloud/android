/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2012  Bartek Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.files;

import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.FileStorageUtils;


public class InstantUploadBroadcastReceiver extends BroadcastReceiver {

    private static String TAG = InstantUploadBroadcastReceiver.class.getName();
    // Image action
    // Unofficial action, works for most devices but not HTC. See: https://github.com/owncloud/android/issues/6
    private static String NEW_PHOTO_ACTION_UNOFFICIAL = "com.android.camera.NEW_PICTURE";
    // Officially supported action since SDK 14:
    // http://developer.android.com/reference/android/hardware/Camera.html#ACTION_NEW_PICTURE
    private static String NEW_PHOTO_ACTION = "android.hardware.action.NEW_PICTURE";
    // Video action
    // Officially supported action since SDK 14:
    // http://developer.android.com/reference/android/hardware/Camera.html#ACTION_NEW_VIDEO
    private static String NEW_VIDEO_ACTION = "android.hardware.action.NEW_VIDEO";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log_OC.d(TAG, "Received: " + intent.getAction());
        if (intent.getAction().equals(NEW_PHOTO_ACTION_UNOFFICIAL)) {
            handleNewPictureAction(context, intent); 
            Log_OC.d(TAG, "UNOFFICIAL processed: com.android.camera.NEW_PICTURE");
        } else if (intent.getAction().equals(NEW_PHOTO_ACTION)) {
            handleNewPictureAction(context, intent); 
            Log_OC.d(TAG, "OFFICIAL processed: android.hardware.action.NEW_PICTURE");
        } else if (intent.getAction().equals(NEW_VIDEO_ACTION)) {
            handleNewVideoAction(context, intent);
            Log_OC.d(TAG, "OFFICIAL processed: android.hardware.action.NEW_VIDEO");            
        } else {
            Log_OC.e(TAG, "Incorrect intent sent: " + intent.getAction());
        }
    }

    /**
     * Because we support NEW_PHOTO_ACTION and NEW_PHOTO_ACTION_UNOFFICIAL it can happen that 
     * handleNewPictureAction is called twice for the same photo. Use this simple static variable to
     * remember last uploaded photo to filter duplicates. Must not be null!
     */
    static String lastUploadedPhotoPath = "";
    private void handleNewPictureAction(Context context, Intent intent) {
        Cursor c = null;
        String file_path = null;
        String file_name = null;
        String mime_type = null;

        Log_OC.w(TAG, "New photo received");
        
        if (!instantPictureUploadEnabled(context)) {
            Log_OC.d(TAG, "Instant picture upload disabled, ignoring new picture");
            return;
        }

        Account account = AccountUtils.getCurrentOwnCloudAccount(context);
        if (account == null) {
            Log_OC.w(TAG, "No ownCloud account found for instant upload, aborting");
            return;
        }

        String[] CONTENT_PROJECTION = { Images.Media.DATA, Images.Media.DISPLAY_NAME, Images.Media.MIME_TYPE,
                Images.Media.SIZE };
        c = context.getContentResolver().query(intent.getData(), CONTENT_PROJECTION, null, null, null);
        if (!c.moveToFirst()) {
            Log_OC.e(TAG, "Couldn't resolve given uri: " + intent.getDataString());
            return;
        }
        file_path = c.getString(c.getColumnIndex(Images.Media.DATA));
        file_name = c.getString(c.getColumnIndex(Images.Media.DISPLAY_NAME));
        mime_type = c.getString(c.getColumnIndex(Images.Media.MIME_TYPE));
        c.close();
        
        if(file_path.equals(lastUploadedPhotoPath)) {
            Log_OC.d(TAG, "Duplicate detected: " + file_path + ". Ignore.");
            return;
        }

        lastUploadedPhotoPath = file_path;
        Log_OC.d(TAG, "Path: " + file_path + "");        
        
        Intent i = new Intent(context, FileUploader.class);
        i.putExtra(FileUploader.KEY_ACCOUNT, account);
        i.putExtra(FileUploader.KEY_LOCAL_FILE, file_path);
        i.putExtra(FileUploader.KEY_REMOTE_FILE, FileStorageUtils.getInstantUploadFilePath(context, file_name));
        i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_SINGLE_FILE);
        i.putExtra(FileUploader.KEY_MIME_TYPE, mime_type);
        i.putExtra(FileUploader.KEY_CREATE_REMOTE_FOLDER, true);
        i.putExtra(FileUploader.KEY_WIFI_ONLY, instantPictureUploadViaWiFiOnly(context));

// On master
//        Intent i = new Intent(context, FileUploader.class);
//        i.putExtra(FileUploader.KEY_ACCOUNT, account);
//        i.putExtra(FileUploader.KEY_LOCAL_FILE, file_path);
//        i.putExtra(FileUploader.KEY_REMOTE_FILE, FileStorageUtils.getInstantUploadFilePath(context, file_name));
//        i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_SINGLE_FILE);
//        i.putExtra(FileUploader.KEY_MIME_TYPE, mime_type);
//        i.putExtra(FileUploader.KEY_INSTANT_UPLOAD, true);

        // instant upload behaviour
        i = addInstantUploadBehaviour(i, context);

        context.startService(i);
    }

    private Intent addInstantUploadBehaviour(Intent i, Context context){
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String behaviour = appPreferences.getString("prefs_instant_behaviour", "NOTHING");

        if (behaviour.equalsIgnoreCase("NOTHING")) {
            Log_OC.d(TAG, "upload file and do nothing");
            i.putExtra(FileUploader.KEY_LOCAL_BEHAVIOUR, FileUploader.LOCAL_BEHAVIOUR_FORGET);
        } else if (behaviour.equalsIgnoreCase("MOVE")) {
            i.putExtra(FileUploader.KEY_LOCAL_BEHAVIOUR, FileUploader.LOCAL_BEHAVIOUR_MOVE);
            Log_OC.d(TAG, "upload file and move file to oc folder");
        }
        return i;
    }

    private void handleNewVideoAction(Context context, Intent intent) {
        Cursor c = null;
        String file_path = null;
        String file_name = null;
        String mime_type = null;

        Log_OC.w(TAG, "New video received");
        
        if (!instantVideoUploadEnabled(context)) {
            Log_OC.d(TAG, "Instant video upload disabled, ignoring new video");
            return;
        }

        Account account = AccountUtils.getCurrentOwnCloudAccount(context);
        if (account == null) {
            Log_OC.w(TAG, "No owncloud account found for instant upload, aborting");
            return;
        }

        String[] CONTENT_PROJECTION = { Video.Media.DATA, Video.Media.DISPLAY_NAME, Video.Media.MIME_TYPE,
                Video.Media.SIZE };
        c = context.getContentResolver().query(intent.getData(), CONTENT_PROJECTION, null, null, null);
        if (!c.moveToFirst()) {
            Log_OC.e(TAG, "Couldn't resolve given uri: " + intent.getDataString());
            return;
        } 
        file_path = c.getString(c.getColumnIndex(Video.Media.DATA));
        file_name = c.getString(c.getColumnIndex(Video.Media.DISPLAY_NAME));
        mime_type = c.getString(c.getColumnIndex(Video.Media.MIME_TYPE));
        c.close();
        Log_OC.d(TAG, file_path + "");

        Intent i = new Intent(context, FileUploader.class);
        i.putExtra(FileUploader.KEY_ACCOUNT, account);
        i.putExtra(FileUploader.KEY_LOCAL_FILE, file_path);
        i.putExtra(FileUploader.KEY_REMOTE_FILE, FileStorageUtils.getInstantUploadFilePath(context, file_name));
        i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_SINGLE_FILE);
        i.putExtra(FileUploader.KEY_MIME_TYPE, mime_type);
        i.putExtra(FileUploader.KEY_CREATE_REMOTE_FOLDER, true);
        i.putExtra(FileUploader.KEY_WIFI_ONLY, instantVideoUploadViaWiFiOnly(context));
        context.startService(i);
// On master
//        if (!isOnline(context) || (instantVideoUploadViaWiFiOnly(context) && !isConnectedViaWiFi(context))) {
//            return;
//        }
//
//        Intent i = new Intent(context, FileUploader.class);
//        i.putExtra(FileUploader.KEY_ACCOUNT, account);
//        i.putExtra(FileUploader.KEY_LOCAL_FILE, file_path);
//        i.putExtra(FileUploader.KEY_REMOTE_FILE, FileStorageUtils.getInstantVideoUploadFilePath(context, file_name));
//        i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_SINGLE_FILE);
//        i.putExtra(FileUploader.KEY_MIME_TYPE, mime_type);
//        i.putExtra(FileUploader.KEY_INSTANT_UPLOAD, true);

        // instant upload behaviour
        i = addInstantUploadBehaviour(i, context);

        context.startService(i);

    }

    public static boolean instantPictureUploadEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("instant_uploading", false);
    }

    public static boolean instantVideoUploadEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("instant_video_uploading", false);
    }

    public static boolean instantPictureUploadViaWiFiOnly(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("instant_upload_on_wifi", false);
    }
    
    public static boolean instantVideoUploadViaWiFiOnly(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("instant_video_upload_on_wifi", false);
    }
}
