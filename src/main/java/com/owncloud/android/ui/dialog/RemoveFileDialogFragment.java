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
import android.os.Bundle;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;

import java.util.ArrayList;
import java.util.List;

public class RemoveFileDialogFragment extends ConfirmationDialogFragment 
implements ConfirmationDialogFragmentListener {

    private OCFile mTargetFile;

    private static final String ARG_TARGET_FILE = "TARGET_FILE";

    /**
     * Public factory method to create new RemoveFileDialogFragment instances.
     * 
     * @param file            File to remove.
     * @return                Dialog ready to show.
     */
    public static RemoveFileDialogFragment newInstance(OCFile file) {
        RemoveFileDialogFragment frag = new RemoveFileDialogFragment();
        Bundle args = new Bundle();
        
        int messageStringId = R.string.confirmation_remove_file_alert;
        
        int localRemoveButton = (!file.isAvailableOffline() && (file.isFolder() || file.isDown())) ?
            R.string.confirmation_remove_local : -1;

        if (file.isFolder()) {
            messageStringId = R.string.confirmation_remove_folder_alert;
        }

        args.putInt(ARG_MESSAGE_RESOURCE_ID, messageStringId);
        args.putStringArray(ARG_MESSAGE_ARGUMENTS, new String[]{file.getFileName()});
        args.putInt(ARG_POSITIVE_BTN_RES, R.string.common_yes);
        args.putInt(ARG_NEUTRAL_BTN_RES, R.string.common_no);
        args.putInt(ARG_NEGATIVE_BTN_RES, localRemoveButton);
        args.putParcelable(ARG_TARGET_FILE, file);
        frag.setArguments(args);
        
        return frag;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        mTargetFile = getArguments().getParcelable(ARG_TARGET_FILE);
        
        setOnConfirmationListener(this);
        
        return dialog;
    }    

    /**
     * Performs the removal of the target file, both locally and in the server.
     */
    @Override
    public void onConfirmation(String callerTag) {
        ComponentsGetter cg = (ComponentsGetter)getActivity();
        FileDataStorageManager storageManager = cg.getStorageManager();
        if (storageManager.getFileById(mTargetFile.getFileId()) != null) {
            ArrayList<OCFile> list = new ArrayList<>();
            list.add(mTargetFile);
            cg.getFileOperationsHelper().removeFiles(list, false);
        }
    }
    
    /**
     * Performs the removal of the local copy of the target file
     */
    @Override
    public void onCancel(String callerTag) {
        ComponentsGetter cg = (ComponentsGetter)getActivity();
        ArrayList<OCFile> list = new ArrayList<>();
        list.add(mTargetFile);
        cg.getFileOperationsHelper().removeFiles(list, true);
        
        FileDataStorageManager storageManager = cg.getStorageManager();
        
        boolean containsFavorite = false;
        if (mTargetFile.isFolder()) {
            List<OCFile> files = storageManager.getFolderContent(mTargetFile, false);
            
            for(OCFile file: files) {
                containsFavorite = file.isAvailableOffline() || containsFavorite;

                if (containsFavorite) {
                    break;
                }
            }
        }

        // Remove etag for parent, if file is a favorite
        // or is a folder and contains favorite
        if (mTargetFile.isAvailableOffline() || containsFavorite) {
            OCFile folder = null;
            if (mTargetFile.isFolder()) {
                folder = mTargetFile;
            } else {
                folder = storageManager.getFileById(mTargetFile.getParentId());
            }
            
           folder.setEtag("");
           storageManager.saveFile(folder);
        }
    }

    @Override
    public void onNeutral(String callerTag) {
        // nothing to do here
    }
}