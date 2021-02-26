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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

import com.owncloud.android.R;
import com.owncloud.android.databinding.EditBoxDialogBinding;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeTextInputUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;


/**
 *  Dialog to input a new name for a file or folder to rename.
 *
 *  Triggers the rename operation when name is confirmed.
 */
public class RenameFileDialogFragment
        extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String ARG_TARGET_FILE = "TARGET_FILE";

    private EditBoxDialogBinding binding;
    private OCFile mTargetFile;

    /**
     * Public factory method to create new RenameFileDialogFragment instances.
     *
     * @param file            File to rename.
     * @return                Dialog ready to show.
     */
    public static RenameFileDialogFragment newInstance(OCFile file) {
        RenameFileDialogFragment frag = new RenameFileDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_TARGET_FILE, file);
        frag.setArguments(args);
        return frag;

    }

    @Override
    public void onStart() {
        super.onStart();

        int color = ThemeColorUtils.primaryAccentColor(getContext());

        AlertDialog alertDialog = (AlertDialog) getDialog();

        if (alertDialog != null) {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
            alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(color);
        }
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
        ThemeTextInputUtils.colorTextInput(binding.userInputContainer,
                                           binding.userInput,
                                           ThemeColorUtils.primaryColor(getActivity()));
        int selectionStart = 0;
        int extensionStart = mTargetFile.isFolder() ? -1 : currentName.lastIndexOf('.');
        int selectionEnd = extensionStart >= 0 ? extensionStart : currentName.length();
        binding.userInput.setSelection(Math.min(selectionStart, selectionEnd), Math.max(selectionStart, selectionEnd));
        binding.userInput.requestFocus();

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(view)
            .setPositiveButton(R.string.file_rename, this)
            .setNeutralButton(R.string.common_cancel, this)
            .setTitle(R.string.rename_dialog_title);
        Dialog d = builder.create();

        Window window = d.getWindow();
        if (window != null) {
            window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        return d;
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
