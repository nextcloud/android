/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
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

package com.owncloud.android.utils;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.webkit.MimeTypeMap;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.ui.activity.Preferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import third_parties.daveKoeller.AlphanumComparator;


/**
 * Static methods to help in access to local file system.
 */
public class FileStorageUtils {
    public static final Integer SORT_NAME = 0;
    public static final Integer SORT_DATE = 1;
    public static final Integer SORT_SIZE = 2;
    public static Integer mSortOrder = SORT_NAME;
    public static Boolean mSortAscending = true;

    
    public static final String getSavePath(String accountName) {
//        File sdCard = Environment.getExternalStorageDirectory();

        return MainApp.getStoragePath() + File.separator + MainApp.getDataFolder() + File.separator + Uri.encode(accountName, "@");
        // URL encoding is an 'easy fix' to overcome that NTFS and FAT32 don't allow ":" in file names, that can be in the accountName since 0.1.190B
    }

    public static final String getDefaultSavePathFor(String accountName, OCFile file) {
        return getSavePath(accountName) + file.getRemotePath();
    }

    public static final String getTemporalPath(String accountName) {
        return MainApp.getStoragePath() + File.separator + MainApp.getDataFolder() + File.separator + "tmp" + File.separator + Uri.encode(accountName, "@");
            // URL encoding is an 'easy fix' to overcome that NTFS and FAT32 don't allow ":" in file names, that can be in the accountName since 0.1.190B
    }

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

    public static String getInstantUploadFilePath(Context context, String fileName) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String uploadPathdef = context.getString(R.string.instant_upload_path);
        String uploadPath = pref.getString(Preferences.Keys.INSTANT_UPLOAD_PATH, uploadPathdef);
        String value = uploadPath + OCFile.PATH_SEPARATOR +  (fileName == null ? "" : fileName);
        return value;
    }

    /**
     * Gets the composed path when video is or must be stored
     * @param context
     * @param fileName: video file name
     * @return String: video file path composed
     */
    public static String getInstantVideoUploadFilePath(Context context, String fileName) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String uploadVideoPathdef = context.getString(R.string.instant_upload_path);
        String uploadVideoPath = pref.getString(Preferences.Keys.INSTANT_VIDEO_UPLOAD_PATH, uploadVideoPathdef);
        String value = uploadVideoPath + OCFile.PATH_SEPARATOR +  (fileName == null ? "" : fileName);
        return value;
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
        file.setFileLength(remote.getLength());
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
           // mFiles = FileStorageUtils.sortBySize(mSortAscending);
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
                // mFiles = FileStorageUtils.sortBySize(mSortAscending);
                break;
        }

        return files;
    }

    /**
     * Sorts list by Date
     * @param files
     */
    public static Vector<OCFile> sortOCFilesByDate(Vector<OCFile> files){
        final Integer val;
        if (mSortAscending){
            val = 1;
        } else {
            val = -1;
        }
        
        Collections.sort(files, new Comparator<OCFile>() {
            public int compare(OCFile o1, OCFile o2) {
                if (o1.getModificationTimestamp() == 0 || o2.getModificationTimestamp() == 0){
                    return 0;
                } else {
                    Long obj1 = o1.getModificationTimestamp();
                    return val * obj1.compareTo(o2.getModificationTimestamp());
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
        final Integer val;
        if (mSortAscending){
            val = 1;
        } else {
            val = -1;
        }

        List<File> files = new ArrayList<File>(Arrays.asList(filesArray));

        Collections.sort(files, new Comparator<File>() {
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isDirectory()) {
                    Long obj1 = o1.lastModified();
                    return val * obj1.compareTo(o2.lastModified());
                }
                else if (o1.isDirectory()) {
                    return -1;
                } else if (o2.isDirectory()) {
                    return 1;
                } else if (o1.lastModified() == 0 || o2.lastModified() == 0){
                    return 0;
                } else {
                    Long obj1 = o1.lastModified();
                    return val * obj1.compareTo(o2.lastModified());
                }
            }
        });

        File[] returnArray = new File[1];
        return files.toArray(returnArray);
    }

//    /**
//     * Sorts list by Size
//     * @param sortAscending true: ascending, false: descending
//     */
//    public static Vector<OCFile> sortBySize(Vector<OCFile> files){
//        final Integer val;
//        if (mSortAscending){
//            val = 1;
//        } else {
//            val = -1;
//        }
//        
//        Collections.sort(files, new Comparator<OCFile>() {
//            public int compare(OCFile o1, OCFile o2) {
//                if (o1.isFolder() && o2.isFolder()) {
//                    Long obj1 = getFolderSize(new File(FileStorageUtils.getDefaultSavePathFor(mAccount.name, o1)));
//                    return val * obj1.compareTo(getFolderSize(new File(FileStorageUtils.getDefaultSavePathFor(mAccount.name, o2))));
//                }
//                else if (o1.isFolder()) {
//                    return -1;
//                } else if (o2.isFolder()) {
//                    return 1;
//                } else if (o1.getFileLength() == 0 || o2.getFileLength() == 0){
//                    return 0;
//                } else {
//                    Long obj1 = o1.getFileLength();
//                    return val * obj1.compareTo(o2.getFileLength());
//                }
//            }
//        });
//        
//        return files;
//    }

    /**
     * Sorts list by Name
     * @param files     files to sort
     */
    public static Vector<OCFile> sortOCFilesByName(Vector<OCFile> files){
        final Integer val;
        if (mSortAscending){
            val = 1;
        } else {
            val = -1;
        }

        Collections.sort(files, new Comparator<OCFile>() {
            public int compare(OCFile o1, OCFile o2) {
                if (o1.isFolder() && o2.isFolder()) {
                    return val * new AlphanumComparator().compare(o1, o2);
                } else if (o1.isFolder()) {
                    return -1;
                } else if (o2.isFolder()) {
                    return 1;
                }
                return val * new AlphanumComparator().compare(o1, o2);
            }
        });
        
        return files;
    }

    /**
     * Sorts list by Name
     * @param filesArray    files to sort
     */
    public static File[] sortLocalFilesByName(File[] filesArray){
        final Integer val;
        if (mSortAscending){
            val = 1;
        } else {
            val = -1;
        }

        List<File> files = new ArrayList<File>(Arrays.asList(filesArray));

        Collections.sort(files, new Comparator<File>() {
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isDirectory()) {
                    return val * o1.getPath().toLowerCase().compareTo(o2.getPath().toLowerCase());
                } else if (o1.isDirectory()) {
                    return -1;
                } else if (o2.isDirectory()) {
                    return 1;
                }
                return val * new AlphanumComparator().compare(o1.getPath().toLowerCase(),
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
