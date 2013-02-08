/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
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
package com.owncloud.android.ui.adapter;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.Preferences;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.owncloud.android.R;

/**
 * Populates the landing screen icons.
 * 
 * @author Lennart Rosam
 * 
 */
public class LandingScreenAdapter extends BaseAdapter {

    private Context mContext;

    private final Integer[] mLandingScreenIcons = { R.drawable.home,
            R.drawable.music, R.drawable.contacts, R.drawable.calendar,
            android.R.drawable.ic_menu_agenda, R.drawable.settings };

    private final Integer[] mLandingScreenTexts = { R.string.main_files,
            R.string.main_music, R.string.main_contacts,
            R.string.main_calendar, R.string.main_bookmarks,
            R.string.main_settings };

    public LandingScreenAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return mLandingScreenIcons.length;
    }

    @Override
    /**
     * Returns the Intent associated with this object
     * or null if the functionality is not yet implemented
     */
    public Object getItem(int position) {
        Intent intent = new Intent();

        switch (position) {
        case 0:
            /*
             * The FileDisplayActivity requires the ownCloud account as an
             * parcableExtra. We will put in the one that is selected in the
             * preferences
             */
            intent.setClass(mContext, FileDisplayActivity.class);
            intent.putExtra("ACCOUNT",
                    AccountUtils.getCurrentOwnCloudAccount(mContext));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            break;
        case 5:
            intent.setClass(mContext, Preferences.class);
            break;
        default:
            intent = null;
        }
        return intent;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflator = LayoutInflater.from(mContext);
            convertView = inflator.inflate(R.layout.landing_page_item, null);

            ImageView icon = (ImageView) convertView
                    .findViewById(R.id.gridImage);
            TextView iconText = (TextView) convertView
                    .findViewById(R.id.gridText);

            icon.setImageResource(mLandingScreenIcons[position]);
            iconText.setText(mLandingScreenTexts[position]);
        }
        return convertView;
    }
}
