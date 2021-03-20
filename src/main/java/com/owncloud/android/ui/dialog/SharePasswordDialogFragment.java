/*
 *   ownCloud Android client application
 *
 *   @author masensio
 *   @author Andy Scherzinger
 *   Copyright (C) 2015 ownCloud GmbH.
 *   Copyright (C) 2018 Andy Scherzinger
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
 */
package com.owncloud.android.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.owncloud.android.R;
import com.owncloud.android.databinding.PasswordDialogBinding;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ThemeButtonUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeTextInputUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

/**
 * Dialog to input the password for sharing a file/folder.
 *
 * Triggers the share when the password is introduced.
 */
public class SharePasswordDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String ARG_FILE = "FILE";
    private static final String ARG_SHARE = "SHARE";
    private static final String ARG_CREATE_SHARE = "CREATE_SHARE";
    private static final String ARG_ASK_FOR_PASSWORD = "ASK_FOR_PASSWORD";
    public static final String PASSWORD_FRAGMENT = "PASSWORD_FRAGMENT";

    private PasswordDialogBinding binding;
    private OCFile file;
    private OCShare share;
    private boolean createShare;
    private boolean askForPassword;

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog alertDialog = (AlertDialog) getDialog();
        if (alertDialog != null) {
            ThemeButtonUtils.themeBorderlessButton(alertDialog.getButton(AlertDialog.BUTTON_POSITIVE),
                                                   alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE));
            ThemeButtonUtils.themeBorderlessButton(getResources().getColor(R.color.highlight_textColor_Warning),
                                                   alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL));

            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String password = binding.sharePassword.getText().toString();

                if (!askForPassword && TextUtils.isEmpty(password)) {
                    DisplayUtils.showSnackMessage(binding.getRoot(), R.string.share_link_empty_password);
                    return;
                }

                if (share == null) {
                    setPassword(createShare, file, password);
                } else {
                    setPassword(share, password);
                }

                alertDialog.dismiss();
            });
        }
    }

    /**
     * Public factory method to create new SharePasswordDialogFragment instances.
     *
     * @param file        OCFile bound to the public share that which password will be set or updated
     * @param createShare When 'true', the request for password will be followed by the creation of a new
     *                    public link; when 'false', a public share is assumed to exist, and the password
     *                    is bound to it.
     * @return Dialog ready to show.
     */
    public static SharePasswordDialogFragment newInstance(OCFile file, boolean createShare, boolean askForPassword) {
        SharePasswordDialogFragment frag = new SharePasswordDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, file);
        args.putBoolean(ARG_CREATE_SHARE, createShare);
        args.putBoolean(ARG_ASK_FOR_PASSWORD, askForPassword);
        frag.setArguments(args);
        return frag;
    }

    /**
     * Public factory method to create new SharePasswordDialogFragment instances.
     *
     * @param share OCFile bound to the public share that which password will be set or updated
     * @return Dialog ready to show.
     */
    public static SharePasswordDialogFragment newInstance(OCShare share, boolean askForPassword) {
        SharePasswordDialogFragment frag = new SharePasswordDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SHARE, share);
        args.putBoolean(ARG_ASK_FOR_PASSWORD, askForPassword);
        frag.setArguments(args);
        return frag;
    }

    /**
     * Public factory method to create new SharePasswordDialogFragment instances.
     *
     * @param share OCFile bound to the public share that which password will be set or updated
     * @return Dialog ready to show.
     */
    public static SharePasswordDialogFragment newInstance(OCShare share) {
        SharePasswordDialogFragment frag = new SharePasswordDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SHARE, share);
        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        file = getArguments().getParcelable(ARG_FILE);
        share = getArguments().getParcelable(ARG_SHARE);
        createShare = getArguments().getBoolean(ARG_CREATE_SHARE, false);
        askForPassword = getArguments().getBoolean(ARG_ASK_FOR_PASSWORD, false);

        // Inflate the layout for the dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        binding = PasswordDialogBinding.inflate(inflater, null, false);
        View view = binding.getRoot();

        // Setup layout
        binding.sharePassword.setText("");
        ThemeTextInputUtils.colorTextInput(binding.sharePasswordContainer,
                                           binding.sharePassword,
                                           ThemeColorUtils.primaryColor(getActivity()));
        binding.sharePassword.requestFocus();

        int negativeButtonCaption;
        int title;
        if (askForPassword) {
            title = R.string.share_link_optional_password_title;
            negativeButtonCaption = R.string.common_skip;
        } else {
            title = R.string.share_link_password_title;
            negativeButtonCaption = R.string.common_cancel;
        }

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setView(view)
            .setPositiveButton(R.string.common_ok, null)
            .setNegativeButton(negativeButtonCaption, this)
                .setNeutralButton(R.string.common_delete, this)
            .setTitle(title);
        Dialog d = builder.create();

        Window window = d.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        return d;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == AlertDialog.BUTTON_NEUTRAL) {
            if (share == null) {
                setPassword(createShare, file, null);
            } else {
                setPassword(share, null);
            }
        } else if (which == AlertDialog.BUTTON_NEGATIVE && askForPassword) {
            if (share == null) {
                setPassword(createShare, file, null);
            } else {
                setPassword(share, null);
            }
        }
    }

    private void setPassword(boolean createShare, OCFile file, String password) {
        if (createShare) {
            ((FileActivity) getActivity()).getFileOperationsHelper().shareFileViaPublicShare(file, password);
        } else {
            ((FileActivity) getActivity()).getFileOperationsHelper().setPasswordToShare(share, password);
        }
    }

    private void setPassword(OCShare share, String password) {
        ((FileActivity) getActivity()).getFileOperationsHelper().setPasswordToShare(share, password);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
