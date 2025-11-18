/*
 * AediNex Music - Android Client
 *
 * SPDX-FileCopyrightText: 2025 AediNex
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.aedinex.music.library;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Helper class for initializing music library on first app launch.
 * Handles permissions and auto-scan.
 */
public class LibraryInitializer {

    private static final String PREFS_NAME = "aedinex_music_prefs";
    private static final String KEY_LIBRARY_INITIALIZED = "library_initialized";
    private static final String KEY_FIRST_SCAN_DONE = "first_scan_done";
    private static final int REQUEST_PERMISSION_CODE = 1001;

    private final Context context;
    private final SharedPreferences prefs;

    public LibraryInitializer(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Check if library has been initialized
     */
    public boolean isLibraryInitialized() {
        return prefs.getBoolean(KEY_LIBRARY_INITIALIZED, false);
    }

    /**
     * Mark library as initialized
     */
    public void markLibraryInitialized() {
        prefs.edit().putBoolean(KEY_LIBRARY_INITIALIZED, true).apply();
    }

    /**
     * Check if first scan has been done
     */
    public boolean isFirstScanDone() {
        return prefs.getBoolean(KEY_FIRST_SCAN_DONE, false);
    }

    /**
     * Mark first scan as done
     */
    public void markFirstScanDone() {
        prefs.edit().putBoolean(KEY_FIRST_SCAN_DONE, true).apply();
    }

    /**
     * Check if we have necessary permissions for music access
     */
    public boolean hasMediaPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: Need READ_MEDIA_AUDIO
            return ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Older Android: Need READ_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Request media permissions from activity
     */
    public void requestMediaPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.READ_MEDIA_AUDIO},
                REQUEST_PERMISSION_CODE);
        } else {
            // Older Android
            ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_PERMISSION_CODE);
        }
    }

    /**
     * Initialize library if needed
     * Returns true if initialization is needed
     */
    public boolean initializeIfNeeded(Activity activity, InitializationCallback callback) {
        if (isLibraryInitialized() && isFirstScanDone()) {
            // Already initialized
            return false;
        }

        if (!hasMediaPermissions()) {
            // Request permissions
            requestMediaPermissions(activity);
            if (callback != null) {
                callback.onPermissionsNeeded();
            }
            return true;
        }

        // Permissions granted, start scan
        if (!isFirstScanDone()) {
            if (callback != null) {
                callback.onScanNeeded();
            }
            return true;
        }

        return false;
    }

    /**
     * Auto-scan library if needed
     */
    public void autoScanIfNeeded(MusicLibraryManager libraryManager, ScanCallback callback) {
        if (isFirstScanDone() || !hasMediaPermissions()) {
            return;
        }

        libraryManager.setScanListener(new MusicLibraryManager.LibraryScanListener() {
            @Override
            public void onScanStarted() {
                if (callback != null) {
                    callback.onScanStarted();
                }
            }

            @Override
            public void onScanCompleted(int trackCount) {
                markFirstScanDone();
                markLibraryInitialized();
                if (callback != null) {
                    callback.onScanCompleted(trackCount);
                }
            }

            @Override
            public void onScanError(Exception e) {
                if (callback != null) {
                    callback.onScanError(e);
                }
            }
        });

        libraryManager.scanLocalLibrary();
    }

    /**
     * Reset initialization state (for testing or re-scan)
     */
    public void reset() {
        prefs.edit()
            .putBoolean(KEY_LIBRARY_INITIALIZED, false)
            .putBoolean(KEY_FIRST_SCAN_DONE, false)
            .apply();
    }

    /**
     * Callback interface for initialization
     */
    public interface InitializationCallback {
        void onPermissionsNeeded();
        void onScanNeeded();
    }

    /**
     * Callback interface for scan
     */
    public interface ScanCallback {
        void onScanStarted();
        void onScanCompleted(int trackCount);
        void onScanError(Exception e);
    }
}
