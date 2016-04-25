/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author Tobias Kaminsky
 * @author David A. Velasco
 * @author masensio
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2016 ownCloud Inc.
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.adapter;


import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.services.OperationsService.OperationsServiceBinder;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimetypeIconUtil;

import java.util.Vector;

/**
 * Changes made by Denis Dijak on 25.4.2016
 */

/**
 * This Adapter populates a ListView with all files and folders in an ownCloud
 * instance.
 */
public class FileListListAdapter extends RecyclerView.Adapter<RecyclerViewHolder> {

    private Context mContext;
    private OCFile mFile = null;
    private Vector<OCFile> mFiles = new Vector<>();
    private Vector<OCFile> mFilesOrig = new Vector<>();
    private boolean mJustFolders;

    private FileDataStorageManager mStorageManager;
    private OCFileListFragment mOCFileListFragment;

    private int viewLayout = 0;
    private Account mAccount;
    private FileFragment.ContainerActivity mContainerActivity;
    private SharedPreferences mAppPreferences;

    private static final int TYPE_LIST = 0;
    private static final int TYPE_GRID = 1;

    public FileListListAdapter(Context mContext, OCFileListFragment mOCFileListFragment, FileFragment.ContainerActivity mContainerActivity) {

        setHasStableIds(true);

        this.mContext = mContext;
        this.mOCFileListFragment = mOCFileListFragment;
        this.mContainerActivity = mContainerActivity;

        mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);
        mAppPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        // Read sorting order, default to sort by name ascending
        FileStorageUtils.mSortOrder = mAppPreferences.getInt("sortOrder", 0);
        FileStorageUtils.mSortAscending = mAppPreferences.getBoolean("sortAscending", true);

        // set view layout
        viewLayout = mAppPreferences.getInt("viewLayout", R.layout.list_item);

        // initialise thumbnails cache on background thread
        new ThumbnailsCacheManager.InitDiskCacheTask().execute();
    }

    public Object getItem(int position) {
        if (mFiles == null || mFiles.size() <= position)
            return null;
        if (position <= mFiles.size()) {
            return mFiles.get(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        if (mFiles == null || mFiles.size() <= position)
            return 0;
        return mFiles.get(position).getFileId();
    }

    @Override
    public int getItemCount() {
        return (null != mFiles && mFiles.size() > 0 ? mFiles.size() : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (viewLayout == R.layout.grid_item) {
            return TYPE_GRID;
        } else if (viewLayout == R.layout.list_item) {
            return TYPE_LIST;
        } else
            return super.getItemViewType(position);
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewLayout, parent, false);
        return new RecyclerViewHolder(view, this, mContainerActivity, mOCFileListFragment);
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position) {

        OCFile file = null;
        if (mFiles != null && mFiles.size() > position) {
            file = mFiles.get(position);
        }

        if (file != null /*&& getItemViewType(position) == TYPE_ITEM*/) {

            holder.fileName.setText(file.getFileName());
            if (holder.lastModV != null) {
                holder.lastModV.setVisibility(View.VISIBLE);
                holder.lastModV.setText(showRelativeTimestamp(file));
            }

            if (holder.checkBoxV != null) {
                holder.checkBoxV.setVisibility(View.GONE);
            }

            if (holder.fileSizeV != null) {
                holder.fileSizeV.setVisibility(View.VISIBLE);
                holder.fileSizeV.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));
            }

            // sharedIcon
            if (file.isSharedViaLink()) {
                holder.sharedIconV.setVisibility(View.VISIBLE);
            } else {
                holder.sharedIconV.setVisibility(View.GONE);
            }
            // share with me icon
            /*if (file.isSharedWithMe()) {
                holder.sharedWithMeIconV.setVisibility(View.VISIBLE);
            } else {
                holder.sharedWithMeIconV.setVisibility(View.GONE);
            }

            // share with others icon
            if (file.isSharedWithSharee()) {
                holder.sharedWithOthersIconV.setVisibility(View.VISIBLE);
            } else {
                holder.sharedWithOthersIconV.setVisibility(View.GONE);
            }*/

            // local state
            FileDownloaderBinder downloaderBinder = mContainerActivity.getFileDownloaderBinder();
            FileUploaderBinder uploaderBinder = mContainerActivity.getFileUploaderBinder();
            OperationsServiceBinder opsBinder = mContainerActivity.getOperationsServiceBinder();

            boolean downloading = (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, file));

            downloading |= (opsBinder != null && opsBinder.isSynchronizing(mAccount, file.getRemotePath()));
            if (downloading) {
                holder.localStateView.setImageResource(R.drawable.downloading_file_indicator);
                holder.localStateView.setVisibility(View.VISIBLE);
            } else if (uploaderBinder != null && uploaderBinder.isUploading(mAccount, file)) {
                holder.localStateView.setImageResource(R.drawable.uploading_file_indicator);
                holder.localStateView.setVisibility(View.VISIBLE);
            } else if (file.isDown()) {
                holder.localStateView.setImageResource(R.drawable.local_file_indicator);
                holder.localStateView.setVisibility(View.VISIBLE);
            } else {
                holder.localStateView.setVisibility(View.GONE);
            }

            // this if-else is needed even though favorite icon is visible by default
            // because android reuses views in listview
            if (!file.isFavorite()) {
                holder.favoriteIcon.setVisibility(View.GONE);
            } else {
                holder.favoriteIcon.setVisibility(View.VISIBLE);
            }

            // Icons and thumbnail utils
            // TODO : image processing, this has to be fixed !!!!

            // No Folder
            if (!file.isFolder()) {
                if (file.isImage() && file.getRemoteId() != null) {
                    // Thumbnail in Cache?
                    Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                            String.valueOf(file.getRemoteId())
                    );
                    if (thumbnail != null && !file.needsUpdateThumbnail()) {
                        holder.fileIcon.setImageBitmap(thumbnail);
                    } else {
                        // generate new Thumbnail
                        if (ThumbnailsCacheManager.cancelPotentialWork(file, holder.fileIcon)) {
                            final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                                    new ThumbnailsCacheManager.ThumbnailGenerationTask(
                                            holder.fileIcon, mStorageManager, mAccount
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
                            holder.fileIcon.setImageDrawable(asyncDrawable);
                            task.execute(file);
                        }
                    }

                    if (file.getMimetype().equalsIgnoreCase("image/png")) {
                        holder.fileIcon.setBackgroundColor(mContext.getResources()
                                .getColor(R.color.background_color));
                    }
                } else {
                    holder.fileIcon.setImageResource(MimetypeIconUtil.getFileTypeIconId(file.getMimetype(),
                            file.getFileName()));
                }
            } else {
                // Folder
                holder.fileIcon.setImageResource(
                        MimetypeIconUtil.getFolderTypeIconId(
                                file.isSharedWithMe() || file.isSharedWithSharee(),
                                file.isSharedViaLink()
                        )
                );
            }
        }

    }

    /**
     * Return list of current files
     *
     * @return Vector of OCFiles
     */
    public Vector<OCFile> getCurrentFiles() {
        return mFiles;
    }

    /**
     * Change the adapted directory for a new one
     * @param directory                 New file to adapt. Can be NULL, meaning 
     *                                  "no content to adapt".
     * @param updatedStorageManager     Optional updated storage manager; used to replace 
     *                                  mStorageManager if is different (and not NULL)
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
        SharedPreferences.Editor editor = mAppPreferences.edit();
        editor.putInt("sortOrder", order);
        editor.putBoolean("sortAscending", ascending);
        editor.commit();

        FileStorageUtils.mSortOrder = order;
        FileStorageUtils.mSortAscending = ascending;


        mFiles = FileStorageUtils.sortFolder(mFiles);
        notifyDataSetChanged();

    }

    private CharSequence showRelativeTimestamp(OCFile file) {
        return DisplayUtils.getRelativeDateTimeString(mContext, file.getModificationTimestamp(),
                DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
    }
}
