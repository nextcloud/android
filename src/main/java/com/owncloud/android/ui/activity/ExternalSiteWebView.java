/**
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.io.InputStream;

/**
 * This activity shows an URL as a web view
 */

public class ExternalSiteWebView extends FileActivity {
    public static final String EXTRA_TITLE = "TITLE";
    public static final String EXTRA_URL = "URL";
    public static final String EXTRA_SHOW_SIDEBAR = "SHOW_SIDEBAR";
    public static final String EXTRA_MENU_ITEM_ID = "MENU_ITEM_ID";

    private static final String TAG = ExternalSiteWebView.class.getSimpleName();

    private boolean showSidebar = false;
    private int menuItemId;
    private WebView webview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");

        Bundle extras = getIntent().getExtras();
        String title = extras.getString(EXTRA_TITLE);
        String url = extras.getString(EXTRA_URL);
        menuItemId = extras.getInt(EXTRA_MENU_ITEM_ID);
        showSidebar = extras.getBoolean(EXTRA_SHOW_SIDEBAR);

        // show progress
        getWindow().requestFeature(Window.FEATURE_PROGRESS);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.externalsite_webview);

        webview = (WebView) findViewById(R.id.webView);
        final WebSettings webSettings = webview.getSettings();

        webview.setFocusable(true);
        webview.setFocusableInTouchMode(true);
        webview.setClickable(true);

        // setup toolbar
        setupToolbar();

        // setup drawer
        setupDrawer(menuItemId);

        if (!showSidebar) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            ThemeUtils.setColoredTitle(actionBar, title, this);

            if (showSidebar) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            } else {
                setDrawerIndicatorEnabled(false);
            }
        }

        // enable zoom
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // Non-responsive webs are zoomed out when loaded
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        // user agent
        webSettings.setUserAgentString(MainApp.getUserAgent());

        // no private data storing
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);

        // disable local file access
        webSettings.setAllowFileAccess(false);

        // enable javascript
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);

        webview.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                progressBar.setProgress(progress * 1000);
            }
        });

        webview.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                InputStream resources = getResources().openRawResource(R.raw.custom_error);
                String customError = DisplayUtils.getData(resources);

                if (!customError.isEmpty()) {
                    webview.loadData(customError, "text/html; charset=UTF-8", null);
                }
            }
        });

        webview.loadUrl(url);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval;
        switch (item.getItemId()) {
            case android.R.id.home:
                if (showSidebar) {
                    if (isDrawerOpen()) {
                        closeDrawer();
                    } else {
                        openDrawer();
                    }
                } else {
                    finish();
                }
            retval = true;
            break;

            default:
                retval = super.onOptionsItemSelected(item);
                break;
        }
        return retval;
    }

    @Override
    public void showFiles(boolean onDeviceOnly) {
        super.showFiles(onDeviceOnly);
        Intent fileDisplayActivity = new Intent(getApplicationContext(), FileDisplayActivity.class);
        fileDisplayActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(fileDisplayActivity);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setDrawerMenuItemChecked(menuItemId);

    }
}
