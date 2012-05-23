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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

import eu.alefzero.owncloud.DisplayUtils;
import eu.alefzero.owncloud.FileDownloader;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.datamodel.OCFile;

/**
 * This Fragment is used to display the details about a file.
 * 
 * @author Bartek Przybylski
 * 
 */
public class FileDetailFragment extends SherlockFragment implements
        OnClickListener {

    public static final String EXTRA_FILE = "FILE";

    private DownloadFinishReceiver mDownloadFinishReceiver;
    private Intent mIntent;
    private int mLayout;
    private View mView;
    private OCFile mFile;
    private static final String TAG = "FileDetailFragment";

    /**
     * Default constructor - contains real layout
     */
    public FileDetailFragment(){
        mLayout = R.layout.file_details_fragment;
    }
    
    /**
     * Creates a dummy layout. For use if the user never has
     * tapped on a file before
     * 
     * @param useEmptyView If true, use empty layout
     */
    public FileDetailFragment(boolean useEmptyView){
        if(useEmptyView){
            mLayout = R.layout.file_details_empty;
        } else {
            mLayout = R.layout.file_details_fragment;
        }
    }
    
    /**
     * Use this when creating the fragment and display
     * a file at the same time
     * 
     * @param showDetailsIntent The Intent with the required parameters
     * @see FileDetailFragment#updateFileDetails(Intent)
     */
    public FileDetailFragment(Intent showDetailsIntent) {
        mIntent = showDetailsIntent;
        mLayout = R.layout.file_details_fragment;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = null;
        view = inflater.inflate(mLayout, container, false);
        mView = view;
        if(mLayout == R.layout.file_details_fragment){
            // Phones will launch an activity with this intent
            if(mIntent == null){
                mIntent = getActivity().getIntent();
            }
            updateFileDetails();
        }
        
        return view;
    }

    @Override
    public View getView() {
        return super.getView() == null ? mView : super.getView();
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(getActivity(), "Downloading", Toast.LENGTH_LONG).show();
        Intent i = new Intent(getActivity(), FileDownloader.class);
        i.putExtra(FileDownloader.EXTRA_ACCOUNT,
                mIntent.getParcelableExtra(FileDownloader.EXTRA_ACCOUNT));
        i.putExtra(FileDownloader.EXTRA_FILE_PATH, mFile.getPath());
        getActivity().startService(i);
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
     * @param intent The {@link Intent} that contains extra information about
     *            this file The intent needs to have these extras:
     *            <p>
     * 
     *            {@link FileDetailFragment#EXTRA_FILE}: An {@link OCFile}
     *            {@link FileDownloader#EXTRA_ACCOUNT}: The Account that file
     *            belongs to (required for downloading)
     */
    public void updateFileDetails(Intent intent) {
        mIntent = intent;
        updateFileDetails();
    }

    /**
     * Updates the view with all relevant details about that file.
     */
    private void updateFileDetails() {
        mFile = mIntent.getParcelableExtra(EXTRA_FILE);
        Button downloadButton = (Button) getView().findViewById(R.id.fdDownloadBtn);

        if (mFile != null) {
            // set file details
            setFilename(mFile.getFileName());
            setFiletype(DisplayUtils.convertMIMEtoPrettyPrint(mFile
                    .getMimetype()));
            setFilesize(mFile.getFileLength());
            setTimeCreated(mFile.getCreationTimestamp());
            setTimeModified(mFile.getModificationTimestamp());
            
            // Update preview
            if (mFile.getStoragePath() != null) {
                try {
                    if (mFile.getMimetype().startsWith("image/")) {
                        ImageView preview = (ImageView) getView().findViewById(
                                R.id.fdPreview);
                        Bitmap bmp = BitmapFactory.decodeFile(mFile.getStoragePath());
                        preview.setImageBitmap(bmp);
                    }
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, "Out of memory occured for file with size " + mFile.getFileLength());
                }
                downloadButton.setText(R.string.filedetails_open);
                downloadButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setDataAndType(Uri.parse("file://"+mFile.getStoragePath()), mFile.getMimetype());
                        startActivity(i);
                    }
                });
            } else {
                // Make download button effective
                downloadButton.setOnClickListener(this);
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
        if(tv != null){
            tv.setText(DisplayUtils.unixTimeToHumanReadable(milliseconds));
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
     * Once the file download has finished -> update view
     * @author Bartek Przybylski
     */
    private class DownloadFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateFileDetails();
        }

    }

}
