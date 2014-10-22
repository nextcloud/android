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

import java.util.Vector;

import android.accounts.Account;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
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
public class FileListListAdapter extends BaseAdapter implements ListAdapter {
    private final static String PERMISSION_SHARED_WITH_ME = "S";

    private Context mContext;
    private OCFile mFile = null;
    private Vector<OCFile> mFiles = null;
    private boolean mJustFolders;

    private FileDataStorageManager mStorageManager;
    private Account mAccount;
    private ComponentsGetter mTransferServiceGetter;
    private enum ViewType {LIST_ITEM, GRID_IMAGE, GRID_ITEM };
    
    public FileListListAdapter(
            boolean justFolders, 
            Context context, 
            ComponentsGetter transferServiceGetter
            ) {
        mJustFolders = justFolders;
        mContext = context;
        mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);
        mTransferServiceGetter = transferServiceGetter;
    }

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
    public View getView(int position, View convertView, ViewGroup parent) {
     // decide image vs. file view
        double count = 0;
        
        
        for (OCFile file : mFiles){
            if (file.isImage()){
                count++;
            }
        }
        
        // TODO threshold as constant in Preferences
        // > 50% Images --> image view
        boolean fileView = true;
        if ((count / mFiles.size()) >= 0.5){
            fileView = false;
        } else {
            fileView = true;
        }
        
        View view = convertView;
        OCFile file = null;
        LayoutInflater inflator = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        if (mFiles != null && mFiles.size() > position) {
            file = mFiles.get(position);
        }
        
        // Find out which layout should be displayed
        ViewType viewType;
        if (fileView){
            viewType = ViewType.LIST_ITEM;
        } else if (file.isImage()){
            viewType = ViewType.GRID_IMAGE;
        } else {
            viewType = ViewType.GRID_ITEM;
        }

        // Create View
        switch (viewType){
        case GRID_IMAGE:
            view = inflator.inflate(R.layout.grid_image, null);
            break;
        case GRID_ITEM:
            view = inflator.inflate(R.layout.grid_item, null);
            break;
        case LIST_ITEM:
            view = inflator.inflate(R.layout.list_item, null);
            break;
        }

        view.invalidate();

        if (file != null){

            ImageView fileIcon = (ImageView) view.findViewById(R.id.thumbnail);
            TextView fileName;
            String name;
            
            switch (viewType){
            case LIST_ITEM:
                fileName = (TextView) view.findViewById(R.id.Filename);
                name = file.getFileName();
                fileName.setText(name);
                
                TextView fileSizeV = (TextView) view.findViewById(R.id.file_size);
                TextView lastModV = (TextView) view.findViewById(R.id.last_mod);
                ImageView checkBoxV = (ImageView) view.findViewById(R.id.custom_checkbox);
                
                lastModV.setVisibility(View.VISIBLE);
                lastModV.setText(DisplayUtils.unixTimeToHumanReadable(file.getModificationTimestamp()));
                
                checkBoxV.setVisibility(View.GONE);
                
                fileSizeV.setVisibility(View.VISIBLE);
                fileSizeV.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));
                
                ImageView sharedIconV = (ImageView) view.findViewById(R.id.sharedIcon);
                

                if (file.isShareByLink()) {
                    sharedIconV.setVisibility(View.VISIBLE);
                } else {
                    sharedIconV.setVisibility(View.GONE);
                }
                
                ImageView localStateView = (ImageView) view.findViewById(R.id.localFileIndicator);
                
                if (!file.isFolder()) {
                    GridView parentList = (GridView)parent;
                    if (parentList.getChoiceMode() == GridView.CHOICE_MODE_NONE) { 
                        checkBoxV.setVisibility(View.GONE);
                    } else {
                        if (parentList.isItemChecked(position)) {
                            checkBoxV.setImageResource(android.R.drawable.checkbox_on_background);
                        } else {
                            checkBoxV.setImageResource(android.R.drawable.checkbox_off_background);
                        }
                        checkBoxV.setVisibility(View.VISIBLE);
                    }
                    
                    localStateView.bringToFront();
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
                    
                    ImageView sharedWithMeIconV = (ImageView) view.findViewById(R.id.sharedWithMeIcon);
                    if (checkIfFileIsSharedWithMe(file)) {
                        sharedWithMeIconV.setVisibility(View.VISIBLE);
                    } else {
                        sharedWithMeIconV.setVisibility(View.GONE);
                    }
                } else {
                    localStateView.setVisibility(View.INVISIBLE);
                }
                break;
            case GRID_ITEM:
                fileName = (TextView) view.findViewById(R.id.Filename);
                name = file.getFileName();
                fileName.setText(name);
                break;
            case GRID_IMAGE:
                break;
            }
            
            // For all Views
            
            // this if-else is needed even though favorite icon is visible by default
            // because android reuses views in listview
            if (!file.keepInSync()) {
                view.findViewById(R.id.favoriteIcon).setVisibility(View.GONE);
            } else {
                view.findViewById(R.id.favoriteIcon).setVisibility(View.VISIBLE);
            }
            
            // No Folder
            if (!file.isFolder()) {
                if (file.isImage() && file.isDown()){
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getStoragePath());
                    fileIcon.setImageBitmap(ThumbnailUtils.extractThumbnail(bitmap, 200, 200));
                } else {
                    fileIcon.setImageResource(DisplayUtils.getResourceId(file.getMimetype(), file.getFileName()));
                }
            } else {
                // Folder
                if (checkIfFileIsSharedWithMe(file)) {
                    fileIcon.setImageResource(R.drawable.shared_with_me_folder);
                } else if (file.isShareByLink()) {
                    // If folder is sharedByLink, icon folder must be changed to
                    // folder-public one
                    fileIcon.setImageResource(R.drawable.folder_public);
                } else {
                    fileIcon.setImageResource(DisplayUtils.getResourceId(file.getMimetype(), file.getFileName()));
                }
            }           
        }

        return view;
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

    /**
     * Change the adapted directory for a new one
     * @param directory                 New file to adapt. Can be NULL, meaning "no content to adapt".
     * @param updatedStorageManager     Optional updated storage manager; used to replace mStorageManager if is different (and not NULL)
     */
    public void swapDirectory(OCFile directory, FileDataStorageManager updatedStorageManager) {
        mFile = directory;
        if (updatedStorageManager != null && updatedStorageManager != mStorageManager) {
            mStorageManager = updatedStorageManager;
            mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);
        }
        if (mStorageManager != null) {
            mFiles = mStorageManager.getFolderContent(mFile);
            if (mJustFolders) {
                mFiles = getFolders(mFiles);
            }
        } else {
            mFiles = null;
        }
        notifyDataSetChanged();
    }
    
    
    /**
     * Filter for getting only the folders
     * @param files
     * @return Vector<OCFile>
     */
    public Vector<OCFile> getFolders(Vector<OCFile> files) {
        Vector<OCFile> ret = new Vector<OCFile>(); 
        OCFile current = null; 
        for (int i=0; i<files.size(); i++) {
            current = files.get(i);
            if (current.isFolder()) {
                ret.add(current);
            }
        }
        return ret;
    }
    
    
    /**
     * Check if parent folder does not include 'S' permission and if file/folder
     * is shared with me
     * 
     * @param file: OCFile
     * @return boolean: True if it is shared with me and false if it is not
     */
    private boolean checkIfFileIsSharedWithMe(OCFile file) {
        return (mFile.getPermissions() != null && !mFile.getPermissions().contains(PERMISSION_SHARED_WITH_ME)
                && file.getPermissions() != null && file.getPermissions().contains(PERMISSION_SHARED_WITH_ME));
    }
}
