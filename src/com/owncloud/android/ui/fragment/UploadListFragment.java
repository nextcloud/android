/**
 *   ownCloud Android client application
 *
 *   @author LukeOwncloud
 *   Copyright (C) 2015 ownCloud Inc.
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
package com.owncloud.android.ui.fragment;

import android.accounts.Account;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.adapter.ExpandableUploadListAdapter;

/**
 * A Fragment that lists all files and folders in a given LOCAL path.
 * 
 */
public class UploadListFragment extends ExpandableListFragment {
    static private String TAG = UploadListFragment.class.getSimpleName();

    /**
     * Reference to the Activity which this fragment is attached to. For
     * callbacks
     */
    private UploadListFragment.ContainerActivity mContainerActivity;

    private ExpandableUploadListAdapter mAdapter;

    /** Is binder ready in the Activity? */
    private boolean mBinderReady = false;

    public void setBinderReady(boolean ready) {
        mBinderReady = ready;
    }
    public boolean isBinderReady(){
        return mBinderReady;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        setMessageForEmptyList(getString(R.string.upload_list_empty));
        setOnRefreshListener(this);
        return v;
    }
    
    @Override
    public void onRefresh() {
        mAdapter.notifyDataSetChanged();        
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mContainerActivity = (ContainerActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement "
                    + UploadListFragment.ContainerActivity.class.getSimpleName());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log_OC.d(TAG, "onActivityCreated() start");
        super.onActivityCreated(savedInstanceState);
//        mAdapter = new ExpandableUploadListAdapter((FileActivity)getActivity());
//        setListAdapter(mAdapter);
        //mAdapter.setFileActivity(((FileActivity) getActivity()));
        
        registerForContextMenu(getListView());
        getListView().setOnCreateContextMenuListener(this);
    }

    @Override
    public void onStart() {
        Log_OC.d(TAG, "onStart() start");
        super.onStart();
        mAdapter = new ExpandableUploadListAdapter((FileActivity)getActivity());
        setListAdapter(mAdapter);
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        boolean handled = false;
        OCUpload OCUpload = (OCUpload) mAdapter.getChild(groupPosition, childPosition);
        if (OCUpload != null) {
            // notify the click to container Activity
            handled = mContainerActivity.onUploadItemClick(OCUpload);
        } else {
            Log_OC.w(TAG, "Null object in ListAdapter!!");
        }
        return handled;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.upload_actions_menu, menu);
        
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;  
        int childPosition = ExpandableListView.getPackedPositionChild(info.packedPosition);
        int groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        OCUpload uploadFile = (OCUpload) mAdapter.getChild(groupPosition, childPosition);
        if (uploadFile.userCanCancelUpload()) {
            MenuItem item = menu.findItem(R.id.action_remove_upload);
            if (item != null) {
                item.setVisible(false);
                item.setEnabled(false);
            }
        } else {
            MenuItem item = menu.findItem(R.id.action_cancel_upload);
            if (item != null) {
                item.setVisible(false);
                item.setEnabled(false);
            }
        }
        if (!uploadFile.userCanRetryUpload()) {
            MenuItem item = menu.findItem(R.id.action_retry_upload);
            if (item != null) {
                item.setVisible(false);
                item.setEnabled(false);
            }
        }
    }
    
    @Override
    public boolean onContextItemSelected (MenuItem item) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();  
        int childPosition = ExpandableListView.getPackedPositionChild(info.packedPosition);
        int groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        OCUpload uploadFile = (OCUpload) mAdapter.getChild(groupPosition, childPosition);
        switch (item.getItemId()) {
        case R.id.action_cancel_upload:
            FileUploader.FileUploaderBinder uploaderBinder = ((FileActivity) getActivity()).getFileUploaderBinder();
            if (uploaderBinder != null) {
                uploaderBinder.cancel(uploadFile);
            }
            return true;
        case R.id.action_remove_upload: {
            ((FileActivity) getActivity()).getFileOperationsHelper().removeUploadFromList(uploadFile);
            return true;
//        }case R.id.action_retry_upload: {
//            ((FileActivity) getActivity()).getFileOperationsHelper().retryUpload(uploadFile);
//            return true;
        } case R.id.action_see_details: {
            Toast.makeText(getActivity(), "TO DO", Toast.LENGTH_SHORT).show();
            /*
            Intent showDetailsIntent = new Intent(getActivity(), FileDisplayActivity.class);
            showDetailsIntent.putExtra(FileActivity.EXTRA_FILE, uploadFile.getOCFile());
            showDetailsIntent.putExtra(FileActivity.EXTRA_ACCOUNT, uploadFile.getAccount(getActivity()));
            startActivity(showDetailsIntent);
            */
            return true;
        }
        case R.id.action_open_file_with: {
            Toast.makeText(getActivity(), "TO DO", Toast.LENGTH_SHORT).show();
            //((FileActivity) getActivity()).getFileOperationsHelper().openFile(uploadFile.getOCFile());
            return true;
        }
        default:
            return super.onContextItemSelected(item);
        }
    }

    /**
     * Interface to implement by any Activity that includes some instance of
     * UploadListFragment
     * 
     * @author LukeOwncloud
     */
    public interface ContainerActivity {

        /**
         * Callback method invoked when an upload item is clicked by the user on
         * the upload list
         * 
         * @param file
         * @return return true if click was handled.
         */
        public boolean onUploadItemClick(OCUpload file);

    }

    public void binderReady(){
        setBinderReady(true);

        if (mAdapter != null) {
            mAdapter.addBinder();
        }
    }

    public void updateUploads(){
        if (mAdapter != null) {
            mAdapter.refreshView();
        }
    }

}