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

import android.accounts.Account;
import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.UploadFilesActivity;


/**
 * Dialog showing two options to allow the user upload files from the filesystem or from other apps.
 *
 * Assumes that its parent activity extends {@link FileActivity}
 */
public class UploadSourceDialogFragment extends DialogFragment {

    private final static String TAG =  UploadSourceDialogFragment.class.getSimpleName();
    private final static String ARG_ACCOUNT =  UploadSourceDialogFragment.class.getSimpleName() +
            ".ARG_ACCOUNT";

    public static final int ACTION_SELECT_CONTENT_FROM_APPS = 1;
    public static final int ACTION_SELECT_MULTIPLE_FILES = 2;

    public static UploadSourceDialogFragment newInstance(Account account) {
        UploadSourceDialogFragment f = new UploadSourceDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_ACCOUNT, account);
        f.setArguments(args);
        return f;
    }

    public UploadSourceDialogFragment() {
        super();
        Log_OC.v(TAG, "constructor");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String[] allTheItems = {
                getString(R.string.actionbar_upload_files),
                getString(R.string.actionbar_upload_from_apps)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.actionbar_upload);
        builder.setItems(allTheItems, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (item == 0) {
                    Intent action = new Intent(getActivity(), UploadFilesActivity.class);
                    action.putExtra(
                            UploadFilesActivity.EXTRA_ACCOUNT,
                            ((FileActivity)getActivity()).getAccount()
                    );
                    //startActivityForResult(action, ACTION_SELECT_MULTIPLE_FILES);
                    // this flow seems broken;
                    // Actionbarsherlock, maybe?
                    getActivity().startActivityForResult(action, ACTION_SELECT_MULTIPLE_FILES);

                } else if (item == 1) {
                    Intent action = new Intent(Intent.ACTION_GET_CONTENT);
                    action = action.setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
                    //Intent.EXTRA_ALLOW_MULTIPLE is only supported on api level 18+, Jelly Bean
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        action.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    }
                    //startActivityForResult(   // this flow seems broken;
                                                // Actionbarsherlock, maybe?
                    getActivity().startActivityForResult(
                            Intent.createChooser(action, getString(R.string.upload_chooser_title)),
                            ACTION_SELECT_CONTENT_FROM_APPS
                    );
                }
            }
        });
        return builder.create();
    }

}
