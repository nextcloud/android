/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2014 ownCloud Inc.
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

/*
 *  Dialog to input a new name for an {@link OCFile} being renamed.
 *
 *  Triggers the rename operation.
 */

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.collect.Sets;
import com.nextcloud.client.di.Injectable;
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
 * Dialog to input a new name for a file or folder to rename.
 * <p>
 * Triggers the rename operation when name is confirmed.
 */
public class RenameFileDialogFragment
    extends DialogFragment implements DialogInterface.OnClickListener, Injectable {

    private static final String ARG_TARGET_FILE = "TARGET_FILE";
    private static final String ARG_PARENT_FOLDER = "PARENT_FOLDER";

    @Inject ViewThemeUtils viewThemeUtils;
    @Inject FileDataStorageManager fileDataStorageManager;
    @Inject KeyboardUtils keyboardUtils;

    private EditBoxDialogBinding binding;
    private OCFile mTargetFile;
    private Button positiveButton;


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

        AlertDialog alertDialog = (AlertDialog) getDialog();

        if (alertDialog != null) {
            positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            viewThemeUtils.platform.colorTextButtons(positiveButton,
                                                     alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        keyboardUtils.showKeyboardForEditText(binding.userInput);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mTargetFile = requireArguments().getParcelable(ARG_TARGET_FILE);

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

        OCFile parentFolder = requireArguments().getParcelable(ARG_PARENT_FOLDER);
        List<OCFile> folderContent = fileDataStorageManager.getFolderContent(parentFolder, false);
        Set<String> fileNames = Sets.newHashSetWithExpectedSize(folderContent.size());

        for (OCFile file : folderContent) {
            fileNames.add(file.getFileName());
        }

        // Add TextChangedListener to handle showing/hiding the input warning message
        binding.userInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
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
        });

        // Build the dialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
        builder.setView(view)
            .setPositiveButton(R.string.file_rename, this)
            .setNeutralButton(R.string.common_cancel, this)
            .setTitle(R.string.rename_dialog_title);

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.userInputContainer.getContext(), builder);

        return builder.create();
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
}
