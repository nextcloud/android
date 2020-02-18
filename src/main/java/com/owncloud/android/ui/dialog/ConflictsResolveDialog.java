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

import com.owncloud.android.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


/**
 * Dialog which will be displayed to user upon keep-in-sync file conflict.
 */
public class ConflictsResolveDialog extends DialogFragment {

    public enum Decision {
        CANCEL,
        KEEP_BOTH,
        KEEP_LOCAL,
        KEEP_SERVER,
    }

    private final OnConflictDecisionMadeListener listener;
    private final boolean canKeepServer;

    public ConflictsResolveDialog(OnConflictDecisionMadeListener listener, boolean canKeepServer) {
        this.listener = listener;
        this.canKeepServer = canKeepServer;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity(), R.style.Theme_ownCloud_Dialog)
            .setIcon(R.drawable.ic_warning)
            .setTitle(R.string.conflict_title)
            .setMessage(getString(R.string.conflict_message))
            .setPositiveButton(R.string.conflict_use_local_version,
                               (dialog, which) -> {
                                   if (listener != null) {
                                       listener.conflictDecisionMade(Decision.KEEP_LOCAL);
                                   }
                               })
            .setNeutralButton(R.string.conflict_keep_both,
                              (dialog, which) -> {
                                  if (listener != null) {
                                      listener.conflictDecisionMade(Decision.KEEP_BOTH);
                                  }
                              });

        if (this.canKeepServer) {
            builder.setNegativeButton(R.string.conflict_use_server_version,
                                      (dialog, which) -> {
                                          if (listener != null) {
                                              listener.conflictDecisionMade(Decision.KEEP_SERVER);
                                          }
                                      });
        }

        return builder.create();
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

    @Override
    public void onCancel(DialogInterface dialog) {
        if (listener != null) {
            listener.conflictDecisionMade(Decision.CANCEL);
        }
    }

    public interface OnConflictDecisionMadeListener {
        void conflictDecisionMade(Decision decision);
    }
}
