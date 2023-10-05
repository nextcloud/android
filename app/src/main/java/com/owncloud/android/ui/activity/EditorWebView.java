/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
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
import com.nextcloud.client.preferences.DarkMode;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.R;
import com.owncloud.android.databinding.RichdocumentsWebviewBinding;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;

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

        setFile(getIntent().getParcelableExtra(ExternalSiteWebView.EXTRA_FILE));

        if (getFile() == null) {
            Toast.makeText(getApplicationContext(),
                           R.string.richdocuments_failed_to_load_document, Toast.LENGTH_LONG).show();
            finish();
        }

        if (getFile() != null) {
            fileName = getFile().getFileName();
        }

        Optional<User> user = getUser();
        if (!user.isPresent()) {
            finish();
            return;
        }
        initLoadingScreen(user.get());
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

        uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
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
        Intent intent = new Intent(this, ShareActivity.class);
        intent.putExtra(FileActivity.EXTRA_FILE, getFile());
        intent.putExtra(FileActivity.EXTRA_USER, getUser().orElseThrow(RuntimeException::new));
        startActivity(intent);
    }

    protected void setThumbnailView(final User user) {
        // Todo minimize: only icon by mimetype
        OCFile file = getFile();
        if (file.isFolder()) {
            boolean isAutoUploadFolder = SyncedFolderProvider.isAutoUploadFolder(syncedFolderProvider, file, user);

            Integer overlayIconId = file.getFileOverlayIconId(isAutoUploadFolder);
            LayerDrawable drawable = MimeTypeUtil.getFileIcon(preferences.isDarkModeEnabled(), overlayIconId, this, viewThemeUtils);
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
    }

}
