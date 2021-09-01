/*
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
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.utils.theme.ThemeColorUtils;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class LoadingDialog extends DialogFragment {

    private String mMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setCancelable(false);
    }

    public static LoadingDialog newInstance(String message) {
        LoadingDialog loadingDialog = new LoadingDialog();
        loadingDialog.mMessage = message;
        return loadingDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Create a view by inflating desired layout
        View v = inflater.inflate(R.layout.loading_dialog, container,  false);

        // set value
        TextView tv = v.findViewById(R.id.loadingText);
        tv.setText(mMessage);

        // set progress wheel color
        ProgressBar progressBar = v.findViewById(R.id.loadingBar);
        progressBar.getIndeterminateDrawable().setColorFilter(ThemeColorUtils.primaryAccentColor(getContext()),
                                                              PorterDuff.Mode.SRC_IN);

        return v;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }
}
