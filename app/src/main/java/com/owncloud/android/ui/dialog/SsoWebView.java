/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2013 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only
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
