/**
 *   Nextcloud Android client application
 *
 *   @author Andy Scherzinger
 *   Copyright (C) 2016 Andy Scherzinger
 *   Copyright (C) 2016 Nextcloud
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 *   License as published by the Free Software Foundation; either
 *   version 3 of the License, or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.SyncedFolderItem;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.MimetypeIconUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter to display all auto-synced folders and/or instant upload media folders.
 */
public class FolderSyncAdapter extends SectionedRecyclerViewAdapter<FolderSyncAdapter.MainViewHolder> {

    private static final String TAG = FolderSyncAdapter.class.getSimpleName();

    private final Context mContext;
    private final int mGridWidth;
    private final ClickListener mListener;
    private final List<SyncedFolderItem> mSyncFolderItems;
    private final RecyclerView mRecyclerView;

    public FolderSyncAdapter(Context context, int gridWidth, ClickListener listener, RecyclerView recyclerView) {
        mContext = context;
        mGridWidth = gridWidth * 2;
        mListener = listener;
        mSyncFolderItems = new ArrayList<>();
        mRecyclerView = recyclerView;
    }

    public void setSyncFolderItems(List<SyncedFolderItem> syncFolderItems) {
        mSyncFolderItems.clear();
        mSyncFolderItems.addAll(syncFolderItems);
        notifyDataSetChanged();
    }

    @Override
    public int getSectionCount() {
        return mSyncFolderItems.size();
    }

    @Override
    public int getItemCount(int section) {
        return mSyncFolderItems.get(section).getFilePaths().size();
    }

    @Override
    public void onBindHeaderViewHolder(final MainViewHolder holder, final int section) {
        holder.title.setText(mSyncFolderItems.get(section).getFolderName());
        holder.syncStatusButton.setVisibility(View.VISIBLE);
        holder.syncStatusButton.setTag(section);
        holder.syncStatusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSyncFolderItems.get(section).setEnabled(!mSyncFolderItems.get(section).isEnabled());
                setSyncButtonActiveIcon(holder.syncStatusButton, mSyncFolderItems.get(section).isEnabled());
                mListener.onSyncStatusToggleClick(section, mSyncFolderItems.get(section));
            }
        });
        setSyncButtonActiveIcon(holder.syncStatusButton, mSyncFolderItems.get(section).isEnabled());

        holder.menuButton.setVisibility(View.VISIBLE);
        holder.menuButton.setTag(section);
        holder.menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onSyncFolderSettingsClick(section, mSyncFolderItems.get(section));
            }
        });
    }

    @Override
    public void onBindViewHolder(MainViewHolder holder, int section, int relativePosition, int absolutePosition) {
        final Context c = holder.itemView.getContext();

        File file = new File(mSyncFolderItems.get(section).getFilePaths().get(relativePosition));

        /** Cancellation needs do be checked and done before changing the drawable in fileIcon, or
         * {@link ThumbnailsCacheManager#cancelPotentialThumbnailWork} will NEVER cancel any task.
         **/
        boolean allowedToCreateNewThumbnail = (ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, holder.image));

        if (!file.isDirectory()) {
            holder.image.setImageResource(R.drawable.file);
        } else {
            holder.image.setImageResource(R.drawable.ic_menu_archive);
        }
        // set proper tag
        holder.image.setTag(file.hashCode());

        // get Thumbnail if file is image
        if (BitmapUtils.isImage(file)) {
            // Thumbnail in Cache?
            Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    String.valueOf(file.hashCode())
            );
            if (thumbnail != null) {
                holder.image.setImageBitmap(thumbnail);
            } else {

                // generate new Thumbnail
                if (allowedToCreateNewThumbnail) {
                    final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                            new ThumbnailsCacheManager.ThumbnailGenerationTask(holder.image);
                    if (thumbnail == null) {
                        if (BitmapUtils.isVideo(file)) {
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
                    holder.image.setImageDrawable(asyncDrawable);
                    task.execute(file);
                    Log_OC.v(TAG, "Executing task to generate a new thumbnail");

                } // else, already being generated, don't restart it
            }
        } else {
            holder.image.setImageResource(MimetypeIconUtil.getFileTypeIconId(null, file.getName()));
        }

        holder.itemView.setTag(relativePosition % (mGridWidth/2));

        if (mSyncFolderItems.get(section).getNumberOfFiles() > 8 && relativePosition >= 8 - 1) {
            holder.counterValue.setText(Long.toString(mSyncFolderItems.get(section).getNumberOfFiles() - 8));
            holder.counterBar.setVisibility(View.VISIBLE);
            holder.thumbnailDarkener.setVisibility(View.VISIBLE);
        } else {
            holder.counterBar.setVisibility(View.GONE);
            holder.thumbnailDarkener.setVisibility(View.GONE);
        }

        //holder.itemView.setTag(String.format(Locale.getDefault(), "%d:%d:%d", section, relativePos, absolutePos));
        //holder.itemView.setOnClickListener(this);
    }

    @Override
    public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(
                viewType == VIEW_TYPE_HEADER ?
                        R.layout.folder_sync_item_header : R.layout.grid_sync_item, parent, false);
        return new MainViewHolder(v);
    }

    public interface ClickListener {
        void onSyncStatusToggleClick(int section, SyncedFolderItem syncedFolderItem);
        void onSyncFolderSettingsClick(int section, SyncedFolderItem syncedFolderItem);
    }

    static class MainViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView title;
        final ImageButton menuButton;
        final ImageButton syncStatusButton;
        final LinearLayout counterBar;
        final TextView counterValue;
        final ImageView thumbnailDarkener;

        public MainViewHolder(View itemView) {
            super(itemView);
            image = (ImageView) itemView.findViewById(R.id.thumbnail);
            title = (TextView) itemView.findViewById(R.id.title);
            menuButton = (ImageButton) itemView.findViewById(R.id.settingsButton);
            syncStatusButton = (ImageButton) itemView.findViewById(R.id.syncStatusButton);
            counterBar = (LinearLayout) itemView.findViewById(R.id.counterLayout);
            counterValue = (TextView) itemView.findViewById(R.id.counter);
            thumbnailDarkener = (ImageView) itemView.findViewById(R.id.thumbnailDarkener);
        }
    }

    private void setSyncButtonActiveIcon(ImageButton syncStatusButton, boolean enabled) {
        if(enabled) {
            syncStatusButton.setImageResource(R.drawable.ic_cloud_sync_on);
        } else {
            syncStatusButton.setImageResource(R.drawable.ic_cloud_sync_off);
        }
    }
}
