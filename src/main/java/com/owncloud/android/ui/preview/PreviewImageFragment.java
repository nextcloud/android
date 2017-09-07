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
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.AnalyticsUtils;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import third_parties.michaelOrtiz.TouchImageViewCustom;


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

    public static final String EXTRA_FILE = "FILE";

    private static final String ARG_FILE = "FILE";
    private static final String ARG_IGNORE_FIRST = "IGNORE_FIRST";

    private static final String SCREEN_NAME = "Image Preview";

    private TouchImageViewCustom mImageView;
    private RelativeLayout mMultiView;

    protected LinearLayout mMultiListContainer;
    protected TextView mMultiListMessage;
    protected TextView mMultiListHeadline;
    protected ImageView mMultiListIcon;
    protected ProgressBar mMultiListProgress;

    public Bitmap mBitmap = null;

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
    public static PreviewImageFragment newInstance(@NonNull OCFile imageFile, boolean ignoreFirstSavedState) {
        PreviewImageFragment frag = new PreviewImageFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, imageFile);
        args.putBoolean(ARG_IGNORE_FIRST, ignoreFirstSavedState);
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
        setHasOptionsMenu(true);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.preview_image_fragment, container, false);
        mImageView = (TouchImageViewCustom) view.findViewById(R.id.image);
        mImageView.setVisibility(View.GONE);

        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((PreviewImageActivity) getActivity()).toggleFullScreen();
                toggleImageBackground();
            }
        });

        mImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((PreviewImageActivity) getActivity()).toggleFullScreen();
                toggleImageBackground();
            }
        });

        mMultiView = (RelativeLayout) view.findViewById(R.id.multi_view);

        setupMultiView(view);
        setMultiListLoadingMessage();

        return view;
    }

    protected void setupMultiView(View view) {
        mMultiListContainer = (LinearLayout) view.findViewById(R.id.empty_list_view);
        mMultiListMessage = (TextView) view.findViewById(R.id.empty_list_view_text);
        mMultiListHeadline = (TextView) view.findViewById(R.id.empty_list_view_headline);
        mMultiListIcon = (ImageView) view.findViewById(R.id.empty_list_icon);
        mMultiListProgress = (ProgressBar) view.findViewById(R.id.empty_list_progress);
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PreviewImageFragment.EXTRA_FILE, getFile());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getFile() == null || !getFile().isDown()) {
            showErrorMessage(R.string.preview_image_error_no_local_file);
        } else {
            mLoadBitmapTask = new LoadBitmapTask(mImageView);
            mLoadBitmapTask.execute(getFile());
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
                    getActivity()
            );
            mf.filter(menu);
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

        if(getFile().isSharedWithMe() && !getFile().canReshare()){
            // additional restriction for this fragment
            item = menu.findItem(R.id.action_share_file);
            if(item != null){
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
            case R.id.action_share_file:
                mContainerActivity.getFileOperationsHelper().showShareFile(getFile());
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

            case R.id.action_send_file:
                mContainerActivity.getFileOperationsHelper().sendDownloadedFile(getFile());
                return true;

            case R.id.action_sync_file:
                mContainerActivity.getFileOperationsHelper().syncFile(getFile());
                return true;

            case R.id.action_set_as_wallpaper:
                mContainerActivity.getFileOperationsHelper().setPictureAs(getFile());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void seeDetails() {
        mContainerActivity.showDetails(getFile());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            AnalyticsUtils.setCurrentScreenName(getActivity(), SCREEN_NAME, TAG);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @SuppressFBWarnings("Dm")
    @Override
    public void onDestroy() {
        if (mBitmap != null) {
            mBitmap.recycle();
            System.gc();
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
        private final WeakReference<ImageViewCustom> mImageViewRef;

        /**
         * Error message to show when a load fails
         */
        private int mErrorMessageId;


        /**
         * Constructor.
         *
         * @param imageView Target {@link ImageView} where the bitmap will be loaded into.
         */
        public LoadBitmapTask(ImageViewCustom imageView) {
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

                    if (ocFile.getMimetype().equalsIgnoreCase("image/svg+xml")) {
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
            final ImageViewCustom imageView = mImageViewRef.get();
            Bitmap bitmap = result.bitmap;


            if (imageView != null) {
                if (bitmap != null) {
                    Log_OC.d(TAG, "Showing image with resolution " + bitmap.getWidth() + "x" +
                            bitmap.getHeight());
                }

                if (result.ocFile.getMimetype().equalsIgnoreCase("image/png") ||
                        result.ocFile.getMimetype().equals("image/svg+xml")) {
                    if (getResources() != null) {
                        Resources r = getResources();
                        Drawable[] layers = new Drawable[2];
                        layers[0] = r.getDrawable(R.color.white);
                        Drawable bitmapDrawable;
                        if (result.ocFile.getMimetype().equalsIgnoreCase("image/png") ) {
                            bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
                        } else {
                            bitmapDrawable = result.drawable;
                        }
                        layers[1] = bitmapDrawable;
                        LayerDrawable layerDrawable = new LayerDrawable(layers);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (result.ocFile.getMimetype().equalsIgnoreCase("image/png")) {
                                layerDrawable.setLayerHeight(0, convertDpToPixel(bitmap.getHeight(), getActivity()));
                                layerDrawable.setLayerHeight(1, convertDpToPixel(bitmap.getHeight(), getActivity()));
                                layerDrawable.setLayerWidth(0, convertDpToPixel(bitmap.getWidth(), getActivity()));
                                layerDrawable.setLayerWidth(1, convertDpToPixel(bitmap.getWidth(), getActivity()));
                            } else {
                                layerDrawable.setLayerHeight(0, convertDpToPixel(bitmapDrawable.getIntrinsicHeight(),
                                        getActivity()));
                                layerDrawable.setLayerHeight(1, convertDpToPixel(bitmapDrawable.getIntrinsicHeight(),
                                        getActivity()));
                                layerDrawable.setLayerWidth(0, convertDpToPixel(bitmapDrawable.getIntrinsicWidth(),
                                        getActivity()));
                                layerDrawable.setLayerWidth(1, convertDpToPixel(bitmapDrawable.getIntrinsicWidth(),
                                        getActivity()));
                            }
                        }
                        imageView.setImageDrawable(layerDrawable);
                    } else {
                        imageView.setImageBitmap(bitmap);
                    }
                }

                if (result.ocFile.getMimetype().equalsIgnoreCase("image/gif")) {
                    imageView.setGIFImageFromStoragePath(result.ocFile.getStoragePath());
                } else if (!result.ocFile.getMimetype().equalsIgnoreCase("image/png") &&
                        !result.ocFile.getMimetype().equals("image/svg+xml")) {
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

    public void setMessageForMultiList(@StringRes int headline, @StringRes int message, @DrawableRes int icon) {
        if (mMultiListContainer != null && mMultiListMessage != null) {
            mMultiListHeadline.setText(headline);
            mMultiListMessage.setText(message);
            mMultiListIcon.setImageResource(icon);

            mMultiListMessage.setVisibility(View.VISIBLE);
            mMultiListIcon.setVisibility(View.VISIBLE);
            mMultiListProgress.setVisibility(View.GONE);
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

    private void toggleImageBackground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getFile() != null
                && (getFile().getMimetype().equalsIgnoreCase("image/png") ||
                getFile().getMimetype().equalsIgnoreCase("image/svg+xml")) && getActivity() != null
                && getActivity() instanceof PreviewImageActivity && getResources() != null) {
            PreviewImageActivity previewImageActivity = (PreviewImageActivity) getActivity();
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


    private static int convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int px = (int) (dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }


    public TouchImageViewCustom getImageView() {
        return mImageView;
    }

    private class LoadImage {
        private Bitmap bitmap;
        private Drawable drawable;
        private OCFile ocFile;

        public LoadImage(Bitmap bitmap, Drawable drawable, OCFile ocFile) {
            this.bitmap = bitmap;
            this.drawable = drawable;
            this.ocFile = ocFile;
        }

    }

}
