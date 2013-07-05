/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
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

package com.owncloud.android.utils;

import java.io.File;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;

/**
 * Static methods to help in access to local file system.
 * 
 * @author David A. Velasco
 */
public class FileStorageUtils {
    private static final String LOG_TAG = "FileStorageUtils";

    public static final String getSavePath(String accountName) {
        File sdCard = Environment.getExternalStorageDirectory();
        return sdCard.getAbsolutePath() + "/owncloud/" + Uri.encode(accountName, "@");
        // URL encoding is an 'easy fix' to overcome that NTFS and FAT32 don't allow ":" in file names, that can be in the accountName since 0.1.190B
    }

    public static final String getDefaultSavePathFor(String accountName, OCFile file) {
        return getSavePath(accountName) + file.getRemotePath();
    }

    public static final String getTemporalPath(String accountName) {
        File sdCard = Environment.getExternalStorageDirectory();
        return sdCard.getAbsolutePath() + "/owncloud/tmp/" + Uri.encode(accountName, "@");
            // URL encoding is an 'easy fix' to overcome that NTFS and FAT32 don't allow ":" in file names, that can be in the accountName since 0.1.190B
    }

    @SuppressLint("NewApi")
    public static final long getUsableSpace(String accountName) {
        File savePath = Environment.getExternalStorageDirectory();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
            return savePath.getUsableSpace();

        } else {
            StatFs stats = new StatFs(savePath.getAbsolutePath());
            return stats.getAvailableBlocks() * stats.getBlockSize();
        }

    }
    
    public static final String getLogPath()  {
        return Environment.getExternalStorageDirectory() + File.separator + "owncloud" + File.separator + "log";
    }

    public static String getInstantUploadFilePath(Context context, String fileName) {
        String uploadPath = context.getString(R.string.instant_upload_path);
        String value = uploadPath + OCFile.PATH_SEPARATOR +  (fileName == null ? "" : fileName);
        return value;
    }
  
}