/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils;

import android.os.Build;
import android.text.TextUtils;

import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation;

import org.lukhnos.nnio.file.FileVisitResult;
import org.lukhnos.nnio.file.FileVisitor;
import org.lukhnos.nnio.file.Path;
import org.lukhnos.nnio.file.impl.FileBasedPathImpl;

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

    public static Path walkFileTree(Path start, FileVisitor<? super Path> visitor) throws IOException {
        if (org.lukhnos.nnio.file.Files.isDirectory(start)) {
            org.lukhnos.nnio.file.FileVisitResult preVisitDirectoryResult = visitor.preVisitDirectory(start, null);
            if (preVisitDirectoryResult == FileVisitResult.CONTINUE) {
                for (File child : start.toFile().listFiles()) {
                    walkFileTree(FileBasedPathImpl.get(child), visitor);
                }
            }
            visitor.postVisitDirectory(start, null);
        } else {
            visitor.visitFile(start, new org.lukhnos.nnio.file.attribute.BasicFileAttributes(start.toFile()));
        }
        return start;
    }
}
