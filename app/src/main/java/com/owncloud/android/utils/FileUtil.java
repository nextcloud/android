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

import android.os.Build;
import android.text.TextUtils;

import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class FileUtil {

    private FileUtil() {
        // utility class -> private constructor
    }

    /**
     * returns the file name of a given path.
     *
     * @param filePath (absolute) file path
     * @return the filename including its file extension, <code>empty String</code> for invalid input values
     */
    public static @NonNull
    String getFilenameFromPathString(@Nullable String filePath) {
        if (!TextUtils.isEmpty(filePath)) {
            File file = new File(filePath);
            if (file.isFile()) {
                return file.getName();
            } else {
                return "";
            }
        } else {
            return "";
        }
    }

    public static @Nullable
    Long getCreationTimestamp(File file) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return null;
        }

        try {
            return Files.readAttributes(file.toPath(), BasicFileAttributes.class)
                .creationTime()
                .to(TimeUnit.SECONDS);
        } catch (IOException e) {
            Log_OC.e(UploadFileRemoteOperation.class.getSimpleName(),
                     "Failed to read creation timestamp for file: " + file.getName());
            return null;
        }
    }
}
