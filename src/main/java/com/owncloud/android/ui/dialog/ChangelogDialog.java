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
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.webkit.WebView;

import com.owncloud.android.R;


/**
 * Dialog to show the contents of res/raw/CHANGELOG.txt
 */
public class ChangelogDialog extends DialogFragment {

    private static final String ARG_CANCELABLE = ChangelogDialog.class.getCanonicalName() +
            ".ARG_CANCELABLE";


    /**
     * Public factory method to get dialog instances.
     * 
     * @param cancelable    If 'true', the dialog can be cancelled by the user input
     *                      (BACK button, touch outside...)
     * @return              New dialog instance, ready to show.
     */
    public static ChangelogDialog newInstance(boolean cancelable) {
        ChangelogDialog fragment = new ChangelogDialog();
        Bundle args = new Bundle();
        args.putBoolean(ARG_CANCELABLE, cancelable);
        fragment.setArguments(args);
        return fragment;
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        /// load the custom view to insert in the dialog, between title and 
        WebView webview = new WebView(getActivity());
        webview.loadUrl("file:///android_res/raw/" +
                getResources().getResourceEntryName(R.raw.changelog) + ".html");
        
        /// build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        Dialog dialog = builder.setView(webview)
                                .setIcon(R.mipmap.ic_launcher)
                                //.setTitle(R.string.whats_new)
                                .setPositiveButton(R.string.common_ok,
                                        new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).create();
        
        dialog.setCancelable(getArguments().getBoolean(ARG_CANCELABLE));
        return dialog;
    } 
    
    /**
     * {@inheritDoc}
     *-/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    Bundle savedInstanceState) {
        /// load the custom layout
        View view = inflater.inflate(R.layout.fragment_changelog, container);
        mEditText = (EditText) view.findViewById(R.id.txt_your_name);
        getDialog().setTitle(R.string.whats_new);
        
        /// read full contents of the change log file (don't make it too big)
        InputStream changeLogStream = getResources().openRawResource(R.raw.changelog);
        Scanner scanner = new java.util.Scanner(changeLogStream).useDelimiter("\\A");
        String text = scanner.hasNext() ? scanner.next() : "";
        
        /// make clickable the links in the change log file
        SpannableString sText = new SpannableString(text);
        Linkify.addLinks(sText, Linkify.ALL);

        return view;
    }
    */
}


