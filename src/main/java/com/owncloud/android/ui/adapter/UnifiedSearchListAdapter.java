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
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
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

import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.afollestad.sectionedrecyclerview.SectionedViewHolder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.elyeproj.loaderviewlibrary.LoaderImageView;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.UnifiedSearchHeaderBinding;
import com.owncloud.android.databinding.UnifiedSearchItemBinding;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datamodel.VirtualFolderType;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.SearchResult;
import com.owncloud.android.lib.common.SearchResultEntry;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.operations.RemoteOperationFailedException;
import com.owncloud.android.ui.TextDrawable;
import com.owncloud.android.ui.fragment.ExtendedListFragment;
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface;
import com.owncloud.android.ui.interfaces.UnifiedSearchListInterface;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import kotlin.NotImplementedError;

/**
 * This Adapter populates a RecyclerView with all files and folders in a Nextcloud instance.
 */
public class UnifiedSearchListAdapter extends SectionedRecyclerViewAdapter<SectionedViewHolder>
    implements DisplayUtils.AvatarGenerationListener {

    private static final int showFilenameColumnThreshold = 4;
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

    private List<SearchResult> list = new ArrayList<>();

    private FileDataStorageManager storageManager;
    private User user;
    private OCFileListFragmentInterface ocFileListFragmentInterface;

    private FilesFilter mFilesFilter;
    private OCFile currentDirectory;
    private static final String TAG = UnifiedSearchListAdapter.class.getSimpleName();

    private static final int VIEWTYPE_FOOTER = 0;
    private static final int VIEWTYPE_ITEM = 1;
    private static final int VIEWTYPE_IMAGE = 2;
    private static final int VIEWTYPE_HEADER = 3;

    private List<ThumbnailsCacheManager.ThumbnailGenerationTask> asyncTasks = new ArrayList<>();
    private boolean onlyOnDevice;
    private boolean showShareAvatar = false;
    private OCFile highlightedItem;
    private Context context;
    private UnifiedSearchListInterface listInterface;

    public UnifiedSearchListAdapter(FileDataStorageManager storageManager,
                                    UnifiedSearchListInterface listInterface,
                                    Context context) {
//        this.ocFileListFragmentInterface = ocFileListFragmentInterface;
//        this.activity = activity;
//        this.preferences = preferences;
//        this.accountManager = accountManager;
//        this.user = user;
//        this.gridView = gridView;
        checkedFiles = new HashSet<>();
        this.context = context;
        this.storageManager = storageManager;
        this.listInterface = listInterface;

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

    @NonNull
    @Override
    public SectionedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            UnifiedSearchHeaderBinding binding = UnifiedSearchHeaderBinding.inflate(LayoutInflater.from(context),
                                                                                    parent,
                                                                                    false);

            return new UnifiedSearchHeaderViewHolder(binding, context);
        } else {
            UnifiedSearchItemBinding binding = UnifiedSearchItemBinding.inflate(LayoutInflater.from(context),
                                                                                parent,
                                                                                false);

            return new UnifiedSearchItemViewHolder(binding, context, storageManager, listInterface);
        }
    }

    @Override
    public int getSectionCount() {
        return list.size();
    }

    @Override
    public int getItemCount(int section) {
        return list.get(section).getEntries().size();
    }

    @Override
    public void onBindHeaderViewHolder(SectionedViewHolder holder, int section, boolean expanded) {
        UnifiedSearchHeaderViewHolder headerViewHolder = (UnifiedSearchHeaderViewHolder) holder;

        headerViewHolder.bind(list.get(section));
    }

    @Override
    public void onBindFooterViewHolder(SectionedViewHolder holder, int section) {
        throw new NotImplementedError();
    }

    @Override
    public void onBindViewHolder(SectionedViewHolder holder, int section, int relativePosition, int absolutePosition) {
        UnifiedSearchItemViewHolder itemViewHolder = (UnifiedSearchItemViewHolder) holder;
        SearchResultEntry entry = list.get(section).getEntries().get(relativePosition);

        itemViewHolder.bind(entry);
    }

    public boolean isMultiSelect() {
        return multiSelect;
    }

    public void setMultiSelect(boolean bool) {
        multiSelect = bool;
        notifyDataSetChanged();
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
                storageManager.saveFile(file);

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
            placeholder = ThemeUtils.tintDrawable(ResourcesCompat.getDrawable(resources,
                                                                              R.drawable.account_circle_white, null),
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
        setThumbnail(file, thumbnailView, user, storageManager, asyncTasks, gridView, context, null, null);
    }

    private static void setThumbnail(OCFile file,
                                     ImageView thumbnailView,
                                     User user,
                                     FileDataStorageManager storageManager,
                                     List<ThumbnailsCacheManager.ThumbnailGenerationTask> asyncTasks,
                                     boolean gridView,
                                     Context context,
                                     LoaderImageView shimmerThumbnail,
                                     AppPreferences preferences) {
        if (file.isFolder()) {
            thumbnailView.setImageDrawable(MimeTypeUtil
                                               .getFolderTypeIcon(file.isSharedWithMe() || file.isSharedWithSharee(),
                                                                  file.isSharedViaLink(), file.isEncrypted(),
                                                                  file.getMountType(), context));
        } else {
            if (file.getRemoteId() != null && file.isPreviewAvailable()) {
                // Thumbnail in cache?
                Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.getRemoteId()
                                                                                );

                if (thumbnail != null && !file.isUpdateThumbnailNeeded()) {
                    if (MimeTypeUtil.isVideo(file)) {
                        Bitmap withOverlay = ThumbnailsCacheManager.addVideoOverlay(thumbnail);
                        thumbnailView.setImageBitmap(withOverlay);
                    } else {
                        if (gridView) {
                            BitmapUtils.setRoundedBitmapForGridMode(thumbnail, thumbnailView);
                        } else {
                            BitmapUtils.setRoundedBitmap(thumbnail, thumbnailView);
                        }
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
                            }
                            final ThumbnailsCacheManager.AsyncThumbnailDrawable asyncDrawable =
                                new ThumbnailsCacheManager.AsyncThumbnailDrawable(context.getResources(),
                                                                                  thumbnail, task);

                            if (shimmerThumbnail != null && shimmerThumbnail.getVisibility() == View.GONE) {
                                if (gridView) {
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
                thumbnailView.setImageDrawable(MimeTypeUtil.getFileTypeIcon(file.getMimeType(),
                                                                            file.getFileName(),
                                                                            user,
                                                                            context));
            }
        }
    }

//    @Override
//    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
//        if (holder instanceof OCFileListGridImageViewHolder) {
//            LoaderImageView thumbnailShimmer = ((OCFileListGridImageViewHolder) holder).shimmerThumbnail;
//            if (thumbnailShimmer.getVisibility() == View.VISIBLE){
//                thumbnailShimmer.setImageResource(R.drawable.background);
//                thumbnailShimmer.resetLoader();
//            }
//        }
//    }

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
            thumbnailView.setVisibility(View.VISIBLE);
        }
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

        return !TextUtils.isEmpty(currentDirectory.getRichWorkspace());
    }

    private void showShareIcon(OCFileListGridImageViewHolder gridViewHolder, OCFile file) {
        ImageView sharedIconView = gridViewHolder.shared;

        if (gridViewHolder instanceof OCFileListItemViewHolder || file.getUnreadCommentsCount() == 0) {
            sharedIconView.setVisibility(View.VISIBLE);

            if (file.isSharedWithSharee() || file.isSharedWithMe()) {
                if (showShareAvatar) {
                    sharedIconView.setVisibility(View.GONE);
                } else {
                    sharedIconView.setVisibility(View.VISIBLE);
                    sharedIconView.setImageResource(R.drawable.shared_via_users);
                    sharedIconView.setContentDescription(activity.getString(R.string.shared_icon_shared));
                }
            } else if (file.isSharedViaLink()) {
                sharedIconView.setImageResource(R.drawable.shared_via_link);
                sharedIconView.setContentDescription(activity.getString(R.string.shared_icon_shared_via_link));
            } else {
                sharedIconView.setImageResource(R.drawable.ic_unshared);
                sharedIconView.setContentDescription(activity.getString(R.string.shared_icon_share));
            }
            if (accountManager.accountOwnsFile(file, user.toPlatformAccount())) {
                sharedIconView.setOnClickListener(view -> ocFileListFragmentInterface.onShareIconClick(file));
            } else {
                sharedIconView.setOnClickListener(view -> ocFileListFragmentInterface.showShareDetailView(file));
            }
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

        if (updatedStorageManager != null && !updatedStorageManager.equals(storageManager)) {
            storageManager = updatedStorageManager;
            showShareAvatar = storageManager.getCapability(account.getAccountName()).getVersion().isShareesOnDavSupported();
            this.user = account;
        }
        if (storageManager != null) {
            mFiles = storageManager.getFolderContent(directory, onlyOnDevice);

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
        if (storageManager != null && this.storageManager == null) {
            this.storageManager = storageManager;
            showShareAvatar = this.storageManager.getCapability(user.getAccountName()).getVersion().isShareesOnDavSupported();
        }

        if (this.storageManager == null) {
            this.storageManager = new FileDataStorageManager(user.toPlatformAccount(), activity.getContentResolver());
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
                case PHOTO_SEARCH:
                    type = VirtualFolderType.PHOTOS;
                    break;
                default:
                    type = VirtualFolderType.NONE;
                    break;
            }

            this.storageManager.deleteVirtuals(type);
        }

        // early exit
        if (objects.size() > 0 && this.storageManager != null) {
            if (searchType == ExtendedListFragment.SearchType.SHARED_FILTER) {
                parseShares(objects);
            } else {
                parseVirtuals(objects, searchType);
            }
        }

        if (searchType != ExtendedListFragment.SearchType.PHOTO_SEARCH &&
            searchType != ExtendedListFragment.SearchType.PHOTOS_SEARCH_FILTER &&
            searchType != ExtendedListFragment.SearchType.RECENTLY_MODIFIED_SEARCH &&
            searchType != ExtendedListFragment.SearchType.RECENTLY_MODIFIED_SEARCH_FILTER) {
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
                    file = storageManager.saveFileWithParent(file, activity);

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

                    storageManager.saveFile(file);

                    if (!mFiles.contains(file)) {
                        mFiles.add(file);
                    }
                } else {
                    Log_OC.e(TAG, "Error in getting prop for file: " + ocShare.getPath());
                }
            }
        }

        storageManager.saveShares(shares);
    }

    private void parseVirtuals(List<Object> objects, ExtendedListFragment.SearchType searchType) {
        VirtualFolderType type;
        boolean onlyImages = false;

        switch (searchType) {
            case FAVORITE_SEARCH:
                type = VirtualFolderType.FAVORITE;
                break;
            case PHOTO_SEARCH:
                type = VirtualFolderType.PHOTOS;
                onlyImages = true;

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
                if (ExtendedListFragment.SearchType.PHOTO_SEARCH == searchType) {
                    storageManager.saveFile(ocFile);
                } else {

                    ocFile = storageManager.saveFileWithParent(ocFile, activity);

                    // also sync folder content
                    if (ocFile.isFolder()) {
                        long currentSyncTime = System.currentTimeMillis();
                        RemoteOperation refreshFolderOperation = new RefreshFolderOperation(ocFile,
                                                                                            currentSyncTime,
                                                                                            true,
                                                                                            false,
                                                                                            storageManager,
                                                                                            user.toPlatformAccount(),
                                                                                            activity);
                        refreshFolderOperation.execute(user.toPlatformAccount(), activity);
                    }
                }

                if (!onlyImages || MimeTypeUtil.isImage(ocFile)) {
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
        storageManager.saveVirtuals(contentValues);
    }

    public void showVirtuals(VirtualFolderType type, boolean onlyImages, FileDataStorageManager storageManager) {
        mFiles = storageManager.getVirtualFolderContent(type, onlyImages);

        if (VirtualFolderType.PHOTOS == type) {
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
        protected void publishResults(CharSequence constraint, FilterResults results) {

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

    public void setList(List<SearchResult> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    public void setGridView(boolean bool) {
        gridView = bool;
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
