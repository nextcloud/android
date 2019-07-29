/*
 *   ownCloud Android client application
 *
 *   @author LukeOwncloud
 *   Copyright (C) 2016 ownCloud Inc.
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
 */
package com.owncloud.android.ui.errorhandling;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.owncloud.android.R;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ErrorShowActivity extends AppCompatActivity {
    private static final String TAG = ErrorShowActivity.class.getSimpleName();
    public static final String EXTRA_ERROR_TEXT = "error";

    TextView error;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "ErrorShowActivity was called. See above for StackTrace.");
        setContentView(R.layout.errorhandling_showerror);
        error = findViewById(R.id.errorTextView);
        error.setText(getIntent().getStringExtra(EXTRA_ERROR_TEXT));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle(R.string.common_error);
    }
}
