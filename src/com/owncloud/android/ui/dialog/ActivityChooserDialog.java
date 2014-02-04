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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.owncloud.android.R;
import com.owncloud.android.utils.Log_OC;

/**
 * Dialog showing a list activities able to resolve a given Intent, 
 * filtering out the activities matching give package names.
 * 
 * @author David A. Velasco
 */
public class ActivityChooserDialog  extends SherlockDialogFragment {
    
    private final static String TAG =  ActivityChooserDialog.class.getSimpleName();
    private final static String ARG_INTENT =  ActivityChooserDialog.class.getSimpleName() + ".ARG_INTENT";
    private final static String ARG_PACKAGES_TO_EXCLUDE =  ActivityChooserDialog.class.getSimpleName() + ".ARG_PACKAGES_TO_EXCLUDE";
    
    private ListAdapter mAdapter;
    
    public static ActivityChooserDialog newInstance(Intent intent, String[] packagesToExclude/*OnConflictDecisionMadeListener listener*/) {
        ActivityChooserDialog f = new ActivityChooserDialog();
        Bundle args = new Bundle();
        args.putParcelable(ARG_INTENT, intent);
        args.putStringArray(ARG_PACKAGES_TO_EXCLUDE, packagesToExclude);
        f.setArguments(args);
        return f;
    }
    
    public ActivityChooserDialog() {
        super();
        Log_OC.d(TAG, "constructor");
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Intent intent = getArguments().getParcelable(ARG_INTENT);
        String[] packagesToExclude = getArguments().getStringArray(ARG_PACKAGES_TO_EXCLUDE);
        List<String> packagesToExcludeList = Arrays.asList(packagesToExclude != null ? packagesToExclude : new String[0]);
        
        PackageManager pm= getSherlockActivity().getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        Iterator<ResolveInfo> it = activities.iterator();
        ResolveInfo resolveInfo;
        while (it.hasNext()) {
            resolveInfo = it.next();
            if (packagesToExcludeList.contains(resolveInfo.activityInfo.packageName.toLowerCase())) {
                it.remove();
            }
        }
        Collections.sort(activities, new ResolveInfo.DisplayNameComparator(pm)); 
        mAdapter = new ActivityAdapter(getSherlockActivity(), pm, activities);
        
        return new AlertDialog.Builder(getSherlockActivity())
                   .setTitle(R.string.activity_chooser_title)
                   .setAdapter(mAdapter, new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialog, int which) {
                               // The 'which' argument contains the index position
                               // of the selected item                           
                           }
                       })
                   .create();
    }

    
    class ActivityAdapter extends ArrayAdapter<ResolveInfo> {
        
        private PackageManager mPackageManager;
        
        ActivityAdapter(Context context, PackageManager pm, List<ResolveInfo> apps) {
            //super(context, android.R.layout.activity_list_item, apps);
            super(context, R.layout.activity_row, apps);
            this.mPackageManager = pm;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = newView(parent);
            }
            bindView(position, convertView);
            return convertView;
        }
        
        private View newView(ViewGroup parent) {
            return(((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.activity_row, parent, false));
            //return(((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(android.R.layout.activity_list_item, parent, false));
        }
        
        private void bindView(int position, View row) {
            TextView label = (TextView) row.findViewById(R.id.title);
            //TextView label = (TextView) row.findViewById(android.R.id.text1);
            label.setText(getItem(position).loadLabel(mPackageManager));
            ImageView icon = (ImageView) row.findViewById(R.id.icon);
            //ImageView icon = (ImageView) row.findViewById(android.R.id.icon);
            icon.setImageDrawable(getItem(position).loadIcon(mPackageManager));
        }
    }
    
}
