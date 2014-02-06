/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc. 
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.network.webdav.WebdavUtils;
import com.owncloud.android.lib.operations.common.OnRemoteOperationListener;
import com.owncloud.android.lib.operations.common.RemoteOperation;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.fragment.ConfirmationDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.Log_OC;


/**
 * This fragment shows a preview of a downloaded image.
 * 
 * Trying to get an instance with NULL {@link OCFile} or ownCloud {@link Account} values will produce an {@link IllegalStateException}.
 * 
 * If the {@link OCFile} passed is not downloaded, an {@link IllegalStateException} is generated on instantiation too.
 * 
 * @author David A. Velasco
 */
public class PreviewImageFragment extends FileFragment implements   OnRemoteOperationListener, 
                                                                        ConfirmationDialogFragment.ConfirmationDialogFragmentListener {
    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";

    private View mView;
    private Account mAccount;
    private FileDataStorageManager mStorageManager;
    private ImageView mImageView;
    private TextView mMessageView;
    private ProgressBar mProgressWheel;

    public Bitmap mBitmap = null;
    
    private Handler mHandler;
    private RemoteOperation mLastRemoteOperation;
    
    private static final String TAG = PreviewImageFragment.class.getSimpleName();

    private boolean mIgnoreFirstSavedState;

    
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
        mStorageManager = null; // we need a context to init this; the container activity is not available yet at this moment
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
        mStorageManager = null;
        mIgnoreFirstSavedState = false;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
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
        mImageView = (ImageView)mView.findViewById(R.id.image);
        mImageView.setVisibility(View.GONE);
        mView.setOnTouchListener((OnTouchListener)getActivity());   // WATCH OUT THAT CAST
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof FileFragment.ContainerActivity))
            throw new ClassCastException(activity.toString() + " must implement " + FileFragment.ContainerActivity.class.getSimpleName());
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mStorageManager = new FileDataStorageManager(mAccount, getActivity().getApplicationContext().getContentResolver());
        if (savedInstanceState != null) {
            if (!mIgnoreFirstSavedState) {
                setFile((OCFile)savedInstanceState.getParcelable(PreviewImageFragment.EXTRA_FILE));
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
           BitmapLoader bl = new BitmapLoader(mImageView, mMessageView, mProgressWheel);
           bl.execute(new String[]{getFile().getStoragePath()});
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.file_actions_menu, menu);
        List<Integer> toHide = new ArrayList<Integer>();    
        
        MenuItem item = null;
        toHide.add(R.id.action_cancel_download);
        toHide.add(R.id.action_cancel_upload);
        toHide.add(R.id.action_download_file);
        toHide.add(R.id.action_rename_file);    // by now

        for (int i : toHide) {
            item = menu.findItem(i);
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
            case R.id.action_share_file: {
                FileActivity act = (FileActivity)getSherlockActivity();
                act.getFileOperationsHelper().shareFileWithLink(getFile(), act);
                return true;
            }
            case R.id.action_open_file_with: {
                openFile();
                return true;
            }
            case R.id.action_remove_file: {
                removeFile();
                return true;
            }
            case R.id.action_see_details: {
                seeDetails();
                return true;
            }
            
            default:
                return false;
        }
    }

    
    private void seeDetails() {
        ((FileFragment.ContainerActivity)getActivity()).showDetails(getFile());        
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
        super.onDestroy();
        if (mBitmap != null) {
            mBitmap.recycle();
        }
    }

    
    /**
     * Opens the previewed image with an external application.
     * 
     * TODO - improve this; instead of prioritize the actions available for the MIME type in the server, 
     * we should get a list of available apps for MIME tpye in the server and join it with the list of 
     * available apps for the MIME type known from the file extension, to let the user choose
     */
    private void openFile() {
        OCFile file = getFile();
        String storagePath = file.getStoragePath();
        String encodedStoragePath = WebdavUtils.encodePath(storagePath);
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.parse("file://"+ encodedStoragePath), file.getMimetype());
            i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivity(i);
            
        } catch (Throwable t) {
            Log_OC.e(TAG, "Fail when trying to open with the mimeType provided from the ownCloud server: " + file.getMimetype());
            boolean toastIt = true; 
            String mimeType = "";
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(storagePath.substring(storagePath.lastIndexOf('.') + 1));
                if (mimeType == null || !mimeType.equals(file.getMimetype())) {
                    if (mimeType != null) {
                        i.setDataAndType(Uri.parse("file://"+ encodedStoragePath), mimeType);
                    } else {
                        // desperate try
                        i.setDataAndType(Uri.parse("file://"+ encodedStoragePath), "*-/*");
                    }
                    i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startActivity(i);
                    toastIt = false;
                }
                
            } catch (IndexOutOfBoundsException e) {
                Log_OC.e(TAG, "Trying to find out MIME type of a file without extension: " + storagePath);
                
            } catch (ActivityNotFoundException e) {
                Log_OC.e(TAG, "No activity found to handle: " + storagePath + " with MIME type " + mimeType + " obtained from extension");
                
            } catch (Throwable th) {
                Log_OC.e(TAG, "Unexpected problem when opening: " + storagePath, th);
                
            } finally {
                if (toastIt) {
                    Toast.makeText(getActivity(), "There is no application to handle file " + file.getFileName(), Toast.LENGTH_SHORT).show();
                }
            }
            
        }
        finish();
    }
    
    
    /**
     * Starts a the removal of the previewed file.
     * 
     * Shows a confirmation dialog. The action continues in {@link #onConfirmation(String)} , {@link #onNeutral(String)} or {@link #onCancel(String)},
     * depending upon the user selection in the dialog. 
     */
    private void removeFile() {
        ConfirmationDialogFragment confDialog = ConfirmationDialogFragment.newInstance(
                R.string.confirmation_remove_alert,
                new String[]{getFile().getFileName()},
                R.string.confirmation_remove_remote_and_local,
                R.string.confirmation_remove_local,
                R.string.common_cancel);
        confDialog.setOnConfirmationListener(this);
        confDialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
    }

    
    /**
     * Performs the removal of the previewed file, both locally and in the server.
     */
    @Override
    public void onConfirmation(String callerTag) {
        if (mStorageManager.getFileById(getFile().getFileId()) != null) {   // check that the file is still there;
            mLastRemoteOperation = new RemoveFileOperation( getFile(),      // TODO we need to review the interface with RemoteOperations, and use OCFile IDs instead of OCFile objects as parameters
                                                            true, 
                                                            mStorageManager);
            mLastRemoteOperation.execute(mAccount, getSherlockActivity(), this, mHandler, getSherlockActivity());
            
            ((PreviewImageActivity) getActivity()).showLoadingDialog();
        }
    }
    
    
    /**
     * Removes the file from local storage
     */
    @Override
    public void onNeutral(String callerTag) {
        OCFile file = getFile();
        mStorageManager.removeFile(file, false, true);    // TODO perform in background task / new thread
        finish();
    }
    
    /**
     * User cancelled the removal action.
     */
    @Override
    public void onCancel(String callerTag) {
        // nothing to do here
    }
    

    private class BitmapLoader extends AsyncTask<String, Void, Bitmap> {

        /**
         * Weak reference to the target {@link ImageView} where the bitmap will be loaded into.
         * 
         * Using a weak reference will avoid memory leaks if the target ImageView is retired from memory before the load finishes.
         */
        private final WeakReference<ImageView> mImageViewRef;

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
        public BitmapLoader(ImageView imageView, TextView messageView, ProgressBar progressWheel) {
            mImageViewRef = new WeakReference<ImageView>(imageView);
            mMessageViewRef = new WeakReference<TextView>(messageView);
            mProgressWheelRef = new WeakReference<ProgressBar>(progressWheel);
        }
        
        
        @SuppressWarnings("deprecation")
        @SuppressLint({ "NewApi", "NewApi", "NewApi" }) // to avoid Lint errors since Android SDK r20
		@Override
        protected Bitmap doInBackground(String... params) {
            Bitmap result = null;
            if (params.length != 1) return result;
            String storagePath = params[0];
            try {
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
                result = BitmapFactory.decodeFile(storagePath, options);
                //Log_OC.d(TAG, "Image loaded - width: " + options.outWidth + ", loaded height: " + options.outHeight);

                if (result == null) {
                    mErrorMessageId = R.string.preview_image_error_unknown_format;
                    Log_OC.e(TAG, "File could not be loaded as a bitmap: " + storagePath);
                }
                
            } catch (OutOfMemoryError e) {
                mErrorMessageId = R.string.preview_image_error_unknown_format;
                Log_OC.e(TAG, "Out of memory occured for file " + storagePath, e);
                    
            } catch (NoSuchFieldError e) {
                mErrorMessageId = R.string.common_error_unknown;
                Log_OC.e(TAG, "Error from access to unexisting field despite protection; file " + storagePath, e);
                    
            } catch (Throwable t) {
                mErrorMessageId = R.string.common_error_unknown;
                Log_OC.e(TAG, "Unexpected error loading " + getFile().getStoragePath(), t);
                
            }
            return result;
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
        
        private void showLoadedImage(Bitmap result) {
            if (mImageViewRef != null) {
                final ImageView imageView = mImageViewRef.get();
                if (imageView != null) {
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
     * {@inheritDoc}
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        if (operation.equals(mLastRemoteOperation) && operation instanceof RemoveFileOperation) {
            onRemoveFileOperationFinish((RemoveFileOperation)operation, result);
        }
    }
    
    private void onRemoveFileOperationFinish(RemoveFileOperation operation, RemoteOperationResult result) {
        ((PreviewImageActivity) getActivity()).dismissLoadingDialog();
        
        if (result.isSuccess()) {
            Toast msg = Toast.makeText(getActivity().getApplicationContext(), R.string.remove_success_msg, Toast.LENGTH_LONG);
            msg.show();
            finish();
                
        } else {
            Toast msg = Toast.makeText(getActivity(), R.string.remove_fail_msg, Toast.LENGTH_LONG); 
            msg.show();
            if (result.isSslRecoverableException()) {
                // TODO show the SSL warning dialog
            }
        }
    }

    /**
     * Finishes the preview
     */
    private void finish() {
        Activity container = getActivity();
        container.finish();
    }
    
    
}
