/**
 * ownCloud Android client application
 *
 * Copyright (C) 2015 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.view.View;

import com.owncloud.android.R;

public class RadioButtonPreference extends CheckBoxPreference implements View.OnLongClickListener {

    public RadioButtonPreference(Context context) {
        super(context, null, android.R.attr.checkBoxPreferenceStyle);
        setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
    }

    @Override
    public boolean onLongClick(View v) {
        return true;
    }
}
