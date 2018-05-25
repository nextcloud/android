/*
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * Copyright (C) 2015 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.preview;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PictureDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.github.chrisbanes.photoview.PhotoView;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import pl.droidsonroids.gif.GifDrawable;


/**
 * This fragment shows a preview of a downloaded image.
 *
 * Trying to get an instance with a NULL {@link OCFile} will produce an
 * {@link IllegalStateException}.
 *
 * If the {@link OCFile} passed is not downloaded, an {@link IllegalStateException} is generated on
 * instantiation too.
 */
public class PreviewImageFragment extends FileFragment {

    private static final String EXTRA_FILE = "FILE";

    private static final String ARG_FILE = "FILE";
    private static final String ARG_IGNORE_FIRST = "IGNORE_FIRST";
    private static final String ARG_SHOW_RESIZED_IMAGE = "SHOW_RESIZED_IMAGE";
    private static final String MIME_TYPE_PNG = "image/png";
    private static final String MIME_TYPE_GIF = "image/gif";
    private static final String MIME_TYPE_SVG = "image/svg+xml";

    private PhotoView mImageView;
    private RelativeLayout mMultiView;

    private LinearLayout mMultiListContainer;
    private TextView mMultiListMessage;
    private TextView mMultiListHeadline;
    private ImageView mMultiListIcon;
    private ProgressBar mMultiListProgress;

    private Boolean mShowResizedImage = false;

    private Bitmap mBitmap = null;

    private static final String TAG = PreviewImageFragment.class.getSimpleName();

    private boolean mIgnoreFirstSavedState;

    private LoadBitmapTask mLoadBitmapTask = null;

    /**
     * Public factory method to create a new fragment that previews an image.
     *
     * Android strongly recommends keep the empty constructor of fragments as the only public
     * constructor, and
     * use {@link #setArguments(Bundle)} to set the needed arguments.
     *
     * This method hides to client objects the need of doing the construction in two steps.
     *
     * @param imageFile             An {@link OCFile} to preview as an image in the fragment
     * @param ignoreFirstSavedState Flag to work around an unexpected behaviour of
     *                              {@link FragmentStatePagerAdapter}
     *                              ; TODO better solution
     */
    public static PreviewImageFragment newInstance(@NonNull OCFile imageFile, boolean ignoreFirstSavedState,
                                                   boolean showResizedImage) {
        PreviewImageFragment frag = new PreviewImageFragment();
        frag.mShowResizedImage = showResizedImage;
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, imageFile);
        args.putBoolean(ARG_IGNORE_FIRST, ignoreFirstSavedState);
        args.putBoolean(ARG_SHOW_RESIZED_IMAGE, showResizedImage);
        frag.setArguments(args);
        return frag;
    }


    /**
     * Creates an empty fragment for image previews.
     *
     * MUST BE KEPT: the system uses it when tries to reinstantiate a fragment automatically
     * (for instance, when the device is turned a aside).
     *
     * DO NOT CALL IT: an {@link OCFile} and {@link Account} must be provided for a successful
     * construction
     */
    public PreviewImageFragment() {
        mIgnoreFirstSavedState = false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        setFile(args.getParcelable(ARG_FILE));
        // TODO better in super, but needs to check ALL the class extending FileFragment;
        // not right now

        mIgnoreFirstSavedState = args.getBoolean(ARG_IGNORE_FIRST);
        mShowResizedImage = args.getBoolean(ARG_SHOW_RESIZED_IMAGE);
        setHasOptionsMenu(true);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.preview_image_fragment, container, false);
        mImageView = view.findViewById(R.id.image);
        mImageView.setVisibility(View.GONE);

        view.setOnClickListener(v -> togglePreviewImageFullScreen());

        mImageView.setOnClickListener(v -> togglePreviewImageFullScreen());

        mMultiView = view.findViewById(R.id.multi_view);

        setupMultiView(view);
        setMultiListLoadingMessage();

        return view;
    }

    private void setupMultiView(View view) {
        mMultiListContainer = view.findViewById(R.id.empty_list_view);
        mMultiListMessage = view.findViewById(R.id.empty_list_view_text);
        mMultiListHeadline = view.findViewById(R.id.empty_list_view_headline);
        mMultiListIcon = view.findViewById(R.id.empty_list_icon);
        mMultiListProgress = view.findViewById(R.id.empty_list_progress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            if (!mIgnoreFirstSavedState) {
                OCFile file = savedInstanceState.getParcelable(PreviewImageFragment.EXTRA_FILE);
                setFile(file);
            } else {
                mIgnoreFirstSavedState = false;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PreviewImageFragment.EXTRA_FILE, getFile());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getFile() != null) {
            mImageView.setTag(getFile().getFileId());

            if (mShowResizedImage) {
                Bitmap resizedImage = ThumbnailsCacheManager.getBitmapFromDiskCache(
                        String.valueOf(ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE + getFile().getRemoteId()));

                if (resizedImage != null && !getFile().needsUpdateThumbnail()) {
                    mImageView.setImageBitmap(resizedImage);
                    mImageView.setVisibility(View.VISIBLE);
                    mBitmap = resizedImage;
                } else {
                    // show thumbnail while loading resized image
                    Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                            String.valueOf(ThumbnailsCacheManager.PREFIX_THUMBNAIL + getFile().getRemoteId()));

                    if (thumbnail != null) {
                        mImageView.setImageBitmap(thumbnail);
                        mImageView.setVisibility(View.VISIBLE);
                        mBitmap = thumbnail;
                    } else {
                        thumbnail = ThumbnailsCacheManager.mDefaultImg;
                    }

                    // generate new resized image
                    if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(getFile(), mImageView) &&
                            mContainerActivity.getStorageManager() != null) {
                        final ThumbnailsCacheManager.ResizedImageGenerationTask task =
                                new ThumbnailsCacheManager.ResizedImageGenerationTask(PreviewImageFragment.this,
                                        mImageView,
                                        mContainerActivity.getStorageManager(),
                                        mContainerActivity.getStorageManager().getAccount());
                        if (resizedImage == null) {
                            resizedImage = thumbnail;
                        }
                        final ThumbnailsCacheManager.AsyncResizedImageDrawable asyncDrawable =
                                new ThumbnailsCacheManager.AsyncResizedImageDrawable(
                                        MainApp.getAppContext().getResources(),
                                        resizedImage,
                                        task
                                );
                        mImageView.setImageDrawable(asyncDrawable);
                        task.execute(getFile());
                    }
                }
                mMultiView.setVisibility(View.GONE);
                if (getResources() != null) {
                    mImageView.setBackgroundColor(getResources().getColor(R.color.black));
                }
                mImageView.setVisibility(View.VISIBLE);

            } else {
                mLoadBitmapTask = new LoadBitmapTask(mImageView);
                mLoadBitmapTask.execute(getFile());
            }
        } else {
            showErrorMessage(R.string.preview_image_error_no_local_file);
        }
    }

    @Override
    public void onStop() {
        Log_OC.d(TAG, "onStop starts");
        if (mLoadBitmapTask != null) {
            mLoadBitmapTask.cancel(true);
            mLoadBitmapTask = null;
        }
        super.onStop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.file_actions_menu, menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mContainerActivity.getStorageManager() != null && getFile() != null) {
            // Update the file
            setFile(mContainerActivity.getStorageManager().getFileById(getFile().getFileId()));

            FileMenuFilter mf = new FileMenuFilter(
                    getFile(),
                    mContainerActivity.getStorageManager().getAccount(),
                    mContainerActivity,
                    getActivity(),
                    false
            );
            mf.filter(menu, true);
        }

        // additional restriction for this fragment 
        // TODO allow renaming in PreviewImageFragment
        MenuItem item = menu.findItem(R.id.action_rename_file);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment 
        // TODO allow refresh file in PreviewImageFragment
        item = menu.findItem(R.id.action_sync_file);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_select_all);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_move);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_copy);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_favorite);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_unset_favorite);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        if (getFile().isSharedWithMe() && !getFile().canReshare()) {
            // additional restriction for this fragment
            item = menu.findItem(R.id.action_send_share_file);
            if (item != null) {
                item.setVisible(false);
                item.setEnabled(false);
            }
        }

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send_share_file:
                if (getFile().isSharedWithMe() && !getFile().canReshare()) {
                    Snackbar.make(getView(),
                            R.string.resharing_is_not_allowed,
                            Snackbar.LENGTH_LONG
                    )
                            .show();
                } else {
                    mContainerActivity.getFileOperationsHelper().sendShareFile(getFile());
                }
                return true;

            case R.id.action_open_file_with:
                openFile();
                return true;

            case R.id.action_remove_file:
                RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstance(getFile());
                dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
                return true;

            case R.id.action_see_details:
                seeDetails();
                return true;

            case R.id.action_download_file:
            case R.id.action_sync_file:
                mContainerActivity.getFileOperationsHelper().syncFile(getFile());
                return true;

            case R.id.action_set_as_wallpaper:
                mContainerActivity.getFileOperationsHelper().setPictureAs(getFile(), getImageView());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void seeDetails() {
        mContainerActivity.showDetails(getFile());
    }

    @SuppressFBWarnings("Dm")
    @Override
    public void onDestroy() {
        if (mBitmap != null) {
            mBitmap.recycle();
            // putting this in onStop() is just the same; the fragment is always destroyed by
            // {@link FragmentStatePagerAdapter} when the fragment in swiped further than the
            // valid offscreen distance, and onStop() is never called before than that
        }
        super.onDestroy();
    }


    /**
     * Opens the previewed image with an external application.
     */
    private void openFile() {
        mContainerActivity.getFileOperationsHelper().openFile(getFile());
        finish();
    }


    private class LoadBitmapTask extends AsyncTask<OCFile, Void, LoadImage> {

        /**
         * Weak reference to the target {@link ImageView} where the bitmap will be loaded into.
         *
         * Using a weak reference will avoid memory leaks if the target ImageView is retired from
         * memory before the load finishes.
         */
        private final WeakReference<PhotoView> mImageViewRef;

        /**
         * Error message to show when a load fails
         */
        private int mErrorMessageId;


        /**
         * Constructor.
         *
         * @param imageView Target {@link ImageView} where the bitmap will be loaded into.
         */
        LoadBitmapTask(PhotoView imageView) {
            mImageViewRef = new WeakReference<>(imageView);
        }

        @Override
        protected LoadImage doInBackground(OCFile... params) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE);

            Bitmap bitmapResult = null;
            Drawable drawableResult = null;

            if (params.length != 1) {
                return null;
            }
            OCFile ocFile = params[0];
            String storagePath = ocFile.getStoragePath();
            try {

                int maxDownScale = 3;   // could be a parameter passed to doInBackground(...)
                Point screenSize = DisplayUtils.getScreenSize(getActivity());
                int minWidth = screenSize.x;
                int minHeight = screenSize.y;
                for (int i = 0; i < maxDownScale && bitmapResult == null && drawableResult == null; i++) {

                    if (ocFile.getMimetype().equalsIgnoreCase(MIME_TYPE_SVG)) {
                        if (isCancelled()) {
                            return null;
                        }

                        try {
                            SVG svg = SVG.getFromInputStream(new FileInputStream(storagePath));
                            drawableResult = new PictureDrawable(svg.renderToPicture());

                            if (isCancelled()) {
                                return new LoadImage(null, drawableResult, ocFile);
                            }
                        } catch (FileNotFoundException e) {
                            mErrorMessageId = R.string.common_error_unknown;
                            Log_OC.e(TAG, "File not found trying to load " + getFile().getStoragePath(), e);
                        } catch (SVGParseException e) {
                            mErrorMessageId = R.string.common_error_unknown;
                            Log_OC.e(TAG, "Couldn't parse SVG " + getFile().getStoragePath(), e);
                        }
                    } else {
                        if (isCancelled()) {
                            return null;
                        }

                        try {
                            bitmapResult = BitmapUtils.decodeSampledBitmapFromFile(storagePath, minWidth,
                                    minHeight);

                            if (isCancelled()) {
                                return new LoadImage(bitmapResult, null, ocFile);
                            }

                            if (bitmapResult == null) {
                                mErrorMessageId = R.string.preview_image_error_unknown_format;
                                Log_OC.e(TAG, "File could not be loaded as a bitmap: " + storagePath);
                                break;
                            } else {
                                if (ocFile.getMimetype().equalsIgnoreCase("image/jpeg")) {
                                    // Rotate image, obeying exif tag.
                                    bitmapResult = BitmapUtils.rotateImage(bitmapResult, storagePath);
                                }
                            }

                        } catch (OutOfMemoryError e) {
                            mErrorMessageId = R.string.common_error_out_memory;
                            if (i < maxDownScale - 1) {
                                Log_OC.w(TAG, "Out of memory rendering file " + storagePath + " ; scaling down");
                                minWidth = minWidth / 2;
                                minHeight = minHeight / 2;

                            } else {
                                Log_OC.w(TAG, "Out of memory rendering file " + storagePath + " ; failing");
                            }
                            if (bitmapResult != null) {
                                bitmapResult.recycle();
                            }
                            bitmapResult = null;
                        }
                    }
                }

            } catch (NoSuchFieldError e) {
                mErrorMessageId = R.string.common_error_unknown;
                Log_OC.e(TAG, "Error from access to non-existing field despite protection; file "
                        + storagePath, e);

            } catch (Throwable t) {
                mErrorMessageId = R.string.common_error_unknown;
                Log_OC.e(TAG, "Unexpected error loading " + getFile().getStoragePath(), t);

            }

            return new LoadImage(bitmapResult, drawableResult, ocFile);
        }

        @Override
        protected void onCancelled(LoadImage result) {
            if (result != null && result.bitmap != null) {
                result.bitmap.recycle();
            }
        }

        @Override
        protected void onPostExecute(LoadImage result) {
            if (result.bitmap != null || result.drawable != null) {
                showLoadedImage(result);
            } else {
                showErrorMessage(mErrorMessageId);
            }
            if (result.bitmap != null && mBitmap != result.bitmap) {
                // unused bitmap, release it! (just in case)
                result.bitmap.recycle();
            }
        }

        private void showLoadedImage(LoadImage result) {
            final PhotoView imageView = mImageViewRef.get();
            Bitmap bitmap = result.bitmap;

            if (imageView != null && bitmap != null) {
                Log_OC.d(TAG, "Showing image with resolution " + bitmap.getWidth() + "x" +
                        bitmap.getHeight());

                if (result.ocFile.getMimetype().equalsIgnoreCase(MIME_TYPE_PNG) ||
                        result.ocFile.getMimetype().equalsIgnoreCase(MIME_TYPE_SVG) ||
                        result.ocFile.getMimetype().equalsIgnoreCase(MIME_TYPE_GIF)) {
                    if (getResources() != null) {
                        imageView.setImageDrawable(generateCheckerboardLayeredDrawable(result, bitmap));
                    } else {
                        imageView.setImageBitmap(bitmap);
                    }
                } else {
                    imageView.setImageBitmap(bitmap);
                }

                imageView.setVisibility(View.VISIBLE);
                mBitmap = bitmap;  // needs to be kept for recycling when not useful
            }

            mMultiView.setVisibility(View.GONE);
            if (getResources() != null) {
                mImageView.setBackgroundColor(getResources().getColor(R.color.black));
            }
            mImageView.setVisibility(View.VISIBLE);

        }
    }

    private LayerDrawable generateCheckerboardLayeredDrawable(LoadImage result, Bitmap bitmap) {
        Resources r = getResources();
        Drawable[] layers = new Drawable[2];
        layers[0] = r.getDrawable(R.color.white);
        Drawable bitmapDrawable;

        if (result.ocFile.getMimetype().equalsIgnoreCase(MIME_TYPE_PNG)) {
            bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
        } else if (result.ocFile.getMimetype().equalsIgnoreCase(MIME_TYPE_SVG)) {
            bitmapDrawable = result.drawable;
        } else if (result.ocFile.getMimetype().equalsIgnoreCase(MIME_TYPE_GIF)) {
            try {
                bitmapDrawable = new GifDrawable(result.ocFile.getStoragePath());
            } catch (IOException exception) {
                bitmapDrawable = result.drawable;
            }
        } else {
            bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
        }

        layers[1] = bitmapDrawable;
        LayerDrawable layerDrawable = new LayerDrawable(layers);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Activity activity = getActivity();
            if (activity != null) {
                int bitmapWidth;
                int bitmapHeight;

                if (result.ocFile.getMimetype().equalsIgnoreCase(MIME_TYPE_PNG)) {
                    bitmapWidth = convertDpToPixel(bitmap.getWidth(),
                            getActivity());
                    bitmapHeight = convertDpToPixel(bitmap.getHeight(),
                            getActivity());
                    layerDrawable.setLayerSize(0, bitmapWidth, bitmapHeight);
                    layerDrawable.setLayerSize(1, bitmapWidth, bitmapHeight);
                } else {
                    bitmapWidth = convertDpToPixel(bitmapDrawable.getIntrinsicWidth(),
                            getActivity());
                    bitmapHeight = convertDpToPixel(bitmapDrawable.getIntrinsicHeight(),
                            getActivity());
                    layerDrawable.setLayerSize(0, bitmapWidth,
                            bitmapHeight);
                    layerDrawable.setLayerSize(1, bitmapWidth,
                            bitmapHeight);
                }
            }
        }

        return layerDrawable;
    }

    private void showErrorMessage(@StringRes int errorMessageId) {
        mImageView.setBackgroundColor(Color.TRANSPARENT);
        setMessageForMultiList(R.string.preview_sorry, errorMessageId, R.drawable.file_image);
    }

    private void setMultiListLoadingMessage() {
        if (mMultiView != null) {
            mMultiListHeadline.setText(R.string.file_list_loading);
            mMultiListMessage.setText("");

            mMultiListIcon.setVisibility(View.GONE);
            mMultiListProgress.setVisibility(View.VISIBLE);
        }
    }

    private void setMessageForMultiList(@StringRes int headline, @StringRes int message, @DrawableRes int icon) {
        if (mMultiListContainer != null && mMultiListMessage != null) {
            mMultiListHeadline.setText(headline);
            mMultiListMessage.setText(message);
            mMultiListIcon.setImageResource(icon);

            mMultiView.setBackgroundColor(Color.BLACK);
            mMultiListHeadline.setTextColor(getResources().getColor(R.color.standard_grey));
            mMultiListMessage.setTextColor(getResources().getColor(R.color.standard_grey));

            mMultiListMessage.setVisibility(View.VISIBLE);
            mMultiListIcon.setVisibility(View.VISIBLE);
            mMultiListProgress.setVisibility(View.GONE);
        }
    }

    public void setErrorPreviewMessage() {
        try {
            if (getActivity() != null) {
                Snackbar.make(mMultiView, R.string.resized_image_not_possible_download, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.common_yes, v ->
                                ((PreviewImageActivity) getActivity())
                                        .requestForDownload(getFile())).show();
            } else {
                Snackbar.make(mMultiView, R.string.resized_image_not_possible, Snackbar.LENGTH_INDEFINITE).show();
            }
        } catch (IllegalArgumentException e) {
            Log_OC.d(TAG, e.getMessage());
        }
    }

    public void setNoConnectionErrorMessage() {
        try {
            Snackbar.make(mMultiView, R.string.auth_no_net_conn_title, Snackbar.LENGTH_LONG).show();
        } catch (IllegalArgumentException e) {
            Log_OC.d(TAG, e.getMessage());
        }
    }

    /**
     * Helper method to test if an {@link OCFile} can be passed to a {@link PreviewImageFragment}
     * to be previewed.
     *
     * @param file File to test if can be previewed.
     * @return 'True' if the file can be handled by the fragment.
     */
    public static boolean canBePreviewed(OCFile file) {
        return (file != null && MimeTypeUtil.isImage(file));
    }


    /**
     * Finishes the preview
     */
    private void finish() {
        Activity container = getActivity();
        container.finish();
    }

    private void togglePreviewImageFullScreen() {
        ((PreviewImageActivity) getActivity()).toggleFullScreen();
        toggleImageBackground();
    }

    private void toggleImageBackground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getFile() != null
                && (getFile().getMimetype().equalsIgnoreCase(MIME_TYPE_PNG) ||
                getFile().getMimetype().equalsIgnoreCase(MIME_TYPE_SVG)) && getActivity() != null
                && getActivity() instanceof PreviewImageActivity && getResources() != null) {
            PreviewImageActivity previewImageActivity = (PreviewImageActivity) getActivity();

            if (mImageView.getDrawable() instanceof LayerDrawable) {
                LayerDrawable layerDrawable = (LayerDrawable) mImageView.getDrawable();
                Drawable layerOne;

                if (previewImageActivity.getSystemUIVisible()) {
                    layerOne = getResources().getDrawable(R.color.white);
                } else {
                    layerOne = getResources().getDrawable(R.drawable.backrepeat);
                }

                layerDrawable.setDrawableByLayerId(layerDrawable.getId(0), layerOne);

                mImageView.setImageDrawable(layerDrawable);
                mImageView.invalidate();
            }
        }
    }


    private static int convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) (dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }


    public PhotoView getImageView() {
        return mImageView;
    }

    private class LoadImage {
        private final Bitmap bitmap;
        private final Drawable drawable;
        private final OCFile ocFile;

        LoadImage(Bitmap bitmap, Drawable drawable, OCFile ocFile) {
            this.bitmap = bitmap;
            this.drawable = drawable;
            this.ocFile = ocFile;
        }

    }

}
