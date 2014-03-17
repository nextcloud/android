/* ownCloud Android client application
 *   Copyright (C) 2012-2014 ownCloud Inc.
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

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.owncloud.android.R;

public class AlertMessageDialog extends SherlockDialogFragment {
    
    private static final String ARG_TITLE_ID = AlertMessageDialog.class.getCanonicalName() + ".ARG_TITLE_ID";
    private static final String ARG_MESSAGE_ID = AlertMessageDialog.class.getCanonicalName() + ".ARG_MESSAGE_ID";


    public static AlertMessageDialog newInstance(int title, int message) {
        AlertMessageDialog frag = new AlertMessageDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_TITLE_ID, title);
        args.putInt(ARG_MESSAGE_ID, message);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getSherlockActivity());
        
        int title = getArguments().getInt(ARG_TITLE_ID);
        int message = getArguments().getInt(ARG_MESSAGE_ID);
        
        dialogBuilder.setIcon(R.drawable.common_error)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(true)
            .setPositiveButton(R.string.common_ok, 
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                }
            });
        return dialogBuilder.create();
    }

}
