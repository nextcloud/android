/**
 *   ownCloud Android client application
 *   @author masensio
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
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.utils.ThemeUtils;

/**
 * Dialog to input the password for sharing a file/folder.
 *
 * Triggers the share when the password is introduced.
 */

public class SharePasswordDialogFragment extends DialogFragment
        implements DialogInterface.OnClickListener {

    private static final String ARG_FILE = "FILE";
    private static final String ARG_CREATE_SHARE = "CREATE_SHARE";
    public static final String PASSWORD_FRAGMENT = "PASSWORD_FRAGMENT";

    private OCFile mFile;
    private boolean mCreateShare;

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog alertDialog = (AlertDialog) getDialog();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ThemeUtils.primaryAccentColor());
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ThemeUtils.primaryAccentColor());
    }

    /**
     * Public factory method to create new SharePasswordDialogFragment instances.
     *
     * @param   file            OCFile bound to the public share that which password will be set or updated
     * @param   createShare     When 'true', the request for password will be followed by the creation of a new
     *                          public link; when 'false', a public share is assumed to exist, and the password
     *                          is bound to it.
     * @return                  Dialog ready to show.
     */
    public static SharePasswordDialogFragment newInstance(OCFile file, boolean createShare) {
        SharePasswordDialogFragment frag = new SharePasswordDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, file);
        args.putBoolean(ARG_CREATE_SHARE, createShare);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mFile = getArguments().getParcelable(ARG_FILE);
        mCreateShare = getArguments().getBoolean(ARG_CREATE_SHARE, false);

        // Inflate the layout for the dialog
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.password_dialog, null);

        // Setup layout
        EditText inputText = ((EditText)v.findViewById(R.id.share_password));
        inputText.getBackground().setColorFilter(ThemeUtils.primaryAccentColor(), PorterDuff.Mode.SRC_ATOP);
        inputText.setText("");
        inputText.requestFocus();

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                R.style.Theme_ownCloud_Dialog_NoButtonBarStyle);
        builder.setView(v)
               .setPositiveButton(R.string.common_ok, this)
               .setNegativeButton(R.string.common_cancel, this)
               .setTitle(R.string.share_link_password_title);
        Dialog d = builder.create();
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return d;
    }


    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            String password =
                    ((TextView)(getDialog().findViewById(R.id.share_password)))
                        .getText().toString();

            if (password.length() <= 0) {
                Toast.makeText(
                        getActivity(),
                        R.string.share_link_empty_password,
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (mCreateShare) {
                // Share the file
                ((FileActivity) getActivity()).getFileOperationsHelper().
                        shareFileViaLink(mFile, password);

            } else {
                // updat existing link
                ((FileActivity) getActivity()).getFileOperationsHelper().
                        setPasswordToShareViaLink(mFile, password);
            }
        }
    }
}
