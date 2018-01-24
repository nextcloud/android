/*
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2015 ownCloud Inc.
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

import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.interfaces.LocalFileListFragmentInterface;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ThemeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * This Adapter populates a RecycylerView with all files and directories contained in a local directory
 */
public class LocalFileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FilterableListAdapter {

    private static final String TAG = LocalFileListAdapter.class.getSimpleName();

    private static final int showFilenameColumnThreshold = 4;
    private Context mContext;
    private ArrayList<File> mFiles = new ArrayList<>();
    private ArrayList<File> mFilesAll = new ArrayList<>();
    private boolean mLocalFolderPicker;
    private boolean gridView = false;
    private LocalFileListFragmentInterface localFileListFragmentInterface;
    private Set<File> checkedFiles;

    private static final int VIEWTYPE_ITEM = 0;
    private static final int VIEWTYPE_FOOTER = 1;

    public LocalFileListAdapter(boolean localFolderPickerMode, File directory,
                                LocalFileListFragmentInterface localFileListFragmentInterface, Context context) {
        mContext = context;
        mLocalFolderPicker = localFolderPickerMode;
        swapDirectory(directory);
        this.localFileListFragmentInterface = localFileListFragmentInterface;
        checkedFiles = new HashSet<>();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        // footer is not enabled
        return position < getItemCount();
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        // not needed
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        // not needed
    }

    @Override
    public int getItemCount() {
        return mFiles.size() + 1;
    }

    @Override
    public int getCount() {
        return mFiles.size() + 1;
    }

    @Override
    public File getItem(int position) {
        return mFiles.get(position);
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
        return mFilesAll.indexOf(file);
    }

    public String[] getCheckedFilesPath() {
        ArrayList<String> result = new ArrayList<>();

        for (File file : checkedFiles) {
            result.add(file.getAbsolutePath());
        }

        Log_OC.d(TAG, "Returning " + result.size() + " selected files");

        return result.toArray(new String[result.size()]);
    }

    @Override
    public long getItemId(int position) {
        return mFiles.size() <= position ? position : -1;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LocalFileListFooterViewHolder) {
            ((LocalFileListFooterViewHolder) holder).footerText.setText(getFooterText());
        } else {
            File file = null;
            if (mFiles.size() > position && mFiles.get(position) != null) {
                file = mFiles.get(position);
            }

            if (file != null) {
                LocalFileListGridViewHolder gridViewHolder = (LocalFileListGridViewHolder) holder;

                // checkbox
                if (isCheckedFile(file)) {
                    gridViewHolder.itemLayout.setBackgroundColor(mContext.getResources()
                            .getColor(R.color.selected_item_background));
                    gridViewHolder.checkbox.setImageDrawable(ThemeUtils.tintDrawable(R.drawable.ic_checkbox_marked,
                            ThemeUtils.primaryColor()));
                } else {
                    gridViewHolder.itemLayout.setBackgroundColor(Color.WHITE);
                    gridViewHolder.checkbox.setImageResource(R.drawable.ic_checkbox_blank_outline);
                }

                gridViewHolder.thumbnail.setTag(file.hashCode());
                setThumbnail(file, gridViewHolder.thumbnail);

                if (file.isDirectory()) {
                    gridViewHolder.checkbox.setVisibility(View.GONE);
                } else {
                    gridViewHolder.checkbox.setVisibility(View.VISIBLE);
                }

                File finalFile = file;
                gridViewHolder.itemLayout.setOnClickListener(v -> localFileListFragmentInterface
                        .onItemClicked(finalFile));

                gridViewHolder.fileName.setText(file.getName());

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

                if (gridView && (MimeTypeUtil.isImage(file) || MimeTypeUtil.isVideo(file) ||
                        localFileListFragmentInterface.getColumnSize() > showFilenameColumnThreshold)) {
                    gridViewHolder.fileName.setVisibility(View.GONE);
                } else {
                    gridViewHolder.fileName.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void setThumbnail(File file, ImageView thumbnailView) {
        if (file.isDirectory()) {
            thumbnailView.setImageDrawable(MimeTypeUtil.getDefaultFolderIcon());
        } else {
            thumbnailView.setImageResource(R.drawable.file);

            /** Cancellation needs do be checked and done before changing the drawable in fileIcon, or
             * {@link ThumbnailsCacheManager#cancelPotentialThumbnailWork} will NEVER cancel any task.
             **/
            boolean allowedToCreateNewThumbnail = (ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, thumbnailView));


            // get Thumbnail if file is image
            if (MimeTypeUtil.isImage(file)) {
                // Thumbnail in Cache?
                Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                        ThumbnailsCacheManager.PREFIX_THUMBNAIL + String.valueOf(file.hashCode())
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
                                        mContext.getResources(),
                                        thumbnail,
                                        task
                                );
                        thumbnailView.setImageDrawable(asyncDrawable);
                        task.execute(new ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file, null));
                        Log_OC.v(TAG, "Executing task to generate a new thumbnail");

                    } // else, already being generated, don't restart it
                }
            } else {
                thumbnailView.setImageDrawable(MimeTypeUtil.getFileTypeIcon(null, file.getName(), null)
                );
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mFiles.size()) {
            return VIEWTYPE_FOOTER;
        } else {
            return VIEWTYPE_ITEM;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            default:
            case VIEWTYPE_ITEM:
                if (gridView) {
                    View itemView = LayoutInflater.from(mContext).inflate(R.layout.grid_item, parent, false);
                    return new LocalFileListGridViewHolder(itemView);
                } else {
                    View itemView = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
                    return new LocalFileListItemViewHolder(itemView);
                }

            case VIEWTYPE_FOOTER:
                View itemView = LayoutInflater.from(mContext).inflate(R.layout.list_footer, parent, false);
                return new LocalFileListFooterViewHolder(itemView);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        File file = null;

        boolean isGridView = true;

        ImageView checkBoxV = view.findViewById(R.id.custom_checkbox);
        TextView fileSizeV = view.findViewById(R.id.file_size);
        TextView fileSizeSeparatorV = view.findViewById(R.id.file_separator);
        if (!isGridView) {
            TextView lastModV = view.findViewById(R.id.last_mod);
            lastModV.setVisibility(View.VISIBLE);
            lastModV.setText(DisplayUtils.getRelativeTimestamp(mContext, file.lastModified()));
            view.findViewById(R.id.overflow_menu).setVisibility(View.GONE);
        }

        if (!file.isDirectory()) {
            if (!isGridView) {
                fileSizeSeparatorV.setVisibility(View.VISIBLE);
                fileSizeV.setVisibility(View.VISIBLE);
                fileSizeV.setText(DisplayUtils.bytesToHumanReadable(file.length()));
            }

            AbsListView parentList = (AbsListView) parent;
            if (parentList.getChoiceMode() == ListView.CHOICE_MODE_NONE) {
                checkBoxV.setVisibility(View.GONE);
            } else {
                if (parentList.isItemChecked(position)) {
                    checkBoxV.setImageResource(R.drawable.ic_checkbox_marked);
                } else {
                    checkBoxV.setImageResource(R.drawable.ic_checkbox_blank_outline);
                }
                checkBoxV.setVisibility(View.VISIBLE);
            }
        } else {
            if (!isGridView) {
                fileSizeSeparatorV.setVisibility(View.GONE);
                fileSizeV.setVisibility(View.GONE);
            }
            checkBoxV.setVisibility(View.GONE);
        }

        // not GONE; the alignment changes; ugly way to keep it
        view.findViewById(R.id.localFileIndicator).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.keptOfflineIcon).setVisibility(View.GONE);
        view.findViewById(R.id.favorite_action).setVisibility(View.GONE);

        view.findViewById(R.id.sharedIcon).setVisibility(View.GONE);

        return view;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return mFiles.isEmpty();
    }

    /**
     * Change the adapted directory for a new one
     * @param directory     New file to adapt. Can be NULL, meaning "no content to adapt".
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

        Collections.sort(mFiles, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                if (lhs.isDirectory() && !rhs.isDirectory()) {
                    return -1;
                } else if (!lhs.isDirectory() && rhs.isDirectory()) {
                    return 1;
                }
                return compareNames(lhs, rhs);
            }

            private int compareNames(File lhs, File rhs) {
                return lhs.getName().toLowerCase(Locale.getDefault()).compareTo(
                        rhs.getName().toLowerCase(Locale.getDefault()));
            }
        });

        FileSortOrder sortOrder = PreferenceManager.getSortOrder(mContext, null);
        mFiles = sortOrder.sortLocalFiles(mFiles);

        // Fetch preferences for showing hidden files
        boolean showHiddenFiles = PreferenceManager.showHiddenFilesEnabled(mContext);
        if (!showHiddenFiles) {
            mFiles = filterHiddenFiles(mFiles);
        }

        mFilesAll.clear();
        mFilesAll.addAll(mFiles);

        notifyDataSetChanged();
    }

    public void setSortOrder(FileSortOrder sortOrder) {
        PreferenceManager.setSortOrder(mContext, null, sortOrder);
        mFiles = sortOrder.sortLocalFiles(mFiles);
        notifyDataSetChanged();
    }

    private ArrayList<File> getFolders(final File directory) {
        File[] folders = directory.listFiles(File::isFile);

        if (folders != null && folders.length > 0) {
            return new ArrayList<>(Arrays.asList(folders));
        } else {
            return new ArrayList<>();
        }
    }

    private ArrayList<File> getFiles(final File directory) {
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
            ArrayList<File> result = new ArrayList<>();
            text = text.toLowerCase(Locale.getDefault());
            for (File file : mFilesAll) {
                if (file.getName().toLowerCase(Locale.getDefault()).contains(text)) {
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
     * @param files             ArrayList of files to filter
     * @return Non-hidden files
     */
    public ArrayList<File> filterHiddenFiles(ArrayList<File> files) {
        ArrayList<File> ret = new ArrayList<>();
        
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

    static class LocalFileListItemViewHolder extends LocalFileListGridViewHolder {
        private final TextView fileSize;
        private final TextView lastModification;
        private final TextView fileSeparator;

        private LocalFileListItemViewHolder(View itemView) {
            super(itemView);

            fileSize = itemView.findViewById(R.id.file_size);
            fileSeparator = itemView.findViewById(R.id.file_separator);
            lastModification = itemView.findViewById(R.id.last_mod);
        }
    }

    static class LocalFileListGridViewHolder extends RecyclerView.ViewHolder {
        private final ImageView thumbnail;
        private final TextView fileName;
        private final ImageView checkbox;
        private final LinearLayout itemLayout;

        private LocalFileListGridViewHolder(View itemView) {
            super(itemView);

            thumbnail = itemView.findViewById(R.id.thumbnail);
            fileName = itemView.findViewById(R.id.Filename);
            checkbox = itemView.findViewById(R.id.custom_checkbox);
            itemLayout = itemView.findViewById(R.id.ListItemLayout);

            itemView.findViewById(R.id.overflow_menu).setVisibility(View.GONE);
            itemView.findViewById(R.id.sharedIcon).setVisibility(View.GONE);
            itemView.findViewById(R.id.favorite_action).setVisibility(View.GONE);
            itemView.findViewById(R.id.keptOfflineIcon).setVisibility(View.GONE);
            itemView.findViewById(R.id.localFileIndicator).setVisibility(View.GONE);
        }
    }

    static class LocalFileListFooterViewHolder extends RecyclerView.ViewHolder {
        private final TextView footerText;

        private LocalFileListFooterViewHolder(View itemView) {
            super(itemView);

            footerText = itemView.findViewById(R.id.footerText);
        }
    }
}
