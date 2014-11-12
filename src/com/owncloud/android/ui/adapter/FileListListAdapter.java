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


import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import third_parties.daveKoeller.AlphanumComparator;
import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datamodel.ThumbnailsCacheManager.AsyncDrawable;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;


/**
 * This Adapter populates a ListView with all files and folders in an ownCloud
 * instance.
 * 
 * @author Bartek Przybylski
 * @author Tobias Kaminsky
 * @author David A. Velasco
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
    private Integer mSortOrder;
    public static final Integer SORT_NAME = 0;
    public static final Integer SORT_DATE = 1;
    public static final Integer SORT_SIZE = 2;
    private Boolean mSortAscending;
    private SharedPreferences mAppPreferences;
    
    public FileListListAdapter(
            boolean justFolders, 
            Context context, 
            ComponentsGetter transferServiceGetter
            ) {

        mJustFolders = justFolders;
        mContext = context;
        mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);

        mTransferServiceGetter = transferServiceGetter;
        
        mAppPreferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        
        // Read sorting order, default to sort by name ascending
        mSortOrder = mAppPreferences
                .getInt("sortOrder", 0);
        mSortAscending = mAppPreferences.getBoolean("sortAscending", true);
        
        // initialise thumbnails cache on background thread
        new ThumbnailsCacheManager.InitDiskCacheTask().execute();

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
        View view = convertView;
        if (view == null) {
            LayoutInflater inflator = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflator.inflate(R.layout.list_item, null);
        }
         
        if (mFiles != null && mFiles.size() > position) {
            OCFile file = mFiles.get(position);
            TextView fileName = (TextView) view.findViewById(R.id.Filename);           
            String name = file.getFileName();

            fileName.setText(name);
            ImageView fileIcon = (ImageView) view.findViewById(R.id.imageView1);
            fileIcon.setTag(file.getFileId());
            ImageView sharedIconV = (ImageView) view.findViewById(R.id.sharedIcon);
            ImageView sharedWithMeIconV = (ImageView) view.findViewById(R.id.sharedWithMeIcon);
            sharedWithMeIconV.setVisibility(View.GONE);

            ImageView localStateView = (ImageView) view.findViewById(R.id.imageView2);
            localStateView.bringToFront();
            FileDownloaderBinder downloaderBinder = 
                    mTransferServiceGetter.getFileDownloaderBinder();
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
                lastModV.setText(showRelativeTimestamp(file));
                // this if-else is needed even thoe fav icon is visible by default
                // because android reuses views in listview
                if (!file.keepInSync()) {
                    view.findViewById(R.id.imageView3).setVisibility(View.GONE);
                } else {
                    view.findViewById(R.id.imageView3).setVisibility(View.VISIBLE);
                }
                
                ListView parentList = (ListView)parent;
                if (parentList.getChoiceMode() == ListView.CHOICE_MODE_NONE) { 
                    checkBoxV.setVisibility(View.GONE);
                } else {
                    if (parentList.isItemChecked(position)) {
                        checkBoxV.setImageResource(android.R.drawable.checkbox_on_background);
                    } else {
                        checkBoxV.setImageResource(android.R.drawable.checkbox_off_background);
                    }
                    checkBoxV.setVisibility(View.VISIBLE);
                }               
                
                // get Thumbnail if file is image
                if (file.isImage() && file.getRemoteId() != null){
                     // Thumbnail in Cache?
                    Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                            String.valueOf(file.getRemoteId())
                    );
                    if (thumbnail != null && !file.needsUpdateThumbnail()){
                        fileIcon.setImageBitmap(thumbnail);
                    } else {
                        // generate new Thumbnail
                        if (ThumbnailsCacheManager.cancelPotentialWork(file, fileIcon)) {
                            final ThumbnailsCacheManager.ThumbnailGenerationTask task = 
                                    new ThumbnailsCacheManager.ThumbnailGenerationTask(
                                            fileIcon, mStorageManager, mAccount
                                    );
                            if (thumbnail == null) {
                                thumbnail = ThumbnailsCacheManager.mDefaultImg;
                            }
                            final AsyncDrawable asyncDrawable = new AsyncDrawable(
                                    mContext.getResources(), 
                                    thumbnail, 
                                    task
                            );
                            fileIcon.setImageDrawable(asyncDrawable);
                            task.execute(file);
                        }
                    }
                } else {
                    fileIcon.setImageResource(
                            DisplayUtils.getResourceId(file.getMimetype(), file.getFileName())
                    );
                }

                if (checkIfFileIsSharedWithMe(file)) {
                    sharedWithMeIconV.setVisibility(View.VISIBLE);
                }
            } 
            else {
                  // TODO Re-enable when server supports folder-size calculation
//                if (FileStorageUtils.getDefaultSavePathFor(mAccount.name, file) != null){
//                    fileSizeV.setVisibility(View.VISIBLE);
//                    fileSizeV.setText(getFolderSizeHuman(FileStorageUtils.getDefaultSavePathFor(mAccount.name, file)));
//                } else {
                    fileSizeV.setVisibility(View.INVISIBLE);
//                }

                lastModV.setVisibility(View.VISIBLE);
                lastModV.setText(showRelativeTimestamp(file));
                checkBoxV.setVisibility(View.GONE);
                view.findViewById(R.id.imageView3).setVisibility(View.GONE);

                if (checkIfFileIsSharedWithMe(file)) {
                    fileIcon.setImageResource(R.drawable.shared_with_me_folder);
                    sharedWithMeIconV.setVisibility(View.VISIBLE);
                } else {
                    fileIcon.setImageResource(
                            DisplayUtils.getResourceId(file.getMimetype(), file.getFileName())
                    );
                }

                // If folder is sharedByLink, icon folder must be changed to
                // folder-public one
                if (file.isShareByLink()) {
                    fileIcon.setImageResource(R.drawable.folder_public);
                }
            }

            if (file.isShareByLink()) {
                sharedIconV.setVisibility(View.VISIBLE);
            } else {
                sharedIconV.setVisibility(View.GONE);
            }
        }

        return view;
    }

    /**
     * Local Folder size in human readable format
     * 
     * @param path
     *            String
     * @return Size in human readable format
     */
    private String getFolderSizeHuman(String path) {

        File dir = new File(path);

        if (dir.exists()) {
            long bytes = getFolderSize(dir);
            return DisplayUtils.bytesToHumanReadable(bytes);
        }

        return "0 B";
    }

    /**
     * Local Folder size
     * @param dir File
     * @return Size in bytes
     */
    private long getFolderSize(File dir) {
        if (dir.exists()) {
            long result = 0;
            File[] fileList = dir.listFiles();
            for(int i = 0; i < fileList.length; i++) {
                if(fileList[i].isDirectory()) {
                    result += getFolderSize(fileList[i]);
                } else {
                    result += fileList[i].length();
                }
            }
            return result;
        }
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

    /**
     * Change the adapted directory for a new one
     * @param directory                 New file to adapt. Can be NULL, meaning 
     *                                  "no content to adapt".
     * @param updatedStorageManager     Optional updated storage manager; used to replace 
     *                                  mStorageManager if is different (and not NULL)
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

        sortDirectory();
    }
    
    /**
     * Sorts all filenames, regarding last user decision 
     */
    private void sortDirectory(){
        switch (mSortOrder){
        case 0:
            sortByName(mSortAscending);
            break;
        case 1:
            sortByDate(mSortAscending);
            break;
        case 2: 
            sortBySize(mSortAscending);
            break;
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
        return (mFile.getPermissions() != null 
                && !mFile.getPermissions().contains(PERMISSION_SHARED_WITH_ME)
                && file.getPermissions() != null 
                && file.getPermissions().contains(PERMISSION_SHARED_WITH_ME));
    }

    /**
     * Sorts list by Date
     * @param sortAscending true: ascending, false: descending
     */
    private void sortByDate(boolean sortAscending){
        final Integer val;
        if (sortAscending){
            val = 1;
        } else {
            val = -1;
        }
        
        Collections.sort(mFiles, new Comparator<OCFile>() {
            public int compare(OCFile o1, OCFile o2) {
                if (o1.isFolder() && o2.isFolder()) {
                    Long obj1 = o1.getModificationTimestamp();
                    return val * obj1.compareTo(o2.getModificationTimestamp());
                }
                else if (o1.isFolder()) {
                    return -1;
                } else if (o2.isFolder()) {
                    return 1;
                } else if (o1.getModificationTimestamp() == 0 || o2.getModificationTimestamp() == 0){
                    return 0;
                } else {
                    Long obj1 = o1.getModificationTimestamp();
                    return val * obj1.compareTo(o2.getModificationTimestamp());
                }
            }
        });
    }

    /**
     * Sorts list by Size
     * @param sortAscending true: ascending, false: descending
     */
    private void sortBySize(boolean sortAscending){
        final Integer val;
        if (sortAscending){
            val = 1;
        } else {
            val = -1;
        }
        
        Collections.sort(mFiles, new Comparator<OCFile>() {
            public int compare(OCFile o1, OCFile o2) {
                if (o1.isFolder() && o2.isFolder()) {
                    Long obj1 = getFolderSize(new File(FileStorageUtils.getDefaultSavePathFor(mAccount.name, o1)));
                    return val * obj1.compareTo(getFolderSize(new File(FileStorageUtils.getDefaultSavePathFor(mAccount.name, o2))));
                }
                else if (o1.isFolder()) {
                    return -1;
                } else if (o2.isFolder()) {
                    return 1;
                } else if (o1.getFileLength() == 0 || o2.getFileLength() == 0){
                    return 0;
                } else {
                    Long obj1 = o1.getFileLength();
                    return val * obj1.compareTo(o2.getFileLength());
                }
            }
        });
    }

    /**
     * Sorts list by Name
     * @param sortAscending true: ascending, false: descending
     */
    private void sortByName(boolean sortAscending){
        final Integer val;
        if (sortAscending){
            val = 1;
        } else {
            val = -1;
        }

        Collections.sort(mFiles, new Comparator<OCFile>() {
            public int compare(OCFile o1, OCFile o2) {
                if (o1.isFolder() && o2.isFolder()) {
                    return val * o1.getRemotePath().toLowerCase().compareTo(o2.getRemotePath().toLowerCase());
                } else if (o1.isFolder()) {
                    return -1;
                } else if (o2.isFolder()) {
                    return 1;
                }
                return val * new AlphanumComparator().compare(o1, o2);
            }
        });
    }

    public void setSortOrder(Integer order, boolean ascending) {
        SharedPreferences.Editor editor = mAppPreferences.edit();
        editor.putInt("sortOrder", order);
        editor.putBoolean("sortAscending", ascending);
        editor.commit();
        
        mSortOrder = order;
        mSortAscending = ascending;
        
        sortDirectory();
    }    
    
    private CharSequence showRelativeTimestamp(OCFile file){
        return DisplayUtils.getRelativeDateTimeString(mContext, file.getModificationTimestamp(),
                DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
    }
}
