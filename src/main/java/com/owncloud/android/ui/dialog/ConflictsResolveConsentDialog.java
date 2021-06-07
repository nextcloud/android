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
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.nextcloud.client.account.User;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ConflictResolveConsentDialogBinding;
import com.owncloud.android.databinding.ConflictResolveDialogBinding;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.adapter.LocalFileListAdapter;
import com.owncloud.android.ui.adapter.OCFileListAdapter;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


/**
 * Dialog which will be displayed to user upon keep-in-sync file conflict.
 */
public class ConflictsResolveConsentDialog extends DialogFragment {

    private ConflictResolveConsentDialogBinding binding;

    private OCFile existingFile;
    private File newFile;
    public ConflictsResolveDialog.OnConflictDecisionMadeListener listener;
    private User user;

    private static final String KEY_NEW_FILE = "file";
    private static final String KEY_EXISTING_FILE = "ocfile";
    private static final String KEY_USER = "user";

    public static ConflictsResolveConsentDialog newInstance(OCFile existingFile, OCFile newFile, User user) {
        ConflictsResolveConsentDialog dialog = new ConflictsResolveConsentDialog();

        Bundle args = new Bundle();
        args.putParcelable(KEY_EXISTING_FILE, existingFile);
        args.putSerializable(KEY_NEW_FILE, new File(newFile.getStoragePath()));
        args.putParcelable(KEY_USER, user);
        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (ConflictsResolveDialog.OnConflictDecisionMadeListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity of this dialog must implement OnConflictDecisionMadeListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog alertDialog = (AlertDialog) getDialog();

        if (alertDialog == null) {
            Toast.makeText(getContext(), "Failed to create conflict dialog", Toast.LENGTH_LONG).show();
            return;
        }

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            existingFile = savedInstanceState.getParcelable(KEY_EXISTING_FILE);
            newFile = (File) savedInstanceState.getSerializable(KEY_NEW_FILE);
            user = savedInstanceState.getParcelable(KEY_USER);
        } else if (getArguments() != null) {
            existingFile = getArguments().getParcelable(KEY_EXISTING_FILE);
            newFile = (File) getArguments().getSerializable(KEY_NEW_FILE);
            user = getArguments().getParcelable(KEY_USER);
        } else {
            Toast.makeText(getContext(), "Failed to create conflict dialog", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(KEY_EXISTING_FILE, existingFile);
        outState.putSerializable(KEY_NEW_FILE, newFile);
        outState.putParcelable(KEY_USER, user);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Inflate the layout for the dialog
        binding = ConflictResolveConsentDialogBinding.inflate(requireActivity().getLayoutInflater());

        // TODO: 26-05-2021 change replace and keep both button text for multiple files
        binding.replaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.conflictDecisionMade(ConflictsResolveDialog.Decision.KEEP_LOCAL);
                }
            }
        });

        binding.keepBothBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.conflictDecisionMade(ConflictsResolveDialog.Decision.KEEP_BOTH);
                }
            }
        });

        binding.moreDetailsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        binding.cancelKeepExistingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.conflictDecisionMade(ConflictsResolveDialog.Decision.KEEP_SERVER);
                }
            }
        });

        // Build the dialog
        // TODO: 26-05-2021 Handle multiple dialog message
        String dialogMessage = String.format(getString(R.string.conflict_dialog_message),
                                             existingFile.getFileName());
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(binding.getRoot())
            // TODO: 26-05-2021 handle multiple dialog title
            .setTitle(getString(R.string.conflict_dialog_title))
            .setMessage(dialogMessage);


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
    public void onCancel(@NonNull DialogInterface dialog) {
        if (listener != null) {
            listener.conflictDecisionMade(ConflictsResolveDialog.Decision.CANCEL);
        }
    }

}
