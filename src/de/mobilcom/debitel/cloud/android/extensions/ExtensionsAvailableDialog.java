/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
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

package de.mobilcom.debitel.cloud.android.extensions;

import de.mobilcom.debitel.cloud.android.Log_OC;
import de.mobilcom.debitel.cloud.android.R;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ExtensionsAvailableDialog extends DialogFragment implements
        OnClickListener {

    public ExtensionsAvailableDialog() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.extensions_available_dialog,
                container);
        Button btnYes = (Button) view.findViewById(R.id.buttonYes);
        Button btnNo = (Button) view.findViewById(R.id.buttonNo);
        
        // Set background of buttons
        boolean customButtons = getResources().getBoolean(R.bool.custom_buttons);
        if (customButtons) {
            btnYes.setBackgroundResource(R.drawable.btn_default);
            btnNo.setBackgroundResource(R.drawable.btn_default);
        }
        
        btnYes.setOnClickListener(this);
        btnNo.setOnClickListener(this);
        getDialog().setTitle(R.string.extensions_avail_title);
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.buttonYes: {
            Intent i = new Intent(getActivity(), ExtensionsListActivity.class);
            startActivity(i);
            getActivity().finish();
        }
            break;
        case R.id.buttonNo:
            getActivity().finish();
            break;
        default:
            Log_OC.e("EAD", "Button with unknown id clicked " + v.getId());
        }
    }

}
