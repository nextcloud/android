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
import android.os.Bundle;
import android.view.ActionMode;

import com.google.android.material.button.MaterialButton;
import com.nextcloud.client.di.Injectable;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;

import java.util.ArrayList;
import java.util.Collection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

/**
 * Dialog requiring confirmation before removing a collection of given OCFiles.
 * <p>
 * Triggers the removal according to the user response.
 */
public class RemoveFilesDialogFragment extends ConfirmationDialogFragment implements
    ConfirmationDialogFragmentListener, Injectable {

    private static final int SINGLE_SELECTION = 1;
    private static final String ARG_TARGET_FILES = "TARGET_FILES";

    private Collection<OCFile> mTargetFiles;
    private ActionMode actionMode;

    /**
     * Public factory method to create new RemoveFilesDialogFragment instances.
     *
     * @param files           Files to remove.
     * @param actionMode      ActionMode to finish on confirmation
     * @return Dialog ready to show.
     */
    public static RemoveFilesDialogFragment newInstance(ArrayList<OCFile> files, ActionMode actionMode) {
        RemoveFilesDialogFragment dialogFragment = newInstance(files);
        dialogFragment.setActionMode(actionMode);
        return dialogFragment;
    }

    /**
     * Public factory method to create new RemoveFilesDialogFragment instances.
     *
     * @param files           Files to remove.
     * @return                Dialog ready to show.
     */
    public static RemoveFilesDialogFragment newInstance(ArrayList<OCFile> files) {
        RemoveFilesDialogFragment frag = new RemoveFilesDialogFragment();
        Bundle args = new Bundle();
        int messageStringId;

        boolean containsFolder = false;
        boolean containsDown = false;

        for (OCFile file: files) {
            containsFolder |= file.isFolder();
            containsDown |= file.isDown();
        }

        if (files.size() == SINGLE_SELECTION) {
            // choose message for a single file
            OCFile file = files.get(0);

            messageStringId = file.isFolder() ?
                R.string.confirmation_remove_folder_alert :
                R.string.confirmation_remove_file_alert;

        } else {
            // choose message for more than one file
            messageStringId = containsFolder ?
                R.string.confirmation_remove_folders_alert :
                R.string.confirmation_remove_files_alert;
        }

        args.putInt(ARG_MESSAGE_RESOURCE_ID, messageStringId);
        if (files.size() == SINGLE_SELECTION) {
            args.putStringArray(ARG_MESSAGE_ARGUMENTS, new String[] { files.get(0).getFileName() } );
        }

        args.putInt(ARG_POSITIVE_BTN_RES, R.string.file_delete);

        if (containsFolder || containsDown) {
            args.putInt(ARG_NEGATIVE_BTN_RES, R.string.confirmation_remove_local);
            args.putInt(ARG_NEUTRAL_BTN_RES, R.string.file_keep);
        } else {
            args.putInt(ARG_NEGATIVE_BTN_RES, R.string.file_keep);
        }

        args.putParcelableArrayList(ARG_TARGET_FILES, files);
        frag.setArguments(args);

        return frag;
    }


    /**
     * Convenience factory method to create new RemoveFilesDialogFragment instances for a single file
     *
     * @param file           File to remove.
     * @return                Dialog ready to show.
     */
    public static RemoveFilesDialogFragment newInstance(OCFile file) {
        ArrayList<OCFile> list = new ArrayList<>();
        list.add(file);
        return newInstance(list);
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog alertDialog = (AlertDialog) getDialog();

        if (alertDialog != null) {
            MaterialButton positiveButton = (MaterialButton) alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            viewThemeUtils.material.colorMaterialButtonPrimaryTonal(positiveButton);

            MaterialButton negativeButton = (MaterialButton) alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(negativeButton);

            MaterialButton neutralButton = (MaterialButton) alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (neutralButton != null) {
                viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(neutralButton);
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        Bundle arguments = getArguments();

        if (arguments == null) {
            return dialog;
        }

        mTargetFiles = arguments.getParcelableArrayList(ARG_TARGET_FILES);
        setOnConfirmationListener(this);
        return dialog;
    }

    /**
     * Performs the removal of the target file, both locally and in the server and
     * finishes the supplied ActionMode if one was given.
     */
    @Override
    public void onConfirmation(String callerTag) {
        removeFiles(false);
    }

    /**
     * Performs the removal of the local copy of the target file
     */
    @Override
    public void onCancel(String callerTag) {
        removeFiles(true);
    }

    private void removeFiles(boolean onlyLocalCopy) {
        ComponentsGetter cg = (ComponentsGetter) getActivity();
        if (cg != null) {
            cg.getFileOperationsHelper().removeFiles(mTargetFiles, onlyLocalCopy, false);
        }
        finishActionMode();
    }

    @Override
    public void onNeutral(String callerTag) {
        // nothing to do here
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
