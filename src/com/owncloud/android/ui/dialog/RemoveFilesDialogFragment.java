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
 *  Dialog requiring confirmation before removing a given OCFile.  
 * 
 *  Triggers the removal according to the user response.
 */

import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;

import java.util.ArrayList;
import java.util.Vector;

public class RemoveFilesDialogFragment extends ConfirmationDialogFragment
implements ConfirmationDialogFragmentListener {

    private ArrayList<OCFile> mTargetFiles;

    private static final String ARG_TARGET_FILES = "TARGET_FILES";

    /**
     * Public factory method to create new RemoveFileDialogFragment instances.
     * 
     * @param files            Files to remove.
     * @return                Dialog ready to show.
     */
    public static RemoveFilesDialogFragment newInstance(ArrayList<OCFile> files) {
        RemoveFilesDialogFragment frag = new RemoveFilesDialogFragment();
        Bundle args = new Bundle();
        
        int messageStringId = R.string.confirmation_remove_files_alert;
        
        int posBtn = R.string.confirmation_remove_file_remote;
        int negBtn = -1;

        boolean containsFolder = false;
        boolean containsDown = false;
        for (OCFile file: files) {
            if (file.isFolder()) containsFolder = true;
            if (file.isDown()) containsDown = true;
        }

        if (containsFolder) {
            messageStringId = R.string.confirmation_remove_folders_alert;
            posBtn = R.string.confirmation_remove_remote_and_local;
            negBtn = R.string.confirmation_remove_local;
        } else if (containsDown) {
            posBtn = R.string.confirmation_remove_remote_and_local;
            negBtn = R.string.confirmation_remove_local;
        }
        
        args.putInt(ARG_CONF_RESOURCE_ID, messageStringId);
        args.putStringArray(ARG_CONF_ARGUMENTS, new String[]{MainApp.getAppContext().getString(R.string.confirmation_remove_files_alert)});
        args.putInt(ARG_POSITIVE_BTN_RES, posBtn);
        args.putInt(ARG_NEUTRAL_BTN_RES, R.string.common_no);
        args.putInt(ARG_NEGATIVE_BTN_RES, negBtn);
        args.putParcelableArrayList(ARG_TARGET_FILES, files);
        frag.setArguments(args);
        
        return frag;
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
        FileDataStorageManager storageManager = cg.getStorageManager();
        for (OCFile targetFile : mTargetFiles) {
            if (storageManager.getFileById(targetFile.getFileId()) != null) {
                cg.getFileOperationsHelper().removeFile(targetFile, false);
            }
        }
    }
    
    /**
     * Performs the removal of the local copy of the target file
     */
    @Override
    public void onCancel(String callerTag) {
        ComponentsGetter cg = (ComponentsGetter) getActivity();

        for (OCFile targetFile : mTargetFiles) {
            cg.getFileOperationsHelper().removeFile(targetFile, true);

            FileDataStorageManager storageManager = cg.getStorageManager();

            boolean containsFavorite = false;
            if (targetFile.isFolder()) {
                // TODO Enable when "On Device" is recovered ?
                Vector<OCFile> files = storageManager.getFolderContent(targetFile/*, false*/);
                for (OCFile file : files) {
                    containsFavorite = file.isFavorite() || containsFavorite;

                    if (containsFavorite)
                        break;
                }
            }

            // Remove etag for parent, if file is a favorite
            // or is a folder and contains favorite
            if (targetFile.isFavorite() || containsFavorite) {
                OCFile folder = null;
                if (targetFile.isFolder()) {
                    folder = targetFile;
                } else {
                    folder = storageManager.getFileById(targetFile.getParentId());
                }

                folder.setEtag("");
                storageManager.saveFile(folder);
            }
        }
    }

    @Override
    public void onNeutral(String callerTag) {
        // nothing to do here
    }
}