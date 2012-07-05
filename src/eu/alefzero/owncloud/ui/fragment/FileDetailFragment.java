/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
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
package eu.alefzero.owncloud.ui.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

import eu.alefzero.owncloud.DisplayUtils;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.datamodel.OCFile;
import eu.alefzero.owncloud.files.services.FileDownloader;

/**
 * This Fragment is used to display the details about a file.
 * 
 * @author Bartek Przybylski
 * 
 */
public class FileDetailFragment extends SherlockFragment implements
        OnClickListener {

    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";

    private int mLayout;
    private View mView;
    private OCFile mFile;
    private Account mAccount;
    
    private DownloadFinishReceiver mDownloadFinishReceiver;

    private static final String TAG = "FileDetailFragment";
    public static final String FTAG = "FileDetails"; 

    
    /**
     * Creates an empty details fragment.
     * 
     * It's necessary to keep a public constructor without parameters; the system uses it when tries to reinstantiate a fragment automatically. 
     */
    public FileDetailFragment() {
        mFile = null;
        mAccount = null;
        mLayout = R.layout.file_details_empty;
    }
    
    
    /**
     * Creates a details fragment.
     * 
     * When 'fileToDetail' or 'ocAccount' are null, creates a dummy layout (to use when a file wasn't tapped before).
     * 
     * @param fileToDetail      An {@link OCFile} to show in the fragment
     * @param ocAccount         An ownCloud account; needed to start downloads
     */
    public FileDetailFragment(OCFile fileToDetail, Account ocAccount){
        mFile = fileToDetail;
        mAccount = ocAccount;
        mLayout = R.layout.file_details_empty;
        
        if(fileToDetail != null && ocAccount != null) {
            mLayout = R.layout.file_details_fragment;
        }
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        
        if (savedInstanceState != null) {
            mFile = savedInstanceState.getParcelable(FileDetailFragment.EXTRA_FILE);
            mAccount = savedInstanceState.getParcelable(FileDetailFragment.EXTRA_ACCOUNT);
        }
        
        View view = null;
        view = inflater.inflate(mLayout, container, false);
        mView = view;
        
        updateFileDetails();
        return view;
    }
    

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i(getClass().toString(), "onSaveInstanceState() start");
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileDetailFragment.EXTRA_FILE, mFile);
        outState.putParcelable(FileDetailFragment.EXTRA_ACCOUNT, mAccount);
        Log.i(getClass().toString(), "onSaveInstanceState() end");
    }

    
    @Override
    public void onResume() {
        super.onResume();
        mDownloadFinishReceiver = new DownloadFinishReceiver();
        IntentFilter filter = new IntentFilter(
                FileDownloader.DOWNLOAD_FINISH_MESSAGE);
        getActivity().registerReceiver(mDownloadFinishReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mDownloadFinishReceiver);
        mDownloadFinishReceiver = null;
    }

    @Override
    public View getView() {
        return super.getView() == null ? mView : super.getView();
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(getActivity(), "Downloading", Toast.LENGTH_LONG).show();
        Intent i = new Intent(getActivity(), FileDownloader.class);
        i.putExtra(FileDownloader.EXTRA_ACCOUNT, mAccount);
        i.putExtra(FileDownloader.EXTRA_REMOTE_PATH, mFile.getRemotePath());
        i.putExtra(FileDownloader.EXTRA_FILE_PATH, mFile.getURLDecodedRemotePath());
        i.putExtra(FileDownloader.EXTRA_FILE_SIZE, mFile.getFileLength());
        v.setEnabled(false);
        getActivity().startService(i);
    }


    /**
     * Check if the fragment was created with an empty layout. An empty fragment can't show file details, must be replaced.
     * 
     * @return  True when the fragment was created with the empty layout.
     */
    public boolean isEmpty() {
        return mLayout == R.layout.file_details_empty;
    }

    
    /**
     * Can be used to get the file that is currently being displayed.
     * @return The file on the screen.
     */
    public OCFile getDisplayedFile(){
        return mFile;
    }
    
    /**
     * Use this method to signal this Activity that it shall update its view.
     * 
     * @param file : An {@link OCFile}
     */
    public void updateFileDetails(OCFile file, Account ocAccount) {
        mFile = file;
        mAccount = ocAccount;
        updateFileDetails();
    }
    

    /**
     * Updates the view with all relevant details about that file.
     */
    public void updateFileDetails() {

        if (mFile != null && mAccount != null && mLayout == R.layout.file_details_fragment) {
            
            Button downloadButton = (Button) getView().findViewById(R.id.fdDownloadBtn);
            // set file details
            setFilename(mFile.getFileName());
            setFiletype(DisplayUtils.convertMIMEtoPrettyPrint(mFile
                    .getMimetype()));
            setFilesize(mFile.getFileLength());
            if(ocVersionSupportsTimeCreated()){
                setTimeCreated(mFile.getCreationTimestamp());
            }
           
            setTimeModified(mFile.getModificationTimestamp());
            
            if (mFile.getStoragePath() != null) {
                // Update preview
                ImageView preview = (ImageView) getView().findViewById(R.id.fdPreview);
                boolean previewIsSet = false;
                try {
                    if (mFile.getMimetype().startsWith("image/")) {
                        BitmapFactory.Options options = new Options();
                        options.inScaled = true;
                        options.inPurgeable = true;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
                            options.inPreferQualityOverSpeed = false;
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                            options.inMutable = false;
                        }

                        Bitmap bmp = BitmapFactory.decodeFile(mFile.getStoragePath(), options);

                        if (bmp != null) {
                            int width = options.outWidth;
                            int height = options.outHeight;
                            int scale = 1;
                            if (width >= 2048 || height >= 2048) {
                                scale = (int) (Math.ceil(Math.max(height, width)/2048.));
                                options.inSampleSize = scale;
                                bmp.recycle();

                                bmp = BitmapFactory.decodeFile(mFile.getStoragePath(), options);
                            }
                        }
                        if (bmp != null) {
                            //preview.setImageBitmap(bmp);
                            preview.setImageDrawable(new BitmapDrawable(preview.getResources(), bmp));
                            previewIsSet = true;
                        }
                    }
                } catch (OutOfMemoryError e) {
                    preview.setVisibility(View.INVISIBLE);
                    Log.e(TAG, "Out of memory occured for file with size " + mFile.getFileLength());
                    
                } catch (NoSuchFieldError e) {
                    preview.setVisibility(View.INVISIBLE);
                    Log.e(TAG, "Error from access to unexisting field despite protection " + mFile.getFileLength());
                    
                } catch (Throwable t) {
                    preview.setVisibility(View.INVISIBLE);
                    Log.e(TAG, "Unexpected error while creating image preview " + mFile.getFileLength(), t);
                    
                } finally {
                    if (!previewIsSet) {
                        resetPreview();
                    }
                }
                // Change download button to open button
                downloadButton.setText(R.string.filedetails_open);
                downloadButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String storagePath = mFile.getStoragePath();
                        try {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setDataAndType(Uri.parse("file://"+ storagePath), mFile.getMimetype());
                            i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            startActivity(i);
                            
                        } catch (Throwable t) {
                            Log.e(TAG, "Fail when trying to open with the mimeType provided from the ownCloud server: " + mFile.getMimetype());
                            boolean toastIt = true; 
                            String mimeType = "";
                            try {
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(storagePath.substring(storagePath.lastIndexOf('.') + 1));
                                if (mimeType != mFile.getMimetype()) {
                                    i.setDataAndType(Uri.parse("file://"+mFile.getStoragePath()), mimeType);
                                    i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                    startActivity(i);
                                    toastIt = false;
                                }
                                
                            } catch (IndexOutOfBoundsException e) {
                                Log.e(TAG, "Trying to find out MIME type of a file without extension: " + storagePath);
                                
                            } catch (ActivityNotFoundException e) {
                                Log.e(TAG, "No activity found to handle: " + storagePath + " with MIME type " + mimeType + " obtained from extension");
                                
                            } catch (Throwable th) {
                                Log.e(TAG, "Unexpected problem when opening: " + storagePath, th);
                                
                            } finally {
                                if (toastIt) {
                                    Toast.makeText(getActivity(), "There is no application to handle file " + mFile.getFileName(), Toast.LENGTH_SHORT).show();
                                }
                            }
                            
                        }
                    }
                });
            } else {
                // Make download button effective
                downloadButton.setOnClickListener(this);
                // Be sure that preview image is reset; the fragment is reused when possible, a preview of other file could be there
                resetPreview();
            }
        }
    }
    
    
    /**
     * Updates the filename in view
     * @param filename to set
     */
    private void setFilename(String filename) {
        TextView tv = (TextView) getView().findViewById(R.id.fdFilename);
        if (tv != null)
            tv.setText(filename);
    }

    /**
     * Updates the MIME type in view
     * @param mimetype to set
     */
    private void setFiletype(String mimetype) {
        TextView tv = (TextView) getView().findViewById(R.id.fdType);
        if (tv != null)
            tv.setText(mimetype);
    }

    /**
     * Updates the file size in view
     * @param filesize in bytes to set
     */
    private void setFilesize(long filesize) {
        TextView tv = (TextView) getView().findViewById(R.id.fdSize);
        if (tv != null)
            tv.setText(DisplayUtils.bytesToHumanReadable(filesize));
    }
    
    /**
     * Updates the time that the file was created in view
     * @param milliseconds Unix time to set
     */
    private void setTimeCreated(long milliseconds){
        TextView tv = (TextView) getView().findViewById(R.id.fdCreated);
        TextView tvLabel = (TextView) getView().findViewById(R.id.fdCreatedLabel);
        if(tv != null){
            tv.setText(DisplayUtils.unixTimeToHumanReadable(milliseconds));
            tv.setVisibility(View.VISIBLE);
            tvLabel.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Updates the time that the file was last modified
     * @param milliseconds Unix time to set
     */
    private void setTimeModified(long milliseconds){
        TextView tv = (TextView) getView().findViewById(R.id.fdModified);
        if(tv != null){
            tv.setText(DisplayUtils.unixTimeToHumanReadable(milliseconds));
        }
    }
    
    /**
     * In ownCloud 3.X.X and 4.X.X there is a bug that SabreDAV does not return
     * the time that the file was created. There is a chance that this will
     * be fixed in future versions. Use this method to check if this version of
     * ownCloud has this fix.
     * @return True, if ownCloud the ownCloud version is supporting creation time
     */
    private boolean ocVersionSupportsTimeCreated(){
        /*if(mAccount != null){
            AccountManager accManager = (AccountManager) getActivity().getSystemService(Context.ACCOUNT_SERVICE);
            OwnCloudVersion ocVersion = new OwnCloudVersion(accManager
                    .getUserData(mAccount, AccountAuthenticator.KEY_OC_VERSION));
            if(ocVersion.compareTo(new OwnCloudVersion(0x030000)) < 0) {
                return true;
            }
        }*/
        return false;
    }

    /**
     * Once the file download has finished -> update view
     * @author Bartek Przybylski
     */
    private class DownloadFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            getView().findViewById(R.id.fdDownloadBtn).setEnabled(true);
            if (intent.getAction().equals(FileDownloader.BAD_DOWNLOAD_MESSAGE)) {
                Toast.makeText(context, R.string.downloader_download_failed , Toast.LENGTH_SHORT).show();
                
            } else if (intent.getAction().equals(FileDownloader.DOWNLOAD_FINISH_MESSAGE)) {
                mFile.setStoragePath(intent.getStringExtra(FileDownloader.EXTRA_FILE_PATH));
                updateFileDetails();
            }
        }
        
    }
    
    
    /**
     * Make the preview image shows the ownCloud logo.
     * 
     * To be called when setting a preview image is not possible.
     */
    private void resetPreview() {
        ImageView preview = (ImageView) getView().findViewById(R.id.fdPreview);
        preview.setImageDrawable(getResources().getDrawable(R.drawable.owncloud_logo));
    }


}
