/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.account.User;
import com.nextcloud.utils.extensions.IntentExtensionsKt;
import com.owncloud.android.R;
import com.owncloud.android.databinding.RichdocumentsWebviewBinding;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.ui.asynctasks.TextEditorLoadUrlTask;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.WebViewUtil;

import java.util.ArrayList;
import java.util.Optional;

import javax.inject.Inject;

public abstract class EditorWebView extends ExternalSiteWebView {
    public static final int REQUEST_LOCAL_FILE = 101;
    public ValueCallback<Uri[]> uploadMessage;
    protected Snackbar loadingSnackbar;

    protected String fileName;

    RichdocumentsWebviewBinding binding;

    @Inject SyncedFolderProvider syncedFolderProvider;

    protected void loadUrl(String url) {
        onUrlLoaded(url);
    }

    protected void hideLoading() {
        binding.thumbnail.setVisibility(View.GONE);
        binding.filename.setVisibility(View.GONE);
        binding.progressBar2.setVisibility(View.GONE);
        getWebView().setVisibility(View.VISIBLE);

        if (loadingSnackbar != null) {
            loadingSnackbar.dismiss();
        }
    }

    public void onUrlLoaded(String loadedUrl) {
        this.url = loadedUrl;

        if (!url.isEmpty()) {
            new WebViewUtil().setProxyKKPlus(this.getWebView());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            this.getWebView().loadUrl(url);

            new Handler().postDelayed(() -> {
                if (this.getWebView().getVisibility() != View.VISIBLE) {
                    Snackbar snackbar = DisplayUtils.createSnackbar(findViewById(android.R.id.content),
                                                                    R.string.timeout_richDocuments, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.common_cancel, v -> closeView());

                    viewThemeUtils.material.themeSnackbar(snackbar);
                    setLoadingSnackbar(snackbar);
                    snackbar.show();
                }
            }, 10 * 1000);
        } else {
            Toast.makeText(getApplicationContext(),
                           R.string.richdocuments_failed_to_load_document, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    public void closeView() {
        getWebView().destroy();
        finish();
    }

    public void reload() {
        if (getWebView().getVisibility() != View.VISIBLE) {
            return;
        }

        User user = getUser();
        if (user == null) {
            return;
        }

        OCFile file = getFile();
        if (file != null) {
            TextEditorLoadUrlTask task = new TextEditorLoadUrlTask(this, user, file, editorUtils);
            task.execute();
        }
    }

    @Override
    protected void bindView() {
        binding = RichdocumentsWebviewBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void postOnCreate() {
        super.postOnCreate();

        getWebView().setWebChromeClient(new WebChromeClient() {
            final EditorWebView activity = EditorWebView.this;

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

        setFile(IntentExtensionsKt.getParcelableArgument(getIntent(), ExternalSiteWebView.EXTRA_FILE, OCFile.class));

        if (getFile() == null) {
            Toast.makeText(getApplicationContext(),
                           R.string.richdocuments_failed_to_load_document, Toast.LENGTH_LONG).show();
            finish();
        }

        if (getFile() != null) {
            fileName = getFile().getFileName();
        }

        User user = getUser();
        if (user == null) {
            finish();
            return;
        }
        initLoadingScreen(user);
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

        handleActivityResult(requestCode, resultCode, data);

        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOCAL_FILE) {
            handleLocalFile(data, resultCode);
        }
    }

    protected void handleLocalFile(Intent data, int resultCode) {
        if (uploadMessage == null) {
            return;
        }

        if (data.getClipData() == null) {
            // one file
            uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));    
        } else {
            ArrayList<Uri> uris = new ArrayList<>();
            // multiple files
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                ClipData.Item item = data.getClipData().getItemAt(i);
                uris.add(item.getUri());
            }
            
            uploadMessage.onReceiveValue(uris.toArray(new Uri[0]));
        }

        uploadMessage = null;
    }

    protected WebView getWebView() {
        return binding.webView;
    }

    protected View getRootView() {
        return binding.getRoot();
    }

    protected boolean showToolbarByDefault() {
        return false;
    }

    protected void initLoadingScreen(final User user) {
        setThumbnailView(user);
        binding.filename.setText(fileName);
    }

    private void openShareDialog() {
        User user = getUser();
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        Intent intent = new Intent(this, ShareActivity.class);
        intent.putExtra(FileActivity.EXTRA_FILE, getFile());
        intent.putExtra(FileActivity.EXTRA_USER, user);
        startActivity(intent);
    }

    protected void setThumbnailView(final User user) {
        // Todo minimize: only icon by mimetype
        OCFile file = getFile();
        if (file.isFolder()) {
            boolean isAutoUploadFolder = SyncedFolderProvider.isAutoUploadFolder(syncedFolderProvider, file, user);

            Integer overlayIconId = file.getFileOverlayIconId(isAutoUploadFolder);
            LayerDrawable drawable = MimeTypeUtil.getFolderIcon(preferences.isDarkModeEnabled(), overlayIconId, this, viewThemeUtils);
            binding.thumbnail.setImageDrawable(drawable);
        } else {
            if ((MimeTypeUtil.isImage(file) || MimeTypeUtil.isVideo(file)) && file.getRemoteId() != null) {
                // Thumbnail in cache?
                Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.getRemoteId());

                if (thumbnail != null && !file.isUpdateThumbnailNeeded()) {
                    if (MimeTypeUtil.isVideo(file)) {
                        Bitmap withOverlay = ThumbnailsCacheManager.addVideoOverlay(thumbnail, this);
                        binding.thumbnail.setImageBitmap(withOverlay);
                    } else {
                        binding.thumbnail.setImageBitmap(thumbnail);
                    }
                }

                if ("image/png".equalsIgnoreCase(file.getMimeType())) {
                    binding.thumbnail.setBackgroundColor(getResources().getColor(R.color.bg_default, getTheme()));
                }
            } else {
                Drawable icon = MimeTypeUtil.getFileTypeIcon(file.getMimeType(),
                                                             file.getFileName(),
                                                             getApplicationContext(),
                                                             viewThemeUtils);
                binding.thumbnail.setImageDrawable(icon);
            }
        }
    }

    protected void downloadFile(Uri url) {
        DownloadManager downloadmanager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        if (downloadmanager == null) {
            DisplayUtils.showSnackMessage(getWebView(), getString(R.string.failed_to_download));
            return;
        }

        DownloadManager.Request request = new DownloadManager.Request(url);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        downloadmanager.enqueue(request);
    }

    public void setLoadingSnackbar(Snackbar loadingSnackbar) {
        this.loadingSnackbar = loadingSnackbar;
    }

    public class MobileInterface {
        @JavascriptInterface
        public void close() {
            runOnUiThread(EditorWebView.this::closeView);
        }

        @JavascriptInterface
        public void share() {
            openShareDialog();
        }

        @JavascriptInterface
        public void loaded() {
            runOnUiThread(EditorWebView.this::hideLoading);
        }

        @JavascriptInterface
        public void reload() {
            EditorWebView.this.reload();
        }
    }

}
