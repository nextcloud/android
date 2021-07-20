/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Chris Narkiewicz <hello@ezaquarii.com>
 *
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Filter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.elyeproj.loaderviewlibrary.LoaderImageView;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datamodel.VirtualFolderType;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.shares.ShareeUser;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.operations.RemoteOperationFailedException;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.ui.TextDrawable;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.decoration.MediaGridItemDecoration;
import com.owncloud.android.ui.fragment.ExtendedListFragment;
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface;
import com.owncloud.android.ui.preview.PreviewTextFragment;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.ThemeAvatarUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeDrawableUtils;
import com.owncloud.android.utils.theme.ThemeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * This Adapter populates a RecyclerView with all files and folders in a Nextcloud instance.
 */
public class OCFileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    implements DisplayUtils.AvatarGenerationListener {

    private static final int showFilenameColumnThreshold = 4;
    private final ComponentsGetter transferServiceGetter;
    private final String userId;
    private Activity activity;
    private AppPreferences preferences;
    private UserAccountManager accountManager;
    private List<OCFile> mFiles = new ArrayList<>();
    private List<OCFile> mFilesAll = new ArrayList<>();
    private boolean hideItemOptions;
    private long lastTimestamp;
    private boolean gridView;
    private boolean multiSelect;
    private Set<OCFile> checkedFiles;

    private FileDataStorageManager mStorageManager;
    private User user;
    private OCFileListFragmentInterface ocFileListFragmentInterface;

    private FilesFilter mFilesFilter;
    private OCFile currentDirectory;
    private static final String TAG = OCFileListAdapter.class.getSimpleName();

    private static final int VIEWTYPE_FOOTER = 0;
    private static final int VIEWTYPE_ITEM = 1;
    private static final int VIEWTYPE_IMAGE = 2;
    private static final int VIEWTYPE_HEADER = 3;

    private List<ThumbnailsCacheManager.ThumbnailGenerationTask> asyncTasks = new ArrayList<>();
    private boolean onlyOnDevice;
    private boolean showShareAvatar = false;
    private OCFile highlightedItem;

    private boolean isMediaGallery = false;//flag to check user is on media gallery

    public OCFileListAdapter(
        Activity activity,
        User user,
        AppPreferences preferences,
        UserAccountManager accountManager,
        ComponentsGetter transferServiceGetter,
        OCFileListFragmentInterface ocFileListFragmentInterface,
        boolean argHideItemOptions,
        boolean gridView
                            ) {
        this.ocFileListFragmentInterface = ocFileListFragmentInterface;
        this.activity = activity;
        this.preferences = preferences;
        this.accountManager = accountManager;
        this.user = user;
        hideItemOptions = argHideItemOptions;
        this.gridView = gridView;
        checkedFiles = new HashSet<>();

        this.transferServiceGetter = transferServiceGetter;

        if (this.user != null) {
            AccountManager platformAccountManager = AccountManager.get(this.activity);
            userId = platformAccountManager.getUserData(this.user.toPlatformAccount(),
                                                        com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID);
        } else {
            userId = "";
        }

        // initialise thumbnails cache on background thread
        new ThumbnailsCacheManager.InitDiskCacheTask().execute();
    }

    public boolean isMultiSelect() {
        return multiSelect;
    }

    public void setMultiSelect(boolean bool) {
        multiSelect = bool;
        notifyDataSetChanged();
    }

    public void setMediaGallery(boolean mediaGallery) {
        isMediaGallery = mediaGallery;
    }

    public boolean isMediaGallery() {
        return isMediaGallery;
    }

    public boolean isCheckedFile(OCFile file) {
        return checkedFiles.contains(file);
    }

    public void removeCheckedFile(OCFile file) {
        checkedFiles.remove(file);
    }

    public void addCheckedFile(OCFile file) {
        checkedFiles.add(file);
        highlightedItem = null;
    }

    public void addAllFilesToCheckedFiles() {
        checkedFiles.addAll(mFiles);
    }

    public void removeAllFilesFromCheckedFiles() {
        checkedFiles.clear();
    }

    public int getItemPosition(OCFile file) {
        int position = mFiles.indexOf(file);

        if (shouldShowHeader()) {
            position = position + 1;
        }

        return position;
    }

    public void setFavoriteAttributeForItemID(String fileId, boolean favorite) {
        for (OCFile file : mFiles) {
            if (file.getRemoteId().equals(fileId)) {
                file.setFavorite(favorite);
                break;
            }
        }

        for (OCFile file : mFilesAll) {
            if (file.getRemoteId().equals(fileId)) {
                file.setFavorite(favorite);
                break;
            }
        }

        FileSortOrder sortOrder = preferences.getSortOrderByFolder(currentDirectory);
        mFiles = sortOrder.sortCloudFiles(mFiles);

        new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
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
        int filesSize = mFiles.size();
        for (int i = 0; i < filesSize; i++) {
            if (mFiles.get(i).getRemoteId().equals(fileId)) {
                OCFile file = mFiles.get(i);
                file.setEncrypted(encrypted);
                mStorageManager.saveFile(file);

                break;
            }
        }

        filesSize = mFilesAll.size();
        for (int i = 0; i < filesSize; i++) {
            if (mFilesAll.get(i).getRemoteId().equals(fileId)) {
                mFilesAll.get(i).setEncrypted(encrypted);
                break;
            }
        }

        new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
    }

    @Override
    public long getItemId(int position) {
        if (mFiles == null || mFiles.size() <= position) {
            return 0;
        }
        return mFiles.get(position).getFileId();
    }

    @Override
    public int getItemCount() {
        if (shouldShowHeader()) {
            return mFiles.size() + 2; // for header and footer
        } else {
            return mFiles.size() + 1; // for footer
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            default:
            case VIEWTYPE_ITEM:
                if (gridView) {
                    View itemView = LayoutInflater.from(activity).inflate(R.layout.grid_item, parent, false);
                    return new OCFileListGridItemViewHolder(itemView);
                } else {
                    View itemView = LayoutInflater.from(activity).inflate(R.layout.list_item, parent, false);
                    return new OCFileListItemViewHolder(itemView);
                }

            case VIEWTYPE_IMAGE:
                if (gridView) {
                    if (isMediaGallery) {
                        View itemView = LayoutInflater.from(activity).inflate(R.layout.grid_image, parent, false);
                        return new OCFileListGridImageViewHolder(itemView);
                    } else {
                        View itemView = LayoutInflater.from(activity).inflate(R.layout.grid_item, parent, false);
                        return new OCFileListGridItemViewHolder(itemView);
                    }
                } else {
                    View itemView = LayoutInflater.from(activity).inflate(R.layout.list_item, parent, false);
                    return new OCFileListItemViewHolder(itemView);
                }

            case VIEWTYPE_FOOTER:
                View itemView = LayoutInflater.from(activity).inflate(R.layout.list_footer, parent, false);
                return new OCFileListFooterViewHolder(itemView);

            case VIEWTYPE_HEADER:
                View headerView = LayoutInflater.from(activity).inflate(R.layout.list_header, parent, false);

                ViewGroup.LayoutParams layoutParams = headerView.getLayoutParams();
                layoutParams.height = (int) (parent.getHeight() * 0.3);
                headerView.setLayoutParams(layoutParams);

                return new OCFileListHeaderViewHolder(headerView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof OCFileListFooterViewHolder) {
            OCFileListFooterViewHolder footerViewHolder = (OCFileListFooterViewHolder) holder;
            footerViewHolder.footerText.setText(getFooterText());
            footerViewHolder.progressBar.getIndeterminateDrawable()
                .setColorFilter(ThemeColorUtils.primaryColor(activity), PorterDuff.Mode.SRC_IN);
            footerViewHolder.progressBar.setVisibility(
                ocFileListFragmentInterface.isLoading() ? View.VISIBLE : View.GONE);
        } else if (holder instanceof OCFileListHeaderViewHolder) {
            OCFileListHeaderViewHolder headerViewHolder = (OCFileListHeaderViewHolder) holder;
            String text = currentDirectory.getRichWorkspace();

            PreviewTextFragment.setText(headerViewHolder.headerText, text, null, activity, true, true);
            headerViewHolder.headerView.setOnClickListener(v -> ocFileListFragmentInterface.onHeaderClicked());
        } else {
            OCFileListGridImageViewHolder gridViewHolder = (OCFileListGridImageViewHolder) holder;

            OCFile file = getItem(position);

            boolean gridImage = MimeTypeUtil.isImage(file) || MimeTypeUtil.isVideo(file);

            gridViewHolder.thumbnail.setTag(file.getFileId());
            setThumbnail(file,
                         gridViewHolder.thumbnail,
                         user,
                         mStorageManager,
                         asyncTasks,
                         gridView,
                         activity,
                         gridViewHolder.shimmerThumbnail,
                         preferences,
                         isMediaGallery);

            //remove padding if gallery media is there else enable padding
            if (isMediaGallery) {
                gridViewHolder.thumbnail.setPadding(0, 0, 0, 0);
            }

            if (highlightedItem != null && file.getFileId() == highlightedItem.getFileId()) {
                if (gridView) {
                    gridViewHolder.selectedItemBackground.setVisibility(View.VISIBLE);
                } else {
                    gridViewHolder.itemLayout.setBackgroundColor(activity.getResources()
                                                                     .getColor(R.color.selected_item_background));
                }
            } else if (isCheckedFile(file)) {
                if (gridView) {
                    gridViewHolder.selectedItemBackground.setVisibility(View.VISIBLE);
                } else {
                    gridViewHolder.itemLayout.setBackgroundColor(activity.getResources().getColor(R.color.selected_item_background));
                }
                gridViewHolder.checkbox.setImageResource(R.drawable.ic_checkbox_marked);

                // gridViewHolder.checkbox.setImageDrawable(
                // ThemeDrawableUtils.tintDrawable(R.drawable.ic_checkbox_marked, R.color.check_green_color));
            } else {
                if (gridView) {
                    gridViewHolder.selectedItemBackground.setVisibility(View.INVISIBLE);
                } else {
                    gridViewHolder.itemLayout.setBackgroundColor(activity.getResources().getColor(R.color.bg_default));
                }
                gridViewHolder.checkbox.setImageResource(R.drawable.ic_checkbox_blank_outline);
            }

            gridViewHolder.itemLayout.setOnClickListener(v -> ocFileListFragmentInterface.onItemClicked(file));

            if (!hideItemOptions) {
                gridViewHolder.itemLayout.setLongClickable(true);
                gridViewHolder.itemLayout.setOnLongClickListener(v ->
                                                                     ocFileListFragmentInterface.onLongItemClicked(file));
            }

            // unread comments
            if (file.getUnreadCommentsCount() > 0) {
                gridViewHolder.unreadComments.setVisibility(View.VISIBLE);
                gridViewHolder.unreadComments.setOnClickListener(view -> ocFileListFragmentInterface
                    .showActivityDetailView(file));
            } else {
                gridViewHolder.unreadComments.setVisibility(View.GONE);
            }

            if (holder instanceof OCFileListItemViewHolder) {
                OCFileListItemViewHolder itemViewHolder = (OCFileListItemViewHolder) holder;

                Resources resources = activity.getResources();
                float avatarRadius = resources.getDimension(R.dimen.list_item_avatar_icon_radius);

                if ((file.isSharedWithMe() || file.isSharedWithSharee()) && !multiSelect && !gridView &&
                    !hideItemOptions) {
                    itemViewHolder.sharedAvatars.setVisibility(View.GONE);//visibility gone as view not required
                    itemViewHolder.sharedAvatars.removeAllViews();

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

                    int shareeSize = Math.min(sharees.size(), 3);
                    int w = DisplayUtils.convertDpToPixel(40, activity);
                    int margin = DisplayUtils.convertDpToPixel(24, activity);
                    int size = 60 * (shareeSize - 1) + w;

                    for (int i = 0; i < shareeSize; i++) {
                        ShareeUser sharee = sharees.get(i);

                        ImageView avatar = new ImageView(activity);

                        if (i == 0 && sharees.size() > 3) {
                            avatar.setImageResource(R.drawable.ic_people);
                            ThemeDrawableUtils.setIconColor(avatar.getDrawable());
                        } else {
                            switch (sharee.getShareType()) {
                                case GROUP:
                                case EMAIL:
                                case ROOM:
                                case CIRCLE:
                                    ThemeAvatarUtils.createAvatar(sharee.getShareType(), avatar, activity);
                                    break;

                                case FEDERATED:
                                    showFederatedShareAvatar(sharee.getUserId(), avatarRadius, resources, avatar);
                                    break;
                                default:
                                    avatar.setTag(sharee);
                                    DisplayUtils.setAvatar(user,
                                                           sharee.getUserId(),
                                                           sharee.getDisplayName(),
                                                           this,
                                                           avatarRadius,
                                                           resources,
                                                           avatar,
                                                           activity);
                                    break;
                            }
                        }

                        avatar.setOnClickListener(view -> ocFileListFragmentInterface.onShareIconClick(file));

                        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(w, w);
                        layoutParams.setMargins(0, 0, i * margin, 0);
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        avatar.setLayoutParams(layoutParams);
                        itemViewHolder.sharedAvatars.addView(avatar);

                        ViewGroup.LayoutParams rememberParam = itemViewHolder.sharedAvatars.getLayoutParams();
                        rememberParam.width = size;

                        itemViewHolder.sharedAvatars.setLayoutParams(rememberParam);
                    }
                } else {
                    itemViewHolder.sharedAvatars.setVisibility(View.GONE);
                    itemViewHolder.sharedAvatars.removeAllViews();
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

                    itemViewHolder.fileSize.setText(DisplayUtils.bytesToHumanReadable(localSize));
                } else {
                    itemViewHolder.fileSize.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));
                }
                itemViewHolder.lastModification.setText(DisplayUtils.getRelativeTimestamp(activity,
                                                                                          file.getModificationTimestamp()));

                if (multiSelect || gridView || hideItemOptions) {
                    itemViewHolder.overflowMenu.setVisibility(View.GONE);
                } else {
                    itemViewHolder.overflowMenu.setVisibility(View.VISIBLE);
                    itemViewHolder.overflowMenu.setOnClickListener(view -> ocFileListFragmentInterface
                        .onOverflowIconClicked(file, view));
                }
            }

            gridViewHolder.localFileIndicator.setVisibility(View.INVISIBLE);   // default first

            //if gallery is not selected then only show icons else don't show
            if (!isMediaGallery) {
                OperationsService.OperationsServiceBinder operationsServiceBinder = transferServiceGetter.getOperationsServiceBinder();
                FileDownloader.FileDownloaderBinder fileDownloaderBinder = transferServiceGetter.getFileDownloaderBinder();
                FileUploader.FileUploaderBinder fileUploaderBinder = transferServiceGetter.getFileUploaderBinder();
                if (operationsServiceBinder != null && operationsServiceBinder.isSynchronizing(user, file)) {
                    //synchronizing
                    gridViewHolder.localFileIndicator.setImageResource(R.drawable.ic_synchronizing);
                    gridViewHolder.localFileIndicator.setVisibility(View.VISIBLE);

                } else if (fileDownloaderBinder != null && fileDownloaderBinder.isDownloading(user, file)) {
                    // downloading
                    gridViewHolder.localFileIndicator.setImageResource(R.drawable.ic_synchronizing);
                    gridViewHolder.localFileIndicator.setVisibility(View.VISIBLE);

                } else if (fileUploaderBinder != null && fileUploaderBinder.isUploading(user, file)) {
                    //uploading
                    gridViewHolder.localFileIndicator.setImageResource(R.drawable.ic_synchronizing);
                    gridViewHolder.localFileIndicator.setVisibility(View.VISIBLE);

                } else if (file.getEtagInConflict() != null) {
                    // conflict
                    gridViewHolder.localFileIndicator.setImageResource(R.drawable.ic_synchronizing_error);
                    gridViewHolder.localFileIndicator.setVisibility(View.VISIBLE);

                } else if (file.isDown()) {
                    gridViewHolder.localFileIndicator.setImageResource(R.drawable.ic_synced);
                    gridViewHolder.localFileIndicator.setVisibility(View.VISIBLE);
                }
            }


            gridViewHolder.favorite.setVisibility(!isMediaGallery && file.isFavorite() ? View.VISIBLE : View.GONE);

            if (multiSelect) {
                gridViewHolder.checkbox.setVisibility(View.VISIBLE);
            } else {
                gridViewHolder.checkbox.setVisibility(gridView ? View.INVISIBLE : View.GONE);
            }

            if (holder instanceof OCFileListGridItemViewHolder) {
                OCFileListGridItemViewHolder gridItemViewHolder = (OCFileListGridItemViewHolder) holder;

                gridItemViewHolder.fileName.setText(file.getDecryptedFileName());

                //if (gridView && gridImage) {
                if (isMediaGallery) {
                    gridItemViewHolder.fileName.setVisibility(View.GONE);
                } else {
                   /* if (gridView && ocFileListFragmentInterface.getColumnsCount() > showFilenameColumnThreshold) {
                        gridItemViewHolder.fileName.setVisibility(View.GONE);
                    } else {*/
                    gridItemViewHolder.fileName.setVisibility(View.VISIBLE);
                    // }
                }
            }

            if (gridView || hideItemOptions || (file.isFolder() && !file.canReshare())) {
                gridViewHolder.shared.setVisibility(View.GONE);
            } else {
                showShareIcon(gridViewHolder, file);
            }
        }
    }

    private void showFederatedShareAvatar(String user, float avatarRadius, Resources resources, ImageView avatar) {
        // maybe federated share
        String[] split = user.split("@");
        String userId = split[0];
        String server = split[1];

        String url = "https://" + server + "/index.php/avatar/" + userId + "/" +
            DisplayUtils.convertDpToPixel(avatarRadius, activity);

        Drawable placeholder;
        try {
            placeholder = TextDrawable.createAvatarByUserId(userId, avatarRadius);
        } catch (Exception e) {
            Log_OC.e(TAG, "Error calculating RGB value for active account icon.", e);
            placeholder = ThemeDrawableUtils.tintDrawable(ResourcesCompat.getDrawable(resources,
                                                                                      R.drawable.account_circle_white,
                                                                                      null),
                                                          R.color.black);
        }

        avatar.setTag(null);
        Glide.with(activity).load(url)
            .asBitmap()
            .placeholder(placeholder)
            .error(placeholder)
            .into(new BitmapImageViewTarget(avatar) {
                @Override
                protected void setResource(Bitmap resource) {
                    RoundedBitmapDrawable circularBitmapDrawable =
                        RoundedBitmapDrawableFactory.create(activity.getResources(), resource);
                    circularBitmapDrawable.setCircular(true);
                    avatar.setImageDrawable(circularBitmapDrawable);
                }
            });
    }

    public static void setThumbnail(OCFile file,
                                    ImageView thumbnailView,
                                    User user,
                                    FileDataStorageManager storageManager,
                                    List<ThumbnailsCacheManager.ThumbnailGenerationTask> asyncTasks,
                                    boolean gridView,
                                    Context context) {
        setThumbnail(file, thumbnailView, user, storageManager, asyncTasks, gridView, context, null, null,
                     false);
    }

    private static void setThumbnail(OCFile file,
                                     ImageView thumbnailView,
                                     User user,
                                     FileDataStorageManager storageManager,
                                     List<ThumbnailsCacheManager.ThumbnailGenerationTask> asyncTasks,
                                     boolean gridView,
                                     Context context,
                                     LoaderImageView shimmerThumbnail,
                                     AppPreferences preferences,
                                     boolean isMediaGallery) {
        if (file.isFolder()) {
            stopShimmer(shimmerThumbnail, thumbnailView);
            updateThumbnailViewSize(thumbnailView, gridView, context, isMediaGallery, R.dimen.standard_folders_grid_item_size);
            //reset the padding as this will change for files and we don't this for folders
            thumbnailView.setPadding(0, 0, 0, 0);
            thumbnailView.setImageDrawable(MimeTypeUtil
                                               .getFolderTypeIcon(file.isSharedWithMe() || file.isSharedWithSharee(),
                                                                  file.isSharedViaLink(), file.isEncrypted(),
                                                                  file.getMountType(), context));
        } else {
            updateThumbnailViewSize(thumbnailView, gridView, context, isMediaGallery, R.dimen.standard_files_grid_item_size);

            if (file.getRemoteId() != null && file.isPreviewAvailable()) {
                // Thumbnail in cache?
                Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.getRemoteId());

                if (thumbnail != null && !file.isUpdateThumbnailNeeded()) {
                    setThumbnailViewPadding(thumbnailView, gridView, context, isMediaGallery, R.dimen.alternate_padding);
                    stopShimmer(shimmerThumbnail, thumbnailView);

                    if (MimeTypeUtil.isVideo(file)) {
                        thumbnail = ThumbnailsCacheManager.addVideoOverlay(thumbnail);
                    }

                    //set the corner for both video and image thumbnail
                    if (gridView) {
                        BitmapUtils.setRoundedBitmapForGridMode(thumbnail, thumbnailView);
                    } else {
                        BitmapUtils.setRoundedBitmap(thumbnail, thumbnailView);
                    }

                } else {
                    // generate new thumbnail
                    if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, thumbnailView)) {
                        try {
                            final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                                new ThumbnailsCacheManager.ThumbnailGenerationTask(thumbnailView,
                                                                                   storageManager,
                                                                                   user.toPlatformAccount(),
                                                                                   asyncTasks,
                                                                                   gridView);
                            if (thumbnail == null) {
                                Drawable drawable = MimeTypeUtil.getFileTypeIcon(file.getMimeType(),
                                                                                 file.getFileName(),
                                                                                 user,
                                                                                 context);
                                if (drawable == null) {
                                    drawable = ResourcesCompat.getDrawable(context.getResources(),
                                                                           R.drawable.file_image,
                                                                           null);
                                }
                                thumbnail = BitmapUtils.drawableToBitmap(drawable);

                                //set thumbnailView padding for no thumbnail
                                setThumbnailViewPadding(thumbnailView, gridView, context, isMediaGallery, R.dimen.standard_quarter_padding);

                            }
                            final ThumbnailsCacheManager.AsyncThumbnailDrawable asyncDrawable =
                                new ThumbnailsCacheManager.AsyncThumbnailDrawable(context.getResources(),
                                                                                  thumbnail, task);

                            if (shimmerThumbnail != null && shimmerThumbnail.getVisibility() == View.GONE) {
                                //resize shimmer for gallery media
                                if (gridView && isMediaGallery) {
                                    configShimmerGridImageSize(shimmerThumbnail, preferences.getGridColumns());
                                }
                                startShimmer(shimmerThumbnail, thumbnailView);
                            }

                            task.setListener(new ThumbnailsCacheManager.ThumbnailGenerationTask.Listener() {
                                @Override
                                public void onSuccess() {
                                    stopShimmer(shimmerThumbnail, thumbnailView);
                                }

                                @Override
                                public void onError() {
                                    stopShimmer(shimmerThumbnail, thumbnailView);
                                }
                            });

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
                stopShimmer(shimmerThumbnail, thumbnailView);
                setThumbnailViewPadding(thumbnailView, gridView, context, isMediaGallery, R.dimen.standard_quarter_padding);
                thumbnailView.setImageDrawable(MimeTypeUtil.getFileTypeIcon(file.getMimeType(),
                                                                            file.getFileName(),
                                                                            user,
                                                                            context));
            }
        }
    }

    /**
     * method to set the padding to thumbnail view this is required for files so that there will be space between file
     * and file name
     *
     * @param thumbnailView
     * @param gridView
     * @param context
     * @param isMediaGallery
     * @param dimensPadding
     */
    private static void setThumbnailViewPadding(ImageView thumbnailView, boolean gridView, Context context,
                                                boolean isMediaGallery, int dimensPadding) {
        if (gridView && !isMediaGallery) {
            int padding = context.getResources().getDimensionPixelSize(dimensPadding);
            thumbnailView.setPadding(0, 0, 0, padding);
        }
    }

    /**
     * method to set manual thumbnail view height and width for folders and files because we are using different size
     * for both files and folders
     *
     * @param thumbnailView
     * @param gridView
     * @param context
     * @param isMediaGallery
     * @param size
     */
    private static void updateThumbnailViewSize(ImageView thumbnailView, boolean gridView, Context context,
                                                boolean isMediaGallery, int size) {
        if (gridView && !isMediaGallery) {
            thumbnailView.getLayoutParams().width =
                context.getResources().getDimensionPixelSize(size);
            thumbnailView.getLayoutParams().height = context.getResources().getDimensionPixelSize(size);
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof OCFileListGridImageViewHolder) {
            LoaderImageView thumbnailShimmer = ((OCFileListGridImageViewHolder) holder).shimmerThumbnail;
            if (thumbnailShimmer.getVisibility() == View.VISIBLE) {
                thumbnailShimmer.setImageResource(R.drawable.background);
                thumbnailShimmer.resetLoader();
            }
        }
    }

    private static Point getScreenSize(Context context) throws Exception {
        final WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            final Point displaySize = new Point();
            windowManager.getDefaultDisplay().getSize(displaySize);
            return displaySize;
        } else {
            throw new Exception("WindowManager not found");
        }
    }

    private static void configShimmerGridImageSize(LoaderImageView thumbnailShimmer, float gridColumns) {
        FrameLayout.LayoutParams targetLayoutParams = (FrameLayout.LayoutParams) thumbnailShimmer.getLayoutParams();

        try {
            final Point screenSize = getScreenSize(thumbnailShimmer.getContext());
            final int marginLeftAndRight = targetLayoutParams.leftMargin + targetLayoutParams.rightMargin;
            final int size = Math.round(screenSize.x / gridColumns - marginLeftAndRight);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            params.setMargins(targetLayoutParams.leftMargin,
                              targetLayoutParams.topMargin,
                              targetLayoutParams.rightMargin,
                              targetLayoutParams.bottomMargin);
            thumbnailShimmer.setLayoutParams(params);
        } catch (Exception exception) {
            Log_OC.e("ConfigShimmer", exception.getMessage());
        }
    }

    private static void startShimmer(LoaderImageView thumbnailShimmer, ImageView thumbnailView) {
        thumbnailShimmer.setImageResource(R.drawable.background);
        thumbnailShimmer.resetLoader();
        thumbnailView.setVisibility(View.GONE);
        thumbnailShimmer.setVisibility(View.VISIBLE);
    }

    private static void stopShimmer(@Nullable LoaderImageView thumbnailShimmer, ImageView thumbnailView) {
        if (thumbnailShimmer != null) {
            thumbnailShimmer.setVisibility(View.GONE);
        }

        thumbnailView.setVisibility(View.VISIBLE);
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

    public OCFile getItem(int position) {
        int newPosition = position;

        if (shouldShowHeader() && position > 0) {
            newPosition = position - 1;
        }

        return mFiles.get(newPosition);
    }

    public boolean shouldShowHeader() {
        if (currentDirectory == null) {
            return false;
        }

        if (MainApp.isOnlyOnDevice()) {
            return false;
        }

        if (currentDirectory.getRichWorkspace() == null) {
            return false;
        }

        return !TextUtils.isEmpty(currentDirectory.getRichWorkspace().trim());
    }

    @Override
    public int getItemViewType(int position) {
        if (shouldShowHeader()) {
            if (position == 0) {
                return VIEWTYPE_HEADER;
            } else {
                if (position == mFiles.size() + 1) {
                    return VIEWTYPE_FOOTER;
                }
            }
        } else {
            if (position == mFiles.size()) {
                return VIEWTYPE_FOOTER;
            }
        }

        if (MimeTypeUtil.isImageOrVideo(getItem(position))) {
            return VIEWTYPE_IMAGE;
        } else {
            return VIEWTYPE_ITEM;
        }
    }

    private void showShareIcon(OCFileListGridImageViewHolder gridViewHolder, OCFile file) {
        ImageView sharedIconView = gridViewHolder.shared;

        if (gridViewHolder instanceof OCFileListItemViewHolder || file.getUnreadCommentsCount() == 0) {
            sharedIconView.setVisibility(View.VISIBLE);

            if (file.isSharedWithSharee() || file.isSharedWithMe()) {
             /*   if (showShareAvatar) {
                    sharedIconView.setVisibility(View.GONE);
                } else {*/
                //sharedIconView.setVisibility(View.VISIBLE);
                sharedIconView.setImageResource(R.drawable.ic_shared);
                sharedIconView.setContentDescription(activity.getString(R.string.shared_icon_shared));
                //  }
            } else if (file.isSharedViaLink()) {
                sharedIconView.setImageResource(R.drawable.ic_shared);
                sharedIconView.setContentDescription(activity.getString(R.string.shared_icon_shared_via_link));
            } else {
                sharedIconView.setImageResource(R.drawable.ic_unshared);
                sharedIconView.setContentDescription(activity.getString(R.string.shared_icon_share));
            }
            sharedIconView.setOnClickListener(view -> ocFileListFragmentInterface.onShareIconClick(file));
        } else {
            sharedIconView.setVisibility(View.GONE);
        }
    }

    /**
     * Change the adapted directory for a new one
     *
     * @param directory             New folder to adapt. Can be NULL, meaning "no content to adapt".
     * @param updatedStorageManager Optional updated storage manager; used to replace
     * @param limitToMimeType       show only files of this mimeType
     */
    public void swapDirectory(
        User account,
        OCFile directory,
        FileDataStorageManager updatedStorageManager,
        boolean onlyOnDevice, String limitToMimeType
                             ) {
        this.onlyOnDevice = onlyOnDevice;

        if (updatedStorageManager != null && !updatedStorageManager.equals(mStorageManager)) {
            mStorageManager = updatedStorageManager;
            showShareAvatar = mStorageManager.getCapability(account.getAccountName()).getVersion().isShareesOnDavSupported();
            this.user = account;
        }
        if (mStorageManager != null) {
            mFiles = mStorageManager.getFolderContent(directory, onlyOnDevice);

            if (!preferences.isShowHiddenFilesEnabled()) {
                mFiles = filterHiddenFiles(mFiles);
            }
            if (!limitToMimeType.isEmpty()) {
                mFiles = filterByMimeType(mFiles, limitToMimeType);
            }
            FileSortOrder sortOrder = preferences.getSortOrderByFolder(directory);
            mFiles = sortOrder.sortCloudFiles(mFiles);
            mFilesAll.clear();
            mFilesAll.addAll(mFiles);

            currentDirectory = directory;
        } else {
            mFiles.clear();
            mFilesAll.clear();
        }

        notifyDataSetChanged();
    }


    public void setData(List<Object> objects,
                        ExtendedListFragment.SearchType searchType,
                        FileDataStorageManager storageManager,
                        @Nullable OCFile folder,
                        boolean clear) {
        if (storageManager != null && mStorageManager == null) {
            mStorageManager = storageManager;
            showShareAvatar = mStorageManager.getCapability(user.getAccountName()).getVersion().isShareesOnDavSupported();
        }

        if (mStorageManager == null) {
            mStorageManager = new FileDataStorageManager(user.toPlatformAccount(), activity.getContentResolver());
        }

        if (clear) {
            mFiles.clear();
            resetLastTimestamp();
            preferences.setPhotoSearchTimestamp(0);

            VirtualFolderType type;
            switch (searchType) {
                case FAVORITE_SEARCH:
                    type = VirtualFolderType.FAVORITE;
                    break;
                case GALLERY_SEARCH:
                    type = VirtualFolderType.GALLERY;
                    break;
                default:
                    type = VirtualFolderType.NONE;
                    break;
            }

            mStorageManager.deleteVirtuals(type);
        }

        // early exit
        if (objects.size() > 0 && mStorageManager != null) {
            if (searchType == ExtendedListFragment.SearchType.SHARED_FILTER) {
                parseShares(objects);
            } else {
                parseVirtuals(objects, searchType);
            }
        }

        if (searchType != ExtendedListFragment.SearchType.GALLERY_SEARCH &&
            searchType != ExtendedListFragment.SearchType.RECENTLY_MODIFIED_SEARCH) {
            FileSortOrder sortOrder = preferences.getSortOrderByFolder(folder);
            mFiles = sortOrder.sortCloudFiles(mFiles);
        } else {
            mFiles = FileStorageUtils.sortOcFolderDescDateModifiedWithoutFavoritesFirst(mFiles);
        }

        mFilesAll.clear();
        mFilesAll.addAll(mFiles);

        new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
    }

    private void parseShares(List<Object> objects) {
        List<OCShare> shares = new ArrayList<>();

        for (Object shareObject : objects) {
            // check type before cast as of long running data fetch it is possible that old result is filled
            if (shareObject instanceof OCShare) {
                OCShare ocShare = (OCShare) shareObject;

                shares.add(ocShare);

                // get ocFile from Server to have an up-to-date copy
                RemoteOperationResult result = new ReadFileRemoteOperation(ocShare.getPath()).execute(user.toPlatformAccount(),
                                                                                                      activity);

                if (result.isSuccess()) {
                    OCFile file = FileStorageUtils.fillOCFile((RemoteFile) result.getData().get(0));
                    FileStorageUtils.searchForLocalFileInDefaultPath(file, user.toPlatformAccount());
                    file = mStorageManager.saveFileWithParent(file, activity);

                    ShareType newShareType = ocShare.getShareType();
                    if (newShareType == ShareType.PUBLIC_LINK) {
                        file.setSharedViaLink(true);
                    } else if (newShareType == ShareType.USER ||
                        newShareType == ShareType.GROUP ||
                        newShareType == ShareType.EMAIL ||
                        newShareType == ShareType.FEDERATED ||
                        newShareType == ShareType.ROOM ||
                        newShareType == ShareType.CIRCLE) {
                        file.setSharedWithSharee(true);
                    }

                    mStorageManager.saveFile(file);

                    if (!mFiles.contains(file)) {
                        mFiles.add(file);
                    }
                } else {
                    Log_OC.e(TAG, "Error in getting prop for file: " + ocShare.getPath());
                }
            }
        }

        mStorageManager.saveShares(shares);
    }

    private void parseVirtuals(List<Object> objects, ExtendedListFragment.SearchType searchType) {
        VirtualFolderType type;
        boolean onlyMedia = false;

        switch (searchType) {
            case FAVORITE_SEARCH:
                type = VirtualFolderType.FAVORITE;
                break;
            case GALLERY_SEARCH:
                type = VirtualFolderType.GALLERY;
                onlyMedia = true;

                int lastPosition = objects.size() - 1;

                if (lastPosition < 0) {
                    lastTimestamp = -1;
                    break;
                }

                RemoteFile lastFile = (RemoteFile) objects.get(lastPosition);
                lastTimestamp = lastFile.getModifiedTimestamp() / 1000;
                break;
            default:
                type = VirtualFolderType.NONE;
                break;
        }

        List<ContentValues> contentValues = new ArrayList<>();

        for (Object remoteFile : objects) {
            OCFile ocFile = FileStorageUtils.fillOCFile((RemoteFile) remoteFile);
            FileStorageUtils.searchForLocalFileInDefaultPath(ocFile, user.toPlatformAccount());

            try {
                if (ExtendedListFragment.SearchType.GALLERY_SEARCH == searchType) {
                    mStorageManager.saveFile(ocFile);
                } else {

                    ocFile = mStorageManager.saveFileWithParent(ocFile, activity);

                    // also sync folder content
                    if (ocFile.isFolder()) {
                        long currentSyncTime = System.currentTimeMillis();
                        RemoteOperation refreshFolderOperation = new RefreshFolderOperation(ocFile,
                                                                                            currentSyncTime,
                                                                                            true,
                                                                                            false,
                                                                                            mStorageManager,
                                                                                            user.toPlatformAccount(),
                                                                                            activity);
                        refreshFolderOperation.execute(user.toPlatformAccount(), activity);
                    }
                }

                if (!onlyMedia || MimeTypeUtil.isImage(ocFile) || MimeTypeUtil.isVideo(ocFile)) {
                    mFiles.add(ocFile);
                }

                ContentValues cv = new ContentValues();
                cv.put(ProviderMeta.ProviderTableMeta.VIRTUAL_TYPE, type.toString());
                cv.put(ProviderMeta.ProviderTableMeta.VIRTUAL_OCFILE_ID, ocFile.getFileId());

                contentValues.add(cv);
            } catch (RemoteOperationFailedException e) {
                Log_OC.e(TAG, "Error saving file with parent" + e.getMessage(), e);
            }
        }

        preferences.setPhotoSearchTimestamp(System.currentTimeMillis());
        mStorageManager.saveVirtuals(contentValues);
    }

    public void showVirtuals(VirtualFolderType type, boolean onlyImages, FileDataStorageManager storageManager) {
        mFiles = storageManager.getVirtualFolderContent(type, onlyImages);

        if (VirtualFolderType.GALLERY == type) {
            mFiles = FileStorageUtils.sortOcFolderDescDateModifiedWithoutFavoritesFirst(mFiles);
        }

        mFilesAll.clear();
        mFilesAll.addAll(mFiles);

        new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
    }


    public void setSortOrder(@Nullable OCFile folder, FileSortOrder sortOrder) {
        preferences.setSortOrder(folder, sortOrder);
        mFiles = sortOrder.sortCloudFiles(mFiles);
        notifyDataSetChanged();
    }

    public Set<OCFile> getCheckedItems() {
        return checkedFiles;
    }

    public void setCheckedItem(Set<OCFile> files) {
        checkedFiles.clear();
        checkedFiles.addAll(files);
    }

    public void clearCheckedItems() {
        checkedFiles.clear();
    }

    public List<OCFile> getFiles() {
        return mFiles;
    }

    public Filter getFilter() {
        if (mFilesFilter == null) {
            mFilesFilter = new FilesFilter();
        }
        return mFilesFilter;
    }

    public void resetLastTimestamp() {
        lastTimestamp = -1;
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }

    @Override
    public void avatarGenerated(Drawable avatarDrawable, Object callContext) {
        ((ImageView) callContext).setImageDrawable(avatarDrawable);
    }

    @Override
    public boolean shouldCallGeneratedCallback(String tag, Object callContext) {
        return ((ImageView) callContext).getTag().equals(tag);
    }

    public void setHighlightedItem(OCFile highlightedItem) {
        this.highlightedItem = highlightedItem;
    }

    private class FilesFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            Vector<OCFile> filteredFiles = new Vector<>();

            if (!TextUtils.isEmpty(constraint)) {
                for (OCFile file : mFilesAll) {
                    if (file.getParentRemotePath().equals(currentDirectory.getRemotePath()) &&
                        file.getFileName().toLowerCase(Locale.getDefault()).contains(
                            constraint.toString().toLowerCase(Locale.getDefault())) &&
                        !filteredFiles.contains(file)) {
                        filteredFiles.add(file);
                    }
                }
            }

            results.values = filteredFiles;
            results.count = filteredFiles.size();

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, Filter.FilterResults results) {

            Vector<OCFile> ocFiles = (Vector<OCFile>) results.values;
            mFiles.clear();
            if (ocFiles != null && ocFiles.size() > 0) {
                mFiles.addAll(ocFiles);
                if (!preferences.isShowHiddenFilesEnabled()) {
                    mFiles = filterHiddenFiles(mFiles);
                }
                FileSortOrder sortOrder = preferences.getSortOrderByFolder(currentDirectory);
                mFiles = sortOrder.sortCloudFiles(mFiles);
            }

            notifyDataSetChanged();
        }
    }

    /**
     * Filter for hidden files
     *
     * @param files Collection of files to filter
     * @return Non-hidden files
     */
    private List<OCFile> filterHiddenFiles(List<OCFile> files) {
        List<OCFile> ret = new ArrayList<>();

        for (OCFile file : files) {
            if (!file.isHidden() && !ret.contains(file)) {
                ret.add(file);
            }
        }

        return ret;
    }

    private List<OCFile> filterByMimeType(List<OCFile> files, String mimeType) {
        List<OCFile> ret = new ArrayList<>();

        for (OCFile file : files) {
            if (file.isFolder() || file.getMimeType().startsWith(mimeType)) {
                ret.add(file);
            }
        }

        return ret;
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

    public void setGridView(boolean bool) {
        gridView = bool;
    }

    @VisibleForTesting
    public void setShowShareAvatar(boolean bool) {
        showShareAvatar = bool;
    }

    @VisibleForTesting
    public void setCurrentDirectory(OCFile folder) {
        currentDirectory = folder;
    }

    static class OCFileListItemViewHolder extends OCFileListGridItemViewHolder {
        @BindView(R.id.file_size)
        public TextView fileSize;

        @BindView(R.id.last_mod)
        public TextView lastModification;

        @BindView(R.id.overflow_menu)
        public ImageView overflowMenu;

        @BindView(R.id.sharedAvatars)
        public RelativeLayout sharedAvatars;

        private OCFileListItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    static class OCFileListGridItemViewHolder extends OCFileListGridImageViewHolder {
        @BindView(R.id.Filename) public TextView fileName;

        private OCFileListGridItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    static class OCFileListGridImageViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.thumbnail)
        public ImageView thumbnail;

        @BindView(R.id.thumbnail_shimmer)
        public LoaderImageView shimmerThumbnail;

        @BindView(R.id.favorite_action)
        public ImageView favorite;

        @BindView(R.id.localFileIndicator)
        public ImageView localFileIndicator;

        @BindView(R.id.sharedIcon)
        public ImageView shared;

        @BindView(R.id.custom_checkbox)
        public ImageView checkbox;

        @BindView(R.id.ListItemLayout)
        public View itemLayout;

        @BindView(R.id.unreadComments)
        public ImageView unreadComments;

        @BindView(R.id.selectedItemBackground)
        public View selectedItemBackground;

        private OCFileListGridImageViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            favorite.getDrawable().mutate();
        }
    }

    static class OCFileListFooterViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.footerText)
        public TextView footerText;

        @BindView(R.id.loadingProgressBar)
        public ProgressBar progressBar;

        private OCFileListFooterViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    static class OCFileListHeaderViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.headerView)
        public View headerView;

        @BindView(R.id.headerText)
        public TextView headerText;

        private OCFileListHeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
