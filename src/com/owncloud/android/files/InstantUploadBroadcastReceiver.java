/**
 *  ownCloud Android client application
 *
 *  @author Bartek Przybylski
 *  @author David A. Velasco
 *  Copyright (C) 2012  Bartek Przybylski
 *  Copyright (C) 2016 ownCloud Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2,
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.files;

import android.Manifest;
import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.support.v4.content.ContextCompat;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;
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
            Log_OC.e(TAG, "Incorrect intent received: " + intent.getAction());
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
        long date_taken = 0;

        Log_OC.i(TAG, "New photo received");

        if (!PreferenceManager.instantPictureUploadEnabled(context)) {
            Log_OC.d(TAG, "Instant picture upload disabled, ignoring new picture");
            return;
        }

        Account account = FileStorageUtils.getInstantUploadAccount(context);
        if (account == null) {
            Log_OC.w(TAG, "No account found for instant upload, aborting");
            return;
        }

        String[] CONTENT_PROJECTION = {
                Images.Media.DATA, Images.Media.DISPLAY_NAME, Images.Media.MIME_TYPE, Images.Media.SIZE };

        int permissionCheck = ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE);

        if (android.content.pm.PackageManager.PERMISSION_GRANTED != permissionCheck) {
            Log_OC.w(TAG, "Read external storage permission isn't granted, aborting");
            return;
        }

        c = context.getContentResolver().query(intent.getData(), CONTENT_PROJECTION, null, null, null);
        if (!c.moveToFirst()) {
            Log_OC.e(TAG, "Couldn't resolve given uri: " + intent.getDataString());
            return;
        }
        file_path = c.getString(c.getColumnIndex(Images.Media.DATA));
        file_name = c.getString(c.getColumnIndex(Images.Media.DISPLAY_NAME));
        mime_type = c.getString(c.getColumnIndex(Images.Media.MIME_TYPE));
        date_taken = System.currentTimeMillis();
        c.close();

        if (file_path.equals(lastUploadedPhotoPath)) {
            Log_OC.d(TAG, "Duplicate detected: " + file_path + ". Ignore.");
            return;
        }

        lastUploadedPhotoPath = file_path;
        Log_OC.d(TAG, "Path: " + file_path + "");

        new FileUploader.UploadRequester();

        int behaviour = getUploadBehaviour(context);
        FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
        requester.uploadNewFile(
                context,
                account,
                file_path,
                FileStorageUtils.getInstantUploadFilePath(context, file_name, date_taken),
                behaviour,
                mime_type,
                true,           // create parent folder if not existent
                UploadFileOperation.CREATED_AS_INSTANT_PICTURE
        );
    }

    private Integer getUploadBehaviour(Context context) {
        SharedPreferences appPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        String behaviour = appPreferences.getString("prefs_instant_behaviour", "NOTHING");

        if (behaviour.equalsIgnoreCase("NOTHING")) {
            Log_OC.d(TAG, "upload file and do nothing");
            return FileUploader.LOCAL_BEHAVIOUR_FORGET;
        } else if (behaviour.equalsIgnoreCase("MOVE")) {
            Log_OC.d(TAG, "upload file and move file to oc folder");
            return FileUploader.LOCAL_BEHAVIOUR_MOVE;
        }
        return null;
    }

    private void handleNewVideoAction(Context context, Intent intent) {
        Cursor c = null;
        String file_path = null;
        String file_name = null;
        String mime_type = null;
        long date_taken = 0;

        Log_OC.i(TAG, "New video received");

        if (!PreferenceManager.instantVideoUploadEnabled(context)) {
            Log_OC.d(TAG, "Instant video upload disabled, ignoring new video");
            return;
        }

        Account account = FileStorageUtils.getInstantVideoUploadAccount(context);
        if (account == null) {
            Log_OC.w(TAG, "No account found for instant upload, aborting");
            return;
        }

        String[] CONTENT_PROJECTION = {Video.Media.DATA, Video.Media.DISPLAY_NAME, Video.Media.MIME_TYPE,
                Video.Media.SIZE};
        c = context.getContentResolver().query(intent.getData(), CONTENT_PROJECTION, null, null, null);
        if (!c.moveToFirst()) {
            Log_OC.e(TAG, "Couldn't resolve given uri: " + intent.getDataString());
            return;
        }
        file_path = c.getString(c.getColumnIndex(Video.Media.DATA));
        file_name = c.getString(c.getColumnIndex(Video.Media.DISPLAY_NAME));
        mime_type = c.getString(c.getColumnIndex(Video.Media.MIME_TYPE));
        c.close();
        date_taken = System.currentTimeMillis();
        Log_OC.d(TAG, file_path + "");

        int behaviour = getUploadBehaviour(context);
        FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
        requester.uploadNewFile(
                context,
                account,
                file_path,
                FileStorageUtils.getInstantVideoUploadFilePath(context, file_name, date_taken),
                behaviour,
                mime_type,
                true,           // create parent folder if not existent
                UploadFileOperation.CREATED_AS_INSTANT_VIDEO
        );
    }

}
