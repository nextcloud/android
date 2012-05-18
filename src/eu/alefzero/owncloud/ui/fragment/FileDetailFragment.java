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

import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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

    public static final String FILE = "FILE";

    private Intent mIntent;
    //private View mView;
    private DownloadFinishReceiver mDownloadFinishReceiver;
    private OCFile mFile;

    private int mLayout;
    private boolean mEmptyLayout;

    /**
     * Default constructor. When inflated by android -> display empty layout
     */
    public FileDetailFragment() {
        mLayout = R.layout.file_details_empty;
        mEmptyLayout = true;
    }

    /**
     * Custom construtor. Use with a {@link FragmentTransaction}. The intent has
     * to contain {@link FileDetailFragment#FILE} with an OCFile and also
     * {@link FileDownloader#EXTRA_ACCOUNT} with the account.
     * 
     * @param intent Intent with an account and a file in it for rendering
     */
    public FileDetailFragment(Intent intent) {
        mLayout = R.layout.file_details_fragment;
        mIntent = intent;
        mEmptyLayout = false;
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

    /**
     * Use this method to signal this Activity that it shall update its view.
     * 
     * @param intent The {@link Intent} that contains extra information about
     *            this file The intent needs to have these extras:
     *            <p>
     * 
     *            {@link FileDetailFragment#FILE}: An {@link OCFile}
     *            {@link FileDownloader#EXTRA_ACCOUNT}: The Account that file
     *            belongs to (required for downloading)
     */
    public void updateFileDetails(Intent intent) {
        mIntent = intent;
        updateFileDetails();
    }

    private void updateFileDetails() {
        mFile = mIntent.getParcelableExtra(FILE);

        if (mFile != null) {
            // set file details
            setFilename(mFile.getFileName());
            setFiletype(DisplayUtils.convertMIMEtoPrettyPrint(mFile
                    .getMimetype()));
            setFilesize(mFile.getFileLength());
            
            // Update preview
            if (mFile.getStoragePath() != null) {
                if (mFile.getMimetype().startsWith("image/")) {
                    ImageView preview = (ImageView) getView().findViewById(
                            R.id.fdPreview);
                    Bitmap bmp = BitmapFactory.decodeFile(mFile.getStoragePath());
                    preview.setImageBitmap(bmp);
                }
            }
            
            // Make download button effective
            Button downloadButton = (Button) getView().findViewById(R.id.fdDownloadBtn);
            downloadButton.setOnClickListener(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = null;
        view = inflater.inflate(mLayout, container, false);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        
        // Fill in required information about file displaying
        if(mIntent == null){
            mIntent = getActivity().getIntent();
        }
        
        // Fill in the details if the layout is not empty
        if(!mEmptyLayout){
            updateFileDetails();
        }
        
    }

    private void setFilename(String filename) {
        TextView tv = (TextView) getView().findViewById(R.id.fdFilename);
        if (tv != null)
            tv.setText(filename);
    }

    private void setFiletype(String mimetype) {
        TextView tv = (TextView) getView().findViewById(R.id.fdType);
        if (tv != null)
            tv.setText(mimetype);
    }

    private void setFilesize(long filesize) {
        TextView tv = (TextView) getView().findViewById(R.id.fdSize);
        if (tv != null)
            tv.setText(DisplayUtils.bitsToHumanReadable(filesize));
    }

    /**
     * Use this to check if the correct layout is loaded. When android
     * instanciates this class using the default constructor, the layout will be
     * empty.
     * 
     * Once a user touches a file for the first time, you must instanciate a new
     * Fragment with the new FileDetailFragment(true) to inflate the actual
     * details
     * 
     * @return If the layout is empty, this method will return true, otherwise
     *         false
     */
    public boolean isEmptyLayout() {
        return mEmptyLayout;
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

    private class DownloadFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateFileDetails();
        }

    }

}
