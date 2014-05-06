/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
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
package com.owncloud.android.ui.adapter;

import android.accounts.Account;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;


import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.Log_OC;


/**
 * This Adapter populates a ListView with all files and folders in an ownCloud
 * instance.
 * 
 * @author Bartek Przybylski
 * 
 */
public class FileListListAdapter extends CursorAdapter implements ListAdapter {

    private static final String TAG = FileListListAdapter.class.getSimpleName();
    
    private Context mContext;
    private FileDataStorageManager mStorageManager;
    private Account mAccount;
    private ComponentsGetter mTransferServiceGetter;
    

    public FileListListAdapter(Context context, ComponentsGetter componentsGetter) {
        super(context, null, 0);
        mContext = context;
        mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);
        mTransferServiceGetter = componentsGetter;
    }

    /*
    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public int getCount() {
        return mFiles != null ? mFiles.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        if (mFiles == null || mFiles.size() <= position)
            return null;
        return mFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        if (mFiles == null || mFiles.size() <= position)
            return 0;
        return mFiles.get(position).getFileId();
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }
    
    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return (mFiles == null || mFiles.isEmpty());
    }
    */

    /**
     * Change the adapted directory for a new one
     * @param folder                    New file to adapt. Can be NULL, meaning "no content to adapt".
     * @param updatedStorageManager     Optional updated storage manager; used to replace mStorageManager if is different (and not NULL)
     */
    public void swapDirectory(OCFile folder, FileDataStorageManager updatedStorageManager) {
        if (updatedStorageManager != null && updatedStorageManager != mStorageManager) {
            mStorageManager = updatedStorageManager;
            mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);
        }
        Cursor newCursor = null; 
        if (mStorageManager != null) {
            //mFiles = mStorageManager.getFolderContent(mFile);
            newCursor = mStorageManager.getContent(folder.getFileId());
        }
        Cursor oldCursor = swapCursor(newCursor);
        if (oldCursor != null){
            oldCursor.close();
        }
        notifyDataSetChanged();
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Log_OC.d(TAG, "bindView start");
        
        OCFile file = mStorageManager.createFileInstance(cursor);
        
        TextView fileName = (TextView) view.findViewById(R.id.Filename);
        String name = file.getFileName();

        fileName.setText(name);
        ImageView fileIcon = (ImageView) view.findViewById(R.id.imageView1);
        fileIcon.setImageResource(DisplayUtils.getResourceId(file.getMimetype()));
        ImageView localStateView = (ImageView) view.findViewById(R.id.imageView2);
        FileDownloaderBinder downloaderBinder = mTransferServiceGetter.getFileDownloaderBinder();
        FileUploaderBinder uploaderBinder = mTransferServiceGetter.getFileUploaderBinder();
        if (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, file)) {
            localStateView.setImageResource(R.drawable.downloading_file_indicator);
            localStateView.setVisibility(View.VISIBLE);
        } else if (uploaderBinder != null && uploaderBinder.isUploading(mAccount, file)) {
            localStateView.setImageResource(R.drawable.uploading_file_indicator);
            localStateView.setVisibility(View.VISIBLE);
        } else if (file.isDown()) {
            localStateView.setImageResource(R.drawable.local_file_indicator);
            localStateView.setVisibility(View.VISIBLE);
        } else {
            localStateView.setVisibility(View.INVISIBLE);
        }
        
        TextView fileSizeV = (TextView) view.findViewById(R.id.file_size);
        TextView lastModV = (TextView) view.findViewById(R.id.last_mod);
        ImageView checkBoxV = (ImageView) view.findViewById(R.id.custom_checkbox);
        
        if (!file.isFolder()) {
            fileSizeV.setVisibility(View.VISIBLE);
            fileSizeV.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));
            lastModV.setVisibility(View.VISIBLE);
            lastModV.setText(DisplayUtils.unixTimeToHumanReadable(file.getModificationTimestamp()));
            // this if-else is needed even thoe fav icon is visible by default
            // because android reuses views in listview
            if (!file.keepInSync()) {
                view.findViewById(R.id.imageView3).setVisibility(View.GONE);
            } else {
                view.findViewById(R.id.imageView3).setVisibility(View.VISIBLE);
            }
            
        } 
        else {
            
            fileSizeV.setVisibility(View.INVISIBLE);
            //fileSizeV.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));
            lastModV.setVisibility(View.VISIBLE);
            lastModV.setText(DisplayUtils.unixTimeToHumanReadable(file.getModificationTimestamp()));
            checkBoxV.setVisibility(View.GONE);
            view.findViewById(R.id.imageView3).setVisibility(View.GONE);
        }
        
        ImageView shareIconV = (ImageView) view.findViewById(R.id.shareIcon);
        if (file.isShareByLink()) {
            shareIconV.setVisibility(View.VISIBLE);
        } else {
            shareIconV.setVisibility(View.INVISIBLE);
        }
        //}
        Log_OC.d(TAG, "bindView end");
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        Log_OC.d(TAG, "newView start");
        LayoutInflater inflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflator.inflate(R.layout.list_item, null);
        
        // TODO check activity to upload
        ListView parentList = (ListView) parent;
        ImageView checkBoxV = (ImageView) view.findViewById(R.id.custom_checkbox);
        if (parentList.getChoiceMode() == ListView.CHOICE_MODE_NONE) { 
            checkBoxV.setVisibility(View.GONE);
        } else {
            /*if (parentList.isItemChecked(position)) {
                checkBoxV.setImageResource(android.R.drawable.checkbox_on_background);
            } else {
                checkBoxV.setImageResource(android.R.drawable.checkbox_off_background);
            }*/
            checkBoxV.setVisibility(View.VISIBLE);
        }
        Log_OC.d(TAG, "newView end");
        return view;
      
    }
    
}
