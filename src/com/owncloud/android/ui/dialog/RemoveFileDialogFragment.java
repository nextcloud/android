/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
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
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;

import android.app.Dialog;
import android.os.Bundle;

public class RemoveFileDialogFragment extends ConfirmationDialogFragment 
implements ConfirmationDialogFragmentListener {

    private static final String ARG_TARGET_FILE = "TARGET_FILE";

    /**
     * Public factory method to create new RemoveFIleDialogFragment instances.
     * 
     * @param string_id         Resource id for a message to show in the dialog.
     * @param arguments         Arguments to complete the message, if it's a format string.
     * @param posBtn            Resource id for the text of the positive button.
     * @param neuBtn            Resource id for the text of the neutral button.
     * @param negBtn            Resource id for the text of the negative button.
     * @return                  Dialog ready to show.
     */
    public static RemoveFileDialogFragment newInstance(OCFile file) {
        RemoveFileDialogFragment frag = new RemoveFileDialogFragment();
        Bundle args = new Bundle();
        
        int messageStringId = R.string.confirmation_remove_alert;
        
        int posBtn = R.string.confirmation_remove_remote;
        int neuBtn = -1;
        if (file.isFolder()) {
            messageStringId = R.string.confirmation_remove_folder_alert;
            posBtn = R.string.confirmation_remove_remote_and_local;
            neuBtn = R.string.confirmation_remove_folder_local;
        } else if (file.isDown()) {
            posBtn = R.string.confirmation_remove_remote_and_local;
            neuBtn = R.string.confirmation_remove_local;
        }
        
        
        args.putInt(ARG_CONF_RESOURCE_ID, messageStringId);
        args.putStringArray(ARG_CONF_ARGUMENTS, new String[]{file.getFileName()});
        args.putInt(ARG_POSITIVE_BTN_RES, posBtn);
        args.putInt(ARG_NEUTRAL_BTN_RES, neuBtn);
        args.putInt(ARG_NEGATIVE_BTN_RES, R.string.common_cancel);
        args.putParcelable(ARG_TARGET_FILE, file);
        frag.setArguments(args);
        
        frag.setOnConfirmationListener(frag);
        
        return frag;
    }

    private OCFile mTargetFile;
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        mTargetFile = getArguments().getParcelable(ARG_TARGET_FILE);
        return dialog;
    }    

    /**
     * Performs the removal of the target file, both locally and in the server.
     */
    @Override
    public void onConfirmation(String callerTag) {
        ComponentsGetter cg = (ComponentsGetter)getSherlockActivity();
        FileDataStorageManager storageManager = cg.getStorageManager();
        if (storageManager.getFileById(mTargetFile.getFileId()) != null) {
            cg.getFileOperationsHelper().removeFile(mTargetFile, false);
        }
    }
    
    /**
     * Performs the removal of the local copy of the taget file
     */
    @Override
    public void onNeutral(String callerTag) {
        ((ComponentsGetter)getSherlockActivity()).getFileOperationsHelper()
            .removeFile(mTargetFile, true);
    }

    @Override
    public void onCancel(String callerTag) {
        // nothing to do here
    }
    
}
