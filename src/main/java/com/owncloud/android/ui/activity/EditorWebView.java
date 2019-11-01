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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.asynctasks.LoadUrlTask;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ThemeUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import lombok.Getter;
import lombok.Setter;

public abstract class EditorWebView extends ExternalSiteWebView {
    @Getter @Setter protected Snackbar loadingSnackbar;
    protected OCFile file;

    @BindView(R.id.progressBar2)
    ProgressBar progressBar;

    @BindView(R.id.thumbnail)
    ImageView thumbnail;

    @BindView(R.id.filename)
    TextView fileName;

    private Unbinder unbinder;

    private static final String TAG = EditorWebView.class.getSimpleName();

    protected void loadUrl(String url, OCFile file) {
        if (TextUtils.isEmpty(url)) {
            new LoadUrlTask(this, getAccount()).execute(file.getLocalId());
        } else {
            webview.loadUrl(url);
        }
    }

    protected void hideLoading() {
        thumbnail.setVisibility(View.GONE);
        fileName.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        webview.setVisibility(View.VISIBLE);

        if (loadingSnackbar != null) {
            loadingSnackbar.dismiss();
        }
    }

    public void onUrlLoaded(String loadedUrl) {
        this.url = loadedUrl;

        if (!url.isEmpty()) {
            getWebview().loadUrl(url);

            new Handler().postDelayed(() -> {
                if (getWebview().getVisibility() != View.VISIBLE) {
                    Snackbar snackbar = DisplayUtils.createSnackbar(findViewById(android.R.id.content),
                                                                    R.string.timeout_richDocuments, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.fallback_weblogin_back, v -> hideLoading());

                    ThemeUtils.colorSnackbar(getApplicationContext(), snackbar);
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
        webview.destroy();
        finish();
    }

    @SuppressLint("AddJavascriptInterface") // suppress warning as webview is only used >= Lollipop
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        webViewLayout = R.layout.richdocuments_webview; // TODO rename

        showToolbar = false;

        super.onCreate(savedInstanceState);

        unbinder = ButterKnife.bind(this);

        file = getIntent().getParcelableExtra(ExternalSiteWebView.EXTRA_FILE);

        initLoadingScreen();
    }

    protected void initLoadingScreen() {
        setThumbnail(file, thumbnail);
        fileName.setText(file.getFileName());
    }

    private void openShareDialog() {
        Intent intent = new Intent(this, ShareActivity.class);
        intent.putExtra(FileActivity.EXTRA_FILE, file);
        intent.putExtra(FileActivity.EXTRA_ACCOUNT, getAccount());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        unbinder.unbind();
        webview.destroy();

        super.onDestroy();
    }

    protected void setThumbnail(OCFile file, ImageView thumbnailView) {
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
                    thumbnailView.setBackgroundColor(getResources().getColor(R.color.bg_default));
                }
            } else {
                thumbnailView.setImageDrawable(MimeTypeUtil.getFileTypeIcon(file.getMimeType(), file.getFileName(),
                                                                            getAccount(), this));
            }
        }
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
