/* ownCloud Android client application
 *   Copyright (C) 2012-2014 ownCloud Inc. 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.ui.preview;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFileDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.TouchImageViewCustom;



/**
 * This fragment shows a preview of a downloaded image.
 * 
 * Trying to get an instance with NULL {@link OCFile} or ownCloud {@link Account} values will produce an {@link IllegalStateException}.
 * 
 * If the {@link OCFile} passed is not downloaded, an {@link IllegalStateException} is generated on instantiation too.
 * 
 * @author David A. Velasco
 */
public class PreviewImageFragment extends FileFragment {

    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";

    private View mView;
    private Account mAccount;
    private TouchImageViewCustom mImageView;
    private TextView mMessageView;
    private ProgressBar mProgressWheel;

    public Bitmap mBitmap = null;
    
    private static final String TAG = PreviewImageFragment.class.getSimpleName();

    private boolean mIgnoreFirstSavedState;
    
    private LoadBitmapTask mLoadBitmapTask = null;

    
    /**
     * Creates a fragment to preview an image.
     * 
     * When 'imageFile' or 'ocAccount' are null
     * 
     * @param imageFile                 An {@link OCFile} to preview as an image in the fragment
     * @param ocAccount                 An ownCloud account; needed to start downloads
     * @param ignoreFirstSavedState     Flag to work around an unexpected behaviour of {@link FragmentStatePagerAdapter}; TODO better solution 
     */
    public PreviewImageFragment(OCFile fileToDetail, Account ocAccount, boolean ignoreFirstSavedState) {
        super(fileToDetail);
        mAccount = ocAccount;
        mIgnoreFirstSavedState = ignoreFirstSavedState;
    }
    
    
    /**
     *  Creates an empty fragment for image previews.
     * 
     *  MUST BE KEPT: the system uses it when tries to reinstantiate a fragment automatically (for instance, when the device is turned a aside).
     * 
     *  DO NOT CALL IT: an {@link OCFile} and {@link Account} must be provided for a successful construction 
     */
    public PreviewImageFragment() {
        super();
        mAccount = null;
        mIgnoreFirstSavedState = false;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mView = inflater.inflate(R.layout.preview_image_fragment, container, false);
        mImageView = (TouchImageViewCustom) mView.findViewById(R.id.image);
        mImageView.setVisibility(View.GONE);
        mImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((PreviewImageActivity) getActivity()).toggleFullScreen();
            }

        });
        mMessageView = (TextView)mView.findViewById(R.id.message);
        mMessageView.setVisibility(View.GONE);
        mProgressWheel = (ProgressBar)mView.findViewById(R.id.progressWheel);
        mProgressWheel.setVisibility(View.VISIBLE);
        return mView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            if (!mIgnoreFirstSavedState) {
                OCFile file = (OCFile)savedInstanceState.getParcelable(PreviewImageFragment.EXTRA_FILE);
                setFile(file);
                mAccount = savedInstanceState.getParcelable(PreviewImageFragment.EXTRA_ACCOUNT);
            } else {
                mIgnoreFirstSavedState = false;
            }
        }
        if (getFile() == null) {
            throw new IllegalStateException("Instanced with a NULL OCFile");
        }
        if (mAccount == null) {
            throw new IllegalStateException("Instanced with a NULL ownCloud Account");
        }
        if (!getFile().isDown()) {
            throw new IllegalStateException("There is no local file to preview");
        }
    }
        

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PreviewImageFragment.EXTRA_FILE, getFile());
        outState.putParcelable(PreviewImageFragment.EXTRA_ACCOUNT, mAccount);
    }
    

    @Override
    public void onStart() {
        super.onStart();
        if (getFile() != null) {
           mLoadBitmapTask = new LoadBitmapTask(mImageView, mMessageView, mProgressWheel);
           mLoadBitmapTask.execute(new String[]{getFile().getStoragePath()});
        }
    }
    
    
    @Override
    public void onStop() {
        super.onStop();
        if (mLoadBitmapTask != null) {
            mLoadBitmapTask.cancel(true);
            mLoadBitmapTask = null;
        }
        
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
        
        if (mContainerActivity.getStorageManager() != null) {
            // Update the file
            setFile(mContainerActivity.getStorageManager().getFileById(getFile().getFileId()));
            
            FileMenuFilter mf = new FileMenuFilter(
                getFile(),
                mContainerActivity.getStorageManager().getAccount(),
                mContainerActivity,
                getSherlockActivity()
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
        
    }

    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share_file: {
                mContainerActivity.getFileOperationsHelper().shareFileWithLink(getFile());
                return true;
            }
            case R.id.action_unshare_file: {
                mContainerActivity.getFileOperationsHelper().unshareFileWithLink(getFile());
                return true;
            }
            case R.id.action_open_file_with: {
                openFile();
                return true;
            }
            case R.id.action_remove_file: {
                RemoveFileDialogFragment dialog = RemoveFileDialogFragment.newInstance(getFile());
                dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
                return true;
            }
            case R.id.action_see_details: {
                seeDetails();
                return true;
            }
            case R.id.action_send_file: {
                mContainerActivity.getFileOperationsHelper().sendDownloadedFile(getFile());
                return true;
            }
            case R.id.action_sync_file: {
                mContainerActivity.getFileOperationsHelper().syncFile(getFile());
                return true;
            }
            
            default:
                return false;
        }
    }
    

    private void seeDetails() {
        mContainerActivity.showDetails(getFile());        
    }


    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mBitmap != null) {
            mBitmap.recycle();
            System.gc();
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
    
    
    private class LoadBitmapTask extends AsyncTask<String, Void, Bitmap> {

        /**
         * Weak reference to the target {@link ImageView} where the bitmap will be loaded into.
         * 
         * Using a weak reference will avoid memory leaks if the target ImageView is retired from memory before the load finishes.
         */
        private final WeakReference<ImageViewCustom> mImageViewRef;

        /**
         * Weak reference to the target {@link TextView} where error messages will be written.
         * 
         * Using a weak reference will avoid memory leaks if the target ImageView is retired from memory before the load finishes.
         */
        private final WeakReference<TextView> mMessageViewRef;

        
        /**
         * Weak reference to the target {@link Progressbar} shown while the load is in progress.
         * 
         * Using a weak reference will avoid memory leaks if the target ImageView is retired from memory before the load finishes.
         */
        private final WeakReference<ProgressBar> mProgressWheelRef;

        
        /**
         * Error message to show when a load fails 
         */
        private int mErrorMessageId;
        
        
        /**
         * Constructor.
         * 
         * @param imageView     Target {@link ImageView} where the bitmap will be loaded into.
         */
        public LoadBitmapTask(ImageViewCustom imageView, TextView messageView, ProgressBar progressWheel) {
            mImageViewRef = new WeakReference<ImageViewCustom>(imageView);
            mMessageViewRef = new WeakReference<TextView>(messageView);
            mProgressWheelRef = new WeakReference<ProgressBar>(progressWheel);
        }
        
        
        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap result = null;
            if (params.length != 1) return result;
            String storagePath = params[0];
            try {

                if (isCancelled()) return result;
                
                File picture = new File(storagePath);

                if (picture != null) {
                    // Decode file into a bitmap in real size for being able to make zoom on 
                    // the image
                    result = BitmapFactory.decodeStream(new FlushedInputStream
                            (new BufferedInputStream(new FileInputStream(picture))));
                }

                if (isCancelled()) return result;
                
                if (result == null) {
                    mErrorMessageId = R.string.preview_image_error_unknown_format;
                    Log_OC.e(TAG, "File could not be loaded as a bitmap: " + storagePath);
                } else {
                    // Rotate image, obeying exif tag.
                    result = BitmapUtils.rotateImage(result, storagePath);
                }
                
            } catch (OutOfMemoryError e) {
                Log_OC.e(TAG, "Out of memory occured for file " + storagePath, e);

                if (isCancelled()) return result;
                
                // If out of memory error when loading or rotating image, try to load it scaled
                result = loadScaledImage(storagePath);

                if (result == null) {
                    mErrorMessageId = R.string.preview_image_error_unknown_format;
                    Log_OC.e(TAG, "File could not be loaded as a bitmap: " + storagePath);
                } else {
                    // Rotate scaled image, obeying exif tag
                    result = BitmapUtils.rotateImage(result, storagePath);
                }
                    
            } catch (NoSuchFieldError e) {
                mErrorMessageId = R.string.common_error_unknown;
                Log_OC.e(TAG, "Error from access to unexisting field despite protection; file " 
                                + storagePath, e);
                    
            } catch (Throwable t) {
                mErrorMessageId = R.string.common_error_unknown;
                Log_OC.e(TAG, "Unexpected error loading " + getFile().getStoragePath(), t);
                
            }
            
            return result;
        }
        
        @Override
        protected void onCancelled(Bitmap result) {
            if (result != null) {
                result.recycle();
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            hideProgressWheel();
            if (result != null) {
                showLoadedImage(result);
            } else {
                showErrorMessage();
            }
        }
        
        @SuppressLint("InlinedApi")
        private void showLoadedImage(Bitmap result) {
            if (mImageViewRef != null) {
                final ImageViewCustom imageView = mImageViewRef.get();
                if (imageView != null) {
                    imageView.setBitmap(result);
                    imageView.setImageBitmap(result);
                    imageView.setVisibility(View.VISIBLE);
                    mBitmap  = result;
                } // else , silently finish, the fragment was destroyed
            }
            if (mMessageViewRef != null) {
                final TextView messageView = mMessageViewRef.get();
                if (messageView != null) {
                    messageView.setVisibility(View.GONE);
                } // else , silently finish, the fragment was destroyed
            }
        }
        
        private void showErrorMessage() {
            if (mImageViewRef != null) {
                final ImageView imageView = mImageViewRef.get();
                if (imageView != null) {
                    // shows the default error icon
                    imageView.setVisibility(View.VISIBLE);
                } // else , silently finish, the fragment was destroyed
            }
            if (mMessageViewRef != null) {
                final TextView messageView = mMessageViewRef.get();
                if (messageView != null) {
                    messageView.setText(mErrorMessageId);
                    messageView.setVisibility(View.VISIBLE);
                } // else , silently finish, the fragment was destroyed
            }
        }
        
        private void hideProgressWheel() {
            if (mProgressWheelRef != null) {
                final ProgressBar progressWheel = mProgressWheelRef.get();
                if (progressWheel != null) {
                    progressWheel.setVisibility(View.GONE);
                }
            }
        }
        
    }

    /**
     * Helper method to test if an {@link OCFile} can be passed to a {@link PreviewImageFragment} to be previewed.
     * 
     * @param file      File to test if can be previewed.
     * @return          'True' if the file can be handled by the fragment.
     */
    public static boolean canBePreviewed(OCFile file) {
        return (file != null && file.isImage());
    }

    
    /**
     * Finishes the preview
     */
    private void finish() {
        Activity container = getActivity();
        container.finish();
    }
    
    public TouchImageViewCustom getImageView() {
        return mImageView;
    }

    static class FlushedInputStream extends FilterInputStream {
        public FlushedInputStream(InputStream inputStream) {
        super(inputStream);
        }

        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                      int byteValue = read();
                      if (byteValue < 0) {
                          break;  // we reached EOF
                      } else {
                          bytesSkipped = 1; // we read one byte
                      }
               }
               totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }

    /**
     * Load image scaled
     * @param storagePath: path of the image
     * @return Bitmap
     */
    @SuppressWarnings("deprecation")
    private Bitmap loadScaledImage(String storagePath) {

        Log_OC.d(TAG, "Loading image scaled");

        // set desired options that will affect the size of the bitmap
        BitmapFactory.Options options = new Options();
        options.inScaled = true;
        options.inPurgeable = true;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
            options.inPreferQualityOverSpeed = false;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            options.inMutable = false;
        }
        // make a false load of the bitmap - just to be able to read outWidth, outHeight and outMimeType
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(storagePath, options);

        int width = options.outWidth;
        int height = options.outHeight;
        int scale = 1;

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        int screenWidth;
        int screenHeight;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR2) {
            display.getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;
        } else {
            screenWidth = display.getWidth();
            screenHeight = display.getHeight();
        }

        if (width > screenWidth) {
            // second try to scale down the image , this time depending upon the screen size 
            scale = (int) Math.floor((float)width / screenWidth);
        }
        if (height > screenHeight) {
            scale = Math.max(scale, (int) Math.floor((float)height / screenHeight));
        }
        options.inSampleSize = scale;

        // really load the bitmap
        options.inJustDecodeBounds = false; // the next decodeFile call will be real
        return BitmapFactory.decodeFile(storagePath, options);

    }
}
