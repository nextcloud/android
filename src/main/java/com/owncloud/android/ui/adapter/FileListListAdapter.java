/*
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author Tobias Kaminsky
 * @author David A. Velasco
 * @author masensio
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2016 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.adapter;


import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datamodel.VirtualFolderType;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.services.OperationsService.OperationsServiceBinder;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.ShareActivity;
import com.owncloud.android.ui.fragment.ExtendedListFragment;
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ThemeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;


/**
 * This Adapter populates a ListView with all files and folders in an ownCloud
 * instance.
 */
public class FileListListAdapter extends BaseAdapter {

    public static final int showFilenameColumnThreshold = 4;
    private Context mContext;
    private Vector<OCFile> mFilesAll = new Vector<OCFile>();
    private Vector<OCFile> mFiles = null;
    private boolean mJustFolders;

    private FileDataStorageManager mStorageManager;
    private Account mAccount;
    private ComponentsGetter mTransferServiceGetter;
    private OCFileListFragmentInterface OCFileListFragmentInterface;

    private FilesFilter mFilesFilter;
    private OCFile currentDirectory;
    private static final String TAG = FileListListAdapter.class.getSimpleName();

    private ArrayList<ThumbnailsCacheManager.ThumbnailGenerationTask> asyncTasks = new ArrayList<>();

    public FileListListAdapter(
            boolean justFolders,
            Context context,
            ComponentsGetter transferServiceGetter,
            OCFileListFragmentInterface OCFileListFragmentInterface
    ) {

        this.OCFileListFragmentInterface = OCFileListFragmentInterface;
        mJustFolders = justFolders;
        mContext = context;
        mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);

        mTransferServiceGetter = transferServiceGetter;

        // Read sorting order, default to sort by name ascending
        FileStorageUtils.mSortOrder = PreferenceManager.getSortOrder(mContext);
        FileStorageUtils.mSortAscending = PreferenceManager.getSortAscending(mContext);

        // initialise thumbnails cache on background thread
        new ThumbnailsCacheManager.InitDiskCacheTask().execute();
    }

    public FileListListAdapter(
            boolean justFolders,
            Context context,
            ComponentsGetter transferServiceGetter,
            OCFileListFragmentInterface OCFileListFragmentInterface,
            FileDataStorageManager fileDataStorageManager
    ) {
        this(justFolders, context, transferServiceGetter, OCFileListFragmentInterface);
        mStorageManager = fileDataStorageManager;
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

    public void setFavoriteAttributeForItemID(String fileId, boolean favorite) {
        for (int i = 0; i < mFiles.size(); i++) {
            if (mFiles.get(i).getRemoteId().equals(fileId)) {
                mFiles.get(i).setFavorite(favorite);
                break;
            }
        }

        for (int i = 0; i < mFilesAll.size(); i++) {
            if (mFilesAll.get(i).getRemoteId().equals(fileId)) {
                mFilesAll.get(i).setFavorite(favorite);
                break;
            }
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
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
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

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
                    view = inflater.inflate(R.layout.grid_image, parent, false);
                    view.setTag(ViewType.GRID_IMAGE);
                    break;
                case GRID_ITEM:
                    view = inflater.inflate(R.layout.grid_item, parent, false);
                    view.setTag(ViewType.GRID_ITEM);
                    break;
                case LIST_ITEM:
                    view = inflater.inflate(R.layout.list_item, parent, false);
                    view.setTag(ViewType.LIST_ITEM);
                    break;
            }
        }

        if (file != null) {
            ImageView fileIcon = (ImageView) view.findViewById(R.id.thumbnail);
            ImageView sharedIconV = (ImageView) view.findViewById(R.id.sharedIcon);
            if (file.isSharedWithSharee() || file.isSharedWithMe()) {
                sharedIconV.setImageResource(R.drawable.shared_via_users);
            }

            fileIcon.setTag(file.getFileId());
            TextView fileName;
            String name = file.getFileName();

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

                    // Shared icon clickable
                    if (file.isSharedViaLink() || file.isSharedWithSharee()) {
                        final OCFile temp = file;
                        sharedIconV.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(mContext, ShareActivity.class);
                                intent.putExtra(FileActivity.EXTRA_FILE, temp);
                                intent.putExtra(FileActivity.EXTRA_ACCOUNT, mAccount);
                                mContext.startActivity(intent);
                            }
                        });
                    }


                case GRID_ITEM:
                    // filename
                    fileName = (TextView) view.findViewById(R.id.Filename);
                    fileName.setText(name);

                    if (OCFileListFragmentInterface.getColumnSize() > showFilenameColumnThreshold
                            && viewType == ViewType.GRID_ITEM) {
                        fileName.setVisibility(View.GONE);
                    }

                case GRID_IMAGE:
                    // sharedIcon
                    if (file.isSharedViaLink()) {
                        sharedIconV.setVisibility(View.VISIBLE);
                        sharedIconV.bringToFront();
                    } else if (file.isSharedWithSharee() || file.isSharedWithMe()) {
                        sharedIconV.setVisibility(View.VISIBLE);
                        sharedIconV.bringToFront();
                    } else {
                        sharedIconV.setVisibility(View.GONE);
                    }

                    // local state
                    ImageView localStateView = (ImageView) view.findViewById(R.id.localFileIndicator);
                    localStateView.bringToFront();
                    FileDownloaderBinder downloaderBinder = mTransferServiceGetter.getFileDownloaderBinder();
                    FileUploaderBinder uploaderBinder = mTransferServiceGetter.getFileUploaderBinder();
                    OperationsServiceBinder opsBinder = mTransferServiceGetter.getOperationsServiceBinder();

                    localStateView.setVisibility(View.INVISIBLE);   // default first

                    if (opsBinder != null && opsBinder.isSynchronizing(mAccount, file)) {
                        //synchronizing
                        localStateView.setImageResource(R.drawable.ic_synchronizing);
                        localStateView.setVisibility(View.VISIBLE);

                    } else if (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, file)) {
                        // downloading
                        localStateView.setImageResource(R.drawable.ic_synchronizing);
                        localStateView.setVisibility(View.VISIBLE);

                    } else if (uploaderBinder != null && uploaderBinder.isUploading(mAccount, file)) {
                        //uploading
                        localStateView.setImageResource(R.drawable.ic_synchronizing);
                        localStateView.setVisibility(View.VISIBLE);

                    } else if (file.getEtagInConflict() != null) {
                        // conflict
                        localStateView.setImageResource(R.drawable.ic_synchronizing_error);
                        localStateView.setVisibility(View.VISIBLE);

                    } else if (file.isDown()) {
                        localStateView.setImageResource(R.drawable.ic_synced);
                        localStateView.setVisibility(View.VISIBLE);
                    }

                    break;
            }

            // For all Views
            if (file.getIsFavorite()) {
                view.findViewById(R.id.favorite_action).setVisibility(View.VISIBLE);
            } else {
                view.findViewById(R.id.favorite_action).setVisibility(View.GONE);
            }

            ImageView checkBoxV = (ImageView) view.findViewById(R.id.custom_checkbox);
            checkBoxV.setVisibility(View.GONE);
            view.setBackgroundColor(Color.WHITE);

            AbsListView parentList = (AbsListView) parent;

            if (parentList.getChoiceMode() != AbsListView.CHOICE_MODE_NONE && parentList.getCheckedItemCount() > 0) {
                if (parentList.isItemChecked(position)) {
                    view.setBackgroundColor(mContext.getResources().getColor(R.color.selected_item_background));
                    checkBoxV.setImageDrawable(ThemeUtils.tintDrawable(R.drawable.ic_checkbox_marked,
                            ThemeUtils.primaryColor()));
                } else {
                    view.setBackgroundColor(Color.WHITE);
                    checkBoxV.setImageResource(R.drawable.ic_checkbox_blank_outline);
                }
                checkBoxV.setVisibility(View.VISIBLE);
            }

            // this if-else is needed even though kept-in-sync icon is visible by default
            // because android reuses views in listview
            if (!file.isAvailableOffline()) {
                view.findViewById(R.id.keptOfflineIcon).setVisibility(View.GONE);
            } else {
                view.findViewById(R.id.keptOfflineIcon).setVisibility(View.VISIBLE);
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
                            try {
                                final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                                        new ThumbnailsCacheManager.ThumbnailGenerationTask(
                                                fileIcon, mStorageManager, mAccount, asyncTasks);

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
                                asyncTasks.add(task);
                                task.execute(file);
                            } catch (IllegalArgumentException e) {
                                Log_OC.d(TAG, "ThumbnailGenerationTask : " + e.getMessage());
                            }
                        }
                    }

                    if (file.getMimetype().equalsIgnoreCase("image/png")) {
                        fileIcon.setBackgroundColor(mContext.getResources().getColor(R.color.background_color));
                    }


                } else {
                    fileIcon.setImageDrawable(
                            MimeTypeUtil.getFileTypeIcon(file.getMimetype(), file.getFileName(), mAccount)
                    );
                }


            } else {
                // Folder
                fileIcon.setImageDrawable(MimeTypeUtil.getFolderTypeIcon(file.isSharedWithMe() ||
                        file.isSharedWithSharee(), file.isSharedViaLink()));
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
            , boolean onlyOnDevice) {
        if (updatedStorageManager != null && !updatedStorageManager.equals(mStorageManager)) {
            mStorageManager = updatedStorageManager;
            mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);
        }
        if (mStorageManager != null) {
            mFiles = mStorageManager.getFolderContent(directory, onlyOnDevice);

            if (mJustFolders) {
                mFiles = getFolders(mFiles);
            }
            if (!PreferenceManager.showHiddenFilesEnabled(mContext)) {
                mFiles = filterHiddenFiles(mFiles);
            }
            mFiles = FileStorageUtils.sortOcFolder(mFiles);
            mFilesAll.clear();
            mFilesAll.addAll(mFiles);

            currentDirectory = directory;
        } else {
            mFiles = null;
            mFilesAll.clear();
        }

        notifyDataSetChanged();
    }

    private void searchForLocalFileInDefaultPath(OCFile file) {
        if (file.getStoragePath() == null && !file.isFolder()) {
            File f = new File(FileStorageUtils.getDefaultSavePathFor(mAccount.name, file));
            if (f.exists()) {
                file.setStoragePath(f.getAbsolutePath());
                file.setLastSyncDateForData(f.lastModified());
            }
        }
    }

    public void setData(ArrayList<Object> objects, ExtendedListFragment.SearchType searchType, FileDataStorageManager storageManager) {
        if (storageManager != null && mStorageManager == null) {
            mStorageManager = storageManager;
        }
        mFiles = new Vector<>();

        // early exit
        if (objects.size() > 0 && mStorageManager != null) {
            if (searchType.equals(ExtendedListFragment.SearchType.SHARED_FILTER)) {
                parseShares(objects);
            } else {
                parseVirtuals(objects, searchType);
            }
        }

        if (!searchType.equals(ExtendedListFragment.SearchType.PHOTO_SEARCH) &&
                !searchType.equals(ExtendedListFragment.SearchType.PHOTOS_SEARCH_FILTER) &&
                !searchType.equals(ExtendedListFragment.SearchType.RECENTLY_MODIFIED_SEARCH) &&
                !searchType.equals(ExtendedListFragment.SearchType.RECENTLY_MODIFIED_SEARCH_FILTER)) {
            mFiles = FileStorageUtils.sortOcFolder(mFiles);
        } else {
            mFiles = FileStorageUtils.sortOcFolderDescDateModified(mFiles);
        }

        mFilesAll = new Vector<>();
        mFilesAll.addAll(mFiles);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
                OCFileListFragmentInterface.finishedFiltering();
            }
        });
    }

    private void parseShares(ArrayList<Object> objects) {
        ArrayList<OCShare> shares = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            // check type before cast as of long running data fetch it is possible that old result is filled
            if (objects.get(i) instanceof OCShare) {
                OCShare ocShare = (OCShare) objects.get(i);

                shares.add(ocShare);

                // get ocFile from Server to have an up-to-date copy
                ReadRemoteFileOperation operation = new ReadRemoteFileOperation(ocShare.getPath());
                RemoteOperationResult result = operation.execute(mAccount, mContext);
                if (result.isSuccess()) {
                    OCFile file = FileStorageUtils.fillOCFile((RemoteFile) result.getData().get(0));
                    searchForLocalFileInDefaultPath(file);
                    file = mStorageManager.saveFileWithParent(file, mContext);

                    ShareType newShareType = ocShare.getShareType();
                    if (newShareType == ShareType.PUBLIC_LINK) {
                        file.setShareViaLink(true);
                    } else if (newShareType == ShareType.USER || newShareType == ShareType.GROUP ||
                                    newShareType == ShareType.EMAIL || newShareType == ShareType.FEDERATED) {
                        file.setShareWithSharee(true);
                    }

                    mStorageManager.saveFile(file);

                    if (!mFiles.contains(file)) {
                        mFiles.add(file);
                    }
                } else {
                    Log_OC.e(TAG, "Error in getting prop for file: " + ocShare.getPath());
                }
            }
        }
        mStorageManager.saveShares(shares);
    }

    private void parseVirtuals(ArrayList<Object> objects, ExtendedListFragment.SearchType searchType) {
        VirtualFolderType type;
        boolean onlyImages = false;
        switch (searchType) {
            case FAVORITE_SEARCH:
                type = VirtualFolderType.FAVORITE;
                break;
            case PHOTO_SEARCH:
                type = VirtualFolderType.PHOTOS;
                onlyImages = true;
                break;
            default:
                type = VirtualFolderType.NONE;
                break;
        }

        mStorageManager.deleteVirtuals(type);

        ArrayList<ContentValues> contentValues = new ArrayList<>();

        for (int i = 0; i < objects.size(); i++) {
            OCFile ocFile = FileStorageUtils.fillOCFile((RemoteFile) objects.get(i));
            searchForLocalFileInDefaultPath(ocFile);
            ocFile = mStorageManager.saveFileWithParent(ocFile, mContext);

            if (!onlyImages || MimeTypeUtil.isImage(ocFile)) {
                mFiles.add(ocFile);
            }

            ContentValues cv = new ContentValues();
            cv.put(ProviderMeta.ProviderTableMeta.VIRTUAL_TYPE, type.toString());
            cv.put(ProviderMeta.ProviderTableMeta.VIRTUAL_OCFILE_ID, ocFile.getFileId());

            contentValues.add(cv);
        }

        mStorageManager.saveVirtuals(type, contentValues);
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
            Vector<OCFile> filteredFiles = new Vector<>();

            if (!TextUtils.isEmpty(constraint)) {
                for (int i = 0; i < mFilesAll.size(); i++) {
                    OCFile currentFile = mFilesAll.get(i);
                    if (currentFile.getParentRemotePath().equals(currentDirectory.getRemotePath()) &&
                            currentFile.getFileName().toLowerCase().contains(constraint.toString().toLowerCase()) &&
                            !filteredFiles.contains(currentFile)) {
                        filteredFiles.add(currentFile);
                    }
                }
            }

            results.values = filteredFiles;
            results.count = filteredFiles.size();

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
            Vector<OCFile> ocFiles = (Vector<OCFile>) results.values;
            mFiles = new Vector<>();
            if (ocFiles != null && ocFiles.size() > 0) {
                mFiles.addAll(ocFiles);
                if (!PreferenceManager.showHiddenFilesEnabled(mContext)) {
                    mFiles = filterHiddenFiles(mFiles);
                }
                mFiles = FileStorageUtils.sortOcFolder(mFiles);
            }

            notifyDataSetChanged();
            OCFileListFragmentInterface.finishedFiltering();

        }
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
        for (int i = 0; i < files.size(); i++) {
            current = files.get(i);
            if (!current.isHidden() && !ret.contains(current)) {
                ret.add(current);
            }
        }
        return ret;
    }

    public void cancelAllPendingTasks() {
        for (ThumbnailsCacheManager.ThumbnailGenerationTask task : asyncTasks) {
            if (task != null) {
                task.cancel(true);
                if (task.getGetMethod() != null) {
                    Log_OC.d(TAG, "cancel: abort get method directly");
                    task.getGetMethod().abort();
                }
            }
        }

        asyncTasks.clear();
    }

}
