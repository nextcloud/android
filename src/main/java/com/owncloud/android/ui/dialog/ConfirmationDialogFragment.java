/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2012 Bartek Przybylski
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
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.owncloud.android.R;


public class ConfirmationDialogFragment extends DialogFragment {

    public final static String ARG_MESSAGE_RESOURCE_ID = "resource_id";
    public final static String ARG_MESSAGE_ARGUMENTS = "string_array";
    private static final String ARG_TITLE_ID = "title_id";

    public final static String ARG_POSITIVE_BTN_RES = "positive_btn_res";
    public final static String ARG_NEUTRAL_BTN_RES = "neutral_btn_res";
    public final static String ARG_NEGATIVE_BTN_RES = "negative_btn_res";
    
    public static final String FTAG_CONFIRMATION = "CONFIRMATION_FRAGMENT";

    private ConfirmationDialogFragmentListener mListener;
    
    /**
     * Public factory method to create new ConfirmationDialogFragment instances.
     * 
     * @param messageResId      Resource id for a message to show in the dialog.
     * @param messageArguments  Arguments to complete the message, if it's a format string. May be null.
     * @param titleResId        Resource id for a text to show in the title.
     *                          0 for default alert title, -1 for no title.
     * @param posBtn            Resource id for the text of the positive button. -1 for no positive button.
     * @param neuBtn            Resource id for the text of the neutral button. -1 for no neutral button.
     * @param negBtn            Resource id for the text of the negative button. -1 for no negative button.
     * @return                  Dialog ready to show.
     */
    public static ConfirmationDialogFragment newInstance(
        int messageResId,
        String[] messageArguments,
        int titleResId,
        int posBtn,
        int neuBtn,
        int negBtn
    ) {

        if (messageResId == -1) {
            throw new IllegalStateException("Calling confirmation dialog without message resource");
        }

        ConfirmationDialogFragment frag = new ConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MESSAGE_RESOURCE_ID, messageResId);
        args.putStringArray(ARG_MESSAGE_ARGUMENTS, messageArguments);
        args.putInt(ARG_TITLE_ID, titleResId);
        args.putInt(ARG_POSITIVE_BTN_RES, posBtn);
        args.putInt(ARG_NEUTRAL_BTN_RES, neuBtn);
        args.putInt(ARG_NEGATIVE_BTN_RES, negBtn);
        frag.setArguments(args);
        return frag;
    }
    
    public void setOnConfirmationListener(ConfirmationDialogFragmentListener listener) {
        mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Object[] messageArguments = getArguments().getStringArray(ARG_MESSAGE_ARGUMENTS);
        int messageId = getArguments().getInt(ARG_MESSAGE_RESOURCE_ID, -1);
        int titleId = getArguments().getInt(ARG_TITLE_ID, -1);
        int posBtn = getArguments().getInt(ARG_POSITIVE_BTN_RES, -1);
        int neuBtn = getArguments().getInt(ARG_NEUTRAL_BTN_RES, -1);
        int negBtn = getArguments().getInt(ARG_NEGATIVE_BTN_RES, -1);

        if (messageArguments == null) {
            messageArguments = new String[]{};
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Theme_ownCloud_Dialog)
                .setIcon(R.drawable.ic_warning)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(String.format(getString(messageId), messageArguments));

        if (titleId == 0) {
            builder.setTitle(android.R.string.dialog_alert_title);
        } else if (titleId != -1) {
            builder.setTitle(titleId);
        }

        if (posBtn != -1) {
            builder.setPositiveButton(posBtn,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (mListener != null) {
                                mListener.onConfirmation(getTag());
                            }
                            dialog.dismiss();
                        }
                    });
        }
        if (neuBtn != -1) {
            builder.setNeutralButton(neuBtn,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (mListener != null) {
                                mListener.onNeutral(getTag());
                            }
                            dialog.dismiss();
                        }
                    });
        }
        if (negBtn != -1) {
            builder.setNegativeButton(negBtn,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (mListener != null) {
                                mListener.onCancel(getTag());
                            }
                            dialog.dismiss();
                        }
                    });
        }
      return builder.create();
    }
    
    
    public interface ConfirmationDialogFragmentListener {
        public void onConfirmation(String callerTag);
        public void onNeutral(String callerTag);
        public void onCancel(String callerTag);
    }
    
}

