/*
 *   Nextcloud Android client application
 *
 *   @author Kilian Périsset
 *   Copyright (C) 2020 Infomaniak Network SA
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License (GPLv3),
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.dialog;

import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.storage.StorageManager;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ThemeButtonUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

/**
 * Dialog requiring confirmation when a file/folder is too "big" to be synchronized/downloaded on device.
 */
public class SyncFileNotEnoughSpaceDialogFragment extends ConfirmationDialogFragment implements
    ConfirmationDialogFragmentListener {

    private static final String ARG_PASSED_FILE = "fragment_parent_caller";
    private static final int REQUEST_CODE_STORAGE = 20;

    private OCFile targetFile;

    public static SyncFileNotEnoughSpaceDialogFragment newInstance(OCFile file, long availableDeviceSpace) {
        Bundle args = new Bundle();
        SyncFileNotEnoughSpaceDialogFragment frag = new SyncFileNotEnoughSpaceDialogFragment();
        String properFileSize = DisplayUtils.bytesToHumanReadable(file.getFileLength());
        String properDiskAvailableSpace = DisplayUtils.bytesToHumanReadable(availableDeviceSpace);

        // Defining title, message and resources
        args.putInt(ARG_TITLE_ID, R.string.sync_not_enough_space_dialog_title);
        args.putInt(ARG_MESSAGE_RESOURCE_ID, R.string.sync_not_enough_space_dialog_placeholder);
        args.putStringArray(ARG_MESSAGE_ARGUMENTS,
                            new String[] {
                                file.getFileName(),
                                properFileSize,
                                properDiskAvailableSpace});
        args.putParcelable(ARG_PASSED_FILE, file);

        // Defining buttons
        if (file.isFolder()) {
            args.putInt(ARG_POSITIVE_BTN_RES, R.string.sync_not_enough_space_dialog_action_choose);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            args.putInt(ARG_NEGATIVE_BTN_RES, R.string.sync_not_enough_space_dialog_action_free_space);
        }
        args.putInt(ARG_NEUTRAL_BTN_RES, R.string.common_cancel);

        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog alertDialog = (AlertDialog) getDialog();

        if (alertDialog != null) {
            ThemeButtonUtils.themeBorderlessButton(alertDialog.getButton(AlertDialog.BUTTON_POSITIVE),
                                                   alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL),
                                                   alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE));
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();

        if (arguments == null) {
            throw new IllegalArgumentException("Arguments may not be null");
        }

        targetFile = arguments.getParcelable(ARG_PASSED_FILE);
        setOnConfirmationListener(this);

        return super.onCreateDialog(savedInstanceState);
    }

    /**
     * (Only if file is a folder), will access the destination folder to allow user to choose what to synchronize
     */
    @Override
    public void onConfirmation(String callerTag) {
        OCFileListFragment frag = (OCFileListFragment) getTargetFragment();
        if (frag != null && targetFile != null) {
            frag.onItemClicked(targetFile);
        }
    }

    /**
     * Will abort/cancel the process (is neutral to "hack" android button position ._.)
     */
    @Override
    public void onNeutral(String callerTag) {
        // Nothing
    }

    /**
     * Will access to storage manager in order to empty useless files
     */
    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    @Override
    public void onCancel(String callerTag) {
        Intent storageIntent = new Intent(StorageManager.ACTION_MANAGE_STORAGE);
        startActivityForResult(storageIntent, REQUEST_CODE_STORAGE);
    }
}
