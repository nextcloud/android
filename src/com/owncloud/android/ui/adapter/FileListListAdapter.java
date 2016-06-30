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


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Vector;

import android.accounts.Account;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
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
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimetypeIconUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Vector;


/**
 * This Adapter populates a ListView with all files and folders in an ownCloud
 * instance.
 */
public class FileListListAdapter extends BaseAdapter implements ListAdapter {

    private static final String SELECTION_KEY = "multiFileSelectionsKey";

    private Context mContext;
    private OCFile mFile = null;
    private Vector<OCFile> mFiles = null;
    private Vector<OCFile> mFilesOrig = new Vector<OCFile>();
    private boolean mJustFolders;

    private FileDataStorageManager mStorageManager;
    private Account mAccount;
    private ComponentsGetter mTransferServiceGetter;
    private boolean mGridMode;
    private boolean isGridViewSelectionRestored = true;

    private enum ViewType {LIST_ITEM, GRID_IMAGE, GRID_ITEM};

    private HashSet<Long> mSelection = new LinkedHashSet<Long>();

    public FileListListAdapter(
            boolean justFolders,
            Context context,
            ComponentsGetter transferServiceGetter
    ) {

        mJustFolders = justFolders;
        mContext = context;
        mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);

        mTransferServiceGetter = transferServiceGetter;

        // Read sorting order, default to sort by name ascending
        FileStorageUtils.mSortOrder = PreferenceManager.getSortOrder(mContext);
        FileStorageUtils.mSortAscending = PreferenceManager.getSortAscending(mContext);
        
        // initialise thumbnails cache on background thread
        new ThumbnailsCacheManager.InitDiskCacheTask().execute();

        mGridMode = false;
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
        restoreGridViewSelection((AbsListView) parent);

        View view = convertView;
        OCFile file = null;
        LayoutInflater inflator = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (mFiles != null && mFiles.size() > position) {
            file = mFiles.get(position);
        }

        // Find out which layout should be displayed
        ViewType viewType;
        if (!mGridMode) {
            viewType = ViewType.LIST_ITEM;
        } else if (file.isImage()) {
            viewType = ViewType.GRID_IMAGE;
        } else {
            viewType = ViewType.GRID_ITEM;
        }

        // create view only if differs, otherwise reuse
        if (convertView == null || (convertView != null && convertView.getTag() != viewType)) {
            switch (viewType) {
                case GRID_IMAGE:
                    view = inflator.inflate(R.layout.grid_image, null);
                    view.setTag(ViewType.GRID_IMAGE);
                    break;
                case GRID_ITEM:
                    view = inflator.inflate(R.layout.grid_item, null);
                    view.setTag(ViewType.GRID_ITEM);
                    break;
                case LIST_ITEM:
                    view = inflator.inflate(R.layout.list_item, null);
                    view.setTag(ViewType.LIST_ITEM);
                    break;
            }
        }

        view.invalidate();

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

                    if (file.isFolder()) {
                        fileSizeSeparatorV.setVisibility(View.GONE);
                        fileSizeV.setVisibility(View.GONE);
                    }

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

                    /*ImageView sharedWithMeIcon = (ImageView) view.findViewById(R.id.sharedWithMeIcon);
                    sharedWithMeIcon.bringToFront();*/

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

            ImageView checkBoxV = (ImageView) view.findViewById(R.id.custom_checkbox);
            checkBoxV.setVisibility(View.GONE);
            view.setBackgroundColor(Color.WHITE);

            AbsListView parentList = (AbsListView) parent;
            if (parentList.getChoiceMode() != AbsListView.CHOICE_MODE_NONE
                    && parentList.getCheckedItemCount() > 0) {
                if (isItemSelected(position)) {
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

            // For all Views

            // this if-else is needed even though favorite icon is visible by default
            // because android reuses views in listview
            if (!file.isFavorite()) {
                view.findViewById(R.id.favoriteIcon).setVisibility(View.GONE);
            } else {
                view.findViewById(R.id.favoriteIcon).setVisibility(View.VISIBLE);
            }

            // No Folder
            if (!file.isFolder()) {
                if (file.isImage() && file.getRemoteId() != null) {
                    // Thumbnail in Cache?
                    Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                            String.valueOf(file.getRemoteId())
                    );
                    if (thumbnail != null && !file.needsUpdateThumbnail()) {
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
                            final ThumbnailsCacheManager.AsyncDrawable asyncDrawable =
                                    new ThumbnailsCacheManager.AsyncDrawable(
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
                    fileIcon.setImageResource(MimetypeIconUtil.getFileTypeIconId(file.getMimetype(),
                            file.getFileName()));
                }


            } else {
                // Folder
                fileIcon.setImageResource(
                        MimetypeIconUtil.getFolderTypeIconId(
                                file.isSharedWithMe() || file.isSharedWithSharee(),
                                file.isSharedViaLink()
                        )
                );
            }
        }
        return view;
    }

    private void restoreGridViewSelection(AbsListView parent) {
        if (parent instanceof GridView && !isGridViewSelectionRestored) {
            isGridViewSelectionRestored = true;
            parent.clearChoices();
            final Vector<OCFile> files = mFiles;
            for (int i = 0; i < files.size(); ++i) {
                if(mSelection.contains(files.get(i).getFileId())){
                    parent.setItemChecked(i, true);
                }
            }
        }
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
     * @param directory             New file to adapt. Can be NULL, meaning
     *                              "no content to adapt".
     * @param updatedStorageManager Optional updated storage manager; used to replace
     *                              mStorageManager if is different (and not NULL)
     */
    public void swapDirectory(OCFile directory, FileDataStorageManager updatedStorageManager
            /*, boolean onlyOnDevice*/) {
        mFile = directory;
        if (updatedStorageManager != null && updatedStorageManager != mStorageManager) {
            mStorageManager = updatedStorageManager;
            mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);
        }
        if (mStorageManager != null) {
            // TODO Enable when "On Device" is recovered ?
            mFiles = mStorageManager.getFolderContent(mFile/*, onlyOnDevice*/);
            mFilesOrig.clear();
            mFilesOrig.addAll(mFiles);

            if (mJustFolders) {
                mFiles = getFolders(mFiles);
            }
        } else {
            mFiles = null;
        }

        mFiles = FileStorageUtils.sortFolder(mFiles);
        notifyDataSetChanged();
    }

    /**
     * Filter for getting only the folders
     *
     * @param files
     * @return Vector<OCFile>
     */
    public Vector<OCFile> getFolders(Vector<OCFile> files) {
        Vector<OCFile> ret = new Vector<OCFile>();
        OCFile current = null;
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

        mFiles = FileStorageUtils.sortFolder(mFiles);
        notifyDataSetChanged();
    }


    private CharSequence showRelativeTimestamp(OCFile file) {
        return DisplayUtils.getRelativeDateTimeString(mContext, file.getModificationTimestamp(),
                DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
    }

    public void setGridMode(boolean gridMode) {
        mGridMode = gridMode;
    }

    public boolean isItemSelected(int position) {
        return mSelection.contains(getItemId(position));
    }

    public void setNewSelection(int position, boolean checked) {
        if (checked) {
            mSelection.add(getItemId(position));
            notifyDataSetChanged();
        } else {
            removeSelection(position);
        }
    }

    public void removeSelection(int position) {
        mSelection.remove(getItemId(position));
        notifyDataSetChanged();
    }

    public void clearSelection() {
        mSelection.clear();
        notifyDataSetChanged();
    }

    public ArrayList<OCFile> getCheckedItems() {
        ArrayList<OCFile> files = new ArrayList<OCFile>();
        if (mFiles != null && mFiles.size() != 0) {
            for (OCFile file : mFiles) {
                if (mSelection.contains(file.getFileId())) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    public void restoreSelectionState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        long[] selectionState = savedInstanceState.getLongArray(SELECTION_KEY);
        mSelection.clear();
        if (selectionState != null) {
            for (long id : selectionState) {
                mSelection.add(id);
            }
        }
        isGridViewSelectionRestored = false;
    }

    public void saveSelectionState(Bundle outState) {
        long[] selectionStatePrimitive = new long[mSelection.size()];
        int i = 0;
        for (Long id : mSelection) {
            selectionStatePrimitive[i++] = id;
        }
        outState.putLongArray(SELECTION_KEY, selectionStatePrimitive);
    }

    public boolean isGridMode() {
        return mGridMode;
    }
}
