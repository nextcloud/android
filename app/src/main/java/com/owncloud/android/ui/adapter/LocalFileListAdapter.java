/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nextcloud.android.common.ui.theme.utils.ColorRole;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.utils.FileHelper;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.adapter.storagePermissionBanner.StoragePermissionBannerViewHolder;
import com.owncloud.android.ui.interfaces.LocalFileListFragmentInterface;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.PermissionUtil;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

/**
 * This Adapter populates a {@link RecyclerView} with all files and directories contained in a local directory
 */
public class LocalFileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FilterableListAdapter {

    private static final String TAG = LocalFileListAdapter.class.getSimpleName();

    private final AppPreferences preferences;
    private final Activity mContext;
    private List<File> mFiles = new ArrayList<>();
    private List<File> mFilesAll = new ArrayList<>();
    private boolean mLocalFolderPicker;
    private boolean gridView = false;
    private LocalFileListFragmentInterface localFileListFragmentInterface;
    private Set<File> checkedFiles;
    private ViewThemeUtils viewThemeUtils;
    private boolean isWithinEncryptedFolder;

    private static final int VIEWTYPE_ITEM = 0;
    private static final int VIEWTYPE_FOOTER = 1;
    private static final int VIEWTYPE_IMAGE = 2;
    private static final int VIEWTYPE_HEADER = 3;

    private static final int PAGE_SIZE = 50;
    private int currentOffset = 0;

    public LocalFileListAdapter(boolean localFolderPickerMode,
                                LocalFileListFragmentInterface localFileListFragmentInterface,
                                AppPreferences preferences,
                                Activity context,
                                final ViewThemeUtils viewThemeUtils,
                                boolean isWithinEncryptedFolder) {
        this.preferences = preferences;
        mContext = context;
        mLocalFolderPicker = localFolderPickerMode;
        this.localFileListFragmentInterface = localFileListFragmentInterface;
        checkedFiles = new HashSet<>();
        this.viewThemeUtils = viewThemeUtils;
        this.isWithinEncryptedFolder = isWithinEncryptedFolder;
        setHasStableIds(true);
    }

    public int getFilesCount() {
        return mFiles.size();
    }

    public boolean isCheckedFile(File file) {
        return checkedFiles.contains(file);
    }

    public void removeCheckedFile(File file) {
        checkedFiles.remove(file);
    }

    public void addCheckedFile(File file) {
        checkedFiles.add(file);
    }

    public void addAllFilesToCheckedFiles() {
        if (isWithinEncryptedFolder) {
            for (File file : mFilesAll) {
                if (file.isFile()) {
                    checkedFiles.add(file);
                }
            }
        } else {
            checkedFiles.addAll(mFiles);
        }
    }

    public void removeAllFilesFromCheckedFiles() {
        checkedFiles.clear();
    }

    public int getItemPosition(File file) {
        return mFiles.indexOf(file);
    }

    public String[] getCheckedFilesPath() {
        List<String> result = FileHelper.INSTANCE.listFilesRecursive(checkedFiles);

        Log_OC.d(TAG, "Returning " + result.size() + " selected files");

        return result.toArray(new String[0]);
    }

    @Override
    public int getItemCount() {
        // footer always exists
        int count = mFiles.size() + 1;

        // check header section visibility
        if (shouldShowHeader()) {
            count += 1;
        }

        return count;
    }

    @Override
    public long getItemId(int position) {
        if (position >= mFiles.size()) {
            return RecyclerView.NO_ID;
        }

        File file = mFiles.get(position);
        return file.getAbsolutePath().hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        boolean header = shouldShowHeader();
        int headerOffset = header ? 1 : 0;

        if (header && position == 0) {
            return VIEWTYPE_HEADER;
        }

        // footer position
        if (position == mFiles.size() + headerOffset) {
            return VIEWTYPE_FOOTER;
        }

        // real file position
        int fileIndex = position - headerOffset;
        File file = mFiles.get(fileIndex);

        if (MimeTypeUtil.isImageOrVideo(file)) {
            return VIEWTYPE_IMAGE;
        } else {
            return VIEWTYPE_ITEM;
        }
    }

    private boolean shouldShowHeader() {
        return !PermissionUtil.checkStoragePermission(mContext);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        boolean headerVisible = shouldShowHeader();
        int headerOffset = headerVisible ? 1 : 0;

        // --- HEADER ---
        if (headerVisible && position == 0) {
            // Header has no dynamic binding
            return;
        }

        // --- FOOTER ---
        if (position == mFiles.size() + headerOffset) {
            LocalFileListFooterViewHolder footer = (LocalFileListFooterViewHolder) holder;
            footer.footerText.setText(getFooterText());
            return;
        }

        int fileIndex = position - headerOffset;
        if (fileIndex < 0 || fileIndex >= mFiles.size()) {
            return;
        }

        File file = mFiles.get(fileIndex);
        if (file == null) {
            return;
        }

        LocalFileListGridItemViewHolder grid = (LocalFileListGridItemViewHolder) holder;

        // Background + checkbox logic
        if (mLocalFolderPicker) {
            grid.itemLayout.setBackgroundColor(mContext.getResources().getColor(R.color.bg_default));
            grid.checkbox.setVisibility(View.GONE);
        } else {
            grid.checkbox.setVisibility(View.VISIBLE);

            if (isCheckedFile(file)) {
                grid.itemLayout.setBackgroundColor(
                    ContextCompat.getColor(mContext, R.color.selected_item_background)
                                                  );
                grid.checkbox.setImageDrawable(
                    viewThemeUtils.platform.tintDrawable(mContext, R.drawable.ic_checkbox_marked, ColorRole.PRIMARY)
                                              );
            } else {
                grid.itemLayout.setBackgroundColor(
                    mContext.getResources().getColor(R.color.bg_default)
                                                  );
                grid.checkbox.setImageResource(R.drawable.ic_checkbox_blank_outline);
            }

            grid.checkbox.setOnClickListener(v ->
                                                 localFileListFragmentInterface.onItemCheckboxClicked(file)
                                            );
        }

        // Thumbnail
        grid.thumbnail.setTag(file.hashCode());
        setThumbnail(file, grid.thumbnail, mContext, viewThemeUtils);

        grid.itemLayout.setOnClickListener(v ->
                                               localFileListFragmentInterface.onItemClicked(file)
                                          );

        if (holder instanceof LocalFileListItemViewHolder item) {
            if (file.isDirectory()) {
                item.fileSize.setVisibility(View.GONE);
                item.fileSeparator.setVisibility(View.GONE);

                if (isWithinEncryptedFolder) {
                    item.checkbox.setVisibility(View.GONE);
                }

            } else {
                item.fileSize.setVisibility(View.VISIBLE);
                item.fileSeparator.setVisibility(View.VISIBLE);
                item.fileSize.setText(DisplayUtils.bytesToHumanReadable(file.length()));
            }

            item.lastModification.setText(
                DisplayUtils.getRelativeTimestamp(mContext, file.lastModified())
                                         );
        }

        // Filename
        grid.fileName.setText(file.getName());
    }

    public static void setThumbnail(File file,
                                    ImageView thumbnailView,
                                    Context context,
                                    ViewThemeUtils viewThemeUtils) {
        if (file.isDirectory()) {
            thumbnailView.setImageDrawable(MimeTypeUtil.getDefaultFolderIcon(context, viewThemeUtils));
        } else {
            thumbnailView.setImageResource(R.drawable.file);

            /* Cancellation needs do be checked and done before changing the drawable in fileIcon, or
             * {@link ThumbnailsCacheManager#cancelPotentialThumbnailWork} will NEVER cancel any task.
             */
            boolean allowedToCreateNewThumbnail = ThumbnailsCacheManager.cancelPotentialThumbnailWork(file,
                                                                                                      thumbnailView);


            // get Thumbnail if file is image
            if (MimeTypeUtil.isImage(file)) {
                // Thumbnail in Cache?
                Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                        ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.hashCode()
                );
                if (thumbnail != null) {
                    thumbnailView.setImageBitmap(thumbnail);
                } else {

                    // generate new Thumbnail
                    if (allowedToCreateNewThumbnail) {
                        final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                                new ThumbnailsCacheManager.ThumbnailGenerationTask(thumbnailView);
                        if (MimeTypeUtil.isVideo(file)) {
                            thumbnail = ThumbnailsCacheManager.mDefaultVideo;
                        } else {
                            thumbnail = ThumbnailsCacheManager.mDefaultImg;
                        }
                        final ThumbnailsCacheManager.AsyncThumbnailDrawable asyncDrawable =
                                new ThumbnailsCacheManager.AsyncThumbnailDrawable(
                                    context.getResources(),
                                    thumbnail,
                                    task
                                );
                        thumbnailView.setImageDrawable(asyncDrawable);
                        task.execute(new ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file, null));
                        Log_OC.v(TAG, "Executing task to generate a new thumbnail");

                    } // else, already being generated, don't restart it
                }
            } else {
                thumbnailView.setImageDrawable(MimeTypeUtil.getFileTypeIcon(null,
                                                                            file.getName(),
                                                                            context,
                                                                            viewThemeUtils));
            }
        }
    }

    private File getItem(int position) {
        return mFiles.get(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEWTYPE_ITEM, VIEWTYPE_IMAGE:
                if (gridView) {
                    View itemView = LayoutInflater.from(mContext).inflate(R.layout.grid_item, parent, false);
                    return new LocalFileListGridItemViewHolder(itemView);
                } else {
                    View itemView = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
                    return new LocalFileListItemViewHolder(itemView);
                }

            case VIEWTYPE_FOOTER:
                View itemView = LayoutInflater.from(mContext).inflate(R.layout.list_footer, parent, false);
                return new LocalFileListFooterViewHolder(itemView);

            case VIEWTYPE_HEADER:
                View headerItemView = LayoutInflater.from(mContext).inflate(R.layout.storage_permission_warning_banner, parent, false);
                return new StoragePermissionBannerViewHolder(mContext, headerItemView);
            default:
                throw new IllegalArgumentException("Invalid viewType: " + viewType);
        }
    }

    /**
     * Change the adapted directory for a new one
     *
     * @param directory New file to adapt. Can be NULL, meaning "no content to adapt".
     */
    public void swapDirectory(final File directory) {
        localFileListFragmentInterface.setLoading(true);
        currentOffset = 0;
        mFiles.clear();
        mFilesAll.clear();

        Executors.newSingleThreadExecutor().execute(() -> {
            // Load first page of folders
            List<File> firstPage = FileHelper.INSTANCE.listDirectoryEntries(directory, currentOffset, PAGE_SIZE, true);

            if (!firstPage.isEmpty()) {
                firstPage = sortAndFilterHiddenEntries(firstPage);
            }

            currentOffset += PAGE_SIZE;
            updateUIForFirstPage(firstPage);

            // Load remaining folders, then all files
            loadRemainingEntries(directory, true);

            // Reset for files
            currentOffset = 0;

            loadRemainingEntries(directory, false);
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateUIForFirstPage(List<File> firstPage) {
        new Handler(Looper.getMainLooper()).post(() -> {
            mFiles = new ArrayList<>(firstPage);
            mFilesAll = new ArrayList<>(firstPage);
            notifyDataSetChanged();
            localFileListFragmentInterface.setLoading(false);
        });
    }

    private List<File> sortAndFilterHiddenEntries(List<File> nextPage) {
        boolean showHiddenFiles = preferences.isShowHiddenFilesEnabled();
        FileSortOrder sortOrder = preferences.getSortOrderByType(FileSortOrder.Type.localFileListView);

        if (!showHiddenFiles) {
            nextPage = filterHiddenFiles(nextPage);
        }

        return sortOrder.sortLocalFiles(nextPage);
    }

    private void loadRemainingEntries(File directory, boolean fetchFolders) {
        while (true) {
            List<File> nextPage = FileHelper.INSTANCE.listDirectoryEntries(directory, currentOffset, PAGE_SIZE, fetchFolders);
            if (nextPage.isEmpty()) {
                break;
            }

            nextPage = sortAndFilterHiddenEntries(nextPage);

            currentOffset += PAGE_SIZE;
            notifyItemRange(nextPage);
        }
    }

    private void notifyItemRange(List<File> updatedList) {
        new Handler(Looper.getMainLooper()).post(() -> {
            int from = mFiles.size();
            int to = updatedList.size();

            mFiles.addAll(updatedList);
            mFilesAll.addAll(updatedList);

            Log_OC.d(TAG, "notifyItemRange, item size: " + mFilesAll.size());

            notifyItemRangeInserted(from, to);
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSortOrder(FileSortOrder sortOrder) {
        localFileListFragmentInterface.setLoading(true);
        final Handler uiHandler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            preferences.setSortOrder(FileSortOrder.Type.localFileListView, sortOrder);
            mFiles = sortOrder.sortLocalFiles(mFiles);

            uiHandler.post(() -> {
                notifyDataSetChanged();
                localFileListFragmentInterface.setLoading(false);
            });
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filter(String text) {
        if (text.isEmpty()) {
            mFiles = mFilesAll;
        } else {
            List<File> result = new ArrayList<>();
            String filterText = text.toLowerCase(Locale.getDefault());
            for (File file : mFilesAll) {
                if (file.getName().toLowerCase(Locale.getDefault()).contains(filterText)) {
                    result.add(file);
                }
            }
            mFiles = result;
        }
        notifyDataSetChanged();
    }

    /**
     * Filter for hidden files
     *
     * @param files ArrayList of files to filter
     * @return Non-hidden files
     */
    private List<File> filterHiddenFiles(List<File> files) {
        List<File> ret = new ArrayList<>();

        for (File file : files) {
            if (!file.isHidden()) {
                ret.add(file);
            }
        }
        return ret;
    }

    private String getFooterText() {
        int filesCount = 0;
        int foldersCount = 0;

        for (File file : mFiles) {
            if (file.isDirectory()) {
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

    public void setGridView(boolean gridView) {
        this.gridView = gridView;
    }

    public int checkedFilesCount() {
        return checkedFiles.size();
    }

    private static class LocalFileListItemViewHolder extends LocalFileListGridItemViewHolder {
        private final TextView fileSize;
        private final TextView lastModification;
        private final TextView fileSeparator;

        private LocalFileListItemViewHolder(View itemView) {
            super(itemView);

            fileSize = itemView.findViewById(R.id.file_size);
            fileSeparator = itemView.findViewById(R.id.file_separator);
            lastModification = itemView.findViewById(R.id.last_mod);

            itemView.findViewById(R.id.sharedAvatars).setVisibility(View.GONE);
            itemView.findViewById(R.id.overflow_menu).setVisibility(View.GONE);
            itemView.findViewById(R.id.tagsGroup).setVisibility(View.GONE);
        }
    }

    private static class LocalFileListGridItemViewHolder extends RecyclerView.ViewHolder {
        protected final TextView fileName;
        protected final ImageView thumbnail;
        protected final ImageView checkbox;
        protected final LinearLayout itemLayout;

        private LocalFileListGridItemViewHolder(View itemView) {
            super(itemView);

            fileName = itemView.findViewById(R.id.Filename);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            checkbox = itemView.findViewById(R.id.custom_checkbox);
            itemLayout = itemView.findViewById(R.id.ListItemLayout);

            itemView.findViewById(R.id.sharedIcon).setVisibility(View.GONE);
            itemView.findViewById(R.id.favorite_action).setVisibility(View.GONE);
            itemView.findViewById(R.id.localFileIndicator).setVisibility(View.GONE);
        }
    }

    private static class LocalFileListFooterViewHolder extends RecyclerView.ViewHolder {
        private final TextView footerText;

        private LocalFileListFooterViewHolder(View itemView) {
            super(itemView);

            footerText = itemView.findViewById(R.id.footerText);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @VisibleForTesting
    public void setFiles(List<File> newFiles) {
        mFiles = newFiles;
        mFilesAll = new ArrayList<>(mFiles);

        notifyDataSetChanged();
        localFileListFragmentInterface.setLoading(false);
    }
}
