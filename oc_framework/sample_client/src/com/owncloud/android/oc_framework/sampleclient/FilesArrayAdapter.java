package com.owncloud.android.oc_framework.sampleclient;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.owncloud.android.oc_framework.operations.RemoteFile;

public class FilesArrayAdapter extends ArrayAdapter<RemoteFile> {
	
	public FilesArrayAdapter(Context context, int resource) {
		super(context, resource);
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView textView = (TextView)super.getView(position, convertView, parent);
	    textView.setText(getItem(position).getRemotePath());
	    return textView;
	}		
}

