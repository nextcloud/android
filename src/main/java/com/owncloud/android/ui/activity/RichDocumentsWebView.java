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

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.account.CurrentAccountProvider;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.Template;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.RichDocumentsCreateAssetOperation;
import com.owncloud.android.ui.asynctasks.LoadUrlTask;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.glide.CustomGlideStreamLoader;

import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import javax.inject.Inject;

import androidx.annotation.RequiresApi;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import lombok.Getter;
import lombok.Setter;

/**
 * Opens document for editing via Richdocuments app in a web view
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RichDocumentsWebView extends ExternalSiteWebView {

    private static final String TAG = RichDocumentsWebView.class.getSimpleName();
    private static final int REQUEST_REMOTE_FILE = 100;

    public static final int REQUEST_LOCAL_FILE = 101;

    public static final int MINIMUM_API = Build.VERSION_CODES.LOLLIPOP;

    private Unbinder unbinder;
    private OCFile file;
    @Getter @Setter private Snackbar loadingSnackbar;

    public ValueCallback<Uri[]> uploadMessage;

    @BindView(R.id.progressBar2)
    ProgressBar progressBar;

    @BindView(R.id.thumbnail)
    ImageView thumbnail;

    @BindView(R.id.filename)
    TextView fileName;

    @Inject
    protected CurrentAccountProvider currentAccountProvider;

    @SuppressLint("AddJavascriptInterface") // suppress warning as webview is only used >= Lollipop
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        showToolbar = false;
        webViewLayout = R.layout.richdocuments_webview;
        super.onCreate(savedInstanceState);

        unbinder = ButterKnife.bind(this);

        file = getIntent().getParcelableExtra(EXTRA_FILE);

        // TODO make file nullable
        if (file == null) {
            fileName.setText(R.string.create_file_from_template);

            Template template = Parcels.unwrap(getIntent().getParcelableExtra(EXTRA_TEMPLATE));

            int placeholder;

            switch (template.getType()) {
                case "document":
                    placeholder = R.drawable.file_doc;
                    break;

                case "spreadsheet":
                    placeholder = R.drawable.file_xls;
                    break;

                case "presentation":
                    placeholder = R.drawable.file_ppt;
                    break;

                default:
                    placeholder = R.drawable.file;
                    break;
            }

            Glide.with(this).using(new CustomGlideStreamLoader(currentAccountProvider)).load(template.getThumbnailLink())
                .placeholder(placeholder)
                .error(placeholder)
                .into(thumbnail);
        } else {
            setThumbnail(file, thumbnail);
            fileName.setText(file.getFileName());
        }

        webview.addJavascriptInterface(new RichDocumentsMobileInterface(), "RichDocumentsMobileInterface");

        webview.setWebChromeClient(new WebChromeClient() {
            RichDocumentsWebView activity = RichDocumentsWebView.this;

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
        url = getIntent().getStringExtra(EXTRA_URL);
        if (TextUtils.isEmpty(url)) {
            new LoadUrlTask(this, getAccount()).execute(file.getLocalId());
        } else {
            webview.loadUrl(url);
        }
    }

    private void setThumbnail(OCFile file, ImageView thumbnailView) {
        // Todo minimize: only icon by mimetype

        if (file.isFolder()) {
            thumbnailView.setImageDrawable(MimeTypeUtil.getFolderTypeIcon(file.isSharedWithMe() ||
                    file.isSharedWithSharee(), file.isSharedViaLink(), file.isEncrypted(), file.getMountType(),
                this));
        } else {
            if ((MimeTypeUtil.isImage(file) || MimeTypeUtil.isVideo(file)) && file.getRemoteId() != null) {
                // Thumbnail in cache?
                Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.getRemoteId());

                if (thumbnail != null && !file.isUpdateThumbnailNeeded()) {
                    if (MimeTypeUtil.isVideo(file)) {
                        Bitmap withOverlay = ThumbnailsCacheManager.addVideoOverlay(thumbnail);
                        thumbnailView.setImageBitmap(withOverlay);
                    } else {
                        thumbnailView.setImageBitmap(thumbnail);
                    }
                } else {
                    // generate new thumbnail
                    if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, thumbnailView)) {
                        try {
                            final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                                new ThumbnailsCacheManager.ThumbnailGenerationTask(thumbnailView,
                                    getStorageManager(), getAccount());

                            if (thumbnail == null) {
                                if (MimeTypeUtil.isVideo(file)) {
                                    thumbnail = ThumbnailsCacheManager.mDefaultVideo;
                                } else {
                                    thumbnail = ThumbnailsCacheManager.mDefaultImg;
                                }
                            }
                            final ThumbnailsCacheManager.AsyncThumbnailDrawable asyncDrawable =
                                new ThumbnailsCacheManager.AsyncThumbnailDrawable(getResources(), thumbnail, task);
                            thumbnailView.setImageDrawable(asyncDrawable);
                            task.execute(new ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file,
                                file.getRemoteId()));
                        } catch (IllegalArgumentException e) {
                            Log_OC.d(TAG, "ThumbnailGenerationTask : " + e.getMessage());
                        }
                    }
                }

                if ("image/png".equalsIgnoreCase(file.getMimeType())) {
                    thumbnailView.setBackgroundColor(getResources().getColor(R.color.background_color));
                }
            } else {
                thumbnailView.setImageDrawable(MimeTypeUtil.getFileTypeIcon(file.getMimeType(), file.getFileName(),
                    getAccount(), this));
            }
        }
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

    private void openShareDialog() {
        Intent intent = new Intent(this, ShareActivity.class);
        intent.putExtra(FileActivity.EXTRA_FILE, file);
        intent.putExtra(FileActivity.EXTRA_ACCOUNT, getAccount());
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (RESULT_OK != resultCode) {
            // TODO
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
            Account account = currentAccountProvider.getCurrentAccount();
            RichDocumentsCreateAssetOperation operation = new RichDocumentsCreateAssetOperation(file.getRemotePath());
            RemoteOperationResult result = operation.execute(account, this);

            if (result.isSuccess()) {
                String asset = (String) result.getSingleData();

                runOnUiThread(() -> webview.evaluateJavascript("OCA.RichDocuments.documentsMain.postAsset('" +
                    file.getFileName() + "', '" + asset + "');", null));
            } else {
                runOnUiThread(() -> DisplayUtils.showSnackMessage(this, "Inserting image failed!"));
            }
        }).start();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(EXTRA_URL, url);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        url = savedInstanceState.getString(EXTRA_URL);
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        unbinder.unbind();
        webview.destroy();

        super.onDestroy();
    }

    public void closeView() {
        webview.destroy();
        finish();
    }

    private void hideLoading() {
        thumbnail.setVisibility(View.GONE);
        fileName.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        webview.setVisibility(View.VISIBLE);

        if (loadingSnackbar != null) {
            loadingSnackbar.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        webview.evaluateJavascript("if (typeof OCA.RichDocuments.documentsMain.postGrabFocus !== 'undefined') " +
                                       "{ OCA.RichDocuments.documentsMain.postGrabFocus(); }", null);
    }

    private class RichDocumentsMobileInterface {
        @JavascriptInterface
        public void close() {
            runOnUiThread(RichDocumentsWebView.this::closeView);
        }

        @JavascriptInterface
        public void insertGraphic() {
            openFileChooser();
        }

        @JavascriptInterface
        public void share() {
            openShareDialog();
        }

        @JavascriptInterface
        public void documentLoaded() {
            runOnUiThread(RichDocumentsWebView.this::hideLoading);
        }

        @JavascriptInterface
        public void downloadAs(String json) {
            Uri downloadUrl;
            try {
                JSONObject downloadJson = new JSONObject(json);
                downloadUrl = Uri.parse(downloadJson.getString("URL"));
            } catch (JSONException e) {
                Log_OC.e(this, "Failed to parse rename json message: " + e);
                return;
            }

            DownloadManager downloadmanager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

            if (downloadmanager == null) {
                DisplayUtils.showSnackMessage(webview, getString(R.string.failed_to_download));
                return;
            }

            DownloadManager.Request request = new DownloadManager.Request(downloadUrl);
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            downloadmanager.enqueue(request);
        }
    }


}
