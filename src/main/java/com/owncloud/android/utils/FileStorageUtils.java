/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * Copyright (C) 2016 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils;

import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.webkit.MimeTypeMap;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.RemoteFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import third_parties.daveKoeller.AlphanumComparator;


/**
 * Static methods to help in access to local file system.
 */
public class FileStorageUtils {
    private static final String TAG = FileStorageUtils.class.getSimpleName();

    public static final Integer SORT_NAME = 0;
    public static final Integer SORT_DATE = 1;
    public static final Integer SORT_SIZE = 2;
    public static final String PATTERN_YYYY_MM = "yyyy/MM/";
    public static Integer mSortOrder = SORT_NAME;
    public static Boolean mSortAscending = true;

    /**
     * Get local owncloud storage path for accountName.
     */
    public static String getSavePath(String accountName) {
        return MainApp.getStoragePath()
                + File.separator
                + MainApp.getDataFolder()
                + File.separator
                + Uri.encode(accountName, "@");
        // URL encoding is an 'easy fix' to overcome that NTFS and FAT32 don't allow ":" in file names,
        // that can be in the accountName since 0.1.190B
    }

    /**
     * Get local path where OCFile file is to be stored after upload. That is,
     * corresponding local path (in local owncloud storage) to remote uploaded
     * file.
     */
    public static String getDefaultSavePathFor(String accountName, OCFile file) {
        return getSavePath(accountName) + file.getRemotePath();
    }

    /**
     * Get absolute path to tmp folder inside datafolder in sd-card for given accountName.
     */
    public static String getTemporalPath(String accountName) {
        return MainApp.getStoragePath()
                + File.separator
                + MainApp.getDataFolder()
                + File.separator
                + "tmp"
                + File.separator
                + Uri.encode(accountName, "@");
        // URL encoding is an 'easy fix' to overcome that NTFS and FAT32 don't allow ":" in file names,
        // that can be in the accountName since 0.1.190B
    }

    /**
     * Optimistic number of bytes available on sd-card. accountName is ignored.
     *
     * @param accountName not used. can thus be null.
     * @return Optimistic number of available bytes (can be less)
     */
    public static long getUsableSpace(String accountName) {
        File savePath = new File(MainApp.getStoragePath());
        return savePath.getUsableSpace();
    }

    /**
     * Returns the a string like 2016/08/ for the passed date. If date is 0 an empty
     * string is returned
     *
     * @param date: date in microseconds since 1st January 1970
     * @return string: yyyy/mm/
     */
    private static String getSubpathFromDate(long date, Locale currentLocale) {
        if (date == 0) {
            return "";
        }

        Date d = new Date(date);

        DateFormat df = new SimpleDateFormat(PATTERN_YYYY_MM, currentLocale);
        df.setTimeZone(TimeZone.getTimeZone(TimeZone.getDefault().getID()));

        return df.format(d);
    }

    private static String getSubpathFromDate(long date) {
        if (date == 0) {
            return "";
        }

        Date d = new Date(date);

        DateFormat df = new SimpleDateFormat(PATTERN_YYYY_MM);

        return df.format(d);
    }

    /**
     * Returns the InstantUploadFilePath on the nextcloud instance
     *
     * @param fileName complete file name
     * @param dateTaken: Time in milliseconds since 1970 when the picture was taken.
     * @return instantUpload path, eg. /Camera/2017/01/fileName
     */
    public static String getInstantUploadFilePath(Locale current,
                                                  String remotePath,
                                                  String fileName,
                                                  long dateTaken,
                                                  Boolean subfolderByDate) {
        String subPath = "";
        if (subfolderByDate) {
            subPath = getSubpathFromDate(dateTaken, current);
        }

        return remotePath + OCFile.PATH_SEPARATOR + subPath + (fileName == null ? "" : fileName);
    }

    public static String getInstantUploadFilePath(String remotePath,
                                                  String fileName,
                                                  long dateTaken,
                                                  Boolean subfolderByDate) {
        String subPath = "";
        if (subfolderByDate) {
            subPath = getSubpathFromDate(dateTaken);
        }

        return remotePath + OCFile.PATH_SEPARATOR + subPath + (fileName == null ? "" : fileName);
    }


    /**
     * Gets the composed path when video is or must be stored.
     *
     * @param context app context
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
        return uploadVideoPath + OCFile.PATH_SEPARATOR + subPath + (fileName == null ? "" : fileName);
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
     * @return New OCFile instance representing the remote resource described by remote.
     */
    public static OCFile fillOCFile(RemoteFile remote) {
        OCFile file = new OCFile(remote.getRemotePath());
        file.setCreationTimestamp(remote.getCreationTimestamp());
        if (remote.getMimeType().equalsIgnoreCase(MimeType.DIRECTORY)) {
            file.setFileLength(remote.getSize());
        } else {
            file.setFileLength(remote.getLength());
        }
        file.setMimetype(remote.getMimeType());
        file.setModificationTimestamp(remote.getModifiedTimestamp());
        file.setEtag(remote.getEtag());
        file.setPermissions(remote.getPermissions());
        file.setRemoteId(remote.getRemoteId());
        file.setFavorite(remote.getIsFavorite());
        return file;
    }

    /**
     * Creates and populates a new {@link RemoteFile} object with the data read from an {@link OCFile}.
     *
     * @param ocFile    OCFile
     * @return New RemoteFile instance representing the resource described by ocFile.
     */
    public static RemoteFile fillRemoteFile(OCFile ocFile) {
        RemoteFile file = new RemoteFile(ocFile.getRemotePath());
        file.setCreationTimestamp(ocFile.getCreationTimestamp());
        file.setLength(ocFile.getFileLength());
        file.setMimeType(ocFile.getMimetype());
        file.setModifiedTimestamp(ocFile.getModificationTimestamp());
        file.setEtag(ocFile.getEtag());
        file.setPermissions(ocFile.getPermissions());
        file.setRemoteId(ocFile.getRemoteId());
        file.setFavorite(ocFile.getIsFavorite());
        return file;
    }

    public static Vector<OCFile> sortOcFolderDescDateModified(Vector<OCFile> files) {
        final int multiplier = -1;
        Collections.sort(files, new Comparator<OCFile>() {
            @SuppressFBWarnings(value = "Bx", justification = "Would require stepping up API level")
            public int compare(OCFile o1, OCFile o2) {
                Long obj1 = o1.getModificationTimestamp();
                return multiplier * obj1.compareTo(o2.getModificationTimestamp());
            }
        });

        return sortOCFilesByFavourite(files);
    }

    /**
     * Sorts all filenames, regarding last user decision 
     */
    public static Vector<OCFile> sortOcFolder(Vector<OCFile> files) {
        switch (mSortOrder) {
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

        files = FileStorageUtils.sortOCFilesByFavourite(files);

        return files;
    }

    /**
     * Sorts all filenames, regarding last user decision.
     *
     * @param files of files to sort
     */
    public static File[] sortLocalFolder(File[] files) {
        switch (mSortOrder) {
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
     * Sorts list by Date.
     *
     * @param files list of files to sort
     */
    public static Vector<OCFile> sortOCFilesByDate(Vector<OCFile> files) {
        final int multiplier = mSortAscending ? 1 : -1;

        Collections.sort(files, new Comparator<OCFile>() {
            @SuppressFBWarnings(value = "Bx", justification = "Would require stepping up API level")
            public int compare(OCFile o1, OCFile o2) {
                Long obj1 = o1.getModificationTimestamp();
                return multiplier * obj1.compareTo(o2.getModificationTimestamp());
            }
        });

        return files;
    }

    /**
     * Sorts list by Date.
     *
     * @param filesArray list of files to sort
     */
    public static File[] sortLocalFilesByDate(File[] filesArray) {
        final int multiplier = mSortAscending ? 1 : -1;

        List<File> files = new ArrayList<File>(Arrays.asList(filesArray));

        Collections.sort(files, new Comparator<File>() {
            @SuppressFBWarnings(value = "Bx")
            public int compare(File o1, File o2) {
                Long obj1 = o1.lastModified();
                return multiplier * obj1.compareTo(o2.lastModified());
            }
        });

        File[] returnArray = new File[files.size()];
        return files.toArray(returnArray);
    }

    /**
     * Sorts list by Size.
     *
     * @param files list of files to sort
     */
    public static Vector<OCFile> sortOCFilesBySize(Vector<OCFile> files) {
        final int multiplier = mSortAscending ? 1 : -1;

        Collections.sort(files, new Comparator<OCFile>() {
            @SuppressFBWarnings(value = "Bx")
            public int compare(OCFile o1, OCFile o2) {
                if (o1.isFolder() && o2.isFolder()) {
                    Long obj1 = o1.getFileLength();
                    return multiplier * obj1.compareTo(o2.getFileLength());
                } else if (o1.isFolder()) {
                    return -1;

                } else if (o2.isFolder()) {
                    return 1;
                } else {
                    Long obj1 = o1.getFileLength();
                    return multiplier * obj1.compareTo(o2.getFileLength());
                }
            }
        });

        return files;
    }

    /**
     * Sorts list by Size.
     *
     * @param filesArray list of files to sort
     */
    public static File[] sortLocalFilesBySize(File[] filesArray) {
        final int multiplier = mSortAscending ? 1 : -1;

        List<File> files = new ArrayList<>(Arrays.asList(filesArray));

        Collections.sort(files, new Comparator<File>() {
            @SuppressFBWarnings(value = "Bx")
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isDirectory()) {
                    // Long obj1 = getFolderSize(o1);
                    // return multiplier * obj1.compareTo(getFolderSize(o2));
                    return o1.getPath().toLowerCase().compareTo(o2.getPath().toLowerCase());
                } else if (o1.isDirectory()) {
                    return -1;
                } else if (o2.isDirectory()) {
                    return 1;
                } else {
                    Long obj1 = o1.length();
                    return multiplier * obj1.compareTo(o2.length());
                }
            }
        });

        File[] returnArray = new File[files.size()];
        return files.toArray(returnArray);
    }

    /**
     * Sorts list by Name.
     *
     * @param files files to sort
     */
    @SuppressFBWarnings(value = "Bx")
    public static Vector<OCFile> sortOCFilesByName(Vector<OCFile> files) {
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
     * Sorts list by Name.
     *
     * @param filesArray files to sort
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

        File[] returnArray = new File[files.size()];
        return files.toArray(returnArray);
    }

    /**
     * Sorts list by Favourites.
     *
     * @param files files to sort
     */
    public static Vector<OCFile> sortOCFilesByFavourite(Vector<OCFile> files) {
        Collections.sort(files, new Comparator<OCFile>() {
            public int compare(OCFile o1, OCFile o2) {
                if (o1.getIsFavorite() && o2.getIsFavorite()) {
                    return 0;
                } else if (o1.getIsFavorite()) {
                    return -1;
                } else if (o2.getIsFavorite()) {
                    return 1;
                }
                return 0;
            }
        });

        return files;
    }

    /**
     * Local Folder size.
     *
     * @param dir File
     * @return Size in bytes
     */
    public static long getFolderSize(File dir) {
        if (dir.exists()) {
            long result = 0;
            for (File f : dir.listFiles()) {
                if (f.isDirectory()) {
                    result += getFolderSize(f);
                } else {
                    result += f.length();
                }
            }
            return result;
        }
        return 0;
    }

    /**
     * Mimetype String of a file.
     *
     * @param path the file path
     * @return the mime type based on the file name
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
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log_OC.e(TAG, "Error closing input stream during copy", e);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Log_OC.e(TAG, "Error closing output stream during copy", e);
                }
            }
        }

        return ret;
    }

    public static void deleteRecursively(File file, FileDataStorageManager storageManager) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteRecursively(child, storageManager);
            }
        }

        storageManager.deleteFileInMediaScan(file.getAbsolutePath());
        file.delete();
    }

}
