/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
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
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
 * Dialog to input the name for a new folder to create.
 * <p>
 * Triggers the folder creation when name is confirmed.
 */
public class CreateFolderDialogFragment
    extends DialogFragment implements DialogInterface.OnClickListener, Injectable {

    private static final String ARG_PARENT_FOLDER = "PARENT_FOLDER";

    public static final String CREATE_FOLDER_FRAGMENT = "CREATE_FOLDER_FRAGMENT";

    @Inject FileDataStorageManager fileDataStorageManager;
    @Inject ViewThemeUtils viewThemeUtils;
    @Inject KeyboardUtils keyboardUtils;


    private OCFile mParentFolder;
    private Button positiveButton;


    private EditBoxDialogBinding binding;

    /**
     * Public factory method to create new CreateFolderDialogFragment instances.
     *
     * @param parentFolder Folder to create
     * @return Dialog ready to show.
     */
    public static CreateFolderDialogFragment newInstance(OCFile parentFolder) {
        CreateFolderDialogFragment frag = new CreateFolderDialogFragment();
        Bundle args = new Bundle();
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
        mParentFolder = getArguments().getParcelable(ARG_PARENT_FOLDER);

        // Inflate the layout for the dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        binding = EditBoxDialogBinding.inflate(inflater, null, false);
        View view = binding.getRoot();

        // Setup layout
        binding.userInput.setText("");
        viewThemeUtils.material.colorTextInputLayout(binding.userInputContainer);

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
             * When user enters a hidden file name, the 'hidden file' message is shown. Otherwise,
             * the message is ensured to be hidden.
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
                } else if (!FileUtils.isValidName(newFileName)) {
                    binding.userInputContainer.setError(getString(R.string.filename_forbidden_charaters_from_server));
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
            .setPositiveButton(R.string.folder_confirm_create, this)
            .setNeutralButton(R.string.common_cancel, this)
            .setTitle(R.string.uploader_info_dirname);

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.userInputContainer.getContext(), builder);

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            String newFolderName =
                ((TextView) (getDialog().findViewById(R.id.user_input)))
                    .getText().toString().trim();

            if (TextUtils.isEmpty(newFolderName)) {
                DisplayUtils.showSnackMessage(getActivity(), R.string.filename_empty);
                return;
            }

            if (!FileUtils.isValidName(newFolderName)) {
                DisplayUtils.showSnackMessage(getActivity(), R.string.filename_forbidden_charaters_from_server);

                return;
            }

            String path = mParentFolder.getDecryptedRemotePath() + newFolderName + OCFile.PATH_SEPARATOR;

            ((ComponentsGetter) getActivity()).getFileOperationsHelper().createFolder(path);
        }
    }
}
