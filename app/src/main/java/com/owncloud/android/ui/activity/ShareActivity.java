/*
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author Juan Carlos González Cabrero
 * Copyright (C) 2015 ownCloud Inc.
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

package com.owncloud.android.ui.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.nextcloud.client.account.User;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ShareActivityBinding;
import com.owncloud.android.datamodel.OCFile;
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

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

/**
 * Activity for sharing files.
 */
public class ShareActivity extends FileActivity {

    private static final String TAG = ShareActivity.class.getSimpleName();

    static final String TAG_SHARE_FRAGMENT = "SHARE_FRAGMENT";

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
            binding.shareFileIcon.setImageDrawable(MimeTypeUtil.getFolderTypeIcon(file.isSharedWithMe() ||
                                                                                      file.isSharedWithSharee(),
                                                                                  file.isSharedViaLink(),
                                                                                  file.isEncrypted(),
                                                                                  file.isGroupFolder(),
                                                                                  file.getMountType(),
                                                                                  this,
                                                                                  viewThemeUtils));
        } else {
            binding.shareFileIcon.setImageDrawable(MimeTypeUtil.getFileTypeIcon(file.getMimeType(),
                                                                                file.getFileName(),
                                                                                this,
                                                                                viewThemeUtils));
            if (MimeTypeUtil.isImage(file)) {
                String remoteId = String.valueOf(file.getRemoteId());
                Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(remoteId);
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
                                                                                                             shareType),
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
