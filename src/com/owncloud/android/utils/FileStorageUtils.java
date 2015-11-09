/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2016 ownCloud Inc.
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

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.webkit.MimeTypeMap;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.RemoteFile;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import third_parties.daveKoeller.AlphanumComparator;


import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.net.Uri;
import android.os.StatFs;
import android.webkit.MimeTypeMap;


/**
 * Static methods to help in access to local file system.
 */
public class FileStorageUtils {
    private static final String TAG = FileStorageUtils.class.getSimpleName();

    public static final Integer SORT_NAME = 0;
    public static final Integer SORT_DATE = 1;
    public static final Integer SORT_SIZE = 2;
    public static Integer mSortOrder = SORT_NAME;
    public static Boolean mSortAscending = true;

    /**
     * Takes a full path to owncloud file and removes beginning which is path to ownload data folder.
     * If fullPath does not start with that folder, fullPath is returned as is.
     */
    public static final String removeDataFolderPath(String fullPath) {
        File sdCard = Environment.getExternalStorageDirectory();
        String dataFolderPath = sdCard.getAbsolutePath() + "/" + MainApp.getDataFolder() + "/";
        if(fullPath.indexOf(dataFolderPath) == 0) {
            return fullPath.substring(dataFolderPath.length());
        }
        return fullPath;
    }
    
    /**
     * Get local owncloud storage path for accountName.
     */
    public static final String getSavePath(String accountName) {
        return MainApp.getStoragePath() + File.separator + MainApp.getDataFolder() + File.separator + Uri.encode(accountName, "@");
        // URL encoding is an 'easy fix' to overcome that NTFS and FAT32 don't allow ":" in file names,
        // that can be in the accountName since 0.1.190B
    }

    /**
     * Get local path where OCFile file is to be stored after upload. That is,
     * corresponding local path (in local owncloud storage) to remote uploaded
     * file.
     */
    public static final String getDefaultSavePathFor(String accountName, OCFile file) {
        return getSavePath(accountName) + file.getRemotePath();
    }

    /**
     * Get absolute path to tmp folder inside datafolder in sd-card for given accountName.
     */
    public static final String getTemporalPath(String accountName) {
        return MainApp.getStoragePath() + File.separator + MainApp.getDataFolder() + File.separator + "tmp" + File.separator + Uri.encode(accountName, "@");
        // URL encoding is an 'easy fix' to overcome that NTFS and FAT32 don't allow ":" in file names,
        // that can be in the accountName since 0.1.190B
    }

    /**
     * Optimistic number of bytes available on sd-card. accountName is ignored.
     * @param accountName not used. can thus be null.
     * @return Optimistic number of available bytes (can be less)
     */
    @SuppressLint("NewApi")
    public static final long getUsableSpace(String accountName) {
        File savePath = new File(MainApp.getStoragePath());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
            return savePath.getUsableSpace();

        } else {
            StatFs stats = new StatFs(savePath.getAbsolutePath());
            return stats.getAvailableBlocks() * stats.getBlockSize();
        }

    }
    
    public static final String getLogPath()  {
        return MainApp.getStoragePath() + File.separator + MainApp.getDataFolder() + File.separator + "log";
    }

    /**
     * Returns the a string like 2016/08/ for the passed date. If date is 0 an empty
     * string is returned
     *
     * @param date: date in microseconds since 1st January 1970
     * @return
     */
    private static String getSubpathFromDate(long date) {
        if (date == 0) {
            return "";
        }
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(
                    "yyyy" + OCFile.PATH_SEPARATOR + "MM" + OCFile.PATH_SEPARATOR, Locale.ENGLISH);
            return formatter.format(new Date(date));
        }
        catch(RuntimeException ex) {
            Log_OC.w(TAG, "could not extract date from timestamp");
            return "";
        }
    }

    /**
     * Returns the InstantUploadFilePath on the owncloud instance
     *
     * @param context
     * @param fileName
     * @param dateTaken: Time in milliseconds since 1970 when the picture was taken.
     * @return
     */
    public static String getInstantUploadFilePath(Context context, String fileName, long dateTaken) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String uploadPathdef = context.getString(R.string.instant_upload_path);
        String uploadPath = pref.getString("instant_upload_path", uploadPathdef);
        String subPath = "";
        if (com.owncloud.android.db.PreferenceManager.instantPictureUploadPathUseSubfolders(context)) {
           subPath = getSubpathFromDate(dateTaken);
        }
        return uploadPath + OCFile.PATH_SEPARATOR + subPath
                + (fileName == null ? "" : fileName);
    }

    /**
     * Gets the composed path when video is or must be stored
     * @param context
     * @param fileName: video file name
     * @return String: video file path composed
     */
    public static String getInstantVideoUploadFilePath(Context context, String fileName, long dateTaken) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String uploadVideoPathdef = context.getString(R.string.instant_upload_path);
        String uploadVideoPath = pref.getString("instant_video_upload_path", uploadVideoPathdef);
        String subPath = "";
        if (com.owncloud.android.db.PreferenceManager.instantVideoUploadPathUseSubfolders(context)) {
            subPath = getSubpathFromDate(dateTaken);
        }
        return uploadVideoPath + OCFile.PATH_SEPARATOR + subPath
                + (fileName == null ? "" : fileName);
    }
    
    public static String getParentPath(String remotePath) {
        String parentPath = new File(remotePath).getParent();
        parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath : parentPath + OCFile.PATH_SEPARATOR;
        return parentPath;
    }
    
    /**
     * Creates and populates a new {@link OCFile} object with the data read from the server.
     * 
     * @param remote    remote file read from the server (remote file or folder).
     * @return          New OCFile instance representing the remote resource described by remote.
     */
    public static OCFile fillOCFile(RemoteFile remote) {
        OCFile file = new OCFile(remote.getRemotePath());
        file.setCreationTimestamp(remote.getCreationTimestamp());
        if (remote.getMimeType().equalsIgnoreCase(MimeType.DIRECTORY)){
            file.setFileLength(remote.getSize());
        } else {
            file.setFileLength(remote.getLength());
        }
        file.setMimetype(remote.getMimeType());
        file.setModificationTimestamp(remote.getModifiedTimestamp());
        file.setEtag(remote.getEtag());
        file.setPermissions(remote.getPermissions());
        file.setRemoteId(remote.getRemoteId());
        return file;
    }
    
    /**
     * Creates and populates a new {@link RemoteFile} object with the data read from an {@link OCFile}.
     * 
     * @param ocFile    OCFile
     * @return          New RemoteFile instance representing the resource described by ocFile.
     */
    public static RemoteFile fillRemoteFile(OCFile ocFile){
        RemoteFile file = new RemoteFile(ocFile.getRemotePath());
        file.setCreationTimestamp(ocFile.getCreationTimestamp());
        file.setLength(ocFile.getFileLength());
        file.setMimeType(ocFile.getMimetype());
        file.setModifiedTimestamp(ocFile.getModificationTimestamp());
        file.setEtag(ocFile.getEtag());
        file.setPermissions(ocFile.getPermissions());
        file.setRemoteId(ocFile.getRemoteId());
        return file;
    }
    
    /**
     * Sorts all filenames, regarding last user decision 
     */
    public static Vector<OCFile> sortOcFolder(Vector<OCFile> files){
        switch (mSortOrder){
        case 0:
            files = FileStorageUtils.sortOCFilesByName(files);
            break;
        case 1:
            files = FileStorageUtils.sortOCFilesByDate(files);
            break;
        case 2:
            files = FileStorageUtils.sortOCFilesBySize(files);
            break;
        }
       
        return files;
    }

    /**
     * Sorts all filenames, regarding last user decision
     */
    public static File[] sortLocalFolder(File[] files){
        switch (mSortOrder){
            case 0:
                files = FileStorageUtils.sortLocalFilesByName(files);
                break;
            case 1:
                files = FileStorageUtils.sortLocalFilesByDate(files);
                break;
            case 2:
                files = FileStorageUtils.sortLocalFilesBySize(files);
                break;
        }

        return files;
    }
    
    /**
     * Sorts list by Date
     * @param files
     */
    public static Vector<OCFile> sortOCFilesByDate(Vector<OCFile> files){
        final int multiplier = mSortAscending ? 1 : -1;
        
        Collections.sort(files, new Comparator<OCFile>() {
            public int compare(OCFile o1, OCFile o2) {
                if (o1.isFolder() && o2.isFolder()) {
                    Long obj1 = o1.getModificationTimestamp();
                    return multiplier * obj1.compareTo(o2.getModificationTimestamp());
                }
                else if (o1.isFolder()) {
                    return -1;
                } else if (o2.isFolder()) {
                    return 1;
                } else if (o1.getModificationTimestamp() == 0 || o2.getModificationTimestamp() == 0){
                    return 0;
                } else {
                    Long obj1 = o1.getModificationTimestamp();
                    return multiplier * obj1.compareTo(o2.getModificationTimestamp());
                }
            }
        });
        
        return files;
    }

    /**
     * Sorts list by Date
     * @param filesArray
     */
    public static File[] sortLocalFilesByDate(File[] filesArray){
        final int multiplier = mSortAscending ? 1 : -1;

        List<File> files = new ArrayList<File>(Arrays.asList(filesArray));

        Collections.sort(files, new Comparator<File>() {
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isDirectory()) {
                    Long obj1 = o1.lastModified();
                    return multiplier * obj1.compareTo(o2.lastModified());
                }
                else if (o1.isDirectory()) {
                    return -1;
                } else if (o2.isDirectory()) {
                    return 1;
                } else if (o1.lastModified() == 0 || o2.lastModified() == 0){
                    return 0;
                } else {
                    Long obj1 = o1.lastModified();
                    return multiplier * obj1.compareTo(o2.lastModified());
                }
            }
        });

        File[] returnArray = new File[1];
        return files.toArray(returnArray);
    }

    /**
     * Sorts list by Size
     */
    public static Vector<OCFile> sortOCFilesBySize(Vector<OCFile> files){
        final int multiplier = mSortAscending ? 1 : -1;

        Collections.sort(files, new Comparator<OCFile>() {
            public int compare(OCFile o1, OCFile o2) {
                if (o1.isFolder() && o2.isFolder()) {
                    Long obj1 = o1.getFileLength();
                    return multiplier * obj1.compareTo(o2.getFileLength());
                }
                else if (o1.isFolder()) {
                    return -1;
                } else if (o2.isFolder()) {
                    return 1;
                } else if (o1.getFileLength() == 0 || o2.getFileLength() == 0){
                    return 0;
                } else {
                    Long obj1 = o1.getFileLength();
                    return multiplier * obj1.compareTo(o2.getFileLength());
                }
            }
        });

        return files;
    }

    /**
     * Sorts list by Size
     */
    public static File[] sortLocalFilesBySize(File[] filesArray) {
        final int multiplier = mSortAscending ? 1 : -1;

        List<File> files = new ArrayList<File>(Arrays.asList(filesArray));

        Collections.sort(files, new Comparator<File>() {
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isDirectory()) {
                    Long obj1 = getFolderSize(o1);
                    return multiplier * obj1.compareTo(getFolderSize(o2));
                }
                else if (o1.isDirectory()) {
                    return -1;
                } else if (o2.isDirectory()) {
                    return 1;
                } else if (o1.length() == 0 || o2.length() == 0){
                    return 0;
                } else {
                    Long obj1 = o1.length();
                    return multiplier * obj1.compareTo(o2.length());
                }
            }
        });

        File[] returnArray = new File[1];
        return files.toArray(returnArray);
    }

    /**
     * Sorts list by Name
     * @param files     files to sort
     */
    public static Vector<OCFile> sortOCFilesByName(Vector<OCFile> files){
        final int multiplier = mSortAscending ? 1 : -1;

        Collections.sort(files, new Comparator<OCFile>() {
            public int compare(OCFile o1, OCFile o2) {
                if (o1.isFolder() && o2.isFolder()) {
                    return multiplier * new AlphanumComparator().compare(o1, o2);
                } else if (o1.isFolder()) {
                    return -1;
                } else if (o2.isFolder()) {
                    return 1;
                }
                return multiplier * new AlphanumComparator().compare(o1, o2);
            }
        });
        
        return files;
    }

    /**
     * Sorts list by Name
     * @param filesArray    files to sort
     */
    public static File[] sortLocalFilesByName(File[] filesArray) {
        final int multiplier = mSortAscending ? 1 : -1;

        List<File> files = new ArrayList<File>(Arrays.asList(filesArray));

        Collections.sort(files, new Comparator<File>() {
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isDirectory()) {
                    return multiplier * o1.getPath().toLowerCase().compareTo(o2.getPath().toLowerCase());
                } else if (o1.isDirectory()) {
                    return -1;
                } else if (o2.isDirectory()) {
                    return 1;
                }
                return multiplier * new AlphanumComparator().compare(o1.getPath().toLowerCase(),
                                                              o2.getPath().toLowerCase());
            }
        });

        File[] returnArray = new File[1];
        return files.toArray(returnArray);
    }
    
    /**
     * Local Folder size
     * @param dir File
     * @return Size in bytes
     */
    public static long getFolderSize(File dir) {
        if (dir.exists()) {
            long result = 0;
            for (File f : dir.listFiles()) {
                if (f.isDirectory())
                    result += getFolderSize(f);
                else
                    result += f.length();
            }
            return result;
        }
        return 0;
    }

    /**
     * Mimetype String of a file
     * @param path
     * @return
     */
    public static String getMimeTypeFromName(String path) {
        String extension = "";
        int pos = path.lastIndexOf('.');
        if (pos >= 0) {
            extension = path.substring(pos + 1);
        }
        String result = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        return (result != null) ? result : "";
    }

    /**
     * Scans the default location for saving local copies of files searching for
     * a 'lost' file with the same full name as the {@link OCFile} received as
     * parameter.
     *
     * This method helps to keep linked local copies of the files when the app is uninstalled, and then
     * reinstalled in the device. OR after the cache of the app was deleted in system settings.
     *
     * The method is assuming that all the local changes in the file where synchronized in the past. This is dangerous,
     * but assuming the contrary could lead to massive unnecessary synchronizations of downloaded file after deleting
     * the app cache.
     *
     * This should be changed in the near future to avoid any chance of data loss, but we need to add some options
     * to limit hard automatic synchronizations to wifi, unless the user wants otherwise.
     *
     * @param file      File to associate a possible 'lost' local file.
     * @param account   Account holding file.
     */
    public static void searchForLocalFileInDefaultPath(OCFile file, Account account) {
        if (file.getStoragePath() == null && !file.isFolder()) {
            File f = new File(FileStorageUtils.getDefaultSavePathFor(account.name, file));
            if (f.exists()) {
                file.setStoragePath(f.getAbsolutePath());
                file.setLastSyncDateForData(f.lastModified());
            }
        }
    }

    public static boolean copyFile(File src, File target) {
        boolean ret = true;

        InputStream in = null;
        OutputStream out = null;

        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(target);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException ex) {
            ret = false;
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
            if (out != null) try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }

        return ret;
    }

}
