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

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

public class SsoWebView extends WebView {
    
    public SsoWebView(Context context) {
        super(context);
    }
    
    public SsoWebView(Context context, AttributeSet attr) {
        super(context, attr);
    }
    
    @Override
    public boolean onCheckIsTextEditor () {
        return false;
    }
    
}

