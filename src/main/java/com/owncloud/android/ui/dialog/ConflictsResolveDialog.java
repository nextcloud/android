/*
 *   ownCloud Android client application
 *
 *   @author Bartek Przybylski
 *   Copyright (C) 2012 Bartek Przybylski
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

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.owncloud.android.R;


/**
 * Dialog which will be displayed to user upon keep-in-sync file conflict.
 */
public class ConflictsResolveDialog extends DialogFragment {

    public enum Decision {
        CANCEL,
        KEEP_BOTH,
        OVERWRITE,
        SERVER
    }
    
    OnConflictDecisionMadeListener mListener;

    public static ConflictsResolveDialog newInstance(OnConflictDecisionMadeListener listener) {
        ConflictsResolveDialog f = new ConflictsResolveDialog();
        f.mListener = listener;
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity(), R.style.Theme_ownCloud_Dialog)
                .setIcon(R.drawable.ic_warning)
                .setTitle(R.string.conflict_title)
                .setMessage(getString(R.string.conflict_message))
                .setPositiveButton(R.string.conflict_use_local_version,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mListener != null) {
                                    mListener.conflictDecisionMade(Decision.OVERWRITE);
                                }
                            }
                        })
                .setNeutralButton(R.string.conflict_keep_both,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mListener != null) {
                                    mListener.conflictDecisionMade(Decision.KEEP_BOTH);
                                }
                            }
                        })
                .setNegativeButton(R.string.conflict_use_server_version,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mListener != null) {
                                    mListener.conflictDecisionMade(Decision.SERVER);
                                }
                            }
                        })
                .create();
    }
    
    public void showDialog(AppCompatActivity activity) {
        Fragment prev = activity.getSupportFragmentManager().findFragmentByTag("dialog");
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        this.show(ft, "dialog");
    }

    public void dismissDialog(AppCompatActivity activity) {
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
        void conflictDecisionMade(Decision decision);
    }
}
