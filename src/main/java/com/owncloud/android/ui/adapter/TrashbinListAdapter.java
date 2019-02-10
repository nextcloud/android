/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.adapter;

import android.accounts.Account;
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
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile;
import com.owncloud.android.ui.interfaces.TrashbinActivityInterface;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.MimeTypeUtil;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

import static com.owncloud.android.datamodel.OCFile.PATH_SEPARATOR;
import static com.owncloud.android.datamodel.OCFile.ROOT_PATH;

/**
 * Adapter for the trashbin view
 */
public class TrashbinListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TRASHBIN_ITEM = 100;
    private static final int TRASHBIN_FOOTER = 101;
    private static final String TAG = TrashbinListAdapter.class.getSimpleName();

    private final TrashbinActivityInterface trashbinActivityInterface;
    private List<TrashbinFile> files;
    private final Context context;
    private final Account account;
    private final FileDataStorageManager storageManager;
    private final AppPreferences preferences;

    private final List<ThumbnailsCacheManager.ThumbnailGenerationTask> asyncTasks = new ArrayList<>();

    public TrashbinListAdapter(
        TrashbinActivityInterface trashbinActivityInterface,
        FileDataStorageManager storageManager,
        AppPreferences preferences,
        Context context,
        Account account
    ) {
        this.files = new ArrayList<>();
        this.trashbinActivityInterface = trashbinActivityInterface;
        this.account = account;
        this.storageManager = storageManager;
        this.preferences = preferences;
        this.context = context;
    }

    public void setTrashbinFiles(List<Object> trashbinFiles, boolean clear) {
        if (clear) {
            files.clear();
        }

        for (Object file : trashbinFiles) {
            files.add((TrashbinFile) file);
        }

        files = preferences.getSortOrderByType(FileSortOrder.Type.trashBinView,
            FileSortOrder.sort_new_to_old).sortTrashbinFiles(files);

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TRASHBIN_ITEM) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.trashbin_item, parent, false);
            return new TrashbinFileViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_footer, parent, false);
            return new TrashbinFooterViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof TrashbinFileViewHolder) {
            final TrashbinFileViewHolder trashbinFileViewHolder = (TrashbinFileViewHolder) holder;
            TrashbinFile file = files.get(position);

            // layout
            trashbinFileViewHolder.itemLayout.setOnClickListener(v -> trashbinActivityInterface.onItemClicked(file));

            // thumbnail
            trashbinFileViewHolder.thumbnail.setTag(file.getRemoteId());
            setThumbnail(file, trashbinFileViewHolder.thumbnail);

            // fileName
            trashbinFileViewHolder.fileName.setText(file.getFileName());

            // fileSize
            trashbinFileViewHolder.fileSize.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));

            // originalLocation
            String location;
            int lastIndex = file.getOriginalLocation().lastIndexOf('/');
            if (lastIndex != -1) {
                location = ROOT_PATH + file.getOriginalLocation().substring(0, lastIndex) + PATH_SEPARATOR;
            } else {
                location = ROOT_PATH;
            }
            trashbinFileViewHolder.originalLocation.setText(location);

            // deletion time
            trashbinFileViewHolder.deletionTimestamp.setText(DisplayUtils.getRelativeTimestamp(context,
                    file.getDeletionTimestamp() * 1000));

            // checkbox
            trashbinFileViewHolder.checkbox.setVisibility(View.GONE);

            // overflow menu
            trashbinFileViewHolder.overflowMenu.setOnClickListener(v ->
                    trashbinActivityInterface.onOverflowIconClicked(file, v));

            // restore button
            trashbinFileViewHolder.restoreButton.setOnClickListener(v ->
                    trashbinActivityInterface.onRestoreIconClicked(file, v));

        } else {
            TrashbinFooterViewHolder trashbinFooterViewHolder = (TrashbinFooterViewHolder) holder;
            trashbinFooterViewHolder.title.setText(getFooterText());
        }
    }

    public void removeFile(TrashbinFile file) {
        int index = files.indexOf(file);

        if (index != -1) {
            files.remove(index);
            notifyItemRemoved(index);
        }
    }

    public void removeAllFiles() {
        files.clear();
        notifyDataSetChanged();
    }

    private String getFooterText() {
        int filesCount = 0;
        int foldersCount = 0;
        int count = files.size();
        TrashbinFile file;
        for (int i = 0; i < count; i++) {
            file = files.get(i);
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
        Resources resources = context.getResources();

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

    private void setThumbnail(TrashbinFile file, ImageView thumbnailView) {
        if (file.isFolder()) {
            thumbnailView.setImageDrawable(MimeTypeUtil.getDefaultFolderIcon(context));
        } else {
            if ((MimeTypeUtil.isImage(file) || MimeTypeUtil.isVideo(file)) && file.getRemoteId() != null) {
                // Thumbnail in cache?
                Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                        ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.getRemoteId()
                );

                if (thumbnail != null) {
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
                                    new ThumbnailsCacheManager.ThumbnailGenerationTask(thumbnailView, storageManager,
                                            account, asyncTasks);

                            final ThumbnailsCacheManager.AsyncThumbnailDrawable asyncDrawable =
                                    new ThumbnailsCacheManager.AsyncThumbnailDrawable(context.getResources(),
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
                    thumbnailView.setBackgroundColor(context.getResources().getColor(R.color.bg_default));
                }
            } else {
                thumbnailView.setImageDrawable(MimeTypeUtil.getFileTypeIcon(file.getMimeType(), file.getFileName(),
                        account, context));
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == files.size()) {
            return TRASHBIN_FOOTER;
        } else {
            return TRASHBIN_ITEM;
        }
    }

    @Override
    public int getItemCount() {
        return files.size() + 1;
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

    public void setSortOrder(FileSortOrder sortOrder) {
        preferences.setSortOrder(FileSortOrder.Type.trashBinView, sortOrder);
        files = sortOrder.sortTrashbinFiles(files);
        notifyDataSetChanged();
    }

    public class TrashbinFileViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.thumbnail)
        public ImageView thumbnail;
        @BindView(R.id.Filename)
        public TextView fileName;
        @BindView(R.id.fileSize)
        public TextView fileSize;
        @BindView(R.id.deletionTimestamp)
        public TextView deletionTimestamp;
        @BindView(R.id.originalLocation)
        public TextView originalLocation;
        @BindView(R.id.restore)
        public ImageView restoreButton;
        @BindView(R.id.customCheckbox)
        public ImageView checkbox;
        @BindView(R.id.overflowMenu)
        public ImageView overflowMenu;
        @BindView(R.id.ListItemLayout)
        public LinearLayout itemLayout;

        private TrashbinFileViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            // todo action mode
        }
    }

    public class TrashbinFooterViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.footerText)
        public TextView title;

        private TrashbinFooterViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
