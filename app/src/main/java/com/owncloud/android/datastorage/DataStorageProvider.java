/*
 *   Nextcloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2016 Nextcloud
 *   Copyright (C) 2016 Bartosz Przybylski
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.owncloud.android.datastorage;

import android.content.Context;
import android.os.Environment;

import com.owncloud.android.MainApp;
import com.owncloud.android.datastorage.StoragePoint.PrivacyType;
import com.owncloud.android.datastorage.StoragePoint.StorageType;

import java.io.File;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author Bartosz Przybylski
 */
public class DataStorageProvider {

    private static final String APP_SPECIFIC_DATA_DIR = "/Android/data/";
    private static final DataStorageProvider sInstance = new DataStorageProvider();

    public static DataStorageProvider getInstance() {
        return sInstance;
    }

    private DataStorageProvider() {}

    public StoragePoint[] getAvailableStoragePoints() {
        Context context = MainApp.getAppContext();
        UniqueStorageList storagePoints = new UniqueStorageList();

        for (File mediaDir : context.getExternalMediaDirs()) {
            addSharedStoragePoint(storagePoints, mediaDir, PrivacyType.PUBLIC);
        }

        File filesDir = context.getFilesDir();
        storagePoints.add(createStoragePoint(filesDir, StorageType.INTERNAL, PrivacyType.PRIVATE));

        for (File externalFilesDir : context.getExternalFilesDirs(null)) {
            addSharedStoragePoint(storagePoints, externalFilesDir, PrivacyType.PRIVATE);
        }

        return storagePoints.toArray(new StoragePoint[0]);
    }

    @Nullable
    public StoragePoint findStoragePoint(@NonNull String path) {
        return Arrays.stream(getAvailableStoragePoints())
            .filter(point -> point.getPath().equals(path))
            .findFirst()
            .orElseGet(() -> classifyStoragePath(path));
    }

    public boolean allowAccessFromOtherApps() {
        String storagePath = MainApp.getStoragePath();

        return Arrays.stream(getAvailableStoragePoints())
            .anyMatch(point -> point.getPath().equals(storagePath) &&
                point.getPrivacyType() == PrivacyType.PUBLIC);
    }

    private void addSharedStoragePoint(UniqueStorageList storagePoints, @Nullable File dir, PrivacyType privacyType) {
        if (dir == null || !isMounted(dir)) {
            return;
        }

        storagePoints.add(createStoragePoint(dir, resolveStorageType(dir), privacyType));
    }

    @Nullable
    private StoragePoint classifyStoragePath(String path) {
        File file = new File(path);
        String dataDir = MainApp.getAppContext().getDataDir().getAbsolutePath();

        if (path.startsWith(dataDir)) {
            return createStoragePoint(file, StorageType.INTERNAL, PrivacyType.PRIVATE);
        }

        if (!isMounted(file)) {
            return null;
        }

        PrivacyType privacyType = path.contains(APP_SPECIFIC_DATA_DIR) ? PrivacyType.PRIVATE : PrivacyType.PUBLIC;

        return createStoragePoint(file, resolveStorageType(file), privacyType);
    }

    private boolean isMounted(File dir) {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState(dir));
    }

    private StorageType resolveStorageType(File dir) {
        try {
            return Environment.isExternalStorageRemovable(dir) ? StorageType.EXTERNAL : StorageType.INTERNAL;
        } catch (IllegalArgumentException e) {
            return StorageType.INTERNAL;
        }
    }

    private StoragePoint createStoragePoint(File dir, StorageType storageType, PrivacyType privacyType) {
        String path = dir.getAbsolutePath();

        return new StoragePoint(path, path, storageType, privacyType);
    }
}
