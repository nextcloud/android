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

import java.util.Vector;

import com.actionbarsherlock.app.ActionBar;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;
import eu.alefzero.owncloud.AccountUtils;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.datamodel.DataStorageManager;
import eu.alefzero.owncloud.datamodel.FileDataStorageManager;
import eu.alefzero.owncloud.datamodel.OCFile;
import eu.alefzero.owncloud.files.services.FileDownloader;
import eu.alefzero.owncloud.ui.FragmentListView;
import eu.alefzero.owncloud.ui.activity.FileDetailActivity;
import eu.alefzero.owncloud.ui.activity.FileDisplayActivity;
import eu.alefzero.owncloud.ui.adapter.FileListListAdapter;

/**
 * A Fragment that lists all files and folders in a given path.
 * 
 * @author Bartek Przybylski
 * 
 */
public class FileListFragment extends FragmentListView {
    private static final String TAG = "FileListFragment";
    private Account mAccount;
    private Vector<OCFile> mFiles;
    private DataStorageManager mStorageManager;
    private OCFile mFile;
    private boolean mIsLargeDevice = false; 

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAccount = AccountUtils.getCurrentOwnCloudAccount(getActivity());
        mStorageManager = new FileDataStorageManager(mAccount, getActivity().getContentResolver());

        Intent intent = getActivity().getIntent();
        OCFile directory = intent.getParcelableExtra(FileDetailFragment.EXTRA_FILE);
        mFile = directory;
        
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        getListView().setDivider(getResources().getDrawable(R.drawable.uploader_list_separator));
        getListView().setDividerHeight(1);
        
        //listDirectory(mFile);
        
        return getListView();
    }    

    @Override
    public void onStart() {
        // Create a placeholder upon launch
        View fragmentContainer = getActivity().findViewById(R.id.file_details_container);
        if (fragmentContainer != null) {
            mIsLargeDevice = true;
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.file_details_container, new FileDetailFragment(true));
            transaction.commit();
        }
        super.onStart();
    }

    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        if (mFiles.size() <= position) {
            throw new IndexOutOfBoundsException("Incorrect item selected");
        }
        OCFile file = mFiles.get(position);
        
        // Update ActionBarPath
        if (file.getMimetype().equals("DIR")) {
            mFile = file;
            ((FileDisplayActivity) getActivity()).pushDirname(file);
            ActionBar actionBar = ((FileDisplayActivity) getActivity()).getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            listDirectory(file);
            resetFileFragment();
            return;
        }

        Intent showDetailsIntent = new Intent(getActivity(), FileDetailActivity.class);
        showDetailsIntent.putExtra(FileDetailFragment.EXTRA_FILE, file);
        showDetailsIntent.putExtra(FileDownloader.EXTRA_ACCOUNT, mAccount);

        // If we are on a large device -> update fragment
        if (mIsLargeDevice) {
            FileDetailFragment fileDetails = (FileDetailFragment) getFragmentManager().findFragmentByTag("FileDetails");
            if (fileDetails == null) {
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.file_details_container, new FileDetailFragment(showDetailsIntent), "FileDetails");
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                transaction.commit();
            } else {
                fileDetails.updateFileDetails(showDetailsIntent);
            }
        } else {
            startActivity(showDetailsIntent);
        }
    }

    /**
     * Resets the FileDetailsFragment on Tablets so that it always displays
     * "Tab on a file to display it's details"
     */
    private void resetFileFragment() {
        FileDetailFragment fileDetails = (FileDetailFragment) getFragmentManager().findFragmentByTag("FileDetails");
        if (fileDetails != null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.remove(fileDetails);
            transaction.add(R.id.file_details_container, new FileDetailFragment(true));
            transaction.commit();
        }
    }

    /**
     * Call this, when the user presses the up button
     */
    public void onNavigateUp() {
        OCFile parentDir = null;
        
        if(mFile != null){
            parentDir = mStorageManager.getFileById(mFile.getParentId());
            mFile = parentDir;
        }
        
        listDirectory(parentDir);
        resetFileFragment();
    }

    /**
     * Use this to query the {@link OCFile} that is currently
     * being displayed by this fragment
     * @return The currently viewed OCFile
     */
    public OCFile getCurrentFile(){
        return mFile;
    }
    
    /**
     * Calls {@link FileListFragment#listDirectory(OCFile)} with a null parameter
     */
    public void listDirectory(){
        listDirectory(null);
    }
    
    /**
     * Lists the given directory on the view. When the input parameter is null,
     * it will either refresh the last known directory, or list the root
     * if there never was a directory.
     * 
     * @param directory File to be listed
     */
    public void listDirectory(OCFile directory) {
        
        // Check input parameters for null
        if(directory == null){
            if(mFile != null){
                directory = mFile;
            } else {
                directory = mStorageManager.getFileByPath("/");
                if (directory == null) return; // no files, wait for sync
            }
        }
        
        
        // If that's not a directory -> List its parent
        if(!directory.isDirectory()){
            Log.w(TAG, "You see, that is not a directory -> " + directory.toString());
            directory = mStorageManager.getFileById(directory.getParentId());
        }

        mFile = directory;
        
        mFiles = mStorageManager.getDirectoryContent(directory);
        if (mFiles == null || mFiles.size() == 0) {
            Toast.makeText(getActivity(), "There are no files here", Toast.LENGTH_LONG).show();
        }
        setListAdapter(new FileListListAdapter(directory, mStorageManager, getActivity()));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("ACCOUNT", mAccount);
    }

    /**
     * This should be called every time the current account changes, in order to synchronize mStorageManager without create a new FileListFragment
     */
    public void updateAccount() {
        Account old = mAccount;
        mAccount = AccountUtils.getCurrentOwnCloudAccount(getActivity());
        if (old != mAccount)
            mStorageManager = new FileDataStorageManager(mAccount, getActivity().getContentResolver());
            // dvelasco : a better solution can be provided change the flow between states "wiht account" and "without account", in terms of interactions between AuthenticatorActivity and FileDisplayActivity
    }

}
