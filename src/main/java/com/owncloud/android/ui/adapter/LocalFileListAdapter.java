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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.interfaces.LocalFileListFragmentInterface;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeDrawableUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * This Adapter populates a {@link RecyclerView} with all files and directories contained in a local directory
 */
public class LocalFileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FilterableListAdapter {

    private static final String TAG = LocalFileListAdapter.class.getSimpleName();

    private static final int showFilenameColumnThreshold = 4;
    private AppPreferences preferences;
    private Context mContext;
    private List<File> mFiles = new ArrayList<>();
    private List<File> mFilesAll = new ArrayList<>();
    private boolean mLocalFolderPicker;
    private boolean gridView = false;
    private LocalFileListFragmentInterface localFileListFragmentInterface;
    private Set<File> checkedFiles;

    private static final int VIEWTYPE_ITEM = 0;
    private static final int VIEWTYPE_FOOTER = 1;
    private static final int VIEWTYPE_IMAGE = 2;

    public LocalFileListAdapter(boolean localFolderPickerMode, File directory,
                                LocalFileListFragmentInterface localFileListFragmentInterface, AppPreferences preferences, Context context) {
        this.preferences = preferences;
        mContext = context;
        mLocalFolderPicker = localFolderPickerMode;
        swapDirectory(directory);
        this.localFileListFragmentInterface = localFileListFragmentInterface;
        checkedFiles = new HashSet<>();
    }

    @Override
    public int getItemCount() {
        return mFiles.size() + 1;
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
        checkedFiles.addAll(mFiles);
    }

    public void removeAllFilesFromCheckedFiles() {
        checkedFiles.clear();
    }

    public int getItemPosition(File file) {
        return mFiles.indexOf(file);
    }

    public String[] getCheckedFilesPath() {
        List<String> result = listFilesRecursive(checkedFiles);

        Log_OC.d(TAG, "Returning " + result.size() + " selected files");

        return result.toArray(new String[0]);
    }

    public List<String> listFilesRecursive(Collection<File> files) {
        List<String> result = new ArrayList<>();

        for (File file : files) {
            if (file.isDirectory()) {
                result.addAll(listFilesRecursive(getFiles(file)));
            } else {
                result.add(file.getAbsolutePath());
            }
        }

        return result;
    }

    @Override
    public long getItemId(int position) {
        return mFiles.size() <= position ? position : -1;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LocalFileListFooterViewHolder) {
            ((LocalFileListFooterViewHolder) holder).footerText.setText(getFooterText());
        } else {
            File file = null;
            if (mFiles.size() > position && mFiles.get(position) != null) {
                file = mFiles.get(position);
            }

            if (file != null) {
                LocalFileListGridImageViewHolder gridViewHolder = (LocalFileListGridImageViewHolder) holder;

                // checkbox
                if (isCheckedFile(file)) {
                    gridViewHolder.itemLayout.setBackgroundColor(mContext.getResources()
                            .getColor(R.color.selected_item_background));
                    gridViewHolder.checkbox.setImageDrawable(
                        ThemeDrawableUtils.tintDrawable(R.drawable.ic_checkbox_marked,
                                                        ThemeColorUtils.primaryColor(mContext)));
                } else {
                    gridViewHolder.itemLayout.setBackgroundColor(mContext.getResources().getColor(R.color.bg_default));
                    gridViewHolder.checkbox.setImageResource(R.drawable.ic_checkbox_blank_outline);
                }

                gridViewHolder.thumbnail.setTag(file.hashCode());
                setThumbnail(file, gridViewHolder.thumbnail, mContext);

                gridViewHolder.checkbox.setVisibility(View.VISIBLE);

                File finalFile = file;
                gridViewHolder.itemLayout.setOnClickListener(v -> localFileListFragmentInterface
                        .onItemClicked(finalFile));
                gridViewHolder.checkbox.setOnClickListener(v -> localFileListFragmentInterface
                        .onItemCheckboxClicked(finalFile));


                if (holder instanceof LocalFileListItemViewHolder) {
                    LocalFileListItemViewHolder itemViewHolder = (LocalFileListItemViewHolder) holder;

                    if (file.isDirectory()) {
                        itemViewHolder.fileSize.setVisibility(View.GONE);
                        itemViewHolder.fileSeparator.setVisibility(View.GONE);
                    } else {
                        itemViewHolder.fileSize.setVisibility(View.VISIBLE);
                        itemViewHolder.fileSeparator.setVisibility(View.VISIBLE);
                        itemViewHolder.fileSize.setText(DisplayUtils.bytesToHumanReadable(file.length()));
                    }
                    itemViewHolder.lastModification.setText(DisplayUtils.getRelativeTimestamp(mContext,
                            file.lastModified()));
                }

                if (gridViewHolder instanceof LocalFileListGridItemViewHolder) {
                    LocalFileListGridItemViewHolder itemVH = (LocalFileListGridItemViewHolder) gridViewHolder;
                    itemVH.fileName.setText(file.getName());

                    if (gridView && (MimeTypeUtil.isImage(file) || MimeTypeUtil.isVideo(file) ||
                        localFileListFragmentInterface.getColumnsCount() > showFilenameColumnThreshold)) {
                        itemVH.fileName.setVisibility(View.GONE);
                    } else {
                        itemVH.fileName.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    public static void setThumbnail(File file, ImageView thumbnailView, Context context) {
        if (file.isDirectory()) {
            thumbnailView.setImageDrawable(MimeTypeUtil.getDefaultFolderIcon(context));
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
                thumbnailView.setImageDrawable(MimeTypeUtil.getFileTypeIcon(null, file.getName(), context));
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mFiles.size()) {
            return VIEWTYPE_FOOTER;
        } else {
            if (MimeTypeUtil.isImageOrVideo(getItem(position))) {
                return VIEWTYPE_IMAGE;
            } else {
                return VIEWTYPE_ITEM;
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
            default:
            case VIEWTYPE_ITEM:
                if (gridView) {
                    View itemView = LayoutInflater.from(mContext).inflate(R.layout.grid_item, parent, false);
                    return new LocalFileListGridItemViewHolder(itemView);
                } else {
                    View itemView = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
                    return new LocalFileListItemViewHolder(itemView);
                }

            case VIEWTYPE_IMAGE:
                if (gridView) {
                    View itemView = LayoutInflater.from(mContext).inflate(R.layout.grid_image, parent, false);
                    return new LocalFileListGridImageViewHolder(itemView);
                } else {
                    View itemView = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
                    return new LocalFileListItemViewHolder(itemView);
                }

            case VIEWTYPE_FOOTER:
                View itemView = LayoutInflater.from(mContext).inflate(R.layout.list_footer, parent, false);
                return new LocalFileListFooterViewHolder(itemView);
        }
    }

    /**
     * Change the adapted directory for a new one
     *
     * @param directory New file to adapt. Can be NULL, meaning "no content to adapt".
     */
    public void swapDirectory(final File directory) {
        if (mLocalFolderPicker) {
            if (directory == null) {
                mFiles.clear();
            } else {
                mFiles = getFolders(directory);
            }
        } else {
            if (directory == null) {
                mFiles.clear();
            } else {
                mFiles = getFiles(directory);
            }
        }

        FileSortOrder sortOrder = preferences.getSortOrderByType(FileSortOrder.Type.localFileListView);
        mFiles = sortOrder.sortLocalFiles(mFiles);

        // Fetch preferences for showing hidden files
        boolean showHiddenFiles = preferences.isShowHiddenFilesEnabled();
        if (!showHiddenFiles) {
            mFiles = filterHiddenFiles(mFiles);
        }

        mFilesAll.clear();
        mFilesAll.addAll(mFiles);

        notifyDataSetChanged();
    }

    public void setSortOrder(FileSortOrder sortOrder) {
        preferences.setSortOrder(FileSortOrder.Type.localFileListView, sortOrder);
        mFiles = sortOrder.sortLocalFiles(mFiles);
        notifyDataSetChanged();
    }

    private List<File> getFolders(final File directory) {
        File[] folders = directory.listFiles(File::isDirectory);

        if (folders != null && folders.length > 0) {
            return new ArrayList<>(Arrays.asList(folders));
        } else {
            return new ArrayList<>();
        }
    }

    private List<File> getFiles(File directory) {
        File[] files = directory.listFiles();

        if (files != null && files.length > 0) {
            return new ArrayList<>(Arrays.asList(files));
        } else {
            return new ArrayList<>();
        }
    }

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
        }
    }

    static class LocalFileListGridImageViewHolder extends RecyclerView.ViewHolder {
        protected final ImageView thumbnail;
        protected final ImageView checkbox;
        protected final LinearLayout itemLayout;

        private LocalFileListGridImageViewHolder(View itemView) {
            super(itemView);

            thumbnail = itemView.findViewById(R.id.thumbnail);
            checkbox = itemView.findViewById(R.id.custom_checkbox);
            itemLayout = itemView.findViewById(R.id.ListItemLayout);

            itemView.findViewById(R.id.sharedIcon).setVisibility(View.GONE);
            itemView.findViewById(R.id.favorite_action).setVisibility(View.GONE);
            itemView.findViewById(R.id.localFileIndicator).setVisibility(View.GONE);
        }
    }

    static class LocalFileListGridItemViewHolder extends LocalFileListGridImageViewHolder {
        private final TextView fileName;

        private LocalFileListGridItemViewHolder(View itemView) {
            super(itemView);

            fileName = itemView.findViewById(R.id.Filename);
        }
    }

    private static class LocalFileListFooterViewHolder extends RecyclerView.ViewHolder {
        private final TextView footerText;

        private LocalFileListFooterViewHolder(View itemView) {
            super(itemView);

            footerText = itemView.findViewById(R.id.footerText);
        }
    }
}
