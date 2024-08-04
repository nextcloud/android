/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nextcloud.client.account.User;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ListFooterBinding;
import com.owncloud.android.databinding.TrashbinItemBinding;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile;
import com.owncloud.android.ui.interfaces.TrashbinActivityInterface;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
    private final User user;
    private final FileDataStorageManager storageManager;
    private final AppPreferences preferences;
    private final List<ThumbnailsCacheManager.ThumbnailGenerationTask> asyncTasks = new ArrayList<>();
    private final ViewThemeUtils viewThemeUtils;

    public TrashbinListAdapter(
        TrashbinActivityInterface trashbinActivityInterface,
        FileDataStorageManager storageManager,
        AppPreferences preferences,
        Context context,
        User user,
        ViewThemeUtils viewThemeUtils
                              ) {
        this.files = new ArrayList<>();
        this.trashbinActivityInterface = trashbinActivityInterface;
        this.user = user;
        this.storageManager = storageManager;
        this.preferences = preferences;
        this.context = context;
        this.viewThemeUtils = viewThemeUtils;
    }

    public void setTrashbinFiles(List<TrashbinFile> trashbinFiles, boolean clear) {
        if (clear) {
            files.clear();
        }

        files.addAll(trashbinFiles);

        files = preferences.getSortOrderByType(FileSortOrder.Type.trashBinView,
                                               FileSortOrder.SORT_NEW_TO_OLD).sortTrashbinFiles(files);

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TRASHBIN_ITEM) {
            return new TrashbinFileViewHolder(
                TrashbinItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
            );
        } else {
            return new TrashbinFooterViewHolder(
                ListFooterBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof TrashbinFileViewHolder) {
            final TrashbinFileViewHolder trashbinFileViewHolder = (TrashbinFileViewHolder) holder;
            TrashbinFile file = files.get(position);

            // layout
            trashbinFileViewHolder.binding.ListItemLayout.setOnClickListener(v -> trashbinActivityInterface.onItemClicked(file));

            // thumbnail
            trashbinFileViewHolder.binding.thumbnail.setTag(file.getRemoteId());
            setThumbnail(file, trashbinFileViewHolder.binding.thumbnail);

            // fileName
            trashbinFileViewHolder.binding.Filename.setText(file.getFileName());

            // fileSize
            trashbinFileViewHolder.binding.fileSize.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));

            // originalLocation
            String location;
            int lastIndex = file.getOriginalLocation().lastIndexOf('/');
            if (lastIndex != -1) {
                location = ROOT_PATH + file.getOriginalLocation().substring(0, lastIndex) + PATH_SEPARATOR;
            } else {
                location = ROOT_PATH;
            }
            trashbinFileViewHolder.binding.originalLocation.setText(location);

            // deletion time
            trashbinFileViewHolder.binding.deletionTimestamp.setText(DisplayUtils.getRelativeTimestamp(context,
                    file.getDeletionTimestamp() * 1000));

            // checkbox
            trashbinFileViewHolder.binding.customCheckbox.setVisibility(View.GONE);

            // overflow menu
            trashbinFileViewHolder.binding.overflowMenu.setOnClickListener(v ->
                    trashbinActivityInterface.onOverflowIconClicked(file, v));

            // restore button
            trashbinFileViewHolder.binding.restore.setOnClickListener(v ->
                    trashbinActivityInterface.onRestoreIconClicked(file, v));

        } else {
            TrashbinFooterViewHolder trashbinFooterViewHolder = (TrashbinFooterViewHolder) holder;
            trashbinFooterViewHolder.binding.footerText.setText(getFooterText());
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void removeFile(TrashbinFile file) {
        int index = files.indexOf(file);

        if (index != -1) {
            files.remove(index);
            notifyDataSetChanged(); // needs to be used to also update footer
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
            thumbnailView.setImageDrawable(MimeTypeUtil.getDefaultFolderIcon(context, viewThemeUtils));
        } else {
            if ((MimeTypeUtil.isImage(file) || MimeTypeUtil.isVideo(file)) && file.getRemoteId() != null) {
                // Thumbnail in cache?
                Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                        ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.getRemoteId()
                );

                if (thumbnail != null) {
                    if (MimeTypeUtil.isVideo(file)) {
                        Bitmap withOverlay = ThumbnailsCacheManager.addVideoOverlay(thumbnail, context);
                        thumbnailView.setImageBitmap(withOverlay);
                    } else {
                        thumbnailView.setImageBitmap(thumbnail);
                    }
                } else {
                    thumbnailView.setImageDrawable(MimeTypeUtil.getFileTypeIcon(file.getMimeType(),
                                                                                file.getFileName(),
                                                                                context,
                                                                                viewThemeUtils));

                    // generate new thumbnail
                    if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, thumbnailView)) {
                        try {
                            final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                                new ThumbnailsCacheManager.ThumbnailGenerationTask(thumbnailView,
                                                                                   storageManager,
                                                                                   user,
                                                                                   asyncTasks);

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
                thumbnailView.setImageDrawable(MimeTypeUtil.getFileTypeIcon(file.getMimeType(),
                                                                            file.getFileName(),
                                                                            context,
                                                                            viewThemeUtils));
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
        protected TrashbinItemBinding binding;

        private TrashbinFileViewHolder(TrashbinItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            // todo action mode
        }
    }

    public class TrashbinFooterViewHolder extends RecyclerView.ViewHolder {
        protected ListFooterBinding binding;

        private TrashbinFooterViewHolder(ListFooterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
