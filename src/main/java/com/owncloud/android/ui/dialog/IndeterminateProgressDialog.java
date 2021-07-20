/**
 *   ownCloud Android client application
 *
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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ProgressBar;

import com.owncloud.android.R;
import com.owncloud.android.utils.theme.ThemeColorUtils;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;


public class IndeterminateProgressDialog extends DialogFragment {

    private static final String ARG_MESSAGE_ID = IndeterminateProgressDialog.class.getCanonicalName() + ".ARG_MESSAGE_ID";
    private static final String ARG_CANCELABLE = IndeterminateProgressDialog.class.getCanonicalName() + ".ARG_CANCELABLE";


    /**
     * Public factory method to get dialog instances.
     *
     * @param messageId     Resource id for a message to show in the dialog.
     * @param cancelable    If 'true', the dialog can be cancelled by the user input (BACK button, touch outside...)
     * @return              New dialog instance, ready to show.
     */
    public static IndeterminateProgressDialog newInstance(int messageId, boolean cancelable) {
        IndeterminateProgressDialog fragment = new IndeterminateProgressDialog();
        fragment.setStyle(DialogFragment.STYLE_NO_FRAME, R.style.ownCloud_AlertDialog);
        Bundle args = new Bundle();
        args.putInt(ARG_MESSAGE_ID, messageId);
        args.putBoolean(ARG_CANCELABLE, cancelable);
        fragment.setArguments(args);
        return fragment;
    }


    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        /// create indeterminate progress dialog
        final ProgressDialog progressDialog = new ProgressDialog(getActivity(), R.style.ProgressDialogTheme);
        progressDialog.setIndeterminate(true);
        progressDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                ProgressBar v = progressDialog.findViewById(android.R.id.progress);
                v.getIndeterminateDrawable().setColorFilter(ThemeColorUtils.primaryAccentColor(getContext()),
                                                            android.graphics.PorterDuff.Mode.MULTIPLY);

            }
        });

        /// set message
        int messageId = getArguments().getInt(ARG_MESSAGE_ID, R.string.placeholder_sentence);
        progressDialog.setMessage(getString(messageId));

        /// set cancellation behavior
        boolean cancelable = getArguments().getBoolean(ARG_CANCELABLE, false);
        if (!cancelable) {
            progressDialog.setCancelable(false);
            // disable the back button
            OnKeyListener keyListener = new OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode,
                        KeyEvent event) {

                    return keyCode == KeyEvent.KEYCODE_BACK;
                }

            };
            progressDialog.setOnKeyListener(keyListener);
        }

        return progressDialog;
    }

}


