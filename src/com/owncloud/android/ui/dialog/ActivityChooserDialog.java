/* ownCloud Android client application
 *   Copyright (C) 2012-2014 ownCloud Inc.
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.owncloud.android.R;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.Decision;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.OnConflictDecisionMadeListener;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.Log_OC;

/**
 * Dialog showing a list activities able to resolve a given Intent, 
 * filtering out the activities matching give package names.
 * 
 * @author David A. Velasco
 */
public class ActivityChooserDialog  extends SherlockDialogFragment {
    
    private final static String TAG =  ActivityChooserDialog.class.getSimpleName();
    private final static String ARG_INTENT =  ActivityChooserDialog.class.getSimpleName() + ".ARG_INTENT";
    private final static String ARG_PACKAGES_TO_EXCLUDE =  ActivityChooserDialog.class.getSimpleName() + ".ARG_PACKAGES_TO_EXCLUDE";
    
    private ListAdapter mAdapter = null; //new ArrayAdapter<Intent>();
    
    public static ActivityChooserDialog newInstance(Intent intent, String[] packagesToExclude/*OnConflictDecisionMadeListener listener*/) {
        ActivityChooserDialog f = new ActivityChooserDialog();
        Bundle args = new Bundle();
        args.putParcelable(ARG_INTENT, intent);
        args.putStringArray(ARG_PACKAGES_TO_EXCLUDE, packagesToExclude);
        f.setArguments(args);
        return f;
    }
    
    public ActivityChooserDialog() {
        super();
        Log_OC.d(TAG, "constructor");
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Intent intent = getArguments().getParcelable(ARG_INTENT);
        String [] packagesToExclude = getArguments().getStringArray(ARG_PACKAGES_TO_EXCLUDE);
        
        // TODO init mAdapter
        
        return new AlertDialog.Builder(getSherlockActivity())
                   .setIcon(DisplayUtils.getSeasonalIconId())
                   .setTitle(R.string.activity_chooser_title)
                   .setAdapter(mAdapter, new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialog, int which) {
                               // The 'which' argument contains the index position
                               // of the selected item                           
                           }
                       })
                   .create();
    }
    
}
