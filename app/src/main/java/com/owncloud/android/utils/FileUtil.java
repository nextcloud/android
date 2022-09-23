/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * @author TSI-mc
 * Copyright (C) 2020 Andy Scherzinger
 * Copyright (C) 2022 TSI-mc
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

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation;
import com.owncloud.android.ui.helpers.FileOperationsHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class FileUtil {

    private static final String TAG = FileUtil.class.getSimpleName();

    private static final int JPG_FILE_TYPE = 1;
    private static final int PNG_FILE_TYPE = 2;

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

    public static File saveJpgImage(Context context, Bitmap bitmap, String imageName, int quality) {
        return createFileAndSaveImage(context, bitmap, imageName, quality, JPG_FILE_TYPE);
    }

    public static File savePngImage(Context context, Bitmap bitmap, String imageName, int quality) {
        return createFileAndSaveImage(context, bitmap, imageName, quality, PNG_FILE_TYPE);
    }

    public static File saveJpgImage(Context context, Bitmap bitmap, File file, int quality) {
        return saveImage(file, bitmap, quality, JPG_FILE_TYPE);
    }

    public static File savePngImage(Context context, Bitmap bitmap, File file, int quality) {
        return saveImage(file, bitmap, quality, PNG_FILE_TYPE);
    }

    private static File createFileAndSaveImage(Context context, Bitmap bitmap, String imageName, int quality,
                                               int fileType) {
        File file = fileType == PNG_FILE_TYPE ? getPngImageName(context, imageName) : getJpgImageName(context,
                                                                                                      imageName);
        return saveImage(file, bitmap, quality, fileType);
    }

    private static File saveImage(File file, Bitmap bitmap, int quality, int fileType) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);
            byte[] bitmapData = bos.toByteArray();

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(bitmapData);
            fileOutputStream.flush();
            fileOutputStream.close();
            return file;
        } catch (Exception e) {
            Log_OC.e(TAG, " Failed to save image : " + e.getLocalizedMessage());
            return null;
        }
    }

    private static File getJpgImageName(Context context, String imageName) {
        File imageFile = getOutputMediaFile(context);
        if (!TextUtils.isEmpty(imageName)) {
            return new File(imageFile.getPath() + File.separator + imageName + ".jpg");
        } else {
            return new File(imageFile.getPath() + File.separator + "IMG_" + FileOperationsHelper.getCapturedImageName());
        }
    }

    private static File getPngImageName(Context context, String imageName) {
        File imageFile = getOutputMediaFile(context);
        if (!TextUtils.isEmpty(imageName)) {
            return new File(imageFile.getPath() + File.separator + imageName + ".png");
        } else {
            return new File(imageFile.getPath() + File.separator + "IMG_" + FileOperationsHelper.getCapturedImageName().replace(".jpg", ".png"));
        }
    }

    public static File getOutputMediaFile(Context context) {
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "");
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }
}
