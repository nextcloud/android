/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter;


import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datamodel.VirtualFolderType;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.operations.RemoteOperationFailedException;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.fragment.ExtendedListFragment;
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ThemeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

/**
 * This Adapter populates a RecyclerView with all files and folders in a Nextcloud instance.
 */
public class OCFileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int showFilenameColumnThreshold = 4;
    private final FileDownloader.FileDownloaderBinder downloaderBinder;
    private final FileUploader.FileUploaderBinder uploaderBinder;
    private final OperationsService.OperationsServiceBinder operationsServiceBinder;
    private Context mContext;
    private List<OCFile> mFiles = new ArrayList<>();
    private List<OCFile> mFilesAll = new ArrayList<>();
    private boolean mHideItemOptions;
    private boolean gridView;
    private boolean multiSelect;
    private Set<OCFile> checkedFiles;

    private FileDataStorageManager mStorageManager;
    private Account mAccount;
    private OCFileListFragmentInterface ocFileListFragmentInterface;

    private FilesFilter mFilesFilter;
    private OCFile currentDirectory;
    private static final String TAG = OCFileListAdapter.class.getSimpleName();

    private static final int VIEWTYPE_FOOTER = 0;
    private static final int VIEWTYPE_ITEM = 1;
    private static final int VIEWTYPE_IMAGE = 2;

    private List<ThumbnailsCacheManager.ThumbnailGenerationTask> asyncTasks = new ArrayList<>();
    private boolean onlyOnDevice = false;

    public OCFileListAdapter(Context context, ComponentsGetter transferServiceGetter,
                             OCFileListFragmentInterface ocFileListFragmentInterface, boolean argHideItemOptions,
                             boolean gridView) {

        this.ocFileListFragmentInterface = ocFileListFragmentInterface;
        mContext = context;
        mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);
        mHideItemOptions = argHideItemOptions;
        this.gridView = gridView;
        checkedFiles = new HashSet<>();

        downloaderBinder = transferServiceGetter.getFileDownloaderBinder();
        uploaderBinder = transferServiceGetter.getFileUploaderBinder();
        operationsServiceBinder = transferServiceGetter.getOperationsServiceBinder();

        // initialise thumbnails cache on background thread
        new ThumbnailsCacheManager.InitDiskCacheTask().execute();
    }

    public boolean isMultiSelect() {
        return multiSelect;
    }

    public void setMultiSelect(boolean bool) {
        multiSelect = bool;
        notifyDataSetChanged();
    }

    public boolean isCheckedFile(OCFile file) {
        return checkedFiles.contains(file);
    }

    public void removeCheckedFile(OCFile file) {
        checkedFiles.remove(file);
    }

    public void addCheckedFile(OCFile file) {
        checkedFiles.add(file);
    }

    public void addAllFilesToCheckedFiles() {
        checkedFiles.addAll(mFiles);
    }

    public void removeAllFilesFromCheckedFiles() {
        checkedFiles.clear();
    }

    public int getItemPosition(OCFile file) {
        return mFiles.indexOf(file);
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

    public void setEncryptionAttributeForItemID(String fileId, boolean encrypted) {
        for (int i = 0; i < mFiles.size(); i++) {
            if (mFiles.get(i).getRemoteId().equals(fileId)) {
                mFiles.get(i).setEncrypted(encrypted);
                break;
            }
        }

        for (int i = 0; i < mFilesAll.size(); i++) {
            if (mFilesAll.get(i).getRemoteId().equals(fileId)) {
                mFilesAll.get(i).setEncrypted(encrypted);
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
    public int getItemCount() {
        return mFiles.size() + 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            default:
            case VIEWTYPE_ITEM:
                if (gridView) {
                    View itemView = LayoutInflater.from(mContext).inflate(R.layout.grid_item, parent, false);
                    return new OCFileListGridItemViewHolder(itemView);
                } else {
                    View itemView = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
                    return new OCFileListItemViewHolder(itemView);
                }

            case VIEWTYPE_IMAGE:
                if (gridView) {
                    View itemView = LayoutInflater.from(mContext).inflate(R.layout.grid_image, parent, false);
                    return new OCFileListGridImageViewHolder(itemView);
                } else {
                    View itemView = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
                    return new OCFileListItemViewHolder(itemView);
                }

            case VIEWTYPE_FOOTER:
                View itemView = LayoutInflater.from(mContext).inflate(R.layout.list_footer, parent, false);
                return new OCFileListFooterViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof OCFileListFooterViewHolder) {
            ((OCFileListFooterViewHolder) holder).footerText.setText(getFooterText());
        } else {
            OCFileListGridImageViewHolder gridViewHolder = (OCFileListGridImageViewHolder) holder;

            OCFile file = mFiles.get(position);

            boolean gridImage = MimeTypeUtil.isImage(file) || MimeTypeUtil.isVideo(file);

            gridViewHolder.thumbnail.setTag(file.getFileId());
            setThumbnail(file, gridViewHolder.thumbnail);

            if (isCheckedFile(file)) {
                gridViewHolder.itemLayout.setBackgroundColor(mContext.getResources()
                        .getColor(R.color.selected_item_background));
                gridViewHolder.checkbox.setImageDrawable(ThemeUtils.tintDrawable(R.drawable.ic_checkbox_marked,
                        ThemeUtils.primaryColor(mContext)));
            } else {
                gridViewHolder.itemLayout.setBackgroundColor(Color.WHITE);
                gridViewHolder.checkbox.setImageResource(R.drawable.ic_checkbox_blank_outline);
            }

            gridViewHolder.itemLayout.setOnClickListener(v -> ocFileListFragmentInterface.onItemClicked(file));

            if (!mHideItemOptions) {
                gridViewHolder.itemLayout.setLongClickable(true);
                gridViewHolder.itemLayout.setOnLongClickListener(v ->
                        ocFileListFragmentInterface.onLongItemClicked(file));
            }

            if (holder instanceof OCFileListItemViewHolder) {
                OCFileListItemViewHolder itemViewHolder = (OCFileListItemViewHolder) holder;

                if (onlyOnDevice) {
                    File localFile = new File(file.getStoragePath());

                    long localSize;
                    if (localFile.isDirectory()) {
                        localSize = FileStorageUtils.getFolderSize(localFile);
                    } else {
                        localSize = localFile.length();
                    }

                    itemViewHolder.fileSize.setText(DisplayUtils.bytesToHumanReadable(localSize));
                } else {
                    itemViewHolder.fileSize.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));
                }
                itemViewHolder.lastModification.setText(DisplayUtils.getRelativeTimestamp(mContext,
                        file.getModificationTimestamp()));

                if (multiSelect || gridView || mHideItemOptions) {
                    itemViewHolder.overflowMenu.setVisibility(View.GONE);
                } else {
                    itemViewHolder.overflowMenu.setVisibility(View.VISIBLE);
                    itemViewHolder.overflowMenu.setOnClickListener(view -> ocFileListFragmentInterface
                            .onOverflowIconClicked(file, view));
                }
            }

            gridViewHolder.localFileIndicator.setVisibility(View.INVISIBLE);   // default first

            if (operationsServiceBinder != null && operationsServiceBinder.isSynchronizing(mAccount, file)) {
                //synchronizing
                gridViewHolder.localFileIndicator.setImageResource(R.drawable.ic_synchronizing);
                gridViewHolder.localFileIndicator.setVisibility(View.VISIBLE);

            } else if (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, file)) {
                // downloading
                gridViewHolder.localFileIndicator.setImageResource(R.drawable.ic_synchronizing);
                gridViewHolder.localFileIndicator.setVisibility(View.VISIBLE);

            } else if (uploaderBinder != null && uploaderBinder.isUploading(mAccount, file)) {
                //uploading
                gridViewHolder.localFileIndicator.setImageResource(R.drawable.ic_synchronizing);
                gridViewHolder.localFileIndicator.setVisibility(View.VISIBLE);

            } else if (file.getEtagInConflict() != null) {
                // conflict
                gridViewHolder.localFileIndicator.setImageResource(R.drawable.ic_synchronizing_error);
                gridViewHolder.localFileIndicator.setVisibility(View.VISIBLE);

            } else if (file.isDown()) {
                gridViewHolder.localFileIndicator.setImageResource(R.drawable.ic_synced);
                gridViewHolder.localFileIndicator.setVisibility(View.VISIBLE);
            }

            gridViewHolder.favorite.setVisibility(file.isFavorite() ? View.VISIBLE : View.GONE);
            gridViewHolder.offlineIcon.setVisibility(file.isAvailableOffline() ? View.VISIBLE : View.GONE);

            if (multiSelect) {
                gridViewHolder.checkbox.setVisibility(View.VISIBLE);
            } else {
                gridViewHolder.checkbox.setVisibility(View.GONE);
            }

            if (holder instanceof OCFileListGridItemViewHolder) {
                OCFileListGridItemViewHolder gridItemViewHolder = (OCFileListGridItemViewHolder) holder;

                gridItemViewHolder.fileName.setText(file.getFileName());

                if (gridView && gridImage) {
                    gridItemViewHolder.fileName.setVisibility(View.GONE);
                } else {
                    if (gridView && ocFileListFragmentInterface.getColumnSize() > showFilenameColumnThreshold) {
                        gridItemViewHolder.fileName.setVisibility(View.GONE);
                    } else {
                        gridItemViewHolder.fileName.setVisibility(View.VISIBLE);
                    }
                }
            }

            if (mHideItemOptions) {
                gridViewHolder.shared.setVisibility(View.GONE);
            } else {
                showShareIcon(gridViewHolder, file);
            }
        }
    }

    private void setThumbnail(OCFile file, ImageView thumbnailView) {
        if (file.isFolder()) {
            thumbnailView.setImageDrawable(MimeTypeUtil.getFolderTypeIcon(file.isSharedWithMe() ||
                            file.isSharedWithSharee(), file.isSharedViaLink(), file.isEncrypted(), file.getMountType(),
                    mContext));
        } else {
            if ((MimeTypeUtil.isImage(file) || MimeTypeUtil.isVideo(file)) && file.getRemoteId() != null) {
                // Thumbnail in cache?
                Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                        ThumbnailsCacheManager.PREFIX_THUMBNAIL + String.valueOf(file.getRemoteId())
                );

                if (thumbnail != null && !file.needsUpdateThumbnail()) {
                    if (MimeTypeUtil.isVideo(file)) {
                        Bitmap withOverlay = ThumbnailsCacheManager.addVideoOverlay(thumbnail);
                        thumbnailView.setImageBitmap(withOverlay);
                    } else {
                        thumbnailView.setImageBitmap(thumbnail);
                    }
                } else {
                    // generate new thumbnail
                    if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, thumbnailView)) {
                        try {
                            final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                                    new ThumbnailsCacheManager.ThumbnailGenerationTask(thumbnailView, mStorageManager,
                                            mAccount, asyncTasks);

                            if (thumbnail == null) {
                                if (MimeTypeUtil.isVideo(file)) {
                                    thumbnail = ThumbnailsCacheManager.mDefaultVideo;
                                } else {
                                    thumbnail = ThumbnailsCacheManager.mDefaultImg;
                                }
                            }
                            final ThumbnailsCacheManager.AsyncThumbnailDrawable asyncDrawable =
                                    new ThumbnailsCacheManager.AsyncThumbnailDrawable(mContext.getResources(),
                                            thumbnail, task);
                            thumbnailView.setImageDrawable(asyncDrawable);
                            asyncTasks.add(task);
                            task.execute(new ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file,
                                    file.getRemoteId()));
                        } catch (IllegalArgumentException e) {
                            Log_OC.d(TAG, "ThumbnailGenerationTask : " + e.getMessage());
                        }
                    }
                }

                if ("image/png".equalsIgnoreCase(file.getMimeType())) {
                    thumbnailView.setBackgroundColor(mContext.getResources().getColor(R.color.background_color));
                }
            } else {
                thumbnailView.setImageDrawable(MimeTypeUtil.getFileTypeIcon(file.getMimeType(), file.getFileName(),
                        mAccount, mContext));
            }
        }
    }

    private String getFooterText() {
        int filesCount = 0;
        int foldersCount = 0;
        int count = mFiles.size();
        OCFile file;
        for (int i = 0; i < count; i++) {
            file = getItem(i);
            if (file.isFolder()) {
                foldersCount++;
            } else {
                if (!file.isHidden()) {
                    filesCount++;
                }
            }
        }

        return generateFooterText(filesCount, foldersCount);
    }

    private String generateFooterText(int filesCount, int foldersCount) {
        String output;
        Resources resources = mContext.getResources();

        if (filesCount + foldersCount <= 0) {
            output = "";
        } else if (foldersCount <= 0) {
            output = resources.getQuantityString(R.plurals.file_list__footer__file, filesCount, filesCount);
        } else if (filesCount <= 0) {
            output = resources.getQuantityString(R.plurals.file_list__footer__folder, foldersCount, foldersCount);
        } else {
            output = resources.getQuantityString(R.plurals.file_list__footer__file, filesCount, filesCount) + ", " +
                    resources.getQuantityString(R.plurals.file_list__footer__folder, foldersCount, foldersCount);
        }

        return output;
    }

    public OCFile getItem(int position) {
        return mFiles.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mFiles.size()) {
            return VIEWTYPE_FOOTER;
        } else {
            if (MimeTypeUtil.isImage(getItem(position))) {
                return VIEWTYPE_IMAGE;
            } else {
                return VIEWTYPE_ITEM;
            }
        }
    }

    private void showShareIcon(OCFileListGridImageViewHolder gridViewHolder, OCFile file) {
        ImageView sharedIconView = gridViewHolder.shared;
        sharedIconView.setVisibility(View.VISIBLE);
        
        if (file.isSharedWithSharee() || file.isSharedWithMe()) {
            sharedIconView.setImageResource(R.drawable.shared_via_users);
            sharedIconView.setContentDescription(mContext.getString(R.string.shared_icon_shared));
        } else if (file.isSharedViaLink()) {
            sharedIconView.setImageResource(R.drawable.shared_via_link);
            sharedIconView.setContentDescription(mContext.getString(R.string.shared_icon_shared_via_link));
        } else {
            sharedIconView.setImageResource(R.drawable.ic_unshared);
            sharedIconView.setContentDescription(mContext.getString(R.string.shared_icon_share));
        }
        sharedIconView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ocFileListFragmentInterface.onShareIconClick(file);
            }
        });
    }

    /**
     * Change the adapted directory for a new one
     *
     * @param directory             New folder to adapt. Can be NULL, meaning
     *                              "no content to adapt".
     * @param updatedStorageManager Optional updated storage manager; used to replace
     *                              mStorageManager if is different (and not NULL)
     */
    public void swapDirectory(OCFile directory, FileDataStorageManager updatedStorageManager,
                              boolean onlyOnDevice) {
        this.onlyOnDevice = onlyOnDevice;
        if (updatedStorageManager != null && !updatedStorageManager.equals(mStorageManager)) {
            mStorageManager = updatedStorageManager;
            mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);
        }
        if (mStorageManager != null) {
            mFiles = mStorageManager.getFolderContent(directory, onlyOnDevice);

            if (!PreferenceManager.showHiddenFilesEnabled(mContext)) {
                mFiles = filterHiddenFiles(mFiles);
            }
            FileSortOrder sortOrder = PreferenceManager.getSortOrder(mContext, directory);
            mFiles = sortOrder.sortCloudFiles(mFiles);
            mFilesAll.clear();
            mFilesAll.addAll(mFiles);

            currentDirectory = directory;
        } else {
            mFiles.clear();
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

    public void setData(List<Object> objects, ExtendedListFragment.SearchType searchType,
                        FileDataStorageManager storageManager, OCFile folder) {
        if (storageManager != null && mStorageManager == null) {
            mStorageManager = storageManager;
        }
        mFiles.clear();

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
            FileSortOrder sortOrder = PreferenceManager.getSortOrder(mContext, folder);
            mFiles = sortOrder.sortCloudFiles(mFiles);
        } else {
            mFiles = FileStorageUtils.sortOcFolderDescDateModified(mFiles);
        }

        mFilesAll.clear();
        mFilesAll.addAll(mFiles);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    private void parseShares(List<Object> objects) {
        List<OCShare> shares = new ArrayList<>();

        for (Object shareObject : objects) {
            // check type before cast as of long running data fetch it is possible that old result is filled
            if (shareObject instanceof OCShare) {
                OCShare ocShare = (OCShare) shareObject;

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

    private void parseVirtuals(List<Object> objects, ExtendedListFragment.SearchType searchType) {
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

        List<ContentValues> contentValues = new ArrayList<>();

        for (Object remoteFile : objects) {
            OCFile ocFile = FileStorageUtils.fillOCFile((RemoteFile) remoteFile);
            searchForLocalFileInDefaultPath(ocFile);

            try {
                ocFile = mStorageManager.saveFileWithParent(ocFile, mContext);

                // also sync folder content
                if (ocFile.isFolder()) {
                    long currentSyncTime = System.currentTimeMillis();
                    RemoteOperation refreshFolderOperation = new RefreshFolderOperation(ocFile, currentSyncTime, false,
                            false, mStorageManager, mAccount, mContext);
                    refreshFolderOperation.execute(mAccount, mContext);
                }

                if (!onlyImages || MimeTypeUtil.isImage(ocFile)) {
                    mFiles.add(ocFile);
                }

                ContentValues cv = new ContentValues();
                cv.put(ProviderMeta.ProviderTableMeta.VIRTUAL_TYPE, type.toString());
                cv.put(ProviderMeta.ProviderTableMeta.VIRTUAL_OCFILE_ID, ocFile.getFileId());

                contentValues.add(cv);
            } catch (RemoteOperationFailedException e) {
                Log_OC.e(TAG, "Error saving file with parent" + e.getMessage(), e);
            }
        }

        mStorageManager.saveVirtuals(type, contentValues);
    }

    /**
     * Filter for getting only the folders
     *
     * @param files Collection of files to filter
     * @return Folders in the input
     */
    public List<OCFile> getFolders(List<OCFile> files) {
        List<OCFile> ret = new ArrayList<>();

        for (OCFile file : files) {
            if (file.isFolder()) {
                ret.add(file);
            }
        }

        return ret;
    }


    public void setSortOrder(OCFile folder, FileSortOrder sortOrder) {
        PreferenceManager.setSortOrder(mContext, folder, sortOrder);
        mFiles = sortOrder.sortCloudFiles(mFiles);
        notifyDataSetChanged();
    }

    public Set<OCFile> getCheckedItems() {
        return checkedFiles;
    }

    public void setCheckedItem(Set<OCFile> files) {
        checkedFiles.clear();
        checkedFiles.addAll(files);
    }

    public void clearCheckedItems() {
        checkedFiles.clear();
    }

    public List<OCFile> getFiles() {
        return mFiles;
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
                            currentFile.getFileName().toLowerCase(Locale.getDefault()).contains(
                                    constraint.toString().toLowerCase(Locale.getDefault())) &&
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
            mFiles.clear();
            if (ocFiles != null && ocFiles.size() > 0) {
                mFiles.addAll(ocFiles);
                if (!PreferenceManager.showHiddenFilesEnabled(mContext)) {
                    mFiles = filterHiddenFiles(mFiles);
                }
                FileSortOrder sortOrder = PreferenceManager.getSortOrder(mContext, null);
                mFiles = sortOrder.sortCloudFiles(mFiles);
            }

            notifyDataSetChanged();
        }
    }


    /**
     * Filter for hidden files
     *
     * @param files Collection of files to filter
     * @return Non-hidden files
     */
    public List<OCFile> filterHiddenFiles(List<OCFile> files) {
        List<OCFile> ret = new ArrayList<>();

        for (OCFile file : files) {
            if (!file.isHidden() && !ret.contains(file)) {
                ret.add(file);
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

    public void setGridView(boolean bool) {
        gridView = bool;
    }

    static class OCFileListItemViewHolder extends OCFileListGridItemViewHolder {
        private final TextView fileSize;
        private final TextView lastModification;
        private final ImageView overflowMenu;

        private OCFileListItemViewHolder(View itemView) {
            super(itemView);

            fileSize = itemView.findViewById(R.id.file_size);
            lastModification = itemView.findViewById(R.id.last_mod);
            overflowMenu = itemView.findViewById(R.id.overflow_menu);
        }
    }

    static class OCFileListGridItemViewHolder extends OCFileListGridImageViewHolder {
        private final TextView fileName;

        private OCFileListGridItemViewHolder(View itemView) {
            super(itemView);

            fileName = itemView.findViewById(R.id.Filename);
        }
    }

    static class OCFileListGridImageViewHolder extends RecyclerView.ViewHolder {
        private final ImageView thumbnail;
        private final ImageView favorite;
        private final ImageView offlineIcon;
        private final ImageView localFileIndicator;
        private final ImageView shared;
        private final ImageView checkbox;
        private final LinearLayout itemLayout;

        private OCFileListGridImageViewHolder(View itemView) {
            super(itemView);

            thumbnail = itemView.findViewById(R.id.thumbnail);
            favorite = itemView.findViewById(R.id.favorite_action);
            offlineIcon = itemView.findViewById(R.id.keptOfflineIcon);
            localFileIndicator = itemView.findViewById(R.id.localFileIndicator);
            shared = itemView.findViewById(R.id.sharedIcon);
            checkbox = itemView.findViewById(R.id.custom_checkbox);
            itemLayout = itemView.findViewById(R.id.ListItemLayout);
        }
    }

    static class OCFileListFooterViewHolder extends RecyclerView.ViewHolder {
        private final TextView footerText;

        private OCFileListFooterViewHolder(View itemView) {
            super(itemView);

            footerText = itemView.findViewById(R.id.footerText);
        }
    }
}
