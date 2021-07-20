/*
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

import android.Manifest;
import android.accounts.Account;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.model.RemoteFile;

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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.annotation.Nullable;

import androidx.core.app.ActivityCompat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static android.os.Build.VERSION.SDK_INT;

/**
 * Static methods to help in access to local file system.
 */
public final class FileStorageUtils {
    private static final String TAG = FileStorageUtils.class.getSimpleName();

    private static final String PATTERN_YYYY_MM = "yyyy/MM/";
    private static final String DEFAULT_FALLBACK_STORAGE_PATH = "/storage/sdcard0";

    private FileStorageUtils() {
        // utility class -> private constructor
    }

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
     * Get absolute path to tmp folder inside app folder for given accountName.
     */
    public static String getInternalTemporalPath(String accountName, Context context) {
        return context.getFilesDir()
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
     * @return Optimistic number of available bytes (can be less)
     */
    public static long getUsableSpace() {
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
    private static String getSubPathFromDate(long date, Locale currentLocale) {
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
     * @param dateTaken: Time in milliseconds since 1970 when the picture was taken.
     * @return instantUpload path, eg. /Camera/2017/01/fileName
     */
    public static String getInstantUploadFilePath(File file,
                                                  Locale current,
                                                  String remotePath,
                                                  String syncedFolderLocalPath,
                                                  long dateTaken,
                                                  Boolean subfolderByDate) {
        String subfolderByDatePath = "";
        if (subfolderByDate) {
            subfolderByDatePath = getSubPathFromDate(dateTaken, current);
        }

        File parentFile = new File(file.getAbsolutePath().replace(syncedFolderLocalPath, "")).getParentFile();

        String relativeSubfolderPath = "";
        if (parentFile == null) {
            Log_OC.e("AutoUpload", "Parent folder does not exists!");
        } else {
            relativeSubfolderPath = parentFile.getAbsolutePath();
        }

        // Path must be normalized; otherwise the next RefreshFolderOperation has a mismatch and deletes the local file.
        return (remotePath +
            OCFile.PATH_SEPARATOR +
            subfolderByDatePath +
            OCFile.PATH_SEPARATOR +
            relativeSubfolderPath +
            OCFile.PATH_SEPARATOR +
            file.getName())
            .replaceAll(OCFile.PATH_SEPARATOR + "+", OCFile.PATH_SEPARATOR);
    }


    public static String getParentPath(String remotePath) {
        String parentPath = new File(remotePath).getParent();
        if (parentPath != null) {
            parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath : parentPath + OCFile.PATH_SEPARATOR;
        }
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
        file.setDecryptedRemotePath(remote.getRemotePath());
        file.setCreationTimestamp(remote.getCreationTimestamp());
        if (MimeType.DIRECTORY.equalsIgnoreCase(remote.getMimeType())) {
            file.setFileLength(remote.getSize());
        } else {
            file.setFileLength(remote.getLength());
        }
        file.setMimeType(remote.getMimeType());
        file.setModificationTimestamp(remote.getModifiedTimestamp());
        file.setEtag(remote.getEtag());
        file.setPermissions(remote.getPermissions());
        file.setRemoteId(remote.getRemoteId());
        file.setFavorite(remote.isFavorite());
        if (file.isFolder()) {
            file.setEncrypted(remote.isEncrypted());
        }
        file.setMountType(remote.getMountType());
        file.setPreviewAvailable(remote.isHasPreview());
        file.setUnreadCommentsCount(remote.getUnreadCommentsCount());
        file.setOwnerId(remote.getOwnerId());
        file.setOwnerDisplayName(remote.getOwnerDisplayName());
        file.setNote(remote.getNote());
        file.setSharees(new ArrayList<>(Arrays.asList(remote.getSharees())));
        file.setRichWorkspace(remote.getRichWorkspace());

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
        file.setMimeType(ocFile.getMimeType());
        file.setModifiedTimestamp(ocFile.getModificationTimestamp());
        file.setEtag(ocFile.getEtag());
        file.setPermissions(ocFile.getPermissions());
        file.setRemoteId(ocFile.getRemoteId());
        file.setFavorite(ocFile.isFavorite());
        return file;
    }

    public static List<OCFile> sortOcFolderDescDateModifiedWithoutFavoritesFirst(List<OCFile> files) {
        final int multiplier = -1;
        Collections.sort(files, (o1, o2) -> {
            @SuppressFBWarnings(value = "Bx", justification = "Would require stepping up API level")
            Long obj1 = o1.getModificationTimestamp();
            return multiplier * obj1.compareTo(o2.getModificationTimestamp());
        });

        return files;
    }

    public static List<OCFile> sortOcFolderDescDateModified(List<OCFile> files) {
        files = sortOcFolderDescDateModifiedWithoutFavoritesFirst(files);

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
        if ((file.getStoragePath() == null || !new File(file.getStoragePath()).exists()) && !file.isFolder()) {
            File f = new File(FileStorageUtils.getDefaultSavePathFor(account.name, file));
            if (f.exists()) {
                file.setStoragePath(f.getAbsolutePath());
                file.setLastSyncDateForData(f.lastModified());
            }
        }
    }

    @SuppressFBWarnings(value="OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE",
            justification="False-positive on the output stream")
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

    public static boolean moveFile(File sourceFile, File targetFile) {
        if (copyFile(sourceFile, targetFile)) {
            return sourceFile.delete();
        } else {
            return false;
        }
    }

    public static boolean copyDirs(File sourceFolder, File targetFolder) {
        if (!targetFolder.mkdirs()) {
            return false;
        }

        for (File f : sourceFolder.listFiles()) {
            if (f.isDirectory()) {
                if (!copyDirs(f, new File(targetFolder, f.getName()))) {
                    return false;
                }
            } else if (!FileStorageUtils.copyFile(f, new File(targetFolder, f.getName()))) {
                return false;
            }
        }

        return true;
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

    public static boolean deleteRecursive(File file) {
        boolean res = true;
        if (file.isDirectory()) {
            for (File c : file.listFiles()) {
                res = deleteRecursive(c) && res;
            }
        }
        return file.delete() && res;
    }

    public static void checkIfFileFinishedSaving(OCFile file) {
        long lastModified = 0;
        long lastSize = 0;
        File realFile = new File(file.getStoragePath());

        if (realFile.lastModified() != file.getModificationTimestamp() && realFile.length() != file.getFileLength()) {
            while (realFile.lastModified() != lastModified && realFile.length() != lastSize) {
                lastModified = realFile.lastModified();
                lastSize = realFile.length();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Failed to sleep for a bit");
                }
            }
        }
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

        while (!OCFile.ROOT_PATH.equals(file.getDecryptedRemotePath())) {
            if (file.isEncrypted()) {
                return true;
            }
            file = storageManager.getFileById(file.getParentId());
        }
        return false;
    }

    /**
     * Taken from https://github.com/TeamAmaze/AmazeFileManager/blob/54652548223d151f089bdc6fc868b13ca5ab20a9/app/src
     * /main/java/com/amaze/filemanager/activities/MainActivity.java#L620 on 14.02.2019
     */
    @SuppressFBWarnings(value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
        justification = "Default Android fallback storage path")
    public static List<String> getStorageDirectories(Context context) {
        // Final set of paths
        final List<String> rv = new ArrayList<>();
        // Primary physical SD-CARD (not emulated)
        final String rawExternalStorage = System.getenv("EXTERNAL_STORAGE");
        // All Secondary SD-CARDs (all exclude primary) separated by ":"
        final String rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");
        // Primary emulated SD-CARD
        final String rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET");
        if (TextUtils.isEmpty(rawEmulatedStorageTarget)) {
            // Device has physical external storage; use plain paths.
            if (TextUtils.isEmpty(rawExternalStorage)) {
                // EXTERNAL_STORAGE undefined; falling back to default.
                // Check for actual existence of the directory before adding to list
                if (new File(DEFAULT_FALLBACK_STORAGE_PATH).exists()) {
                    rv.add(DEFAULT_FALLBACK_STORAGE_PATH);
                } else {
                    //We know nothing else, use Environment's fallback
                    rv.add(Environment.getExternalStorageDirectory().getAbsolutePath());
                }
            } else {
                rv.add(rawExternalStorage);
            }
        } else {
            // Device has emulated storage; external storage paths should have
            // userId burned into them.
            final String rawUserId;
            final String path = Environment.getExternalStorageDirectory().getAbsolutePath();
            final String[] folders = OCFile.PATH_SEPARATOR.split(path);
            final String lastFolder = folders[folders.length - 1];
            boolean isDigit = false;
            try {
                Integer.valueOf(lastFolder);
                isDigit = true;
            } catch (NumberFormatException ignored) {
            }
            rawUserId = isDigit ? lastFolder : "";

            // /storage/emulated/0[1,2,...]
            if (TextUtils.isEmpty(rawUserId)) {
                rv.add(rawEmulatedStorageTarget);
            } else {
                rv.add(rawEmulatedStorageTarget + File.separator + rawUserId);
            }
        }
        // Add all secondary storages
        if (!TextUtils.isEmpty(rawSecondaryStoragesStr)) {
            // All Secondary SD-CARDs splited into array
            final String[] rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator);
            Collections.addAll(rv, rawSecondaryStorages);
        }
        if (SDK_INT >= Build.VERSION_CODES.M && checkStoragePermission(context)) {
            rv.clear();
        }

        String[] extSdCardPaths = getExtSdCardPathsForActivity(context);
        File f;
        for (String extSdCardPath : extSdCardPaths) {
            f = new File(extSdCardPath);
            if (!rv.contains(extSdCardPath) && canListFiles(f)) {
                rv.add(extSdCardPath);
            }
        }

        return rv;
    }

    /**
     * Update the local path summary display. If a special directory is recognized, it is replaced by its name.
     * <p>
     * Example: /storage/emulated/0/Movies -> Internal Storage / Movies Example: /storage/ABC/non/standard/directory ->
     * ABC /non/standard/directory
     *
     * @param path the path to display
     * @return a user friendly path as defined in examples, or {@param path} if the storage device isn't recognized.
     */
    public static String pathToUserFriendlyDisplay(String path, Context context, Resources resources) {
        // Determine storage device (external, sdcard...)
        String storageDevice = null;
        for (String storageDirectory : FileStorageUtils.getStorageDirectories(context)) {
            if (path.startsWith(storageDirectory)) {
                storageDevice = storageDirectory;
                break;
            }
        }

        // If storage device was not found, display full path
        if (storageDevice == null) {
            return path;
        }

        // Default to full path without storage device path
        String storageFolder;
        try {
            storageFolder = path.substring(storageDevice.length() + 1);
        } catch (StringIndexOutOfBoundsException e) {
            storageFolder = "";
        }

        FileStorageUtils.StandardDirectory standardDirectory = FileStorageUtils.StandardDirectory.fromPath(storageFolder);
        if (standardDirectory != null) { // Friendly name of standard directory
            storageFolder = " " + resources.getString(standardDirectory.getDisplayName());
        }

        // Shorten the storage device to a friendlier display name
        if (storageDevice.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath())) {
            storageDevice = resources.getString(R.string.storage_internal_storage);
        } else {
            storageDevice = new File(storageDevice).getName();
        }

        return resources.getString(R.string.local_folder_friendly_path, storageDevice, storageFolder);
    }

    /**
     * Taken from https://github.com/TeamAmaze/AmazeFileManager/blob/d11e0d2874c6067910e58e059859431a31ad6aee/app/src
     * /main/java/com/amaze/filemanager/activities/superclasses/PermissionsActivity.java#L47 on 14.02.2019
     */
    private static boolean checkStoragePermission(Context context) {
        // Verify that all required contact permissions have been granted.
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Taken from https://github.com/TeamAmaze/AmazeFileManager/blob/616f2a696823ab0e64ea7a017602dc08e783162e/app/src
     * /main/java/com/amaze/filemanager/filesystem/FileUtil.java#L764 on 14.02.2019
     */
    private static String[] getExtSdCardPathsForActivity(Context context) {
        List<String> paths = new ArrayList<>();
        for (File file : context.getExternalFilesDirs("external")) {
            if (file != null) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index < 0) {
                    Log_OC.w(TAG, "Unexpected external file dir: " + file.getAbsolutePath());
                } else {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    } catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    paths.add(path);
                }
            }
        }
        if (paths.isEmpty()) {
            paths.add("/storage/sdcard1");
        }
        return paths.toArray(new String[0]);
    }

    /**
     * Taken from https://github.com/TeamAmaze/AmazeFileManager/blob/9cf1fd5ff1653c692cb54cf6bc71b572c19a11cd/app/src
     * /main/java/com/amaze/filemanager/utils/files/FileUtils.java#L754 on 14.02.2019
     */
    private static boolean canListFiles(File f) {
        return f.canRead() && f.isDirectory();
    }

    /**
     * Should be converted to an enum when we only support min SDK version for Environment.DIRECTORY_DOCUMENTS
     */
    public static class StandardDirectory {
        public static final StandardDirectory PICTURES = new StandardDirectory(
            Environment.DIRECTORY_PICTURES,
            R.string.storage_pictures,
            R.drawable.ic_image_grey600
        );
        public static final StandardDirectory CAMERA = new StandardDirectory(
            Environment.DIRECTORY_DCIM,
            R.string.storage_camera,
            R.drawable.ic_camera
        );

        public static final StandardDirectory DOCUMENTS;

        static {
            DOCUMENTS = new StandardDirectory(
                Environment.DIRECTORY_DOCUMENTS,
                R.string.storage_documents,
                R.drawable.ic_document_grey600
            );
        }

        public static final StandardDirectory DOWNLOADS = new StandardDirectory(
            Environment.DIRECTORY_DOWNLOADS,
            R.string.storage_downloads,
            R.drawable.ic_download_grey600
        );
        public static final StandardDirectory MOVIES = new StandardDirectory(
            Environment.DIRECTORY_MOVIES,
            R.string.storage_movies,
            R.drawable.ic_movie_grey600
        );
        public static final StandardDirectory MUSIC = new StandardDirectory(
            Environment.DIRECTORY_MUSIC,
            R.string.storage_music,
            R.drawable.ic_music_grey600
        );

        private final String name;
        private final int displayNameResource;
        private final int iconResource;

        private StandardDirectory(String name, int displayNameResource, int iconResource) {
            this.name = name;
            this.displayNameResource = displayNameResource;
            this.iconResource = iconResource;
        }

        public String getName() {
            return this.name;
        }

        public int getDisplayName() {
            return this.displayNameResource;
        }

        public int getIcon() {
            return this.iconResource;
        }

        public static Collection<StandardDirectory> getStandardDirectories() {
            Collection<StandardDirectory> standardDirectories = new HashSet<>();
            standardDirectories.add(PICTURES);
            standardDirectories.add(CAMERA);
            if (DOCUMENTS != null) {
                standardDirectories.add(DOCUMENTS);
            }
            standardDirectories.add(DOWNLOADS);
            standardDirectories.add(MOVIES);
            standardDirectories.add(MUSIC);
            return standardDirectories;
        }

        @Nullable
        public static StandardDirectory fromPath(String path) {
            for (StandardDirectory directory : getStandardDirectories()) {
                if (directory.getName().equals(path)) {
                    return directory;
                }
            }
            return null;
        }
    }
}
