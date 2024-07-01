/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2012 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nextcloud.client.account.User;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.utils.extensions.BundleExtensionsKt;
import com.nextcloud.utils.extensions.FileExtensionsKt;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ConflictResolveDialogBinding;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.adapter.LocalFileListAdapter;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

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
public class ConflictsResolveDialog extends DialogFragment implements Injectable {

    private ConflictResolveDialogBinding binding;

    private OCFile existingFile;
    private File newFile;
    public OnConflictDecisionMadeListener listener;
    private User user;
    private final List<ThumbnailsCacheManager.ThumbnailGenerationTask> asyncTasks = new ArrayList<>();
    private MaterialButton positiveButton;
    @Inject ViewThemeUtils viewThemeUtils;
    @Inject SyncedFolderProvider syncedFolderProvider;

    private static final String TAG = "ConflictsResolveDialog";
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

        File file = new File(newFile.getStoragePath());
        FileExtensionsKt.logFileSize(file, TAG);
        args.putSerializable(KEY_NEW_FILE, file);
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

        positiveButton = (MaterialButton) alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        MaterialButton negativeButton = (MaterialButton) alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        viewThemeUtils.material.colorMaterialButtonPrimaryTonal(positiveButton);
        viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(negativeButton);
        positiveButton.setEnabled(false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            existingFile = BundleExtensionsKt.getParcelableArgument(savedInstanceState, KEY_EXISTING_FILE, OCFile.class);
            newFile = BundleExtensionsKt.getSerializableArgument(savedInstanceState, KEY_NEW_FILE, File.class);
            user = BundleExtensionsKt.getParcelableArgument(savedInstanceState, KEY_USER, User.class);
        } else if (getArguments() != null) {
            existingFile = BundleExtensionsKt.getParcelableArgument(getArguments(), KEY_EXISTING_FILE, OCFile.class);
            newFile = BundleExtensionsKt.getSerializableArgument(getArguments(), KEY_NEW_FILE, File.class);
            user = BundleExtensionsKt.getParcelableArgument(getArguments(), KEY_USER, User.class);
        } else {
            Toast.makeText(getContext(), "Failed to create conflict dialog", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        FileExtensionsKt.logFileSize(existingFile, TAG);
        FileExtensionsKt.logFileSize(newFile, TAG);
        outState.putParcelable(KEY_EXISTING_FILE, existingFile);
        outState.putSerializable(KEY_NEW_FILE, newFile);
        outState.putParcelable(KEY_USER, user);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Inflate the layout for the dialog
        binding = ConflictResolveDialogBinding.inflate(requireActivity().getLayoutInflater());

        viewThemeUtils.platform.themeCheckbox(binding.newCheckbox);
        viewThemeUtils.platform.themeCheckbox(binding.existingCheckbox);

        // Build the dialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
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
            .setNegativeButton(R.string.common_cancel, (dialog, which) -> {
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
        LocalFileListAdapter.setThumbnail(newFile,
                                          binding.newThumbnail,
                                          getContext(),
                                          viewThemeUtils);

        // set info for existing file
        binding.existingSize.setText(DisplayUtils.bytesToHumanReadable(existingFile.getFileLength()));
        binding.existingTimestamp.setText(DisplayUtils.getRelativeTimestamp(getContext(),
                                                                            existingFile.getModificationTimestamp()));

        binding.existingThumbnail.setTag(existingFile.getFileId());
        DisplayUtils.setThumbnail(existingFile,
                                  binding.existingThumbnail,
                                  user,
                                  new FileDataStorageManager(user,
                                                             requireContext().getContentResolver()),
                                  asyncTasks,
                                  false,
                                  getContext(),
                                  null,
                                  syncedFolderProvider.getPreferences(),
                                  viewThemeUtils,
                                  syncedFolderProvider);

        View.OnClickListener checkBoxClickListener = v ->
            positiveButton.setEnabled(binding.newCheckbox.isChecked() || binding.existingCheckbox.isChecked());

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

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.existingFileContainer.getContext(), builder);

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
