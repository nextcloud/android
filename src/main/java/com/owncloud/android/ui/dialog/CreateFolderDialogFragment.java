/**
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
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

/**
 *  Dialog to input the name for a new folder to create.
 *
 *  Triggers the folder creation when name is confirmed.
 */
public class CreateFolderDialogFragment
    extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String ARG_PARENT_FOLDER = "PARENT_FOLDER";

    public static final String CREATE_FOLDER_FRAGMENT = "CREATE_FOLDER_FRAGMENT";

    OCFile mParentFolder;

    Dialog localDialog;
    FragmentActivity activity;

    /**
     * Public factory method to create new CreateFolderDialogFragment instances.
     *
     * @param parentFolder            Folder to create
     * @return                        Dialog ready to show.
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

        int color = ThemeUtils.primaryAccentColor(getContext());

        AlertDialog alertDialog = (AlertDialog) getDialog();
        localDialog = getDialog();
        activity = getActivity();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int accentColor = ThemeUtils.primaryAccentColor(getContext());
        mParentFolder = getArguments().getParcelable(ARG_PARENT_FOLDER);

        // Inflate the layout for the dialog
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.edit_box_dialog, null);

        // Setup layout
        EditText inputText = v.findViewById(R.id.user_input);
        inputText.setText("");
        inputText.requestFocus();
        inputText.getBackground().setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP);
        inputText.setHighlightColor(ThemeUtils.primaryColor(getActivity()));

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(v)
            .setPositiveButton(R.string.folder_confirm_create, this)
            .setNegativeButton(R.string.common_cancel, this)
            .setTitle(ThemeUtils.getColoredTitle(getResources().getString(R.string.uploader_info_dirname),
                                                 accentColor));
        Dialog d = builder.create();

        Window window = d.getWindow();
        if (window != null) {
            window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        return d;
    }


    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which != android.app.AlertDialog.BUTTON_POSITIVE) {
            return;
        }

        String newFolderName =
            ((TextView) (localDialog.findViewById(R.id.user_input)))
                .getText().toString().trim();
            if (TextUtils.isEmpty(newFolderName)) {
                DisplayUtils.showSnackMessage(getActivity(), R.string.filename_empty);
                return;
            }

        try {
            new Controller(((ComponentsGetter) getActivity()).getFileOperationsHelper())
                .verifyAndCreateFolder(which, newFolderName);
        } catch (RuntimeException e) {
            DisplayUtils.showSnackMessage(getActivity(), e.getMessage());
        }
    }

    class Controller {
        FileOperationsHelper fileOperationsHelper;

        Controller(FileOperationsHelper fileOperationsHelper) {
            this.fileOperationsHelper = fileOperationsHelper;
        }

        public void verifyAndCreateFolder(int which, String newFolderName) {
            if (which == AlertDialog.BUTTON_POSITIVE) {

                if (newFolderName.length() <= 0) {
                    throw new RuntimeException(String.valueOf(R.string.filename_empty));
                }

                if (!FileUtils.isValidName(newFolderName)) {
                    DisplayUtils.showSnackMessage(getActivity(), R.string.filename_forbidden_characters);
                    return;
                }

                String path = mParentFolder.getRemotePath();
                path += newFolderName + OCFile.PATH_SEPARATOR;

                fileOperationsHelper.createFolder(path, false);
            }
        }
    }
}
