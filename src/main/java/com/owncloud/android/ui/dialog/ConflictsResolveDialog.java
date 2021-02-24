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
import com.owncloud.android.databinding.ConflictResolveDialogBinding;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.adapter.LocalFileListAdapter;
import com.owncloud.android.ui.adapter.OCFileListAdapter;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ThemeButtonUtils;
import com.owncloud.android.utils.theme.ThemeCheckableUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


/**
 * Dialog which will be displayed to user upon keep-in-sync file conflict.
 */
public class ConflictsResolveDialog extends DialogFragment {

    private ConflictResolveDialogBinding binding;

    private OCFile existingFile;
    private File newFile;
    public OnConflictDecisionMadeListener listener;
    private User user;
    private List<ThumbnailsCacheManager.ThumbnailGenerationTask> asyncTasks = new ArrayList<>();
    private Button positiveButton;

    private static final String KEY_NEW_FILE = "file";
    private static final String KEY_EXISTING_FILE = "ocfile";
    private static final String KEY_USER = "user";

    public enum Decision {
        CANCEL,
        KEEP_BOTH,
        KEEP_LOCAL,
        KEEP_SERVER,
    }

    public static ConflictsResolveDialog newInstance(OCFile existingFile, OCFile newFile, User user) {
        ConflictsResolveDialog dialog = new ConflictsResolveDialog();

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
            listener = (OnConflictDecisionMadeListener) context;
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

        positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        ThemeButtonUtils.themeBorderlessButton(positiveButton, alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL));
        positiveButton.setEnabled(false);
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
        binding = ConflictResolveDialogBinding.inflate(requireActivity().getLayoutInflater());

        ThemeCheckableUtils.tintCheckbox(ThemeColorUtils.primaryColor(getContext()),
                                         binding.newCheckbox,
                                         binding.existingCheckbox);

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(binding.getRoot())
            .setPositiveButton(R.string.common_ok, (dialog, which) -> {
                if (listener != null) {
                    if (binding.newCheckbox.isChecked() && binding.existingCheckbox.isChecked()) {
                        listener.conflictDecisionMade(Decision.KEEP_BOTH);
                    } else if (binding.newCheckbox.isChecked()) {
                        listener.conflictDecisionMade(Decision.KEEP_LOCAL);
                    } else if (binding.existingCheckbox.isChecked()) {
                        listener.conflictDecisionMade(Decision.KEEP_SERVER);
                    }  // else do nothing

                }
            })
            .setNeutralButton(R.string.common_cancel, (dialog, which) -> {
                if (listener != null) {
                    listener.conflictDecisionMade(Decision.CANCEL);
                }
            })
            .setTitle(String.format(getString(R.string.conflict_file_headline), existingFile.getFileName()));

        File parentFile = new File(existingFile.getRemotePath()).getParentFile();
        if (parentFile != null) {
            binding.in.setText(String.format(getString(R.string.in_folder), parentFile.getAbsolutePath()));
        } else {
            binding.in.setVisibility(View.GONE);
        }

        // set info for new file
        binding.newSize.setText(DisplayUtils.bytesToHumanReadable(newFile.length()));
        binding.newTimestamp.setText(DisplayUtils.getRelativeTimestamp(getContext(), newFile.lastModified()));
        binding.newThumbnail.setTag(newFile.hashCode());
        LocalFileListAdapter.setThumbnail(newFile, binding.newThumbnail, getContext());

        // set info for existing file
        binding.existingSize.setText(DisplayUtils.bytesToHumanReadable(existingFile.getFileLength()));
        binding.existingTimestamp.setText(DisplayUtils.getRelativeTimestamp(getContext(),
                                                                    existingFile.getModificationTimestamp()));

        binding.existingThumbnail.setTag(existingFile.getFileId());
        OCFileListAdapter.setThumbnail(existingFile,
                                       binding.existingThumbnail,
                                       user,
                                       new FileDataStorageManager(user.toPlatformAccount(),
                                                                  requireContext().getContentResolver()),
                                       asyncTasks,
                                       false,
                                       getContext());

        View.OnClickListener checkBoxClickListener = v -> {
            positiveButton.setEnabled(binding.newCheckbox.isChecked() || binding.existingCheckbox.isChecked());
        };

        binding.newCheckbox.setOnClickListener(checkBoxClickListener);
        binding.existingCheckbox.setOnClickListener(checkBoxClickListener);

        binding.newFileContainer.setOnClickListener(v -> {
            binding.newCheckbox.setChecked(!binding.newCheckbox.isChecked());
            positiveButton.setEnabled(binding.newCheckbox.isChecked() || binding.existingCheckbox.isChecked());
        });
        binding.existingFileContainer.setOnClickListener(v -> {
            binding.existingCheckbox.setChecked(!binding.existingCheckbox.isChecked());
            positiveButton.setEnabled(binding.newCheckbox.isChecked() || binding.existingCheckbox.isChecked());
        });

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
            listener.conflictDecisionMade(Decision.CANCEL);
        }
    }

    public interface OnConflictDecisionMadeListener {
        void conflictDecisionMade(Decision decision);
    }

    @Override
    public void onStop() {
        super.onStop();

        for (ThumbnailsCacheManager.ThumbnailGenerationTask task : asyncTasks) {
            if (task != null) {
                task.cancel(true);
                if (task.getGetMethod() != null) {
                    Log_OC.d(this, "cancel: abort get method directly");
                    task.getGetMethod().abort();
                }
            }
        }

        asyncTasks.clear();
    }
}
