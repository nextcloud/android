/**
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
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.fragment.ExtendedListFragmentListener;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeTypeUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

/**
 * This Adapter populates a ListView with all files and directories contained
 * in a local directory
 */
public class LocalFileListAdapter extends BaseAdapter {

    private static final String TAG = LocalFileListAdapter.class.getSimpleName();

    private Context mContext;
    private File[] mFiles = null;
    private Vector<File> mFilesAll = new Vector<File>();
    private List<File> mFilteredFiles = new ArrayList<>();
    private File mInitialDirectory;
    private ExtendedListFragmentListener mFragmentListener;

    private LocalFilesFilter mFilesFilter;

    public LocalFileListAdapter(File directory, Context context,
                                ExtendedListFragmentListener extendedListFragmentListener) {
        mContext = context;
        mFragmentListener = extendedListFragmentListener;

        // Read sorting order, default to sort by name ascending
        FileStorageUtils.mSortOrder = PreferenceManager.getSortOrder(context);
        FileStorageUtils.mSortAscending = PreferenceManager.getSortAscending(context);

        swapDirectory(directory, null);
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
        return mFiles != null ? mFiles.length : 0;
    }

    @Override
    public Object getItem(int position) {
        if (mFiles == null || mFiles.length <= position) {
            return null;
        }
        return mFiles[position];
    }

    @Override
    public long getItemId(int position) {
        return mFiles != null && mFiles.length <= position ? position : -1;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        File file = null;
        boolean isGridView = true;
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (mFiles != null && mFiles.length > position && mFiles[position] != null) {
            file = mFiles[position];
        }

        if (file != null) {
            // Find out which layout should be displayed
            ViewType viewType;
            if (parent instanceof GridView) {
                String mimeType = MimeTypeUtil.getBestMimeTypeByFilename(file.getName());
                if (MimeTypeUtil.isImage(mimeType) || MimeTypeUtil.isVideo(mimeType)) {
                    viewType = ViewType.GRID_IMAGE;
                } else {
                    viewType = ViewType.GRID_ITEM;
                }
            } else {
                viewType = ViewType.LIST_ITEM;
                isGridView = false;
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

            if (!ViewType.GRID_IMAGE.equals(viewType)) {
                TextView fileName = (TextView) view.findViewById(R.id.Filename);
                String name = file.getName();
                fileName.setText(name);
            }

            ImageView fileIcon = (ImageView) view.findViewById(R.id.thumbnail);

            /** Cancellation needs do be checked and done before changing the drawable in fileIcon, or
             * {@link ThumbnailsCacheManager#cancelPotentialThumbnailWork} will NEVER cancel any task.
             **/
            boolean allowedToCreateNewThumbnail = (ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, fileIcon));

            if (!file.isDirectory()) {
                fileIcon.setImageResource(R.drawable.file);
            } else {
                fileIcon.setImageResource(R.drawable.ic_menu_archive);
            }
            fileIcon.setTag(file.hashCode());

            ImageView checkBoxV = (ImageView) view.findViewById(R.id.custom_checkbox);
            TextView fileSizeV = (TextView) view.findViewById(R.id.file_size);
            TextView fileSizeSeparatorV = (TextView) view.findViewById(R.id.file_separator);
            if (!isGridView) {
                TextView lastModV = (TextView) view.findViewById(R.id.last_mod);
                lastModV.setVisibility(View.VISIBLE);
                lastModV.setText(DisplayUtils.getRelativeTimestamp(mContext, file.lastModified()));
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

                // get Thumbnail if file is image
                if (MimeTypeUtil.isImage(file)) {
                    // Thumbnail in Cache?
                    Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                            String.valueOf(file.hashCode())
                    );
                    if (thumbnail != null) {
                        fileIcon.setImageBitmap(thumbnail);
                    } else {

                        // generate new Thumbnail
                        if (allowedToCreateNewThumbnail) {
                            final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                                    new ThumbnailsCacheManager.ThumbnailGenerationTask(fileIcon);
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
                            fileIcon.setImageDrawable(asyncDrawable);
                            task.execute(file);
                            Log_OC.v(TAG, "Executing task to generate a new thumbnail");

                        } // else, already being generated, don't restart it
                    }
                } else {
                    fileIcon.setImageResource(MimeTypeUtil.getFileTypeIconId(null, file.getName()));
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
            view.findViewById(R.id.favoriteIcon).setVisibility(View.GONE);

            view.findViewById(R.id.sharedIcon).setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return (mFiles == null || mFiles.length == 0);
    }

    /**
     * Change the adapted directory for a new one
     *
     * @param directory New file to adapt. Can be NULL, meaning "no content to adapt".
     */
    public void swapDirectory(final File directory, @Nullable String constraint) {
        mFiles = (directory != null ? directory.listFiles() : null);
        if (mInitialDirectory == null) {
            mInitialDirectory = directory;
        }

        if (mFiles != null) {
            Arrays.sort(mFiles, new Comparator<File>() {
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
                    return lhs.getName().toLowerCase().compareTo(rhs.getName().toLowerCase());
                }

            });

            mFiles = FileStorageUtils.sortLocalFolder(mFiles);

            // Fetch preferences for showing hidden files
            boolean showHiddenFiles = PreferenceManager.showHiddenFilesEnabled(mContext);
            if (!showHiddenFiles) {
                mFiles = filterHiddenFiles(mFiles);
            }

            mFilesAll.clear();

            Collections.addAll(mFilesAll, mFiles);
        }

        if (constraint != null) {
            getFilter().filter(constraint);
        } else {
            notifyDataSetChanged();
        }
    }

    public void setSortOrder(Integer order, boolean ascending) {
        PreferenceManager.setSortOrder(mContext, order);
        PreferenceManager.setSortAscending(mContext, ascending);

        FileStorageUtils.mSortOrder = order;
        FileStorageUtils.mSortAscending = ascending;

        mFiles = FileStorageUtils.sortLocalFolder(mFiles);
        notifyDataSetChanged();
    }

    /**
     * Filter for hidden files
     *
     * @param files Array of files to filter
     * @return Non-hidden files as an array
     */
    public File[] filterHiddenFiles(File[] files) {
        List<File> ret = new ArrayList<>();
        if (files.length > 0) {
            for (File file : files) {
                if (!file.isHidden()) {
                    if (!ret.contains(file)) {
                        ret.add(file);
                    }
                }
            }
        }

        return ret.toArray(new File[ret.size()]);
    }

    private void fetchFiles(File mWalkInDir, @Nullable String constraint) {


        boolean showHiddenFiles = PreferenceManager.showHiddenFilesEnabled(mContext);
        File[] listFiles = mWalkInDir.listFiles();
        if (!showHiddenFiles) {
            listFiles = filterHiddenFiles(listFiles);
        }

        for (File file : listFiles) {
            if (constraint != null && constraint.isEmpty() && file.getName().contains(constraint)) {
                mFilteredFiles.add(file);
            } else if (constraint == null || constraint.isEmpty()) {
                mFilteredFiles.add(file);
            }

            if (mFilteredFiles.size() >= 100) {
                return;
            }

            if (file.isDirectory()) {
                fetchFiles(file, constraint);
            }

        }

    }


    public Filter getFilter() {
        if (mFilesFilter == null) {
            mFilesFilter = new LocalFilesFilter();
        }
        return mFilesFilter;
    }

    private class LocalFilesFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            mFilteredFiles = new Vector<>();
            fetchFiles(mInitialDirectory, constraint.toString());

            results.values = mFilteredFiles;
            results.count = mFilteredFiles.size();
            mFilteredFiles = new Vector<>();

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            Vector<File> fileVector = (Vector<File>) results.values;
            boolean showHundredFilesMessage = results.count > 100;
            if (results.count > 100) {
                mFiles = new File[100];
            } else {
                mFiles = new File[results.count];
            }
            if (fileVector != null && fileVector.size() > 0) {
                if (results.count > 100) {
                    mFiles = fileVector.subList(0, 100).toArray(new File[1]);
                } else {
                    mFiles = fileVector.toArray(new File[1]);
                }
                mFiles = fileVector.toArray(new File[1]);
                mFiles = FileStorageUtils.sortLocalFolder(mFiles);
            }

            if (mFragmentListener != null) {
                mFragmentListener.showHundredFilesMessage(showHundredFilesMessage);
                mFragmentListener.endFilterRefresh();
            }

            notifyDataSetChanged();

        }
    }


}
