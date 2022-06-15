/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2020 Andy Scherzinger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils;

import com.owncloud.android.datamodel.MediaFolder;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.SyncedFolder;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.Nullable;

/**
 * Utility class with methods for processing synced folders.
 */
public final class SyncedFolderUtils {
    private static final String[] DISQUALIFIED_MEDIA_DETECTION_SOURCE = new String[]{
        "cover.jpg", "cover.jpeg",
        "folder.jpg", "folder.jpeg"
    };
    private static final Set<String> DISQUALIFIED_MEDIA_DETECTION_FILE_SET =
        new HashSet<>(Arrays.asList(DISQUALIFIED_MEDIA_DETECTION_SOURCE));
    private static final Set<MediaFolderType> AUTO_QUALIFYING_FOLDER_TYPE_SET =
        new HashSet<>(Collections.singletonList(MediaFolderType.CUSTOM));
    private static final String THUMBNAIL_FOLDER_PREFIX = ".thumbnail";
    private static final String THUMBNAIL_DATA_FILE_PREFIX = ".thumbdata";
    private static final int SINGLE_FILE = 1;

    private SyncedFolderUtils() {
        // utility class -> private constructor
    }

    /**
     * analyzes a given media folder if its content qualifies for the folder to be handled as a media folder.
     *
     * @param mediaFolder media folder to analyse
     * @return <code>true</code> if it qualifies as a media folder else <code>false</code>
     */
    public static boolean isQualifyingMediaFolder(@Nullable MediaFolder mediaFolder) {
        if (mediaFolder == null) {
            return false;
        }

        // custom folders are always fine
        if (AUTO_QUALIFYING_FOLDER_TYPE_SET.contains(mediaFolder.type)) {
            return true;
        }

        // thumbnail folder
        if (!isQualifiedFolder(mediaFolder.absolutePath)) {
            return false;
        }

        // filter media folders

        // no files
        if (mediaFolder.numberOfFiles < SINGLE_FILE) {
            return false;
        } // music album (just one cover-art image)
        else if (MediaFolderType.IMAGE == mediaFolder.type) {
            return containsQualifiedImages(mediaFolder.filePaths);
        }

        return true;
    }

    /**
     * analyzes a given synced folder if its content qualifies for the folder to be handled as a media folder.
     *
     * @param syncedFolder synced folder to analyse
     * @return <code>true</code> if it qualifies as a media folder else <code>false</code>
     */
    public static boolean isQualifyingMediaFolder(@Nullable SyncedFolder syncedFolder) {
        if (syncedFolder == null) {
            return false;
        }

        // custom folders are always fine
        if (AUTO_QUALIFYING_FOLDER_TYPE_SET.contains(syncedFolder.getType())) {
            return true;
        }

        // thumbnail folder
        if (!isQualifiedFolder(syncedFolder.getLocalPath())) {
            return false;
        }

        // filter media folders
        File[] files = getFileList(new File(syncedFolder.getLocalPath()));

        // no files
        if (files.length < SINGLE_FILE) {
            return false;
        } // music album (just one cover-art image)
        else if (MediaFolderType.IMAGE == syncedFolder.getType()) {
            return containsQualifiedImages(files);
        }

        return true;
    }

    /**
     * analyzes a given folder based on a path-string and type if its content qualifies for the folder to be handled as
     * a media folder.
     *
     * @param folderPath String representation for a folder
     * @param folderType type of the folder
     * @return <code>true</code> if it qualifies as a media folder else <code>false</code>
     */
    public static boolean isQualifyingMediaFolder(String folderPath, MediaFolderType folderType) {
        // custom folders are always fine
        if (MediaFolderType.CUSTOM == folderType) {
            return true;
        }

        // custom folders are always fine
        if (AUTO_QUALIFYING_FOLDER_TYPE_SET.contains(folderType)) {
            return true;
        }

        // thumbnail folder
        if (!isQualifiedFolder(folderPath)) {
            return false;
        }

        // filter media folders
        File[] files = getFileList(new File(folderPath));

        // no files
        if (files.length < SINGLE_FILE) {
            return false;
        } // music album (just one cover-art image)
        else if (MediaFolderType.IMAGE == folderType) {
            return containsQualifiedImages(files);
        }

        return true;
    }

    /**
     * check if folder is qualified for auto upload.
     *
     * @param folderPath the folder's path string
     * @return code>true</code> if folder qualifies for auto upload else <code>false</code>
     */
    public static boolean isQualifiedFolder(String folderPath) {
        File folder = new File(folderPath);
        // check if folder starts with thumbnail prefix
        return folder.isDirectory() && !folder.getName().startsWith(THUMBNAIL_FOLDER_PREFIX);
    }

    /**
     * check if given list contains images that qualify as auto upload relevant files.
     *
     * @param filePaths list of file paths
     * @return <code>true</code> if at least one files qualifies as auto upload relevant else <code>false</code>
     */
    private static boolean containsQualifiedImages(List<String> filePaths) {
        for (String filePath : filePaths) {
            if (isFileNameQualifiedForAutoUpload(FileUtil.getFilenameFromPathString(filePath))
                && MimeTypeUtil.isImage(MimeTypeUtil.getMimeTypeFromPath(filePath))) {
                return true;
            }
        }

        return false;
    }

    /**
     * check if given list of files contains images that qualify as auto upload relevant files.
     *
     * @param files list of files
     * @return <code>true</code> if at least one files qualifies as auto upload relevant else <code>false</code>
     */
    private static boolean containsQualifiedImages(File... files) {
        for (File file : files) {
            if (isFileNameQualifiedForAutoUpload(file.getName()) && MimeTypeUtil.isImage(file)) {
                return true;
            }
        }

        return false;
    }

    /**
     * check if given file is auto upload relevant files.
     *
     * @param fileName file name to be checked
     * @return <code>true</code> if the file qualifies as auto upload relevant else <code>false</code>
     */
    public static boolean isFileNameQualifiedForAutoUpload(String fileName) {
        if (fileName != null) {
            return !DISQUALIFIED_MEDIA_DETECTION_FILE_SET.contains(fileName.toLowerCase(Locale.ROOT))
                && !fileName.startsWith(THUMBNAIL_DATA_FILE_PREFIX);
        } else {
            return false;
        }
    }

    /**
     * return list of files for given folder.
     *
     * @param localFolder folder to scan
     * @return sorted list of folder of given folder
     */
    public static File[] getFileList(File localFolder) {
        File[] files = localFolder.listFiles(pathname -> !pathname.isDirectory());

        if (files != null) {
            Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
        } else {
            files = new File[]{};
        }

        return files;
    }
}
