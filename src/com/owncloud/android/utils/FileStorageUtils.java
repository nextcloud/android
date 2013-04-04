/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
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
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.InstantUploadService;

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

    // to ensure we will not add the slash twice between filename and
    // folder-name
    private static String getFileName(String filepath) {
        if (filepath != null && !"".equals(filepath)) {
            int psi = filepath.lastIndexOf('/');
            String filename = filepath;
            if (psi > -1) {
                filename = filepath.substring(psi + 1, filepath.length());
                Log.d(LOG_TAG, "extracted filename :" + filename);
            }
            return filename;
        } else {
            // Toast
            Log.w(LOG_TAG, "the given filename was null or empty");
            return null;
        }
    }

    public static String getInstantUploadFilePath(String fileName) {
        return InstantUploadService.INSTANT_UPLOAD_DIR + "/" + getFileName(fileName);
    }
}