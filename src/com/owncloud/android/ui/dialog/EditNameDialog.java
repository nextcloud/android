/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.owncloud.android.R;


/**
 * Dialog to request the user about a certificate that could not be validated with the certificates store in the system.
 * 
 * @author Bartek Przybylski
 */
public class EditNameDialog extends SherlockDialogFragment implements OnClickListener {

    private String mNewFilename;
    private boolean mResult;
    private EditNameDialogListener mListener;
    
    static public EditNameDialog newInstance(String filename) {
        EditNameDialog f = new EditNameDialog();
        Bundle args = new Bundle();
        args.putString("filename", filename);
        f.setArguments(args);
        return f;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.edit_box_dialog, container, false);

        String currentName = getArguments().getString("filename");
        if (currentName == null)
            currentName = "";
        
        ((Button)v.findViewById(R.id.cancel)).setOnClickListener(this);
        ((Button)v.findViewById(R.id.ok)).setOnClickListener(this);
        ((TextView)v.findViewById(R.id.user_input)).setText(currentName);
        ((TextView)v.findViewById(R.id.user_input)).requestFocus();
        getDialog().getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        mResult = false;
        return v;
    }
    
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ok: {
                mNewFilename = ((TextView)getView().findViewById(R.id.user_input)).getText().toString();
                mResult = true;
            }
            case R.id.cancel: { // fallthought
                dismiss();
                if (mListener != null)
                    mListener.onDismiss(this);
            }
        }
    }
    
    public void setOnDismissListener(EditNameDialogListener listener) {
        mListener = listener;
    }
    
    public String getNewFilename() {
        return mNewFilename;
    }
    
    // true if user clicked ok
    public boolean getResult() {
        return mResult;
    }

    
    public interface EditNameDialogListener {
        public void onDismiss(EditNameDialog dialog);
    }
    
}

