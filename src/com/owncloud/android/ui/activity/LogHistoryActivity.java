/* ownCloud Android client application
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

package com.owncloud.android.ui.activity;

import java.io.File;
import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.R;
import com.owncloud.android.ui.adapter.LogListAdapter;
import com.owncloud.android.utils.FileStorageUtils;



public class LogHistoryActivity extends SherlockPreferenceActivity implements OnPreferenceChangeListener {
    String logpath = FileStorageUtils.getLogPath();
    File logDIR = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.log_send_file);
        setTitle("Log History");
        ActionBar actionBar = getSherlock().getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        ListView listView = (ListView) findViewById(android.R.id.list);
        Button deleteHistoryButton = (Button) findViewById(R.id.deleteLogHistoryButton);
        deleteHistoryButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                File dir = new File(logpath);
                if (dir != null) {
                    File[] files = dir.listFiles();
                    if(files!=null) { 
                        for(File f: files) {
                                f.delete();
                        }
                    }
                    dir.delete();
                }
                Intent intent = new Intent(getBaseContext(), Preferences.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            
        });
        
       
        if(logpath != null){
        logDIR = new File(logpath);
        }
        
        if(logDIR != null && logDIR.isDirectory()) {
            File[] files = logDIR.listFiles();
          
            if (files != null && files.length != 0) {
                ArrayList<String> logfiles_name = new ArrayList<String>();
                for (File file : files) {
                    logfiles_name.add(file.getName());
                }
                    String[] logFiles2Array = logfiles_name.toArray(new String[logfiles_name.size()]);
                    LogListAdapter listadapter = new LogListAdapter(this,logFiles2Array);
                    listView.setAdapter(listadapter);
            }
        }
    }

    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        super.onMenuItemSelected(featureId, item);
        Intent intent;

        switch (item.getItemId()) {
       
        case android.R.id.home:
            intent = new Intent(getBaseContext(), Preferences.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            break;
        default:
            return false;
        }
        return true;
    }
    @Override
    public boolean onPreferenceChange(Preference arg0, Object arg1) {
        return false;
    }
}