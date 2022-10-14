/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Chris Narkiewicz <hello@ezaquarii.com>
 *
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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

import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.elyeproj.loaderviewlibrary.LoaderImageView;
import com.nextcloud.client.account.User;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.GridImageBinding;
import com.owncloud.android.databinding.GridItemBinding;
import com.owncloud.android.databinding.ListFooterBinding;
import com.owncloud.android.databinding.ListHeaderBinding;
import com.owncloud.android.databinding.ListItemBinding;
import com.owncloud.android.datamodel.DecryptedFolderMetadata;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datamodel.VirtualFolderType;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.shares.ShareeUser;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.operations.RemoteOperationFailedException;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.fragment.SearchType;
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface;
import com.owncloud.android.ui.preview.PreviewTextFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.CapabilityUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.RecyclerView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import me.zhanghai.android.fastscroll.PopupTextProvider;

/**
 * This Adapter populates a RecyclerView with all files and folders in a Nextcloud instance.
 */
public class OCFileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    implements DisplayUtils.AvatarGenerationListener,
    CommonOCFileListAdapterInterface, PopupTextProvider {

    private static final int showFilenameColumnThreshold = 4;
    private final String userId;
    private Activity activity;
    private AppPreferences preferences;
    private List<OCFile> mFiles = new ArrayList<>();
    private List<OCFile> mFilesAll = new ArrayList<>();
    private boolean hideItemOptions;
    private long lastTimestamp;
    private boolean gridView;

    private FileDataStorageManager mStorageManager;
    private User user;
    private OCFileListFragmentInterface ocFileListFragmentInterface;

    private OCFile currentDirectory;
    private static final String TAG = OCFileListAdapter.class.getSimpleName();

    private static final int VIEWTYPE_FOOTER = 0;
    private static final int VIEWTYPE_ITEM = 1;
    private static final int VIEWTYPE_IMAGE = 2;
    private static final int VIEWTYPE_HEADER = 3;

    private boolean onlyOnDevice;
    private final OCFileListDelegate ocFileListDelegate;
    private FileSortOrder sortOrder;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private final ViewThemeUtils viewThemeUtils;
    private SearchType searchType;

    public OCFileListAdapter(
        Activity activity,
        @NonNull User user,
        AppPreferences preferences,
        ComponentsGetter transferServiceGetter,
        OCFileListFragmentInterface ocFileListFragmentInterface,
        boolean argHideItemOptions,
        boolean gridView,
        final ViewThemeUtils viewThemeUtils) {
        this.ocFileListFragmentInterface = ocFileListFragmentInterface;
        this.activity = activity;
        this.preferences = preferences;
        this.user = user;
        hideItemOptions = argHideItemOptions;
        this.gridView = gridView;
        mStorageManager = transferServiceGetter.getStorageManager();

        if (mStorageManager == null) {
            mStorageManager = new FileDataStorageManager(user, activity.getContentResolver());
        }

        userId = AccountManager
            .get(activity)
            .getUserData(this.user.toPlatformAccount(),
                         com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID);

        this.viewThemeUtils = viewThemeUtils;

        ocFileListDelegate = new OCFileListDelegate(activity,
                                                    ocFileListFragmentInterface,
                                                    user,
                                                    mStorageManager,
                                                    hideItemOptions,
                                                    preferences,
                                                    gridView,
                                                    transferServiceGetter,
                                                    true,
                                                    CapabilityUtils
                                                        .getCapability(activity)
                                                        .getVersion()
                                                        .isShareesOnDavSupported(),
                                                    viewThemeUtils);

        // initialise thumbnails cache on background thread
        new ThumbnailsCacheManager.InitDiskCacheTask().execute();
    }

    public boolean isMultiSelect() {
        return ocFileListDelegate.isMultiSelect();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setMultiSelect(boolean bool) {
        ocFileListDelegate.setMultiSelect(bool);
        notifyDataSetChanged();
    }

    public void removeCheckedFile(@NonNull OCFile file) {
        ocFileListDelegate.removeCheckedFile(file);
    }

    public void addAllFilesToCheckedFiles() {
        ocFileListDelegate.addToCheckedFiles(mFiles);
    }

    public int getItemPosition(@NonNull OCFile file) {
        int position = mFiles.indexOf(file);

        if (shouldShowHeader()) {
            position = position + 1;
        }

        return position;
    }

    public void setFavoriteAttributeForItemID(String fileId, boolean favorite, boolean removeFromList) {
        for (OCFile file : mFiles) {
            if (file.getRemoteId().equals(fileId)) {
                file.setFavorite(favorite);

                if (removeFromList) {
                    mFiles.remove(file);
                }

                break;
            }
        }

        for (OCFile file : mFilesAll) {
            if (file.getRemoteId().equals(fileId)) {
                file.setFavorite(favorite);

                mStorageManager.saveFile(file);

                if (removeFromList) {
                    mFiles.remove(file);
                }

                break;
            }
        }

        FileSortOrder sortOrder = preferences.getSortOrderByFolder(currentDirectory);
        mFiles = sortOrder.sortCloudFiles(mFiles);

        new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
    }

    public void refreshCommentsCount(String fileId) {
        for (OCFile file : mFiles) {
            if (file.getRemoteId().equals(fileId)) {
                file.setUnreadCommentsCount(0);
                break;
            }
        }

        for (OCFile file : mFilesAll) {
            if (file.getRemoteId().equals(fileId)) {
                file.setUnreadCommentsCount(0);
                break;
            }
        }

        new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
    }

    public void setEncryptionAttributeForItemID(String fileId, boolean encrypted) {
        for (OCFile file : mFiles) {
            if (file.getRemoteId().equals(fileId)) {
                file.setEncrypted(encrypted);
                mStorageManager.saveFile(file);

                break;
            }
        }

        for (OCFile file : mFilesAll) {
            if (file.getRemoteId().equals(fileId)) {
                file.setEncrypted(encrypted);
            }
        }

        new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
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
        if (shouldShowHeader()) {
            return mFiles.size() + 2; // for header and footer
        } else {
            return mFiles.size() + 1; // for footer
        }
    }

    public boolean isEmpty() {
        return mFiles.isEmpty();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            default:
            case VIEWTYPE_ITEM:
                if (gridView) {
                    return new OCFileListGridItemViewHolder(
                        GridItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
                    );
                } else {
                    return new OCFileListItemViewHolder(
                        ListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
                    );
                }

            case VIEWTYPE_IMAGE:
                if (gridView) {
                    return new OCFileListGridImageViewHolder(
                        GridImageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
                    );
                } else {
                    return new OCFileListItemViewHolder(
                        ListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
                    );
                }

            case VIEWTYPE_FOOTER:
                return new OCFileListFooterViewHolder(
                    ListFooterBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
                );

            case VIEWTYPE_HEADER:
                ListHeaderBinding binding = ListHeaderBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false);

                ViewGroup.LayoutParams layoutParams = binding.headerView.getLayoutParams();
                layoutParams.height = (int) (parent.getHeight() * 0.3);
                binding.headerView.setLayoutParams(layoutParams);

                return new OCFileListHeaderViewHolder(binding);
        }
    }

    @Override
    @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof OCFileListFooterViewHolder) {
            OCFileListFooterViewHolder footerViewHolder = (OCFileListFooterViewHolder) holder;
            footerViewHolder.getFooterText().setText(getFooterText());
            viewThemeUtils.platform.colorCircularProgressBar(footerViewHolder.getLoadingProgressBar());
            footerViewHolder.getLoadingProgressBar().setVisibility(
                ocFileListFragmentInterface.isLoading() ? View.VISIBLE : View.GONE);
        } else if (holder instanceof OCFileListHeaderViewHolder) {
            OCFileListHeaderViewHolder headerViewHolder = (OCFileListHeaderViewHolder) holder;
            String text = currentDirectory.getRichWorkspace();

            PreviewTextFragment.setText(headerViewHolder.getHeaderText(), text, null, activity, true, true, viewThemeUtils);
            headerViewHolder.getHeaderView().setOnClickListener(v -> ocFileListFragmentInterface.onHeaderClicked());
        } else {
            ListGridImageViewHolder gridViewHolder = (ListGridImageViewHolder) holder;
            OCFile file = getItem(position);

            if (file == null) {
                Log_OC.e(this, "Cannot bind on view holder on a null file");
                return;
            }

            ocFileListDelegate.bindGridViewHolder(gridViewHolder, file, searchType);

            if (holder instanceof ListItemViewHolder) {
                bindListItemViewHolder((ListItemViewHolder) gridViewHolder, file);
            }

            if (holder instanceof ListGridItemViewHolder) {
                bindListGridItemViewHolder((ListGridItemViewHolder) holder, file);
            }
        }
    }

    private void bindListItemViewHolder(ListItemViewHolder holder, OCFile file) {
        if ((file.isSharedWithMe() || file.isSharedWithSharee()) && !isMultiSelect() && !gridView &&
            !hideItemOptions) {
            holder.getSharedAvatars().setVisibility(View.VISIBLE);
            holder.getSharedAvatars().removeAllViews();

            String fileOwner = file.getOwnerId();
            List<ShareeUser> sharees = file.getSharees();

            // use fileOwner if not oneself, then add at first
            ShareeUser fileOwnerSharee = new ShareeUser(fileOwner, file.getOwnerDisplayName(), ShareType.USER);
            if (!TextUtils.isEmpty(fileOwner) &&
                !fileOwner.equals(userId) &&
                !sharees.contains(fileOwnerSharee)) {
                sharees.add(fileOwnerSharee);
            }

            Collections.reverse(sharees);

            Log_OC.d(this, "sharees of " + file.getFileName() + ": " + sharees);

            holder.getSharedAvatars().setAvatars(user, sharees, viewThemeUtils);
            holder.getSharedAvatars().setOnClickListener(
                view -> ocFileListFragmentInterface.onShareIconClick(file));
        } else {
            holder.getSharedAvatars().setVisibility(View.GONE);
            holder.getSharedAvatars().removeAllViews();
        }

        // npe fix: looks like file without local storage path somehow get here
        final String storagePath = file.getStoragePath();
        if (onlyOnDevice && storagePath != null) {
            File localFile = new File(storagePath);
            long localSize;
            if (localFile.isDirectory()) {
                localSize = FileStorageUtils.getFolderSize(localFile);
            } else {
                localSize = localFile.length();
            }

            holder.getFileSize().setText(DisplayUtils.bytesToHumanReadable(localSize));
            holder.getFileSize().setVisibility(View.VISIBLE);
            holder.getFileSizeSeparator().setVisibility(View.VISIBLE);
        } else {
            final long fileLength = file.getFileLength();
            if (fileLength >= 0) {
                holder.getFileSize().setText(DisplayUtils.bytesToHumanReadable(fileLength));
                holder.getFileSize().setVisibility(View.VISIBLE);
                holder.getFileSizeSeparator().setVisibility(View.VISIBLE);
            } else {
                holder.getFileSize().setVisibility(View.GONE);
                holder.getFileSizeSeparator().setVisibility(View.GONE);
            }
        }

        final long modificationTimestamp = file.getModificationTimestamp();
        if (modificationTimestamp > 0) {
            holder.getLastModification().setText(DisplayUtils.getRelativeTimestamp(activity,
                                                                                   modificationTimestamp));
            holder.getLastModification().setVisibility(View.VISIBLE);
        } else if (file.getFirstShareTimestamp() > 0) {
            holder.getLastModification().setText(
                DisplayUtils.getRelativeTimestamp(activity, file.getFirstShareTimestamp())
                                                );
            holder.getLastModification().setVisibility(View.VISIBLE);
        } else {
            holder.getLastModification().setVisibility(View.GONE);
        }


        if (isMultiSelect() || gridView || hideItemOptions) {
            holder.getOverflowMenu().setVisibility(View.GONE);
        } else {
            holder.getOverflowMenu().setVisibility(View.VISIBLE);
            holder.getOverflowMenu().setOnClickListener(view -> ocFileListFragmentInterface
                .onOverflowIconClicked(file, view));
        }

        if (file.isLocked()) {
            holder.getOverflowMenu().setImageResource(R.drawable.ic_locked_dots_small);
        } else {
            holder.getOverflowMenu().setImageResource(R.drawable.ic_dots_vertical);
        }
    }

    private void bindListGridItemViewHolder(ListGridItemViewHolder holder, OCFile file) {
        holder.getFileName().setText(file.getDecryptedFileName());

        boolean gridImage = MimeTypeUtil.isImage(file) || MimeTypeUtil.isVideo(file);
        if (gridView && gridImage) {
            holder.getFileName().setVisibility(View.GONE);
        } else {
            if (gridView && ocFileListFragmentInterface.getColumnsCount() > showFilenameColumnThreshold) {
                holder.getFileName().setVisibility(View.GONE);
            } else {
                holder.getFileName().setVisibility(View.VISIBLE);
            }
        }
    }


    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof ListGridImageViewHolder) {
            LoaderImageView thumbnailShimmer = ((ListGridImageViewHolder) holder).getShimmerThumbnail();
            if (thumbnailShimmer.getVisibility() == View.VISIBLE) {
                thumbnailShimmer.setImageResource(R.drawable.background);
                thumbnailShimmer.resetLoader();
            }
        }
    }

    private String getFooterText() {
        int filesCount = 0;
        int foldersCount = 0;
        int count = mFiles.size();
        OCFile file;
        final boolean showHiddenFiles = preferences.isShowHiddenFilesEnabled();
        for (int i = 0; i < count; i++) {
            file = mFiles.get(i);
            if (file.isFolder()) {
                foldersCount++;
            } else {
                if (!file.isHidden() || showHiddenFiles) {
                    filesCount++;
                }
            }
        }


        return generateFooterText(filesCount, foldersCount);
    }

    private String generateFooterText(int filesCount, int foldersCount) {
        String output;
        Resources resources = activity.getResources();

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

    public @Nullable
    OCFile getItem(int position) {
        int newPosition = position;

        if (shouldShowHeader() && position > 0) {
            newPosition = position - 1;
        }

        if (newPosition >= mFiles.size()) {
            return null;
        }

        return mFiles.get(newPosition);
    }

    public boolean shouldShowHeader() {
        if (currentDirectory == null) {
            return false;
        }

        if (MainApp.isOnlyOnDevice()) {
            return false;
        }

        if (currentDirectory.getRichWorkspace() == null) {
            return false;
        }

        return !TextUtils.isEmpty(currentDirectory.getRichWorkspace().trim());
    }

    @Override
    public int getItemViewType(int position) {
        if (shouldShowHeader()) {
            if (position == 0) {
                return VIEWTYPE_HEADER;
            } else {
                if (position == mFiles.size() + 1) {
                    return VIEWTYPE_FOOTER;
                }
            }
        } else {
            if (position == mFiles.size()) {
                return VIEWTYPE_FOOTER;
            }
        }

        OCFile item = getItem(position);
        if (item == null) {
            return VIEWTYPE_ITEM;
        }

        if (MimeTypeUtil.isImageOrVideo(item)) {
            return VIEWTYPE_IMAGE;
        } else {
            return VIEWTYPE_ITEM;
        }
    }

    /**
     * Change the adapted directory for a new one
     *
     * @param directory             New folder to adapt. Can be NULL, meaning "no content to adapt".
     * @param updatedStorageManager Optional updated storage manager; used to replace
     * @param limitToMimeType       show only files of this mimeType
     */
    @SuppressLint("NotifyDataSetChanged")
    public void swapDirectory(
        @NonNull User account,
        @NonNull OCFile directory,
        @NonNull FileDataStorageManager updatedStorageManager,
        boolean onlyOnDevice, @NonNull String limitToMimeType
                             ) {
        this.onlyOnDevice = onlyOnDevice;

        if (!updatedStorageManager.equals(mStorageManager)) {
            mStorageManager = updatedStorageManager;
            ocFileListDelegate.setShowShareAvatar(CapabilityUtils.getCapability(account, activity).getVersion().isShareesOnDavSupported());
            this.user = account;
        }
        if (mStorageManager != null) {
            mFiles = mStorageManager.getFolderContent(directory, onlyOnDevice);

            if (!preferences.isShowHiddenFilesEnabled()) {
                mFiles = filterHiddenFiles(mFiles);
            }
            if (!limitToMimeType.isEmpty()) {
                mFiles = filterByMimeType(mFiles, limitToMimeType);
            }
            sortOrder = preferences.getSortOrderByFolder(directory);
            mFiles = sortOrder.sortCloudFiles(mFiles);
            mFilesAll.clear();
            mFilesAll.addAll(mFiles);

            currentDirectory = directory;
        } else {
            mFiles.clear();
            mFilesAll.clear();
        }

        searchType = null;

        notifyDataSetChanged();
    }

    public void setData(List<Object> objects,
                        SearchType searchType,
                        FileDataStorageManager storageManager,
                        @Nullable OCFile folder,
                        boolean clear) {
        if (storageManager != null && mStorageManager == null) {
            mStorageManager = storageManager;
            ocFileListDelegate.setShowShareAvatar(mStorageManager
                                                      .getCapability(user.getAccountName())
                                                      .getVersion()
                                                      .isShareesOnDavSupported());
        }

        if (mStorageManager == null) {
            mStorageManager = new FileDataStorageManager(user, activity.getContentResolver());
        }

        if (clear) {
            mFiles.clear();
            resetLastTimestamp();
            preferences.setPhotoSearchTimestamp(0);

            VirtualFolderType type;
            switch (searchType) {
                case FAVORITE_SEARCH:
                    type = VirtualFolderType.FAVORITE;
                    break;
                case GALLERY_SEARCH:
                    type = VirtualFolderType.GALLERY;
                    break;
                default:
                    type = VirtualFolderType.NONE;
                    break;
            }

            if (type != VirtualFolderType.GALLERY) {
                mStorageManager.deleteVirtuals(type);
            }
        }

        // early exit
        if (objects.size() > 0 && mStorageManager != null) {
            if (searchType == SearchType.SHARED_FILTER) {
                parseShares(objects);
            } else {
                if (searchType != SearchType.GALLERY_SEARCH) {
                    parseVirtuals(objects, searchType);
                }
            }
        }

        if (searchType == SearchType.GALLERY_SEARCH ||
            searchType == SearchType.RECENTLY_MODIFIED_SEARCH) {
            mFiles = FileStorageUtils.sortOcFolderDescDateModifiedWithoutFavoritesFirst(mFiles);
        } else if (searchType != SearchType.SHARED_FILTER) {
            sortOrder = preferences.getSortOrderByFolder(folder);
            mFiles = sortOrder.sortCloudFiles(mFiles);
        }

        this.searchType = searchType;

        mFilesAll.clear();
        mFilesAll.addAll(mFiles);

        new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
    }

    private void parseShares(List<Object> objects) {
        List<OCShare> shares = new ArrayList<>();

        for (Object shareObject : objects) {
            // check type before cast as of long running data fetch it is possible that old result is filled
            if (shareObject instanceof OCShare) {
                OCShare ocShare = (OCShare) shareObject;
                shares.add(ocShare);
            }
        }

        List<OCFile> files = OCShareToOCFileConverter.buildOCFilesFromShares(shares);
        mFiles.addAll(files);
        mStorageManager.saveShares(shares);
    }

    private void parseVirtuals(List<Object> objects, SearchType searchType) {
        VirtualFolderType type;
        boolean onlyMedia = false;

        switch (searchType) {
            case FAVORITE_SEARCH:
                type = VirtualFolderType.FAVORITE;
                break;
            case GALLERY_SEARCH:
                type = VirtualFolderType.GALLERY;
                onlyMedia = true;

                int lastPosition = objects.size() - 1;

                if (lastPosition < 0) {
                    lastTimestamp = -1;
                    break;
                }

                RemoteFile lastFile = (RemoteFile) objects.get(lastPosition);
                lastTimestamp = lastFile.getModifiedTimestamp() / 1000;
                break;
            default:
                type = VirtualFolderType.NONE;
                break;
        }

        List<ContentValues> contentValues = new ArrayList<>();

        for (Object remoteFile : objects) {
            OCFile ocFile = FileStorageUtils.fillOCFile((RemoteFile) remoteFile);
            FileStorageUtils.searchForLocalFileInDefaultPath(ocFile, user.getAccountName());

            try {
                ocFile = mStorageManager.saveFileWithParent(ocFile, activity);

                OCFile parentFolder = mStorageManager.getFileById(ocFile.getParentId());
                if (parentFolder != null && (ocFile.isEncrypted() || parentFolder.isEncrypted())) {
                    DecryptedFolderMetadata metadata = RefreshFolderOperation.getDecryptedFolderMetadata(
                        true,
                        parentFolder,
                        OwnCloudClientFactory.createOwnCloudClient(user.toPlatformAccount(), activity),
                        user,
                        activity);

                    if (metadata == null) {
                        throw new IllegalStateException("metadata is null!");
                    }

                    // update ocFile
                    RefreshFolderOperation.updateFileNameForEncryptedFile(mStorageManager, metadata, ocFile);
                    ocFile = mStorageManager.saveFileWithParent(ocFile, activity);
                }

                if (SearchType.GALLERY_SEARCH != searchType) {
                    // also sync folder content
                    if (ocFile.isFolder()) {
                        long currentSyncTime = System.currentTimeMillis();
                        RemoteOperation refreshFolderOperation = new RefreshFolderOperation(ocFile,
                                                                                            currentSyncTime,
                                                                                            true,
                                                                                            false,
                                                                                            mStorageManager,
                                                                                            user,
                                                                                            activity);
                        refreshFolderOperation.execute(user, activity);
                    }
                }

                if (!onlyMedia || MimeTypeUtil.isImage(ocFile) || MimeTypeUtil.isVideo(ocFile)) {
                    mFiles.add(ocFile);
                }

                ContentValues cv = new ContentValues();
                cv.put(ProviderMeta.ProviderTableMeta.VIRTUAL_TYPE, type.toString());
                cv.put(ProviderMeta.ProviderTableMeta.VIRTUAL_OCFILE_ID, ocFile.getFileId());

                contentValues.add(cv);
            } catch (
                RemoteOperationFailedException |
                    OperationCanceledException |
                    AuthenticatorException |
                    IOException |
                    AccountUtils.AccountNotFoundException |
                    IllegalStateException e) {
                Log_OC.e(TAG, "Error saving file with parent" + e.getMessage(), e);
            }
        }

        preferences.setPhotoSearchTimestamp(System.currentTimeMillis());
        mStorageManager.saveVirtuals(contentValues);
    }

    public void showVirtuals(VirtualFolderType type, boolean onlyImages, FileDataStorageManager storageManager) {
        mFiles = storageManager.getVirtualFolderContent(type, onlyImages);

        if (VirtualFolderType.GALLERY == type) {
            mFiles = FileStorageUtils.sortOcFolderDescDateModifiedWithoutFavoritesFirst(mFiles);
        }

        mFilesAll.clear();
        mFilesAll.addAll(mFiles);

        new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
    }


    public void setSortOrder(@Nullable OCFile folder, FileSortOrder sortOrder) {
        preferences.setSortOrder(folder, sortOrder);
        mFiles = sortOrder.sortCloudFiles(mFiles);
        notifyDataSetChanged();

        this.sortOrder = sortOrder;
    }

    public Set<OCFile> getCheckedItems() {
        return ocFileListDelegate.getCheckedItems();
    }

    public void setCheckedItem(Set<OCFile> files) {
        ocFileListDelegate.setCheckedItem(files);
    }

    public void clearCheckedItems() {
        ocFileListDelegate.clearCheckedItems();
    }

    public void setFiles(List<OCFile> files) {
        mFiles = files;
    }

    public List<OCFile> getFiles() {
        return mFiles;
    }

    public void resetLastTimestamp() {
        lastTimestamp = -1;
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }

    @Override
    public void avatarGenerated(Drawable avatarDrawable, Object callContext) {
        ((ImageView) callContext).setImageDrawable(avatarDrawable);
    }

    @Override
    public boolean shouldCallGeneratedCallback(String tag, Object callContext) {
        return ((ImageView) callContext).getTag().equals(tag);
    }

    public boolean isCheckedFile(OCFile file) {
        return ocFileListDelegate.isCheckedFile(file);
    }

    public void addCheckedFile(OCFile file) {
        ocFileListDelegate.addCheckedFile(file);
    }

    public void setHighlightedItem(OCFile file) {
        ocFileListDelegate.setHighlightedItem(file);
    }

    /**
     * Filter for hidden files
     *
     * @param files Collection of files to filter
     * @return Non-hidden files
     */
    private List<OCFile> filterHiddenFiles(List<OCFile> files) {
        List<OCFile> ret = new ArrayList<>();

        for (OCFile file : files) {
            if (!file.isHidden() && !ret.contains(file)) {
                ret.add(file);
            }
        }

        return ret;
    }

    private List<OCFile> filterByMimeType(List<OCFile> files, String mimeType) {
        List<OCFile> ret = new ArrayList<>();

        for (OCFile file : files) {
            if (file.isFolder() || file.getMimeType().startsWith(mimeType)) {
                ret.add(file);
            }
        }

        return ret;
    }

    public void cancelAllPendingTasks() {
        ocFileListDelegate.cancelAllPendingTasks();
    }

    public void setGridView(boolean bool) {
        gridView = bool;
    }

    public void setShowMetadata(boolean bool) {
        ocFileListDelegate.setMultiSelect(bool);
    }

    @NonNull
    @Override
    public String getPopupText(int position) {
        OCFile file = getItem(position);

        if (file == null || sortOrder == null) {
            return "";
        }

        switch (sortOrder.getType()) {
            case ALPHABET:
                return String.valueOf(file.getFileName().charAt(0)).toUpperCase(Locale.getDefault());
            case DATE:
                long milliseconds = file.getModificationTimestamp();
                Date date = new Date(milliseconds);
                return dateFormat.format(date);
            case SIZE:
                return DisplayUtils.bytesToHumanReadable(file.getFileLength());
            default:
                Log_OC.d(TAG, "getPopupText: Unsupported sort order: " + sortOrder.getType());
                return "";
        }
    }

    @VisibleForTesting
    public void setShowShareAvatar(boolean bool) {
        ocFileListDelegate.setShowShareAvatar(bool);
    }

    @VisibleForTesting
    public void setCurrentDirectory(OCFile folder) {
        currentDirectory = folder;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<OCFile> getAllFiles() {
        return mFilesAll;
    }

    public OCFile getCurrentDirectory() {
        return currentDirectory;
    }

    @Override
    public int getFilesCount() {
        return mFiles.size();
    }

    @Override
    public void notifyItemChanged(@NonNull OCFile file) {
        notifyItemChanged(getItemPosition(file));
    }
}
