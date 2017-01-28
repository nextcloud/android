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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.owncloud.android.R;

import java.io.File;



public class LogListAdapter extends ArrayAdapter<String> {
    private Context context = null;
    private String[] values;
    private Uri fileUri = null;
   
    
    public LogListAdapter(Context context, String[] values) {
        super(context, R.layout.log_item, values);
        this.context = context;
        this.values = values;
    }

    private static class ViewHolderItem {
		private TextView listText;
	}

	@Override
    public View getView(final int position, View convertView,@NonNull ViewGroup parent) {
        ViewHolderItem viewHolderItem;
		LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
			convertView = inflater.inflate(R.layout.log_item, parent, false);
			viewHolderItem = new ViewHolderItem();
			viewHolderItem.listText = (TextView) convertView.findViewById(R.id.log_item_single);
			convertView.setTag(viewHolderItem);
		} else {
			viewHolderItem = (ViewHolderItem) convertView.getTag();
		}
		View rowView = convertView;
		TextView listText = viewHolderItem.listText;
        listText.setText(values[position]);
        listText.setTextSize(15);
        fileUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory() + File.separator + "owncloud" + File
                .separator+"log"+File.separator+values[position]));
        listText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("text/rtf");
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Nextcloud Logfile");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "This is a automatic E-mail send by nextcloud/android");
                emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            }
        });
        return rowView;
    }
}
