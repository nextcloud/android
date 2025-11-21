/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentCallbacks;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.elyeproj.loaderviewlibrary.LoaderImageView;
import com.google.android.material.chip.Chip;
import com.nextcloud.android.common.ui.theme.utils.ColorRole;
import com.nextcloud.client.account.User;
import com.nextcloud.client.database.entity.OfflineOperationEntity;
import com.nextcloud.client.jobs.upload.FileUploadHelper;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.model.OCFileFilterType;
import com.nextcloud.model.OfflineOperationType;
import com.nextcloud.utils.LinkHelper;
import com.nextcloud.utils.extensions.OCFileExtensionsKt;
import com.nextcloud.utils.extensions.ViewExtensionsKt;
import com.nextcloud.utils.mdm.MDMConfig;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.GridItemBinding;
import com.owncloud.android.databinding.ListFooterBinding;
import com.owncloud.android.databinding.ListHeaderBinding;
import com.owncloud.android.databinding.ListItemBinding;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datamodel.VirtualFolderType;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.shares.ShareeUser;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.lib.resources.tags.Tag;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.activity.DrawerActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.adapter.diffUtil.OCFileListDiffUtil;
import com.owncloud.android.ui.fragment.OCFileListFragment;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import kotlin.Pair;
import me.zhanghai.android.fastscroll.PopupTextProvider;

/**
 * This Adapter populates a RecyclerView with all files and folders in a Nextcloud instance.
 */
@SuppressWarnings("unchecked")
public class OCFileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    implements DisplayUtils.AvatarGenerationListener,
    CommonOCFileListAdapterInterface, PopupTextProvider {

    private final String userId;
    public final Activity activity;
    public final AppPreferences preferences;
    private final OCCapability capability;
    private List<OCFile> mFiles = new ArrayList<>();
    private final List<OCFile> mFilesAll = new ArrayList<>();
    private final boolean hideItemOptions;
    private boolean gridView;
    public ArrayList<String> listOfHiddenFiles = new ArrayList<>();
    public FileDataStorageManager mStorageManager;
    public User user;
    private final OCFileListFragmentInterface ocFileListFragmentInterface;
    private final boolean isRTL;

    private OCFile currentDirectory;
    private static final String TAG = OCFileListAdapter.class.getSimpleName();

    private static final int VIEW_TYPE_FOOTER = 0;
    private static final int VIEW_TYPE_ITEM = 1;
    private static final int VIEW_TYPE_IMAGE = 2;
    private static final int VIEW_TYPE_HEADER = 3;

    private boolean onlyOnDevice;
    private final OCFileListDelegate ocFileListDelegate;
    private FileSortOrder sortOrder;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private final ViewThemeUtils viewThemeUtils;
    private SearchType searchType;

    private final long footerId = UUID.randomUUID().getLeastSignificantBits();
    private final long headerId = UUID.randomUUID().getLeastSignificantBits();

    private ArrayList<OCFile> recommendedFiles = new ArrayList<>();
    private RecommendedFilesAdapter recommendedFilesAdapter;
    private final OCFileListDiffUtil diffUtil = new OCFileListDiffUtil();

    public OCFileListAdapter(
        Activity activity,
        @NonNull User user,
        AppPreferences preferences,
        SyncedFolderProvider syncedFolderProvider,
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
        this.capability = CapabilityUtils.getCapability(user, activity);

        if (activity instanceof FileDisplayActivity) {
            ((FileDisplayActivity) activity).showSortListGroup(true);
        }

        if (mStorageManager == null) {
            mStorageManager = new FileDataStorageManager(user, activity.getContentResolver());
        }

        userId = AccountManager
            .get(activity)
            .getUserData(this.user.toPlatformAccount(),
                         AccountUtils.Constants.KEY_USER_ID);
        this.viewThemeUtils = viewThemeUtils;
        ocFileListDelegate = new OCFileListDelegate(FileUploadHelper.Companion.instance(),
                                                    activity,
                                                    ocFileListFragmentInterface,
                                                    user,
                                                    mStorageManager,
                                                    hideItemOptions,
                                                    preferences,
                                                    gridView,
                                                    transferServiceGetter,
                                                    true,
                                                    true,
                                                    viewThemeUtils,
                                                    syncedFolderProvider);

        setHasStableIds(true);

        // initialise thumbnails cache on background thread
        ThumbnailsCacheManager.initDiskCacheAsync();
        isRTL = DisplayUtils.isRTL();

        activity.registerComponentCallbacks(new ComponentCallbacks() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onConfigurationChanged(@NonNull Configuration newConfig) {
                notifyDataSetChanged(); // force update of orientation-dependent layout (e.g. share button visibility)
            }
            @Override
            public void onLowMemory() {
            }
        });
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

    @Override
    public void selectAll(boolean value) {
        if (value) {
            ocFileListDelegate.addToCheckedFiles(mFiles);
        } else {
            clearCheckedItems();
        }
    }

    public int getItemPosition(@NonNull OCFile file) {
        int position = mFiles.indexOf(file);

        if (shouldShowHeader()) {
            position = position + 1;
        }

        return position;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setFavoriteAttributeForItemID(String remotePath, boolean favorite, boolean removeFromList) {
        List<OCFile> filesToDelete = new ArrayList<>();
        for (OCFile file : mFiles) {
            if (file.getRemotePath().equals(remotePath)) {
                file.setFavorite(favorite);

                if (removeFromList) {
                    filesToDelete.add(file);
                }

                break;
            }
        }

        for (OCFile file : mFilesAll) {
            if (file.getRemotePath().equals(remotePath)) {
                file.setFavorite(favorite);

                mStorageManager.saveFile(file);

                if (removeFromList) {
                    filesToDelete.add(file);
                }

                break;
            }
        }

        FileSortOrder sortOrder = preferences.getSortOrderByFolder(currentDirectory);
        if (searchType == SearchType.SHARED_FILTER) {
            mFiles.sort((o1, o2) -> Long.compare(o2.getFirstShareTimestamp(), o1.getFirstShareTimestamp()));
        } else {
            boolean foldersBeforeFiles = preferences.isSortFoldersBeforeFiles();
            boolean favoritesFirst = preferences.isSortFavoritesFirst();
            mFiles = sortOrder.sortCloudFiles(mFiles, foldersBeforeFiles, favoritesFirst);
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            mFiles.removeAll(filesToDelete);
            notifyDataSetChanged();
        });
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
                file.setE2eCounter(0L);
                mStorageManager.saveFile(file);

                break;
            }
        }

        for (OCFile file : mFilesAll) {
            if (file.getRemoteId().equals(fileId)) {
                file.setEncrypted(encrypted);
                file.setE2eCounter(0L);
            }
        }

        new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
    }

    @Override
    public long getItemId(int position) {
        if (shouldShowHeader()) {
            if (position == 0) {
                return headerId;
            }


            // skip header
            position--;
        }

        if (position == mFiles.size()) {
            return footerId;
        } if (position < mFiles.size()) {
            return mFiles.get(position).getFileId();
        }

        // fallback
        return RecyclerView.NO_ID;
    }

    @Override
    public int getItemCount() {
        return mFiles.size() + (shouldShowHeader() ? 2 : 1);
    }

    @Nullable
    public OCFile getItem(int position) {
        int newPosition = position;

        if (shouldShowHeader() && position > 0) {
            newPosition = position - 1;
        }

        if (newPosition >= mFiles.size()) {
            return null;
        }

        return mFiles.get(newPosition);
    }

    @Override
    public int getItemViewType(int position) {
        if (shouldShowHeader() && position == 0) {
            return VIEW_TYPE_HEADER;
        }

        if (shouldShowHeader() && position == mFiles.size() + 1 ||
            (!shouldShowHeader() && position == mFiles.size())) {
            return VIEW_TYPE_FOOTER;
        }

        OCFile item = getItem(position);
        if (item == null) {
            return VIEW_TYPE_ITEM;
        }

        if (MimeTypeUtil.isImageOrVideo(item)) {
            return VIEW_TYPE_IMAGE;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    public boolean isEmpty() {
        return mFiles.isEmpty();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_FOOTER -> {
                return new OCFileListFooterViewHolder(
                    ListFooterBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
                );
            }
            case VIEW_TYPE_HEADER -> {
                ListHeaderBinding binding = ListHeaderBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false);

                return new OCFileListHeaderViewHolder(binding);
            }
            default -> {
                if (gridView) {
                    return new OCFileListGridItemViewHolder(
                        GridItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
                    );
                } else {
                    return new OCFileListItemViewHolder(
                        ListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
                    );
                }
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof OCFileListFooterViewHolder footerViewHolder) {
            footerViewHolder.getFooterText().setText(getFooterText());
            viewThemeUtils.platform.colorCircularProgressBar(footerViewHolder.getLoadingProgressBar(), ColorRole.ON_SURFACE_VARIANT);
            footerViewHolder.getLoadingProgressBar().setVisibility(
                ocFileListFragmentInterface.isLoading() ? View.VISIBLE : View.GONE);
        } else if (holder instanceof OCFileListHeaderViewHolder headerViewHolder) {
            ListHeaderBinding headerBinding = headerViewHolder.getBinding();
            headerViewHolder.getHeaderView().setOnClickListener(v -> ocFileListFragmentInterface.onHeaderClicked());

            String text = currentDirectory.getRichWorkspace();
            PreviewTextFragment.setText(headerViewHolder.getHeaderText(), text, null, activity, true, true, viewThemeUtils);

            // hide header text if empty (server returns NBSP)
            ViewExtensionsKt.setVisibleIf(headerViewHolder.getHeaderText(), text != null && !text.isBlank() && !" ".equals(text));

            ViewExtensionsKt.setVisibleIf(headerBinding.recommendedFilesRecyclerView, shouldShowRecommendedFiles());
            ViewExtensionsKt.setVisibleIf(headerBinding.recommendedFilesTitle, shouldShowRecommendedFiles());
            ViewExtensionsKt.setVisibleIf(headerBinding.allFilesTitle, shouldShowRecommendedFiles());

            if (shouldShowRecommendedFiles()) {
                final var recommendedFilesRecyclerView = headerBinding.recommendedFilesRecyclerView;

                final LinearLayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false);
                recommendedFilesRecyclerView.setLayoutManager(layoutManager);

                recommendedFilesAdapter = new RecommendedFilesAdapter(this, recommendedFiles);
                recommendedFilesRecyclerView.setAdapter(recommendedFilesAdapter);
            }

            ViewExtensionsKt.setVisibleIf(headerBinding.openIn.getRoot(), shouldShowOpenInNotes());

            if (shouldShowOpenInNotes()) {
                final var listHeaderOpenInBinding = headerBinding.openIn;

                viewThemeUtils.files.themeFilledCardView(listHeaderOpenInBinding.infoCard);

                listHeaderOpenInBinding.infoText.setText(String.format(activity.getString(R.string.folder_best_viewed_in),
                                                                       activity.getString(R.string.ecosystem_apps_notes)));

                listHeaderOpenInBinding.openInButton.setText(String.format(activity.getString(R.string.open_in_app),
                                                                           activity.getString(R.string.ecosystem_apps_display_notes)));

                listHeaderOpenInBinding.openInButton.setOnClickListener(v -> LinkHelper.INSTANCE.openAppOrStore(LinkHelper.APP_NEXTCLOUD_NOTES, user, activity));
            }

        } else {
            ListViewHolder gridViewHolder = (ListViewHolder) holder;
            OCFile file = getItem(position);

            if (file == null) {
                Log_OC.e(this, "Cannot bind on view holder on a null file");
                return;
            }

            bindHolder(holder, gridViewHolder, file);
        }
    }

    public void bindRecommendedFilesHolder(OCFileListRecommendedItemViewHolder holder, @NonNull OCFile file) {
        bindHolder(holder, holder, file);
    }

    private void bindHolder(@NonNull RecyclerView.ViewHolder holder, ListViewHolder gridViewHolder, OCFile file) {
        ocFileListDelegate.bindGridViewHolder(gridViewHolder, file, currentDirectory, searchType);
        checkVisibilityOfFileFeaturesLayout(gridViewHolder);

        if (holder instanceof ListItemViewHolder itemViewHolder) {
            bindListItemViewHolder(itemViewHolder, file);
        }

        if (holder instanceof ListGridItemViewHolder gridItemViewHolder) {
            setFilenameAndExtension(gridItemViewHolder, file);
            checkVisibilityOfFileFeaturesLayout(gridItemViewHolder);
        }

        updateLivePhotoIndicators(gridViewHolder, file);

        if (!MDMConfig.INSTANCE.sharingSupport(activity)) {
            gridViewHolder.getShared().setVisibility(View.GONE);
        }

        setVisibilityOfMoreOption(gridViewHolder);
    }

    private boolean shouldShowRecommendedFiles() {
        return !recommendedFiles.isEmpty() && currentDirectory.isRootDirectory();
    }

    private boolean shouldShowOpenInNotes() {
        if (!preferences.isShowEcosystemApps()) {
            return false;
        }
        String notesFolderPath = capability.getNotesFolderPath();
        String currentPath = currentDirectory.getDecryptedRemotePath();
        return notesFolderPath != null && currentPath != null && currentPath.startsWith(notesFolderPath);
    }

    private void checkVisibilityOfFileFeaturesLayout(ListViewHolder holder) {
        int fileFeaturesVisibility = View.GONE;
        LinearLayout fileFeaturesLayout = holder.getFileFeaturesLayout();

        if (fileFeaturesLayout == null) {
            return;
        }

        for (int i = 0; i < fileFeaturesLayout.getChildCount(); i++) {
            View child = fileFeaturesLayout.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                fileFeaturesVisibility = View.VISIBLE;
            }
        }

        fileFeaturesLayout.setVisibility(fileFeaturesVisibility);
    }

    private void mergeOCFilesForLivePhoto() {
        List<OCFile> filesToRemove = new ArrayList<>();

        for (int i = 0; i < mFiles.size(); i++) {
            OCFile file = mFiles.get(i);

            for (int j = i + 1; j < mFiles.size(); j++) {
                OCFile nextFile = mFiles.get(j);
                String fileLocalId = String.valueOf(file.getLocalId());
                String nextFileLinkedLocalId = nextFile.getLinkedFileIdForLivePhoto();

                if (fileLocalId.equals(nextFileLinkedLocalId)) {
                    if (MimeTypeUtil.isVideo(file.getMimeType())) {
                        nextFile.livePhotoVideo = file;
                        filesToRemove.add(file);
                    } else if (MimeTypeUtil.isVideo(nextFile.getMimeType())) {
                        file.livePhotoVideo = nextFile;
                        filesToRemove.add(nextFile);
                    }
                }
            }
        }

        mFiles.removeAll(filesToRemove);
        filesToRemove.clear();
    }

    private void updateLivePhotoIndicators(ListViewHolder holder, OCFile file) {
        boolean isLivePhoto = file.getLinkedFileIdForLivePhoto() != null;

        if (holder instanceof OCFileListItemViewHolder) {
            holder.getLivePhotoIndicator().setVisibility(isLivePhoto ? (View.VISIBLE) : (View.GONE));
            holder.getLivePhotoIndicatorSeparator().setVisibility(isLivePhoto ? (View.VISIBLE) : (View.GONE));
        } else if (holder instanceof OCFileListViewHolder) {
            holder.getGridLivePhotoIndicator().setVisibility(isLivePhoto ? (View.VISIBLE) : (View.GONE));
        }
    }

    private void setFilenameAndExtension(ListGridItemViewHolder holder, OCFile file) {
        final String filename = mStorageManager.getFilenameConsideringOfflineOperation(file);
        final var pair = FileStorageUtils.getFilenameAndExtension(filename, file.isFolder(), isRTL);
        final boolean isFolder = file.isFolder();

        if (holder instanceof OCFileListGridItemViewHolder gridItemViewHolder) {
            handleGridMode(filename, gridItemViewHolder, pair, file);
        } else {
            handleListMode(holder, pair, isFolder);
        }
    }

    private void handleGridMode(String filename, OCFileListGridItemViewHolder holder, Pair<String, String> filenamePair, OCFile file) {
        boolean containsBidiControlCharacters = FileStorageUtils.containsBidiControlCharacters(filename);
        ViewExtensionsKt.setVisibleIf(holder.getFileName(),!containsBidiControlCharacters);
        ViewExtensionsKt.setVisibleIf(holder.getBinding().bidiFilenameContainer, containsBidiControlCharacters);
        final var extension = holder.getExtension();

        if (containsBidiControlCharacters) {
            holder.getBidiFilename().setText(filenamePair.getFirst());
            if (extension != null) {
                extension.setText(filenamePair.getSecond());
            }
            holder.getBinding().more.setVisibility(View.GONE);
            holder.getBinding().bidiMore.setOnClickListener(v -> ocFileListFragmentInterface.onOverflowIconClicked(file, v));
        } else {
            holder.getFileName().setText(filename);
            if (extension != null) {
                extension.setVisibility(View.GONE);
            }
        }
    }

    private void handleListMode(ListGridItemViewHolder holder,
                                Pair<String, String> filenamePair,
                                boolean isFolder) {
        holder.getFileName().setText(filenamePair.getFirst());

        final var extension = holder.getExtension();
        if (extension != null) {
            if (isFolder) {
                extension.setVisibility(View.GONE);
            } else {
                extension.setVisibility(View.VISIBLE);
                extension.setText(filenamePair.getSecond());
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

        // tags
        if (file.getTags().isEmpty()) {
            holder.getTagsGroup().setVisibility(View.GONE);
            holder.getFileDetailGroup().setVisibility(View.VISIBLE);
        } else {
            holder.getTagsGroup().setVisibility(View.VISIBLE);
            holder.getFileDetailGroup().setVisibility(View.GONE);
            viewThemeUtils.material.themeChipSuggestion(holder.getFirstTag());
            holder.getFirstTag().setVisibility(View.VISIBLE);
            holder.getSecondTag().setVisibility(View.GONE);
            holder.getTagMore().setVisibility(View.GONE);

            applyChipVisuals(holder.getFirstTag(), file.getTags().get(0));

            if (file.getTags().size() > 1) {
                holder.getSecondTag().setVisibility(View.VISIBLE);
                applyChipVisuals(holder.getSecondTag(), file.getTags().get(1));
            }

            if (file.getTags().size() > 2) {
                viewThemeUtils.material.themeChipSuggestion(holder.getTagMore());
                holder.getTagMore().setVisibility(View.VISIBLE);
                holder.getTagMore().setText(String.format(activity.getString(R.string.tags_more),
                                                          (file.getTags().size() - 2)));
            }
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

            prepareFileSize(holder, file, localSize);
        } else {
            final long fileLength = file.getFileLength();
            if (fileLength >= 0) {
                prepareFileSize(holder, file, fileLength);
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

        setVisibilityOfMoreOption(holder);
    }

    private void setVisibilityOfMoreOption(Object holder) {
        boolean showMoreOptions = (!isMultiSelect() && !OCFileListFragment.isMultipleFileSelectedForCopyOrMove);

        if (holder instanceof ListItemViewHolder itemViewHolder) {
            ViewExtensionsKt.setVisibleIf(itemViewHolder.getOverflowMenu(), showMoreOptions);
        } else if (holder instanceof ListViewHolder viewHolder) {
            ViewExtensionsKt.setVisibleIf(viewHolder.getMore(), showMoreOptions);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateRecommendedFiles(ArrayList<OCFile> recommendedFiles) {
        this.recommendedFiles = recommendedFiles;

        if (recommendedFiles == null || recommendedFiles.isEmpty()) {
            notifyDataSetChanged();
        } else {
            notifyItemChanged(0);
        }
    }

    private void applyChipVisuals(Chip chip, Tag tag) {
        viewThemeUtils.material.themeChipSuggestion(chip);
        chip.setText(tag.getName());
        String tagColor = tag.getColor();
        if (TextUtils.isEmpty(tagColor)) {
            return;
        }

        try {
            int color = Color.parseColor(tagColor);
            chip.setChipStrokeColor(ColorStateList.valueOf(color));
            chip.setTextColor(color);
        } catch (IllegalArgumentException e) {
            Log_OC.d(TAG, "Exception applyChipVisuals: " + e);
        }
    }

    private void prepareFileSize(ListItemViewHolder holder, OCFile file, long size) {
        holder.getFileSize().setVisibility(View.VISIBLE);
        String fileSizeText = getFileSizeText(file, size);
        holder.getFileSize().setText(fileSizeText);
    }

    private String getFileSizeText(OCFile file, long size) {
        if (!file.isOfflineOperation()) {
            return DisplayUtils.bytesToHumanReadable(size);
        }

        OfflineOperationEntity entity = mStorageManager.getOfflineEntityFromOCFile(file);
        boolean isRemoveOperation = (entity != null && entity.getType() instanceof OfflineOperationType.RemoveFile);
        if (isRemoveOperation) {
            return activity.getString(R.string.oc_file_list_adapter_offline_operation_remove_description_text);
        }

        return activity.getString(R.string.oc_file_list_adapter_offline_operation_description_text);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof ListViewHolder) {
            LoaderImageView thumbnailShimmer = ((ListViewHolder) holder).getShimmerThumbnail();
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

    public boolean shouldShowHeader() {
        if (currentDirectory == null) {
            return false;
        }

        if (MainApp.isOnlyOnDevice()) {
            return false;
        }

        if (shouldShowRecommendedFiles()) {
            return true;
        }

        if (shouldShowOpenInNotes()) {
            return true;
        }

        if (currentDirectory.getRichWorkspace() == null) {
            return false;
        }

        return !TextUtils.isEmpty(currentDirectory.getRichWorkspace().trim());
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
        boolean onlyOnDevice,
        @NonNull String limitToMimeType) {
        this.onlyOnDevice = onlyOnDevice;

        if (!updatedStorageManager.equals(mStorageManager)) {
            mStorageManager = updatedStorageManager;
            ocFileListDelegate.setShowShareAvatar(true);
            this.user = account;
        }

        if (mStorageManager != null) {
            // Create a new local list to avoid concurrent modification
            List<OCFile> files = mStorageManager.getFolderContent(directory, onlyOnDevice);

            if (!preferences.isShowHiddenFilesEnabled()) {
                files = OCFileExtensionsKt.filterHiddenFiles(files);
            }
            if (!limitToMimeType.isEmpty()) {
                files = OCFileExtensionsKt.filterByMimeType(files, limitToMimeType);
            }
            if (OCFile.ROOT_PATH.equals(directory.getRemotePath()) && MainApp.isOnlyPersonFiles()) {
                files = OCFileExtensionsKt.limitToPersonalFiles(files, userId);
            }

            // TODO refactor add DrawerState instead of using static menuItemId
            if (DrawerActivity.menuItemId == R.id.nav_shared && currentDirectory != null) {
                files = updatedStorageManager.filter(currentDirectory, OCFileFilterType.Shared);
            }
            if (DrawerActivity.menuItemId == R.id.nav_favorites && currentDirectory != null) {
                files = updatedStorageManager.filter(currentDirectory, OCFileFilterType.Favorite);
            }

            // Filter out temp files from the list to prevent duplication
            files = OCFileExtensionsKt.filterTempFilter(files);
            files = OCFileExtensionsKt.filterFilenames(files);

            sortOrder = preferences.getSortOrderByFolder(directory);
            boolean foldersBeforeFiles = preferences.isSortFoldersBeforeFiles();
            boolean favoritesFirst = preferences.isSortFavoritesFirst();
            files = sortOrder.sortCloudFiles(files, foldersBeforeFiles, favoritesFirst);

            // Create new list for mFiles to avoid sharing references
            mFiles = new ArrayList<>(files);

            prepareListOfHiddenFiles();
            mergeOCFilesForLivePhoto();

            mFilesAll.clear();
            addOfflineOperations(directory.getFileId());
            mFilesAll.addAll(mFiles);

            currentDirectory = directory;
        } else {
            mFiles = new ArrayList<>(); // Create new instance instead of clear
            mFilesAll.clear();
        }

        searchType = null;
        activity.runOnUiThread(this::notifyDataSetChanged);
    }

    /**
     * Converts Offline Operations to OCFiles and adds them to the adapter for visual feedback.
     * This function creates pending OCFiles, but they may not consistently appear in the UI.
     * The issue arises when  {@link RefreshFolderOperation} deletes pending Offline Operations, while some may still exist in the table.
     * If only this function is used, it cause crash in {@link FileDisplayActivity mSyncBroadcastReceiver.onReceive}.
     * <p>
     * These function also need to be used: {@link FileDataStorageManager#createPendingDirectory(String, long, long)}, {@link FileDataStorageManager#createPendingFile(String, String, long, long)}.
     */
    private void addOfflineOperations(long fileId) {
        List<OCFile> offlineOperations = mStorageManager.offlineOperationsRepository.convertToOCFiles(fileId);
        if (offlineOperations.isEmpty()) {
            return;
        }

        List<OCFile> newFiles;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            newFiles = offlineOperations.stream()
                .filter(offlineFile -> mFilesAll.stream()
                    .noneMatch(file -> Objects.equals(file.getDecryptedRemotePath(), offlineFile.getDecryptedRemotePath())))
                .toList();
        } else {
            newFiles = offlineOperations.stream()
                .filter(offlineFile -> mFilesAll.stream()
                    .noneMatch(file -> Objects.equals(file.getDecryptedRemotePath(), offlineFile.getDecryptedRemotePath())))
                .collect(Collectors.toList());
        }

        mFilesAll.addAll(newFiles);
    }

    public void setSearchData(List<OCFile> newList, SearchType searchType, FileDataStorageManager storageManager, boolean clear) {
        initStorageManagerShowShareAvatar(storageManager);
        if (clear) {
            clearSearchData(searchType);
        }
        new Handler(Looper.getMainLooper()).post(() -> updateData(newList));
    }

    private void initStorageManagerShowShareAvatar(FileDataStorageManager storageManager) {
        if (mStorageManager == null) {
            mStorageManager = (storageManager != null)
                ? storageManager
                : new FileDataStorageManager(user, activity.getContentResolver());

            if (storageManager != null) {
                ocFileListDelegate.setShowShareAvatar(true);
            }
        }
    }

    private void clearSearchData(SearchType searchType) {
        preferences.setPhotoSearchTimestamp(0);

        VirtualFolderType type = switch (searchType) {
            case FAVORITE_SEARCH -> VirtualFolderType.FAVORITE;
            case GALLERY_SEARCH  -> VirtualFolderType.GALLERY;
            default              -> VirtualFolderType.NONE;
        };

        if (type != VirtualFolderType.GALLERY) {
            mStorageManager.deleteVirtuals(type);
        }
    }

    public void setSortOrder(FileSortOrder newSortOrder) {
        sortOrder = newSortOrder;
    }

    public void updateData(List<OCFile> newList) {
        if (mFiles.isEmpty() && newList.isEmpty()) {
            return;
        }
        if (mFiles == newList) {
            return;
        }

        diffUtil.updateLists(mFiles, newList, shouldShowHeader());
        final var diffResult = DiffUtil.calculateDiff(diffUtil);

        mFiles.clear();
        mFiles.addAll(newList);

        diffResult.dispatchUpdatesTo(this);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSortOrder(@Nullable OCFile folder, @NonNull FileSortOrder sortOrder) {
        if (searchType == SearchType.FAVORITE_SEARCH) {
            preferences.setSortOrder(FileSortOrder.Type.favoritesListView, sortOrder);    
        } else {
            preferences.setSortOrder(folder, sortOrder);
        }

        boolean foldersBeforeFiles = preferences.isSortFoldersBeforeFiles();
        boolean favoritesFirst = preferences.isSortFavoritesFirst();
        mFiles = sortOrder.sortCloudFiles(mFiles, foldersBeforeFiles, favoritesFirst);
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

    private void prepareListOfHiddenFiles() {
        listOfHiddenFiles.clear();

        mFiles.forEach(file -> {
            if (file.shouldHide()) {
                listOfHiddenFiles.add(file.getFileName());
            }
        });
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof ListViewHolder listViewHolder) {
            LoaderImageView thumbnailShimmer = listViewHolder.getShimmerThumbnail();
            DisplayUtils.stopShimmer(thumbnailShimmer,  listViewHolder.getThumbnail());
        }
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
    public String getPopupText(View view, int position) {
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

    @Override
    public int getFilesCount() {
        return mFiles.size();
    }

    @Override
    public void notifyItemChanged(@NonNull OCFile file) {
        if (shouldShowRecommendedFiles() && recommendedFilesAdapter != null && file.isRecommendedFile()) {
            final int position = recommendedFilesAdapter.getItemPosition(file);
            recommendedFilesAdapter.notifyItemChanged(position);
        } else {
            notifyItemChanged(getItemPosition(file));
        }
    }

    @VisibleForTesting
    public void setCurrentDirectory(OCFile folder) {
        currentDirectory = folder;
    }

    public void cleanup() {
        ocFileListDelegate.cleanup();
    }
}
