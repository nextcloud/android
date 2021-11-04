/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud GmbH.
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.account.User;
import com.nextcloud.client.network.ClientFactory;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.RichDocumentsCreateAssetOperation;
import com.owncloud.android.ui.asynctasks.PrintAsyncTask;
import com.owncloud.android.ui.asynctasks.RichDocumentsLoadUrlTask;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Opens document for editing via Richdocuments app in a web view
 */
public class RichDocumentsEditorWebView extends EditorWebView {
    public static final int REQUEST_LOCAL_FILE = 101;
    private static final int REQUEST_REMOTE_FILE = 100;
    private static final String URL = "URL";
    private static final String HYPERLINK = "Url";
    private static final String TYPE = "Type";
    private static final String PRINT = "print";
    private static final String SLIDESHOW = "slideshow";
    private static final String NEW_NAME = "NewName";

    public ValueCallback<Uri[]> uploadMessage;

    @Inject
    protected CurrentAccountProvider currentAccountProvider;

    @Inject
    protected ClientFactory clientFactory;

    @SuppressFBWarnings("ANDROID_WEB_VIEW_JAVASCRIPT_INTERFACE")
    @Override
    protected void postOnCreate() {
        super.postOnCreate();

        getWebView().addJavascriptInterface(new RichDocumentsMobileInterface(), "RichDocumentsMobileInterface");

        getWebView().setWebChromeClient(new WebChromeClient() {
            RichDocumentsEditorWebView activity = RichDocumentsEditorWebView.this;

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }

                activity.uploadMessage = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                intent.setType("image/*");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                try {
                    activity.startActivityForResult(intent, REQUEST_LOCAL_FILE);
                } catch (ActivityNotFoundException e) {
                    uploadMessage = null;
                    Toast.makeText(getBaseContext(), "Cannot open file chooser", Toast.LENGTH_LONG).show();
                    return false;
                }

                return true;
            }
        });

        // load url in background
        loadUrl(getIntent().getStringExtra(EXTRA_URL));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    private void openFileChooser() {
        Intent action = new Intent(this, FilePickerActivity.class);
        action.putExtra(OCFileListFragment.ARG_MIMETYPE, "image/");
        startActivityForResult(action, REQUEST_REMOTE_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (RESULT_OK != resultCode) {
            if (requestCode == REQUEST_LOCAL_FILE) {
                this.uploadMessage.onReceiveValue(null);
                this.uploadMessage = null;
            }
            return;
        }

        switch (requestCode) {
            case REQUEST_LOCAL_FILE:
                handleLocalFile(data, resultCode);
                break;

            case REQUEST_REMOTE_FILE:
                handleRemoteFile(data);
                break;

            default:
                // unexpected, do nothing
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleLocalFile(Intent data, int resultCode) {
        if (uploadMessage == null) {
            return;
        }

        uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
        uploadMessage = null;
    }

    private void handleRemoteFile(Intent data) {
        OCFile file = data.getParcelableExtra(FolderPickerActivity.EXTRA_FILES);

        new Thread(() -> {
            User user = currentAccountProvider.getUser();
            RichDocumentsCreateAssetOperation operation = new RichDocumentsCreateAssetOperation(file.getRemotePath());
            RemoteOperationResult result = operation.execute(user.toPlatformAccount(), this);

            if (result.isSuccess()) {
                String asset = (String) result.getSingleData();

                runOnUiThread(() -> getWebView().evaluateJavascript("OCA.RichDocuments.documentsMain.postAsset('" +
                                                                   file.getFileName() + "', '" + asset + "');", null));
            } else {
                runOnUiThread(() -> DisplayUtils.showSnackMessage(this, "Inserting image failed!"));
            }
        }).start();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(EXTRA_URL, url);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        url = savedInstanceState.getString(EXTRA_URL);
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        getWebView().destroy();

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        getWebView().evaluateJavascript("if (typeof OCA.RichDocuments.documentsMain.postGrabFocus !== 'undefined') " +
                                            "{ OCA.RichDocuments.documentsMain.postGrabFocus(); }",
                                        null);
    }

    private void printFile(Uri url) {
        OwnCloudAccount account = accountManager.getCurrentOwnCloudAccount();

        if (account == null) {
            DisplayUtils.showSnackMessage(getWebView(), getString(R.string.failed_to_print));
            return;
        }

        File targetFile = new File(FileStorageUtils.getTemporalPath(account.getName()) + "/print.pdf");

        new PrintAsyncTask(targetFile, url.toString(), new WeakReference<>(this)).execute();
    }

    @Override
    public void loadUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            new RichDocumentsLoadUrlTask(this, getUser().get(), getFile()).execute();
        } else {
            super.loadUrl(url);
        }
    }

    private void showSlideShow(Uri url) {
        Intent intent = new Intent(this, ExternalSiteWebView.class);
        intent.putExtra(ExternalSiteWebView.EXTRA_URL, url.toString());
        intent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false);
        intent.putExtra(ExternalSiteWebView.EXTRA_SHOW_TOOLBAR, false);
        startActivity(intent);
    }

    private class RichDocumentsMobileInterface extends MobileInterface {
        @JavascriptInterface
        public void insertGraphic() {
            openFileChooser();
        }

        @JavascriptInterface
        public void documentLoaded() {
            runOnUiThread(RichDocumentsEditorWebView.this::hideLoading);
        }

        @JavascriptInterface
        public void downloadAs(String json) {
            try {
                JSONObject downloadJson = new JSONObject(json);

                Uri url = Uri.parse(downloadJson.getString(URL));

                switch (downloadJson.getString(TYPE)) {
                    case PRINT:
                        printFile(url);
                        break;

                    case SLIDESHOW:
                        showSlideShow(url);
                        break;

                    default:
                        downloadFile(url);
                        break;
                }
            } catch (JSONException e) {
                Log_OC.e(this, "Failed to parse download json message: " + e);
            }
        }

        @JavascriptInterface
        public void fileRename(String renameString) {
            // when shared file is renamed in another instance, we will get notified about it
            // need to change filename for sharing
            try {
                JSONObject renameJson = new JSONObject(renameString);
                String newName = renameJson.getString(NEW_NAME);
                getFile().setFileName(newName);
            } catch (JSONException e) {
                Log_OC.e(this, "Failed to parse rename json message: " + e);
            }
        }

        @JavascriptInterface
        public void paste() {
            // Javascript cannot do this by itself, so help out.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                getWebView().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PASTE));
                getWebView().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_PASTE));
            }
        }

        @JavascriptInterface
        public void hyperlink(String hyperlink) {
            try {
                String url = new JSONObject(hyperlink).getString(HYPERLINK);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            } catch (JSONException e) {
                Log_OC.e(this, "Failed to parse download json message: " + e);
            }
        }
    }
}
