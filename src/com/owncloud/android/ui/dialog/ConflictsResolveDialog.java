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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.owncloud.android.R;
import com.owncloud.android.utils.DisplayUtils;


/**
 * Dialog which will be displayed to user upon keep-in-sync file conflict.
 * 
 * @author Bartek Przybylski
 *
 */
public class ConflictsResolveDialog extends SherlockDialogFragment {

    public static enum Decision { 
        CANCEL,
        KEEP_BOTH,
        OVERWRITE
    }
    
    OnConflictDecisionMadeListener mListener;
    
    public static ConflictsResolveDialog newInstance(String path, OnConflictDecisionMadeListener listener) {
        ConflictsResolveDialog f = new ConflictsResolveDialog();
        Bundle args = new Bundle();
        args.putString("remotepath", path);
        f.setArguments(args);
        f.mListener = listener;
        return f;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String remotepath = getArguments().getString("remotepath");
        return new AlertDialog.Builder(getSherlockActivity())
                   .setIcon(DisplayUtils.getSeasonalIconId())
                   .setTitle(R.string.conflict_title)
                   .setMessage(String.format(getString(R.string.conflict_message), remotepath))
                   .setPositiveButton(R.string.conflict_overwrite,
                       new DialogInterface.OnClickListener() {

                           @Override
                           public void onClick(DialogInterface dialog, int which) {
                               if (mListener != null)
                                   mListener.conflictDecisionMade(Decision.OVERWRITE);
                           }
                       })
                   .setNeutralButton(R.string.conflict_keep_both,
                       new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mListener != null)
                                    mListener.conflictDecisionMade(Decision.KEEP_BOTH);
                            }
                        })
                   .setNegativeButton(R.string.conflict_dont_upload,
                       new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialog, int which) {
                               if (mListener != null)
                                   mListener.conflictDecisionMade(Decision.CANCEL);
                           }
                   })
                   .create();
    }
    
    public void showDialog(SherlockFragmentActivity activity) {
        Fragment prev = activity.getSupportFragmentManager().findFragmentByTag("dialog");
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        this.show(ft, "dialog");
    }

    public void dismissDialog(SherlockFragmentActivity activity) {
        Fragment prev = activity.getSupportFragmentManager().findFragmentByTag(getTag());
        if (prev != null) {
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            ft.remove(prev);
            ft.commit();
        }
    }
    
    @Override
    public void onCancel(DialogInterface dialog) {
        mListener.conflictDecisionMade(Decision.CANCEL);
    }
    
    public interface OnConflictDecisionMadeListener {
        public void conflictDecisionMade(Decision decision);
    }
}
