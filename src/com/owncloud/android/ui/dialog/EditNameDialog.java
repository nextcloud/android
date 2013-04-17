/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.owncloud.android.R;


/**
 * Dialog to request the user to input a name, optionally initialized with a former name.
 * 
 * @author Bartek Przybylski
 * @author David A. Velasco
 */
public class EditNameDialog extends SherlockDialogFragment implements DialogInterface.OnClickListener {

    public static final String TAG = EditNameDialog.class.getSimpleName();
    
    protected static final String ARG_TITLE = "TITLE";
    protected static final String ARG_NAME = "NAME";
    protected static final String ARG_SELECTION_START = "SELECTION_START";
    protected static final String ARG_SELECTION_END = "SELECTION_END";
    
    private String mNewFilename;
    private boolean mResult;
    private EditNameDialogListener mListener;
    
    /**
     * Public factory method to get dialog instances.
     * 
     * @param title             Text to show as title in the dialog.
     * @param name              Optional text to include in the text input field when the dialog is shown.
     * @param listener          Instance to notify when the dialog is dismissed.
     * @param selectionStart    Index to the first character to be selected in the input field; negative value for none
     * @param selectionEnd      Index to the last character to be selected in the input field; negative value for none
     * @return              New dialog instance, ready to show.
     */
    static public EditNameDialog newInstance(String title, String name, int selectionStart, int selectionEnd, EditNameDialogListener listener) {
        EditNameDialog f = new EditNameDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_NAME, name);
        args.putInt(ARG_SELECTION_START, selectionStart);
        args.putInt(ARG_SELECTION_END, selectionEnd);
        f.setArguments(args);
        f.setOnDismissListener(listener);
        return f;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String currentName = getArguments().getString(ARG_NAME);
        if (currentName == null)
            currentName = "";
        String title = getArguments().getString(ARG_TITLE);
        
        // Inflate the layout for the dialog
        LayoutInflater inflater = getSherlockActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.edit_box_dialog, null);  // null parent view because it will go in the dialog layout
        EditText inputText = ((EditText)v.findViewById(R.id.user_input));
        inputText.setText(currentName);
        
        // Set it to the dialog 
        AlertDialog.Builder builder = new AlertDialog.Builder(getSherlockActivity());
        builder.setView(v)
               .setPositiveButton(R.string.common_ok, this)
               .setNegativeButton(R.string.common_cancel, this);

        if (title != null) {
            builder.setTitle(title);
        }
        
        mResult = false;
        
        Dialog d = builder.create();

        inputText.requestFocus();
        int selectionStart = getArguments().getInt(ARG_SELECTION_START, -1);
        int selectionEnd = getArguments().getInt(ARG_SELECTION_END, -1);
        if (selectionStart >= 0 && selectionEnd >= 0) {
            inputText.setSelection(Math.min(selectionStart, selectionEnd), Math.max(selectionStart, selectionEnd));
        }
        d.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return d;
    }    

    
    /**
     * Performs the corresponding action when a dialog button is clicked.
     * 
     * Saves the text in the input field to be accessed through {@link #getNewFilename()} when the positive
     * button is clicked.
     * 
     * Notify the current listener in any case.
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case AlertDialog.BUTTON_POSITIVE: {
                mNewFilename = ((TextView)(getDialog().findViewById(R.id.user_input))).getText().toString();
                mResult = true;
            }
            case AlertDialog.BUTTON_NEGATIVE: { // fall through
                dismiss();
                if (mListener != null)
                    mListener.onDismiss(this);
            }
        }
    }
    
    protected void setOnDismissListener(EditNameDialogListener listener) {
        mListener = listener;
    }
    
    /**
     * Returns the text in the input field after the user clicked the positive button.
     * 
     * @return      Text in the input field.
     */
    public String getNewFilename() {
        return mNewFilename;
    }
    
    /**
     * 
     * @return      True when the user clicked the positive button.
     */
    public boolean getResult() {
        return mResult;
    }

    
    /**
     * Interface to receive a notification when any button in the dialog is clicked.
     */
    public interface EditNameDialogListener {
        public void onDismiss(EditNameDialog dialog);
    }


}

