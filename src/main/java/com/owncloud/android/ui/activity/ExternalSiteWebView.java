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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * This activity shows an URL as a web view
 */

public class ExternalSiteWebView extends FileActivity {
    private static final String TAG = ExternalSiteWebView.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");

        // TODO get name, url, boolean showSidebar

        // show progress
        getWindow().requestFeature(Window.FEATURE_PROGRESS);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.externalsite_webview);

        WebView webview = (WebView) findViewById(R.id.webView);
        WebSettings webSettings = webview.getSettings();

        webview.setFocusable(true);
        webview.setFocusableInTouchMode(true);
        webview.setClickable(true);

        // setup toolbar
        setupToolbar();

        // setup drawer
        setupDrawer(R.id.nav_external);
        getSupportActionBar().setTitle("About us");

        // enable zoom
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // next two settings grant that non-responsive webs are zoomed out when loaded
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        webSettings.setUserAgentString(MainApp.getUserAgent());

        // no private data storing
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);

        // disable local file access
        webSettings.setAllowFileAccess(false);

        // enable javascript
        webview.getSettings().setJavaScriptEnabled(true);

        final Activity activity = this;
        webview.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                activity.setProgress(progress * 1000);
            }
        });

        webview.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(activity, getString(R.string.webview_error) + ": " + description, Toast.LENGTH_SHORT).show();
            }
        });

        webview.loadUrl("http://nextcloud.com");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval;
        switch (item.getItemId()) {
            case android.R.id.home: {
                if (isDrawerOpen()) {
                    closeDrawer();
                } else {
                    openDrawer();
                }
            }

            default:
                retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }

    @Override
    public void showFiles(boolean onDeviceOnly) {
        super.showFiles(onDeviceOnly);
        Intent fileDisplayActivity = new Intent(getApplicationContext(),
                FileDisplayActivity.class);
        fileDisplayActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(fileDisplayActivity);
    }
}
