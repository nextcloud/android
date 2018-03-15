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
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.owncloud.android.MainApp;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * Static methods to help in access to local file system.
 */
public class FileStorageUtils {
    private static final String TAG = FileStorageUtils.class.getSimpleName();

    public static final String PATTERN_YYYY_MM = "yyyy/MM/";

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
        return getSavePath(accountName) + file.getDecryptedRemotePath();
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
        if (file.isFolder()) {
            file.setEncrypted(remote.getIsEncrypted());
        }
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

        return FileSortOrder.sortCloudFilesByFavourite(files);
    }


    /**
     * Local Folder size.
     *
     * @param dir File
     * @return Size in bytes
     */
    public static long getFolderSize(File dir) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();

            if (files != null) {
                long result = 0;
                for (File f : files) {
                    if (f.isDirectory()) {
                        result += getFolderSize(f);
                    } else {
                        result += f.length();
                    }
                }
                return result;
            }
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
        String result = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.ROOT));
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

    public static boolean moveFile(File sourceFile, File targetFile) throws IOException {
        if (copyFile(sourceFile, targetFile)) {
            return sourceFile.delete();
        } else {
            return false;
        }
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

    public static boolean checkIfFileFinishedSaving(OCFile file) {
        long lastModified = 0;
        long lastSize = 0;
        File realFile = new File(file.getStoragePath());
        while ((realFile.lastModified() != lastModified) && (realFile.length() != lastSize)) {
            lastModified = realFile.lastModified();
            lastSize = realFile.length();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.d(TAG, "Failed to sleep for a bit");
            }
        }

        return true;
    }

    /**
     * Checks and returns true if file itself or ancestor is encrypted
     *
     * @param file           file to check
     * @param storageManager up to date reference to storage manager
     * @return true if file itself or ancestor is encrypted
     */
    public static boolean checkEncryptionStatus(OCFile file, FileDataStorageManager storageManager) {
        if (file.isEncrypted()) {
            return true;
        }
        
        while (!OCFile.ROOT_PATH.equals(file.getRemotePath())) {
            if (file.isEncrypted()) {
                return true;
            }
            file = storageManager.getFileById(file.getParentId());
        }
        return false;
    }
}
