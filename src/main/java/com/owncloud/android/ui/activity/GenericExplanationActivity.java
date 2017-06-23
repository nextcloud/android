/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
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

package com.owncloud.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.utils.AnalyticsUtils;

import java.util.ArrayList;


/**
 * Activity showing a text message and, optionally, a couple list of single or paired text strings.
 * 
 * Added to show explanations for notifications when the user clicks on them, and there no place
 * better to show them.
 */
public class GenericExplanationActivity  extends AppCompatActivity {

    public static final String EXTRA_LIST = GenericExplanationActivity.class.getCanonicalName() +
            ".EXTRA_LIST";
    public static final String EXTRA_LIST_2 = GenericExplanationActivity.class.getCanonicalName() +
            ".EXTRA_LIST_2";
    public static final String MESSAGE = GenericExplanationActivity.class.getCanonicalName() +
            ".MESSAGE";

    private static final String TAG = GenericExplanationActivity.class.getSimpleName();

    private static final String SCREEN_NAME = "Information";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        String message = intent.getStringExtra(MESSAGE); 
        ArrayList<String> list = intent.getStringArrayListExtra(EXTRA_LIST);
        ArrayList<String> list2 = intent.getStringArrayListExtra(EXTRA_LIST_2);
        
        setContentView(R.layout.generic_explanation);
        
        if (message != null) {
            TextView textView = (TextView) findViewById(R.id.message);
            textView.setText(message);
            textView.setMovementMethod(new ScrollingMovementMethod());
        }
        
        ListView listView = (ListView) findViewById(R.id.list);
        if (list != null && list.size() > 0) {
            //ListAdapter adapter = new ArrayAdapter<String>(this,
            // android.R.layout.simple_list_item_1, list);
            ListAdapter adapter = new ExplanationListAdapterView(this, list, list2);
            listView.setAdapter(adapter);
        } else {
            listView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtils.setCurrentScreenName(this, SCREEN_NAME, TAG);
    }

    public class ExplanationListAdapterView extends ArrayAdapter<String> {
        
        ArrayList<String> mList;
        ArrayList<String> mList2;
        
        ExplanationListAdapterView(Context context, ArrayList<String> list,
                                   ArrayList<String> list2) {
            super(context, android.R.layout.two_line_list_item, android.R.id.text1, list);
            mList = list;
            mList2 = list2;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public View getView (int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            if (mList2 != null && mList2.size() > 0 && position >= 0 &&
                    position < mList2.size()) {
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                if (text2 != null) {
                    text2.setText(mList2.get(position));
                }
            }
            return view;
        }
    }

}
