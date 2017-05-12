/**
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
 *
 */

package com.owncloud.android.ui.dialog;

/**
 *  Dialog requiring confirmation before removing a collection of given OCFiles.
 * 
 *  Triggers the removal according to the user response.
 */

import android.app.Dialog;
import android.os.Bundle;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;

import java.util.ArrayList;
import java.util.Collection;

public class RemoveFilesDialogFragment extends ConfirmationDialogFragment
implements ConfirmationDialogFragmentListener {

    private Collection<OCFile> mTargetFiles;

    private static final String ARG_TARGET_FILES = "TARGET_FILES";

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
        boolean containsFavorite = false;
        for (OCFile file: files) {
            containsFolder |= file.isFolder();
            containsDown |= file.isDown();
            containsFavorite |= file.isAvailableOffline();
        }

        if (files.size() == 1) {
            // choose message for a single file
            OCFile file = files.get(0);

            messageStringId = (file.isFolder()) ?
                R.string.confirmation_remove_folder_alert :
                R.string.confirmation_remove_file_alert;

        } else {
            // choose message for more than one file
            messageStringId = (containsFolder) ?
                R.string.confirmation_remove_folders_alert :
                R.string.confirmation_remove_files_alert;

        }

        int localRemoveButton = (!containsFavorite && (containsFolder || containsDown)) ?
            R.string.confirmation_remove_local :
            -1;

        args.putInt(ARG_MESSAGE_RESOURCE_ID, messageStringId);
        if (files.size() == 1) {
            args.putStringArray(ARG_MESSAGE_ARGUMENTS, new String[]{files.get(0).getFileName()});
        }
        args.putInt(ARG_POSITIVE_BTN_RES, R.string.common_yes);
        args.putInt(ARG_NEUTRAL_BTN_RES, R.string.common_no);
        args.putInt(ARG_NEGATIVE_BTN_RES, localRemoveButton);
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
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        mTargetFiles = getArguments().getParcelableArrayList(ARG_TARGET_FILES);
        
        setOnConfirmationListener(this);
        
        return dialog;
    }    

    /**
     * Performs the removal of the target file, both locally and in the server.
     */
    @Override
    public void onConfirmation(String callerTag) {
        ComponentsGetter cg = (ComponentsGetter) getActivity();
        cg.getFileOperationsHelper().removeFiles(mTargetFiles, false);
    }
    
    /**
     * Performs the removal of the local copy of the target file
     */
    @Override
    public void onCancel(String callerTag) {
        ComponentsGetter cg = (ComponentsGetter) getActivity();
        cg.getFileOperationsHelper().removeFiles(mTargetFiles, true);
    }

    @Override
    public void onNeutral(String callerTag) {
        // nothing to do here
    }
}