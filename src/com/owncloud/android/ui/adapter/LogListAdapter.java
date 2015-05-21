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

package com.owncloud.android.ui.adapter;

import java.io.File;

import com.owncloud.android.R;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;



public class LogListAdapter extends ArrayAdapter<String> {
    private Context context = null;
    private String[] values;
    private Uri fileUri = null;
   
    
    public LogListAdapter(Context context, String[] values) {
        super(context, R.layout.log_item, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.log_item, parent, false);
        TextView listText = (TextView) rowView.findViewById(R.id.log_item_single);
        listText.setText(values[position]);
        listText.setTextSize(15);
        fileUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory()+File.separator+"owncloud"+File.separator+"log"+File.separator+values[position]));
        listText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                emailIntent.setType("text/rtf");
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "OwnCloud Logfile");
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "This is a automatic E-mail send by owncloud/android");
                emailIntent.putExtra(android.content.Intent.EXTRA_STREAM, fileUri);
                emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            }
        });
        return rowView;
    }
}
