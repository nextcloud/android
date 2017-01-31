/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author Tobias Kaminsky
 * @author David A. Velasco
 * @author masensio
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2016 ownCloud Inc.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.adapter;


import android.accounts.Account;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.services.OperationsService.OperationsServiceBinder;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.fragment.ExtendedListFragmentListener;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeTypeUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;


/**
 * This Adapter populates a ListView with all files and folders in an ownCloud
 * instance.
 */
public class FileListListAdapter extends BaseAdapter {

    private Context mContext;
    private Vector<OCFile> mFilesAll = new Vector<OCFile>();
    private Vector<OCFile> mFiles = null;
    private Vector<OCFile> mFilteredFiles = new Vector<>();
    private boolean mOnDeviceonly;
    private boolean mJustFolders;
    private boolean mShowHiddenFiles;
    private ExtendedListFragmentListener mFragmentListener;

    private FileDataStorageManager mStorageManager;
    private Account mAccount;
    private ComponentsGetter mTransferServiceGetter;

    private FilesFilter mFilesFilter;

    public FileListListAdapter(
            boolean justFolders,
            Context context,
            ComponentsGetter transferServiceGetter,
            ExtendedListFragmentListener extendedListFragmentListener
    ) {

        mJustFolders = justFolders;
        mContext = context;
        mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);
        mFragmentListener = extendedListFragmentListener;

        mTransferServiceGetter = transferServiceGetter;

        // Read sorting order, default to sort by name ascending
        FileStorageUtils.mSortOrder = PreferenceManager.getSortOrder(mContext);
        FileStorageUtils.mSortAscending = PreferenceManager.getSortAscending(mContext);

        // Fetch preferences for showing hidden files
        mShowHiddenFiles = PreferenceManager.showHiddenFilesEnabled(mContext);

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
        if (mFiles == null || mFiles.size() <= position) {
            return null;
        }
        return mFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        if (mFiles == null || mFiles.size() <= position) {
            return 0;
        }
        return mFiles.get(position).getFileId();
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        OCFile file = null;
        LayoutInflater inflator = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (mFiles != null && mFiles.size() > position) {
            file = mFiles.get(position);
        }

        // Find out which layout should be displayed
        ViewType viewType;
        if (parent instanceof GridView) {
            if (file != null && (MimeTypeUtil.isImage(file) || MimeTypeUtil.isVideo(file))) {
                viewType = ViewType.GRID_IMAGE;
            } else {
                viewType = ViewType.GRID_ITEM;
            }
        } else {
            viewType = ViewType.LIST_ITEM;
        }

        // create view only if differs, otherwise reuse
        if (convertView == null || convertView.getTag() != viewType) {
            switch (viewType) {
                case GRID_IMAGE:
                    view = inflator.inflate(R.layout.grid_image, parent, false);
                    view.setTag(ViewType.GRID_IMAGE);
                    break;
                case GRID_ITEM:
                    view = inflator.inflate(R.layout.grid_item, parent, false);
                    view.setTag(ViewType.GRID_ITEM);
                    break;
                case LIST_ITEM:
                    view = inflator.inflate(R.layout.list_item, parent, false);
                    view.setTag(ViewType.LIST_ITEM);
                    break;
            }
        }

        if (file != null) {

            ImageView fileIcon = (ImageView) view.findViewById(R.id.thumbnail);

            fileIcon.setTag(file.getFileId());
            TextView fileName;
            String name = file.getFileName();

            LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.ListItemLayout);
            linearLayout.setContentDescription("LinearLayout-" + name);

            switch (viewType) {
                case LIST_ITEM:
                    TextView fileSizeV = (TextView) view.findViewById(R.id.file_size);
                    TextView fileSizeSeparatorV = (TextView) view.findViewById(R.id.file_separator);
                    TextView lastModV = (TextView) view.findViewById(R.id.last_mod);


                    lastModV.setVisibility(View.VISIBLE);
                    lastModV.setText(DisplayUtils.getRelativeTimestamp(mContext, file.getModificationTimestamp()));


                    fileSizeSeparatorV.setVisibility(View.VISIBLE);
                    fileSizeV.setVisibility(View.VISIBLE);
                    fileSizeV.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));

                case GRID_ITEM:
                    // filename
                    fileName = (TextView) view.findViewById(R.id.Filename);
                    name = file.getFileName();
                    fileName.setText(name);

                case GRID_IMAGE:
                    // sharedIcon
                    ImageView sharedIconV = (ImageView) view.findViewById(R.id.sharedIcon);
                    if (file.isSharedViaLink()) {
                        sharedIconV.setImageResource(R.drawable.shared_via_link);
                        sharedIconV.setVisibility(View.VISIBLE);
                        sharedIconV.bringToFront();
                    } else if (file.isSharedWithSharee() || file.isSharedWithMe()) {
                        sharedIconV.setImageResource(R.drawable.shared_via_users);
                        sharedIconV.setVisibility(View.VISIBLE);
                        sharedIconV.bringToFront();
                    } else {
                        sharedIconV.setVisibility(View.GONE);
                    }

                    // local state
                    ImageView localStateView = (ImageView) view.findViewById(R.id.localFileIndicator);
                    localStateView.bringToFront();
                    FileDownloaderBinder downloaderBinder =
                            mTransferServiceGetter.getFileDownloaderBinder();
                    FileUploaderBinder uploaderBinder =
                            mTransferServiceGetter.getFileUploaderBinder();
                    OperationsServiceBinder opsBinder =
                            mTransferServiceGetter.getOperationsServiceBinder();

                    localStateView.setVisibility(View.INVISIBLE);   // default first

                    if ( //synchronizing
                            opsBinder != null &&
                                    opsBinder.isSynchronizing(mAccount, file)
                            ) {
                        localStateView.setImageResource(R.drawable.ic_synchronizing);
                        localStateView.setVisibility(View.VISIBLE);

                    } else if ( // downloading
                            downloaderBinder != null &&
                                    downloaderBinder.isDownloading(mAccount, file)
                            ) {
                        localStateView.setImageResource(R.drawable.ic_synchronizing);
                        localStateView.setVisibility(View.VISIBLE);

                    } else if ( //uploading
                            uploaderBinder != null &&
                                    uploaderBinder.isUploading(mAccount, file)
                            ) {
                        localStateView.setImageResource(R.drawable.ic_synchronizing);
                        localStateView.setVisibility(View.VISIBLE);

                    } else if (file.getEtagInConflict() != null) {   // conflict
                        localStateView.setImageResource(R.drawable.ic_synchronizing_error);
                        localStateView.setVisibility(View.VISIBLE);

                    } else if (file.isDown()) {
                        localStateView.setImageResource(R.drawable.ic_synced);
                        localStateView.setVisibility(View.VISIBLE);
                    }

                    break;
            }

            // For all Views

            ImageView checkBoxV = (ImageView) view.findViewById(R.id.custom_checkbox);
            checkBoxV.setVisibility(View.GONE);
            view.setBackgroundColor(Color.WHITE);

            AbsListView parentList = (AbsListView) parent;
            if (parentList.getChoiceMode() != AbsListView.CHOICE_MODE_NONE &&
                    parentList.getCheckedItemCount() > 0
                    ) {
                if (parentList.isItemChecked(position)) {
                    view.setBackgroundColor(mContext.getResources().getColor(
                            R.color.selected_item_background));
                    checkBoxV.setImageResource(
                            R.drawable.ic_checkbox_marked);
                } else {
                    view.setBackgroundColor(Color.WHITE);
                    checkBoxV.setImageResource(
                            R.drawable.ic_checkbox_blank_outline);
                }
                checkBoxV.setVisibility(View.VISIBLE);
            }

            // this if-else is needed even though favorite icon is visible by default
            // because android reuses views in listview
            if (!file.isFavorite()) {
                view.findViewById(R.id.favoriteIcon).setVisibility(View.GONE);
            } else {
                view.findViewById(R.id.favoriteIcon).setVisibility(View.VISIBLE);
            }

            // No Folder
            if (!file.isFolder()) {
                if ((MimeTypeUtil.isImage(file) || MimeTypeUtil.isVideo(file)) && file.getRemoteId() != null) {
                    // Thumbnail in Cache?
                    Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(file.getRemoteId());
                    if (thumbnail != null && !file.needsUpdateThumbnail()) {

                        if (MimeTypeUtil.isVideo(file)) {
                            Bitmap withOverlay = ThumbnailsCacheManager.addVideoOverlay(thumbnail);
                            fileIcon.setImageBitmap(withOverlay);
                        } else {
                            fileIcon.setImageBitmap(thumbnail);
                        }
                    } else {
                        // generate new Thumbnail
                        if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, fileIcon)) {
                            final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                                    new ThumbnailsCacheManager.ThumbnailGenerationTask(
                                            fileIcon, mStorageManager, mAccount
                                    );
                            if (thumbnail == null) {
                                if (MimeTypeUtil.isVideo(file)) {
                                    thumbnail = ThumbnailsCacheManager.mDefaultVideo;
                                } else {
                                    thumbnail = ThumbnailsCacheManager.mDefaultImg;
                                }
                            }
                            final ThumbnailsCacheManager.AsyncThumbnailDrawable asyncDrawable =
                                    new ThumbnailsCacheManager.AsyncThumbnailDrawable(
                                            mContext.getResources(),
                                            thumbnail,
                                            task
                                    );
                            fileIcon.setImageDrawable(asyncDrawable);
                            task.execute(file);
                        }
                    }

                    if (file.getMimetype().equalsIgnoreCase("image/png")) {
                        fileIcon.setBackgroundColor(mContext.getResources()
                                .getColor(R.color.background_color));
                    }


                } else {
                    fileIcon.setImageResource(MimeTypeUtil.getFileTypeIconId(file.getMimetype(),
                            file.getFileName()));
                }


            } else {
                // Folder
                fileIcon.setImageResource(
                        MimeTypeUtil.getFolderTypeIconId(
                                file.isSharedWithMe() || file.isSharedWithSharee(),
                                file.isSharedViaLink()
                        )
                );
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
     *
     * @param directory             New folder to adapt. Can be NULL, meaning
     *                              "no content to adapt".
     * @param updatedStorageManager Optional updated storage manager; used to replace
     *                              mStorageManager if is different (and not NULL)
     */
    public void swapDirectory(OCFile directory, FileDataStorageManager updatedStorageManager
            , boolean onlyOnDevice, @Nullable String constraint) {

        mOnDeviceonly = onlyOnDevice;

        if (updatedStorageManager != null && !updatedStorageManager.equals(mStorageManager)) {
            mStorageManager = updatedStorageManager;
            mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);
        }
        if (mStorageManager != null) {
            mFiles = mStorageManager.getFolderContent(directory, onlyOnDevice);

            if (mJustFolders) {
                mFiles = getFolders(mFiles);
            }
            if (!mShowHiddenFiles) {
                mFiles = filterHiddenFiles(mFiles);
            }
            mFiles = FileStorageUtils.sortOcFolder(mFiles);
            mFilesAll.addAll(mFiles);
        } else {
            mFilesAll = new Vector<>();
        }

        if (constraint != null) {
            getFilter().filter(constraint);
        } else {
            mFragmentListener.collapseFab();
            notifyDataSetChanged();
        }
    }

    /**
     * Filter for getting only the folders
     *
     * @param files Collection of files to filter
     * @return Folders in the input
     */
    public Vector<OCFile> getFolders(Vector<OCFile> files) {
        Vector<OCFile> ret = new Vector<>();
        OCFile current;
        for (int i = 0; i < files.size(); i++) {
            current = files.get(i);
            if (current.isFolder()) {
                ret.add(current);
            }
        }
        return ret;
    }


    public void setSortOrder(Integer order, boolean ascending) {

        PreferenceManager.setSortOrder(mContext, order);
        PreferenceManager.setSortAscending(mContext, ascending);

        FileStorageUtils.mSortOrder = order;
        FileStorageUtils.mSortAscending = ascending;

        mFiles = FileStorageUtils.sortOcFolder(mFiles);
        notifyDataSetChanged();
    }


    public ArrayList<OCFile> getCheckedItems(AbsListView parentList) {
        SparseBooleanArray checkedPositions = parentList.getCheckedItemPositions();
        ArrayList<OCFile> files = new ArrayList<>();
        Object item;
        for (int i = 0; i < checkedPositions.size(); i++) {
            if (checkedPositions.valueAt(i)) {
                item = getItem(checkedPositions.keyAt(i));
                if (item != null) {
                    files.add((OCFile) item);
                }
            }
        }
        return files;
    }

    /**
     * Filter for hidden files
     *
     * @param files Collection of files to filter
     * @return Non-hidden files
     */
    public Vector<OCFile> filterHiddenFiles(Vector<OCFile> files) {
        Vector<OCFile> ret = new Vector<>();
        OCFile current;
        if (files.size() > 0) {
            for (int i = 0; i < files.size(); i++) {
                current = files.get(i);
                if (!current.isHidden()) {
                    ret.add(current);
                }
            }
        }

        return ret;
    }

    private void fetchFilesRecursively(OCFile mWalkInDir) {

        if (mStorageManager != null) {
            Vector<OCFile> mDirectoryListing = mStorageManager.getFolderContent(mWalkInDir, mOnDeviceonly);

            if (mDirectoryListing.size() > 0) {
                mFilteredFiles.addAll(mDirectoryListing);

                if (mFilteredFiles.size() > 100) {
                    Set<OCFile> unique = new HashSet<>();
                    unique.addAll(mFilteredFiles);
                    if (unique.size() > 100) {
                        return;
                    }
                }

                OCFile current;
                for (int i = 0; i < mDirectoryListing.size(); i++) {
                    current = mDirectoryListing.get(i);
                    if (current.isFolder()) {
                        fetchFilesRecursively(current);
                    }
                }

                if (!mShowHiddenFiles) {
                    mFilteredFiles = filterHiddenFiles(mFilteredFiles);
                }
            }

        } else {
            mFilteredFiles = new Vector<>();
        }
    }


    public Filter getFilter() {
        if (mFilesFilter == null) {
            mFilesFilter = new FilesFilter();
        }
        return mFilesFilter;
    }

    private class FilesFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            mFilteredFiles = new Vector<>();
            Vector<OCFile> ocFileVector = new Vector<>();
            if (mStorageManager != null) {
                fetchFilesRecursively(mStorageManager.getFileByPath("/"));

                if (constraint != null && constraint.length() > 0) {
                    for (int i = 0; i < mFilteredFiles.size(); i++) {
                        OCFile currentFile = mFilteredFiles.get(i);
                        if (currentFile.getFileName().toLowerCase().contains(constraint) &&
                                !ocFileVector.contains(currentFile)) {
                            ocFileVector.add(currentFile);
                        }
                    }
                } else {
                    Set<OCFile> unique = new HashSet<>();
                    unique.addAll(mFilteredFiles);
                    ocFileVector.addAll(unique);
                }
            }
            results.values = ocFileVector;
            results.count = ocFileVector.size();
            mFilteredFiles = new Vector<>();


            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            Vector<OCFile> ocFiles = (Vector<OCFile>) results.values;
            boolean showHundredFilesMessage = results.count > 100;
            mFiles = new Vector<>();
            if (ocFiles != null && ocFiles.size() > 0) {
                if (results.count > 100) {
                    mFiles.addAll(ocFiles.subList(0, 100));
                } else {
                    mFiles.addAll(ocFiles);
                }
                mFiles = FileStorageUtils.sortOcFolder(mFiles);
            }

            if (mFragmentListener != null) {
                mFragmentListener.showHundredFilesMessage(showHundredFilesMessage);
                mFragmentListener.endFilterRefresh();
            }

            notifyDataSetChanged();

        }
    }

}
