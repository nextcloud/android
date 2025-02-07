/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2016 Juan Carlos González Cabrero <malkomich@gmail.com>
 * SPDX-FileCopyrightText: 2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;

import com.nextcloud.client.account.User;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ShareActivityBinding;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.operations.GetSharesForFileOperation;
import com.owncloud.android.ui.fragment.FileDetailSharingFragment;
import com.owncloud.android.ui.fragment.FileDetailsSharingProcessFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;

import java.util.Optional;

import javax.inject.Inject;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

/**
 * Activity for sharing files.
 */
public class ShareActivity extends FileActivity {

    private static final String TAG = ShareActivity.class.getSimpleName();

    static final String TAG_SHARE_FRAGMENT = "SHARE_FRAGMENT";

    @Inject
    SyncedFolderProvider syncedFolderProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShareActivityBinding binding = ShareActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        OCFile file = getFile();
        Optional<User> optionalUser = getUser();
        if (!optionalUser.isPresent()) {
            finish();
            return;
        }

        // Icon
        if (file.isFolder()) {
            boolean isAutoUploadFolder = SyncedFolderProvider.isAutoUploadFolder(syncedFolderProvider, file, optionalUser.get());

            Integer overlayIconId = file.getFileOverlayIconId(isAutoUploadFolder);
            LayerDrawable drawable = MimeTypeUtil.getFolderIcon(preferences.isDarkModeEnabled(), overlayIconId, this, viewThemeUtils);
            binding.shareFileIcon.setImageDrawable(drawable);
        } else {
            binding.shareFileIcon.setImageDrawable(MimeTypeUtil.getFileTypeIcon(file.getMimeType(),
                                                                                file.getFileName(),
                                                                                this,
                                                                                viewThemeUtils));
            if (MimeTypeUtil.isImage(file)) {
                Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(file.getEtag());
                if (thumbnail != null) {
                    binding.shareFileIcon.setImageBitmap(thumbnail);
                }
            }
        }

        // Name
        binding.shareFileName.setText(getResources().getString(R.string.share_file, file.getFileName()));

        viewThemeUtils.platform.colorViewBackground(binding.shareHeaderDivider);

        // Size
        binding.shareFileSize.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));

        Activity activity = this;
        new Thread(() -> {
            RemoteOperationResult result = new ReadFileRemoteOperation(getFile().getRemotePath())
                .execute(optionalUser.get(),
                         activity);

            if (result.isSuccess()) {
                RemoteFile remoteFile = (RemoteFile) result.getData().get(0);
                long length = remoteFile.getLength();

                getFile().setFileLength(length);
                runOnUiThread(() -> binding.shareFileSize.setText(DisplayUtils.bytesToHumanReadable(length)));
            }
        }).start();

        if (savedInstanceState == null) {
            // Add Share fragment on first creation
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment fragment = FileDetailSharingFragment.newInstance(getFile(), optionalUser.get());
            ft.replace(R.id.share_fragment_container, fragment, TAG_SHARE_FRAGMENT);
            ft.commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Load data into the list
        Log_OC.d(TAG, "Refreshing lists on account set");
        refreshSharesFromStorageManager();
    }

    @Override
    protected void doShareWith(String shareeName, ShareType shareType) {
        getSupportFragmentManager().beginTransaction().replace(R.id.share_fragment_container,
                                                               FileDetailsSharingProcessFragment.newInstance(getFile(),
                                                                                                             shareeName,
                                                                                                             shareType,
                                                                                                             false),
                                                               FileDetailsSharingProcessFragment.TAG)
            .commit();
    }

    /**
     * Updates the view associated to the activity after the finish of some operation over files in the current
     * account.
     *
     * @param operation Removal operation performed.
     * @param result    Result of the removal.
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        super.onRemoteOperationFinish(operation, result);

        if (result.isSuccess() ||
                (operation instanceof GetSharesForFileOperation &&
                        result.getCode() == RemoteOperationResult.ResultCode.SHARE_NOT_FOUND
                )
                ) {
            Log_OC.d(TAG, "Refreshing view on successful operation or finished refresh");
            refreshSharesFromStorageManager();
        }
    }

    /**
     * Updates the view, reading data from {@link com.owncloud.android.datamodel.FileDataStorageManager}.
     */
    private void refreshSharesFromStorageManager() {

        FileDetailSharingFragment shareFileFragment = getShareFileFragment();
        if (shareFileFragment != null
                && shareFileFragment.isAdded()) {   // only if added to the view hierarchy!!
            shareFileFragment.refreshCapabilitiesFromDB();
            shareFileFragment.refreshSharesFromDB();
        }
    }

    /**
     * Shortcut to get access to the {@link FileDetailSharingFragment} instance, if any
     *
     * @return A {@link FileDetailSharingFragment} instance, or null
     */
    private FileDetailSharingFragment getShareFileFragment() {
        return (FileDetailSharingFragment) getSupportFragmentManager().findFragmentByTag(TAG_SHARE_FRAGMENT);
    }

    @Override
    public void onShareProcessClosed() {
        finish();
    }
}
