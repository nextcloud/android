/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2017-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2014 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.collect.Sets;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.utils.extensions.BundleExtensionsKt;
import com.owncloud.android.R;
import com.owncloud.android.databinding.EditBoxDialogBinding;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.KeyboardUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

/**
 *  Dialog to input a new name for an {@link OCFile} being renamed.
 *  Triggers the rename operation.
 */
public class RenameFileDialogFragment
    extends DialogFragment implements DialogInterface.OnClickListener, TextWatcher, Injectable {

    private static final String ARG_TARGET_FILE = "TARGET_FILE";
    private static final String ARG_PARENT_FOLDER = "PARENT_FOLDER";

    @Inject ViewThemeUtils viewThemeUtils;
    @Inject FileDataStorageManager fileDataStorageManager;
    @Inject KeyboardUtils keyboardUtils;

    private EditBoxDialogBinding binding;
    private OCFile mTargetFile;
    private MaterialButton positiveButton;
    private Set<String> fileNames;

    /**
     * Public factory method to create new RenameFileDialogFragment instances.
     *
     * @param file File to rename.
     * @return Dialog ready to show.
     */
    public static RenameFileDialogFragment newInstance(OCFile file, OCFile parentFolder) {
        RenameFileDialogFragment frag = new RenameFileDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_TARGET_FILE, file);
        args.putParcelable(ARG_PARENT_FOLDER, parentFolder);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onStart() {
        super.onStart();
        initAlertDialog();
    }

    @Override
    public void onResume() {
        super.onResume();
        keyboardUtils.showKeyboardForEditText(requireDialog().getWindow(), binding.userInput);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mTargetFile = BundleExtensionsKt.getParcelableArgument(requireArguments(), ARG_TARGET_FILE, OCFile.class);

        // Inflate the layout for the dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        binding = EditBoxDialogBinding.inflate(inflater, null, false);
        View view = binding.getRoot();

        // Setup layout
        String currentName = mTargetFile.getFileName();
        binding.userInput.setText(currentName);
        viewThemeUtils.material.colorTextInputLayout(binding.userInputContainer);
        int extensionStart = mTargetFile.isFolder() ? -1 : currentName.lastIndexOf('.');
        int selectionEnd = extensionStart >= 0 ? extensionStart : currentName.length();
        binding.userInput.setSelection(0, selectionEnd);

        OCFile parentFolder = BundleExtensionsKt.getParcelableArgument(getArguments(), ARG_PARENT_FOLDER, OCFile.class);
        List<OCFile> folderContent = fileDataStorageManager.getFolderContent(parentFolder, false);
        fileNames = Sets.newHashSetWithExpectedSize(folderContent.size());

        for (OCFile file : folderContent) {
            fileNames.add(file.getFileName());
        }

        // Add TextChangedListener to handle showing/hiding the input warning message
        binding.userInput.addTextChangedListener(this);

        // Build the dialog
        MaterialAlertDialogBuilder builder = buildMaterialAlertDialog(view);

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.userInputContainer.getContext(), builder);

        return builder.create();
    }

    private MaterialAlertDialogBuilder buildMaterialAlertDialog(View view) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());

        builder
            .setView(view)
            .setPositiveButton(R.string.file_rename, this)
            .setNegativeButton(R.string.common_cancel, this)
            .setTitle(R.string.rename_dialog_title);

        return builder;
    }

    private void initAlertDialog() {
        AlertDialog alertDialog = (AlertDialog) getDialog();

        if (alertDialog != null) {
            positiveButton = (MaterialButton) alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            MaterialButton negativeButton = (MaterialButton) alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            viewThemeUtils.material.colorMaterialButtonPrimaryTonal(positiveButton);
            viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(negativeButton);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            String newFileName = "";

            if (binding.userInput.getText() != null) {
                newFileName = binding.userInput.getText().toString().trim();
            }

            if (TextUtils.isEmpty(newFileName)) {
                DisplayUtils.showSnackMessage(requireActivity(), R.string.filename_empty);
                return;
            }

            if (!FileUtils.isValidName(newFileName)) {
                DisplayUtils.showSnackMessage(requireActivity(), R.string.filename_forbidden_charaters_from_server);

                return;
            }

            ((ComponentsGetter) requireActivity()).getFileOperationsHelper().renameFile(mTargetFile, newFileName);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    /**
     * When user enters a hidden file name, the 'hidden file' message is shown.
     * Otherwise, the message is ensured to be hidden.
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String newFileName = "";
        if (binding.userInput.getText() != null) {
            newFileName = binding.userInput.getText().toString().trim();
        }

        if (!TextUtils.isEmpty(newFileName) && newFileName.charAt(0) == '.') {
            binding.userInputContainer.setError(getText(R.string.hidden_file_name_warning));
        } else if (TextUtils.isEmpty(newFileName)) {
            binding.userInputContainer.setError(getString(R.string.filename_empty));
            positiveButton.setEnabled(false);
        } else if (fileNames.contains(newFileName)) {
            binding.userInputContainer.setError(getText(R.string.file_already_exists));
            positiveButton.setEnabled(false);
        } else if (binding.userInputContainer.getError() != null) {
            binding.userInputContainer.setError(null);
            // Called to remove extra padding
            binding.userInputContainer.setErrorEnabled(false);
            positiveButton.setEnabled(true);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}
