/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
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
import android.content.res.Resources;
import android.os.Bundle;
import android.view.ActionMode;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.Collection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

/**
 *  Dialog requiring confirmation before removing a collection of given OCFiles.
 *
 *  Triggers the removal according to the user response.
 */
public class SyncFileNotEnoughSpaceDialogFragment extends ConfirmationDialogFragment implements
        ConfirmationDialogFragmentListener {


    private static final String ARG_DIALOG_TITLE = "dialog_title_res";
    private static final String ARG_DIALOG_MESSAGE = "dialog_message_res";
    private static final String ARG_FILE_NAME = "dialog_file_name";
    private static final String ARG_FILE_SIZE = "dialog_file_size";
    private static final String ARG_DEVICE_FREE_SPACE = "dialog_device_free_space";

    private ActionMode actionMode;
    String properFileSize;
    String properDiskFreeSpace;
    String fileName;

    public static SyncFileNotEnoughSpaceDialogFragment newInstance(OCFile file, long availableDeviceSpace,
                                                                   ActionMode actionMode) {
        SyncFileNotEnoughSpaceDialogFragment dialogFragment = newInstance(file, availableDeviceSpace);
        dialogFragment.setActionMode(actionMode);
        return dialogFragment;
    }


    public static SyncFileNotEnoughSpaceDialogFragment newInstance(OCFile file, long availableDeviceSpace) {
        SyncFileNotEnoughSpaceDialogFragment frag = new SyncFileNotEnoughSpaceDialogFragment();
        Bundle args = new Bundle();



        // ------------------
        // <EXPORTED>
        // ------------------


/*        String properFileValue = DisplayUtils.bytesToHumanReadable(file.getFileLength());
        String properDiskValue = DisplayUtils.bytesToHumanReadable(availableDeviceSpace);

        String fileName = file.getFileName();

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder();

        // Replace strings by lang-string-xml values
        builder.setTitle("Not enough space");
        builder.setMessage(fileName + " is " + properFileValue + " but there is only " + properDiskValue + " " +
                               "available on device.");

        builder.setPositiveButton("Choose what to synchronize", null);
        builder.setNeutralButton("Free up space", null);
        builder.setNegativeButton("Cancel", null);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();
     */

        // ------------------
        // </EXPORTED>
        // ------------------

        // Here,
        /*
            - Title : Not enough space
            - Base message  : {0} is {1} but there is only {2} available on device. -> applied
            - Button top - positive : Choose what to synchronize -> applied
            - Button mid - neutral : Free up space ->
            - Button bot - negative : Cancel (R.string.common_cancel)
            - {0} : File name (file.getFileName)
            - {1} : File size
            - {2} : Device free space
         */

        // args.putInt(ARG_MESSAGE_RESOURCE_ID, messageStringId);


        args.putInt(ARG_DIALOG_TITLE, R.string.sync_not_enough_space_dialog_title);
        args.putInt(ARG_DIALOG_MESSAGE, R.string.sync_not_enough_space_dialog_placeholder);
        args.putInt(ARG_POSITIVE_BTN_RES, R.string.sync_not_enough_space_dialog_action_choose);
        args.putInt(ARG_NEUTRAL_BTN_RES, R.string.sync_not_enough_space_dialog_action_free_space);
        args.putInt(ARG_NEGATIVE_BTN_RES, R.string.common_cancel);

        args.putString(ARG_FILE_NAME, file.getFileName());
        args.putLong(ARG_FILE_SIZE, file.getFileLength());
        args.putLong(ARG_DEVICE_FREE_SPACE, availableDeviceSpace);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onStart() {
        super.onStart();

        int color = ThemeUtils.primaryAccentColor(getActivity());
        AlertDialog alertDialog = (AlertDialog) getDialog();

        if (alertDialog != null) {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
            alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(color);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        if (getArguments() != null) {
            properDiskFreeSpace = DisplayUtils.bytesToHumanReadable(getArguments().getLong(ARG_DEVICE_FREE_SPACE));
            properFileSize = DisplayUtils.bytesToHumanReadable(getArguments().getLong(ARG_FILE_SIZE));
            fileName = getArguments().getString(ARG_FILE_NAME);
        }

        // mTargetFiles = getArguments().getParcelableArrayList(ARG_TARGET_FILES);

        setOnConfirmationListener(this);

        return dialog;
    }

    /**
     * Performs the removal of the target file, both locally and in the server and
     * finishes the supplied ActionMode if one was given.
     */
    @Override
    public void onConfirmation(String callerTag) {
        // ONLY IF FOLDER -> GO TO DESTINATION
    }

    /**
     * Performs the removal of the local copy of the target file
     */
    @Override
    public void onCancel(String callerTag) {
        // CANCEL -> CLOSE
    }

    @Override
    public void onNeutral(String callerTag) {
        // WIPE SPACE
    }

    private void setActionMode(ActionMode actionMode) {
        this.actionMode = actionMode;
    }

    /**
     * This is used when finishing an actionMode,
     * for example if we want to exit the selection mode
     * after deleting the target files.
     */
    private void finishActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }
}
