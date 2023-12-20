/*
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2015 ownCloud Inc.
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PictureDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.ui.fileactions.FileActionsBottomSheet;
import com.nextcloud.utils.extensions.BundleExtensionsKt;
import com.nextcloud.utils.extensions.ExtensionsKt;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.PreviewImageFragmentBinding;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeType;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import pl.droidsonroids.gif.GifDrawable;

import static com.owncloud.android.datamodel.ThumbnailsCacheManager.PREFIX_THUMBNAIL;


/**
 * This fragment shows a preview of a downloaded image.
 * <p>
 * Trying to get an instance with a NULL {@link OCFile} will produce an {@link IllegalStateException}.
 * <p>
 * If the {@link OCFile} passed is not downloaded, an {@link IllegalStateException} is generated on instantiation too.
 */
public class PreviewImageFragment extends FileFragment implements Injectable {

    private static final String EXTRA_FILE = "FILE";
    private static final String EXTRA_ZOOM = "ZOOM";

    private static final String ARG_FILE = "FILE";
    private static final String ARG_IGNORE_FIRST = "IGNORE_FIRST";
    private static final String ARG_SHOW_RESIZED_IMAGE = "SHOW_RESIZED_IMAGE";
    private static final String MIME_TYPE_PNG = "image/png";
    private static final String MIME_TYPE_GIF = "image/gif";
    private static final String MIME_TYPE_SVG = "image/svg+xml";

    private Boolean showResizedImage;
    private Bitmap bitmap;

    private static final String TAG = PreviewImageFragment.class.getSimpleName();

    private boolean ignoreFirstSavedState;
    private LoadBitmapTask loadBitmapTask;

    @Inject ConnectivityService connectivityService;
    @Inject UserAccountManager accountManager;
    @Inject BackgroundJobManager backgroundJobManager;
    @Inject ViewThemeUtils viewThemeUtils;

    private PreviewImageFragmentBinding binding;

    /**
     * Public factory method to create a new fragment that previews an image.
     * <p>
     * Android strongly recommends keep the empty constructor of fragments as the only public constructor, and use
     * {@link #setArguments(Bundle)} to set the needed arguments.
     * <p>
     * This method hides to client objects the need of doing the construction in two steps.
     *
     * @param imageFile             An {@link OCFile} to preview as an image in the fragment
     * @param ignoreFirstSavedState Flag to work around an unexpected behaviour of {@link FragmentStatePagerAdapter} ;
     *                                                           TODO better solution
     */
    public static PreviewImageFragment newInstance(@NonNull OCFile imageFile,
                                                   boolean ignoreFirstSavedState,
                                                   boolean showResizedImage) {
        PreviewImageFragment frag = new PreviewImageFragment();
        frag.showResizedImage = showResizedImage;
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, imageFile);
        args.putBoolean(ARG_IGNORE_FIRST, ignoreFirstSavedState);
        args.putBoolean(ARG_SHOW_RESIZED_IMAGE, showResizedImage);
        frag.setArguments(args);
        return frag;
    }

    /**
     * Creates an empty fragment for image previews.
     * <p>
     * MUST BE KEPT: the system uses it when tries to re-instantiate a fragment automatically (for instance, when the
     * device is turned a aside).
     * <p>
     * DO NOT CALL IT: an {@link OCFile} and {@link User} must be provided for a successful construction
     */
    public PreviewImageFragment() {
        ignoreFirstSavedState = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();

        if (args == null) {
            throw new IllegalArgumentException("Arguments may not be null!");
        }

        setFile(BundleExtensionsKt.getParcelableArgument(args, ARG_FILE, OCFile.class));
        // TODO better in super, but needs to check ALL the class extending FileFragment;
        // not right now

        ignoreFirstSavedState = args.getBoolean(ARG_IGNORE_FIRST);
        showResizedImage = args.getBoolean(ARG_SHOW_RESIZED_IMAGE);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        binding = PreviewImageFragmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        binding.image.setVisibility(View.GONE);

        view.setOnClickListener(v -> togglePreviewImageFullScreen());

        binding.image.setOnClickListener(v -> togglePreviewImageFullScreen());
        checkLivePhotoAvailability();
        setMultiListLoadingMessage();

        return view;
    }

    private void checkLivePhotoAvailability() {
        OCFile livePhotoVideo = getFile().livePhotoVideo;

        if (livePhotoVideo == null) return;

        binding.livePhotoIndicator.setVisibility(View.VISIBLE);
        ExtensionsKt.clickWithDebounce(binding.livePhotoIndicator, 4000L, () -> {
            playLivePhoto(livePhotoVideo);
            return null;
        });
    }

    private void hideActionBar() {
        PreviewImageActivity activity = (PreviewImageActivity) requireActivity();
        activity.toggleActionBarVisibility(true);
    }

    private void playLivePhoto(OCFile file) {
        if (file == null) {
            return;
        }

        hideActionBar();

        Fragment mediaFragment = PreviewMediaFragment.newInstance(file, accountManager.getUser(), 0, true, true);
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.top, mediaFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            if (!ignoreFirstSavedState) {
                OCFile file = BundleExtensionsKt.getParcelableArgument(savedInstanceState, EXTRA_FILE, OCFile.class);
                setFile(file);
                binding.image.setScale(Math.min(binding.image.getMaximumScale(), savedInstanceState.getFloat(EXTRA_ZOOM)));
            } else {
                ignoreFirstSavedState = false;
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putFloat(EXTRA_ZOOM, binding.image.getScale());
        outState.putParcelable(EXTRA_FILE, getFile());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getFile() != null) {
            binding.image.setTag(getFile().getFileId());

            Point screenSize = DisplayUtils.getScreenSize(getActivity());
            int width = screenSize.x;
            int height = screenSize.y;

            // show thumbnail while loading image
            binding.image.setVisibility(View.GONE);
            binding.emptyListProgress.setVisibility(View.VISIBLE);

            Bitmap thumbnail = getThumbnailBitmap(getFile());
            if (thumbnail != null) {
                binding.shimmer.setVisibility(View.VISIBLE);
                binding.shimmerThumbnail.setImageBitmap(thumbnail);
                binding.image.setVisibility(View.GONE);
                bitmap = thumbnail;
            } else {
                thumbnail = ThumbnailsCacheManager.mDefaultImg;
            }

            if (showResizedImage) {
                Bitmap resizedImage = getResizedBitmap(getFile(), width, height);

                if (resizedImage != null && !getFile().isUpdateThumbnailNeeded()) {
                    binding.image.setImageBitmap(resizedImage);
                    binding.image.setVisibility(View.VISIBLE);
                    binding.emptyListView.setVisibility(View.GONE);
                    binding.emptyListProgress.setVisibility(View.GONE);
                    binding.image.setBackgroundColor(getResources().getColor(R.color.background_color_inverse));

                    bitmap = resizedImage;
                } else {
                    // generate new resized image
                    if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(getFile(), binding.image) &&
                        containerActivity.getStorageManager() != null) {
                        final ThumbnailsCacheManager.ResizedImageGenerationTask task =
                            new ThumbnailsCacheManager.ResizedImageGenerationTask(this,
                                                                                  binding.image,
                                                                                  binding.emptyListProgress,
                                                                                  containerActivity.getStorageManager(),
                                                                                  connectivityService,
                                                                                  containerActivity.getStorageManager().getUser(),
                                                                                  getResources().getColor(R.color.background_color_inverse)
                            );
                        if (resizedImage == null) {
                            resizedImage = thumbnail;
                        }
                        final ThumbnailsCacheManager.AsyncResizedImageDrawable asyncDrawable =
                            new ThumbnailsCacheManager.AsyncResizedImageDrawable(
                                MainApp.getAppContext().getResources(),
                                resizedImage,
                                task
                            );
                        binding.image.setImageDrawable(asyncDrawable);
                        task.execute(getFile());
                    }
                }
            } else {
                loadBitmapTask = new LoadBitmapTask(binding.image, binding.emptyListView, binding.emptyListProgress);
                binding.image.setVisibility(View.GONE);
                binding.emptyListView.setVisibility(View.GONE);
                binding.emptyListProgress.setVisibility(View.VISIBLE);
                loadBitmapTask.execute(getFile());
            }
        } else {
            showErrorMessage(R.string.preview_image_error_no_local_file);
        }
    }

    private @Nullable
    Bitmap getResizedBitmap(OCFile file, int width, int height) {
        Bitmap cachedImage = null;
        int scaledWidth = width;
        int scaledHeight = height;

        for (int i = 0; i < 3 && cachedImage == null; i++) {
            try {
                cachedImage = ThumbnailsCacheManager.getScaledBitmapFromDiskCache(
                    ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE + file.getRemoteId(),
                    scaledWidth,
                    scaledHeight);
            } catch (OutOfMemoryError e) {
                scaledWidth = scaledWidth / 2;
                scaledHeight = scaledHeight / 2;
            }
        }

        return cachedImage;
    }

    private @Nullable
    Bitmap getThumbnailBitmap(OCFile file) {
        return ThumbnailsCacheManager.getBitmapFromDiskCache(PREFIX_THUMBNAIL + file.getRemoteId());
    }

    @Override
    public void onStop() {
        Log_OC.d(TAG, "onStop starts");
        if (loadBitmapTask != null) {
            loadBitmapTask.cancel(true);
            loadBitmapTask = null;
        }
        super.onStop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.custom_menu_placeholder, menu);
        final MenuItem item = menu.findItem(R.id.custom_menu_placeholder_item);
        item.setIcon(viewThemeUtils.platform.colorDrawable(item.getIcon(), ContextCompat.getColor(requireContext(), R.color.white)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.custom_menu_placeholder_item) {
            final OCFile file = getFile();
            if (containerActivity.getStorageManager() != null && file != null) {
                // Update the file
                final OCFile updatedFile = containerActivity.getStorageManager().getFileById(file.getFileId());
                setFile(updatedFile);

                final OCFile fileNew = getFile();
                if (fileNew != null) {
                    showFileActions(file);
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showFileActions(OCFile file) {
        final List<Integer> additionalFilter = new ArrayList<>(
            Arrays.asList(
                R.id.action_rename_file,
                R.id.action_sync_file,
                R.id.action_move_or_copy,
                R.id.action_favorite,
                R.id.action_unset_favorite,
                R.id.action_pin_to_homescreen
                         ));
        if (getFile() != null && getFile().isSharedWithMe() && !getFile().canReshare()) {
            additionalFilter.add(R.id.action_send_share_file);
        }
        final FragmentManager fragmentManager = getChildFragmentManager();
        FileActionsBottomSheet.newInstance(file, false, additionalFilter)
            .setResultListener(fragmentManager, this, this::onFileActionChosen)
            .show(fragmentManager, "actions");
    }

    /**
     * {@inheritDoc}
     */
    public void onFileActionChosen(final int itemId) {
        if (itemId == R.id.action_send_share_file) {
            if (getFile().isSharedWithMe() && !getFile().canReshare()) {
                Snackbar.make(requireView(), R.string.resharing_is_not_allowed, Snackbar.LENGTH_LONG).show();
            } else {
                containerActivity.getFileOperationsHelper().sendShareFile(getFile());
            }
        } else if (itemId == R.id.action_open_file_with) {
            openFile();
        } else if (itemId == R.id.action_remove_file) {
            RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstance(getFile());
            dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
        } else if (itemId == R.id.action_see_details) {
            seeDetails();
        } else if (itemId == R.id.action_download_file || itemId == R.id.action_sync_file) {
            containerActivity.getFileOperationsHelper().syncFile(getFile());
        }else if(itemId == R.id.action_cancel_sync){
            containerActivity.getFileOperationsHelper().cancelTransference(getFile());
        } else if (itemId == R.id.action_set_as_wallpaper) {
            containerActivity.getFileOperationsHelper().setPictureAs(getFile(), getImageView());
        } else if (itemId == R.id.action_export_file) {
            ArrayList<OCFile> list = new ArrayList<>();
            list.add(getFile());
            containerActivity.getFileOperationsHelper().exportFiles(list,
                                                                    getContext(),
                                                                    getView(),
                                                                    backgroundJobManager);
        } else if (itemId == R.id.action_edit) {
            ((PreviewImageActivity) requireActivity()).startImageEditor(getFile());
        }
    }

    private void seeDetails() {
        containerActivity.showDetails(getFile());
    }

    @SuppressFBWarnings("Dm")
    @Override
    public void onDestroy() {
        if (bitmap != null) {
            bitmap.recycle();
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
        containerActivity.getFileOperationsHelper().openFile(getFile());
        finish();
    }

    private class LoadBitmapTask extends AsyncTask<OCFile, Void, LoadImage> {
        private static final int PARAMS_LENGTH = 1;

        /**
         * Weak reference to the target {@link ImageView} where the bitmap will be loaded into.
         * <p>
         * Using a weak reference will avoid memory leaks if the target ImageView is retired from memory before the load
         * finishes.
         */
        private final WeakReference<PhotoView> imageViewRef;
        private final WeakReference<LinearLayout> infoViewRef;
        private final WeakReference<FrameLayout> progressViewRef;

        /**
         * Error message to show when a load fails.
         */
        private int mErrorMessageId;

        /**
         * Constructor.
         *
         * @param imageView Target {@link ImageView} where the bitmap will be loaded into.
         */
        LoadBitmapTask(PhotoView imageView, LinearLayout infoView, FrameLayout progressView) {
            imageViewRef = new WeakReference<>(imageView);
            infoViewRef = new WeakReference<>(infoView);
            progressViewRef = new WeakReference<>(progressView);
        }

        @Override
        protected LoadImage doInBackground(OCFile... params) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE);

            if (params.length != PARAMS_LENGTH) {
                return null;
            }

            Bitmap bitmapResult = null;
            Drawable drawableResult = null;
            OCFile ocFile = params[0];
            String storagePath = ocFile.getStoragePath();
            try {
                int maxDownScale = 3;   // could be a parameter passed to doInBackground(...)
                Point screenSize = DisplayUtils.getScreenSize(getActivity());
                int minWidth = screenSize.x;
                int minHeight = screenSize.y;
                for (int i = 0; i < maxDownScale && bitmapResult == null && drawableResult == null; i++) {

                    if (MIME_TYPE_SVG.equalsIgnoreCase(ocFile.getMimeType())) {
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
                                if (MimeType.JPEG.equalsIgnoreCase(ocFile.getMimeType())) {
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
            if (result.bitmap != null && bitmap != result.bitmap) {
                // unused bitmap, release it! (just in case)
                result.bitmap.recycle();
            }
        }

        private void showLoadedImage(LoadImage result) {
            final PhotoView imageView = imageViewRef.get();
            Bitmap bitmap = result.bitmap;
            Drawable drawable = result.drawable;

            if (imageView != null) {
                if (bitmap != null) {
                    Log_OC.d(TAG, "Showing image with resolution " + bitmap.getWidth() + "x" +
                        bitmap.getHeight());

                    if (MIME_TYPE_PNG.equalsIgnoreCase(result.ocFile.getMimeType()) ||
                        MIME_TYPE_GIF.equalsIgnoreCase(result.ocFile.getMimeType())) {
                        getResources();
                        imageView.setImageDrawable(generateCheckerboardLayeredDrawable(result, bitmap));
                    } else {
                        imageView.setImageBitmap(bitmap);
                    }

                    PreviewImageFragment.this.bitmap = bitmap;  // needs to be kept for recycling when not useful
                } else {
                    if (drawable != null
                        && MIME_TYPE_SVG.equalsIgnoreCase(result.ocFile.getMimeType())) {
                        getResources();
                        imageView.setImageDrawable(generateCheckerboardLayeredDrawable(result, null));
                    }
                }
                final LinearLayout infoView = infoViewRef.get();
                if (infoView != null) {
                    infoView.setVisibility(View.GONE);
                }

                final FrameLayout progressView = progressViewRef.get();
                if (progressView != null) {
                    progressView.setVisibility(View.GONE);
                }
                imageView.setBackgroundColor(getResources().getColor(R.color.background_color_inverse));
                imageView.setVisibility(View.VISIBLE);
            }
        }
    }

    private LayerDrawable generateCheckerboardLayeredDrawable(LoadImage result, Bitmap bitmap) {
        Resources resources = getResources();
        Drawable[] layers = new Drawable[2];
        layers[0] = ResourcesCompat.getDrawable(resources, R.color.bg_default, null);
        Drawable bitmapDrawable;

        if (MIME_TYPE_PNG.equalsIgnoreCase(result.ocFile.getMimeType())) {
            bitmapDrawable = new BitmapDrawable(resources, bitmap);
        } else if (MIME_TYPE_SVG.equalsIgnoreCase(result.ocFile.getMimeType())) {
            bitmapDrawable = result.drawable;
        } else if (MIME_TYPE_GIF.equalsIgnoreCase(result.ocFile.getMimeType())) {
            try {
                bitmapDrawable = new GifDrawable(result.ocFile.getStoragePath());
            } catch (IOException exception) {
                bitmapDrawable = result.drawable;
            }
        } else {
            bitmapDrawable = new BitmapDrawable(resources, bitmap);
        }

        layers[1] = bitmapDrawable;
        LayerDrawable layerDrawable = new LayerDrawable(layers);

        Activity activity = getActivity();
        if (activity != null) {
            int bitmapWidth;
            int bitmapHeight;

            if (MIME_TYPE_PNG.equalsIgnoreCase(result.ocFile.getMimeType())) {
                bitmapWidth = convertDpToPixel(bitmap.getWidth(), getActivity());
                bitmapHeight = convertDpToPixel(bitmap.getHeight(), getActivity());
            } else {
                bitmapWidth = convertDpToPixel(bitmapDrawable.getIntrinsicWidth(), getActivity());
                bitmapHeight = convertDpToPixel(bitmapDrawable.getIntrinsicHeight(), getActivity());
            }
            layerDrawable.setLayerSize(0, bitmapWidth, bitmapHeight);
            layerDrawable.setLayerSize(1, bitmapWidth, bitmapHeight);
        }

        return layerDrawable;
    }

    private void showErrorMessage(@StringRes int errorMessageId) {
        setSorryMessageForMultiList(errorMessageId);
    }

    private void setMultiListLoadingMessage() {
        binding.image.setVisibility(View.GONE);
        binding.emptyListView.setVisibility(View.GONE);
        binding.emptyListProgress.setVisibility(View.VISIBLE);
    }

    private void setSorryMessageForMultiList(@StringRes int message) {
        binding.emptyListViewHeadline.setText(R.string.preview_sorry);
        binding.emptyListViewText.setText(message);
        binding.emptyListIcon.setImageResource(R.drawable.file_image);

        binding.emptyListView.setBackgroundColor(getResources().getColor(R.color.bg_default));
        binding.emptyListViewHeadline.setTextColor(getResources().getColor(R.color.standard_grey));
        binding.emptyListViewText.setTextColor(getResources().getColor(R.color.standard_grey));

        binding.image.setVisibility(View.GONE);
        binding.emptyListView.setVisibility(View.VISIBLE);
        binding.emptyListProgress.setVisibility(View.GONE);
    }

    public void setErrorPreviewMessage() {
        try {
            if (getActivity() != null) {
                Snackbar.make(binding.emptyListView,
                              R.string.resized_image_not_possible_download,
                              Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.common_yes, v -> {
                                   PreviewImageActivity activity = (PreviewImageActivity) getActivity();
                                   if (activity != null) {
                                       activity.requestForDownload(getFile());
                                   } else if (getContext() != null) {
                                       Snackbar.make(binding.emptyListView,
                                                     getResources().getString(R.string.could_not_download_image),
                                                     Snackbar.LENGTH_INDEFINITE).show();
                                   }
                               }
                              ).show();
            }
        } catch (IllegalArgumentException e) {
            Log_OC.d(TAG, e.getMessage());
        }
    }

    public void setNoConnectionErrorMessage() {
        try {
            Snackbar.make(binding.emptyListView, R.string.auth_no_net_conn_title, Snackbar.LENGTH_LONG).show();
        } catch (IllegalArgumentException e) {
            Log_OC.d(TAG, e.getMessage());
        }
    }

    /**
     * Helper method to test if an {@link OCFile} can be passed to a {@link PreviewImageFragment} to be previewed.
     *
     * @param file File to test if can be previewed.
     * @return 'True' if the file can be handled by the fragment.
     */
    public static boolean canBePreviewed(OCFile file) {
        return file != null && MimeTypeUtil.isImage(file);
    }

    /**
     * Finishes the preview
     */
    private void finish() {
        Activity container = getActivity();
        if (container != null) {
            container.finish();
        }
    }

    private void togglePreviewImageFullScreen() {
        Activity activity = getActivity();

        if (activity != null) {
            ((PreviewImageActivity) activity).toggleFullScreen();
        }
        toggleImageBackground();
    }

    private void toggleImageBackground() {
        if (getFile() != null && (MIME_TYPE_PNG.equalsIgnoreCase(getFile().getMimeType()) ||
            MIME_TYPE_SVG.equalsIgnoreCase(getFile().getMimeType())) && getActivity() != null &&
            getActivity() instanceof PreviewImageActivity) {
            PreviewImageActivity previewImageActivity = (PreviewImageActivity) getActivity();

            if (binding.image.getDrawable() instanceof LayerDrawable) {
                LayerDrawable layerDrawable = (LayerDrawable) binding.image.getDrawable();
                Drawable layerOne;

                if (previewImageActivity.isSystemUIVisible()) {
                    layerOne = ResourcesCompat.getDrawable(getResources(), R.color.bg_default, null);
                } else {
                    layerOne = ResourcesCompat.getDrawable(getResources(), R.drawable.backrepeat, null);
                }

                layerDrawable.setDrawableByLayerId(layerDrawable.getId(0), layerOne);

                binding.image.setImageDrawable(layerDrawable);
                binding.image.invalidate();
            }
        }
    }

    private static int convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) (dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public PhotoView getImageView() {
        return binding.image;
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
