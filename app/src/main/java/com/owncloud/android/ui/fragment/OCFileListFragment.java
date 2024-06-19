/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2018-2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2020 Joris Bodin <joris.bodin@infomaniak.com>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2016-2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2011-2012 Bartosz Przybylski
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Toast;

import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.android.lib.resources.files.ToggleFileLockRemoteOperation;
import com.nextcloud.android.lib.richWorkspace.RichWorkspaceDirectEditingRemoteOperation;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.device.DeviceInfo;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.documentscan.AppScanOptionalFeature;
import com.nextcloud.client.documentscan.DocumentScanActivity;
import com.nextcloud.client.editimage.EditImageActivity;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.utils.Throttler;
import com.nextcloud.common.NextcloudClient;
import com.nextcloud.ui.fileactions.FileActionsBottomSheet;
import com.nextcloud.utils.EditorUtils;
import com.nextcloud.utils.ShortcutUtil;
import com.nextcloud.utils.extensions.BundleExtensionsKt;
import com.nextcloud.utils.extensions.FileExtensionsKt;
import com.nextcloud.utils.extensions.IntentExtensionsKt;
import com.nextcloud.utils.view.FastScrollUtils;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.VirtualFolderType;
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile;
import com.owncloud.android.lib.common.Creator;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.e2ee.ToggleEncryptionRemoteOperation;
import com.owncloud.android.lib.resources.files.SearchRemoteOperation;
import com.owncloud.android.lib.resources.files.ToggleFavoriteRemoteOperation;
import com.owncloud.android.lib.resources.status.E2EVersion;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener;
import com.owncloud.android.ui.activity.UploadFilesActivity;
import com.owncloud.android.ui.adapter.CommonOCFileListAdapterInterface;
import com.owncloud.android.ui.adapter.OCFileListAdapter;
import com.owncloud.android.ui.dialog.ChooseRichDocumentsTemplateDialogFragment;
import com.owncloud.android.ui.dialog.ChooseTemplateDialogFragment;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.CreateFolderDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.dialog.RenameFileDialogFragment;
import com.owncloud.android.ui.dialog.SetupEncryptionDialogFragment;
import com.owncloud.android.ui.dialog.SyncFileNotEnoughSpaceDialogFragment;
import com.owncloud.android.ui.events.ChangeMenuEvent;
import com.owncloud.android.ui.events.CommentsEvent;
import com.owncloud.android.ui.events.EncryptionEvent;
import com.owncloud.android.ui.events.FavoriteEvent;
import com.owncloud.android.ui.events.FileLockEvent;
import com.owncloud.android.ui.events.SearchEvent;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.ui.preview.PreviewMediaActivity;
import com.owncloud.android.ui.preview.PreviewTextFileFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.EncryptionUtilsV2;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.PermissionUtil;
import com.owncloud.android.utils.theme.ThemeUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import org.apache.commons.httpclient.HttpStatus;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.owncloud.android.datamodel.OCFile.ROOT_PATH;
import static com.owncloud.android.ui.dialog.SetupEncryptionDialogFragment.SETUP_ENCRYPTION_DIALOG_TAG;
import static com.owncloud.android.ui.dialog.SetupEncryptionDialogFragment.SETUP_ENCRYPTION_REQUEST_CODE;
import static com.owncloud.android.ui.fragment.SearchType.FAVORITE_SEARCH;
import static com.owncloud.android.ui.fragment.SearchType.FILE_SEARCH;
import static com.owncloud.android.ui.fragment.SearchType.NO_SEARCH;
import static com.owncloud.android.ui.fragment.SearchType.RECENTLY_MODIFIED_SEARCH;
import static com.owncloud.android.ui.fragment.SearchType.SHARED_FILTER;
import static com.owncloud.android.utils.DisplayUtils.openSortingOrderDialogFragment;

/**
 * A Fragment that lists all files and folders in a given path.
 * TODO refactor to get rid of direct dependency on FileDisplayActivity
 */
public class OCFileListFragment extends ExtendedListFragment implements
    OCFileListFragmentInterface,
    OCFileListBottomSheetActions,
    Injectable {

    protected static final String TAG = OCFileListFragment.class.getSimpleName();

    private static final String MY_PACKAGE = OCFileListFragment.class.getPackage() != null ?
        OCFileListFragment.class.getPackage().getName() : "com.owncloud.android.ui.fragment";

    public final static String ARG_ONLY_FOLDERS_CLICKABLE = MY_PACKAGE + ".ONLY_FOLDERS_CLICKABLE";
    public final static String ARG_FILE_SELECTABLE = MY_PACKAGE + ".FILE_SELECTABLE";
    public final static String ARG_ALLOW_CONTEXTUAL_ACTIONS = MY_PACKAGE + ".ALLOW_CONTEXTUAL";
    public final static String ARG_HIDE_FAB = MY_PACKAGE + ".HIDE_FAB";
    public final static String ARG_HIDE_ITEM_OPTIONS = MY_PACKAGE + ".HIDE_ITEM_OPTIONS";
    public final static String ARG_SEARCH_ONLY_FOLDER = MY_PACKAGE + ".SEARCH_ONLY_FOLDER";
    public final static String ARG_MIMETYPE = MY_PACKAGE + ".MIMETYPE";

    public static final String DOWNLOAD_BEHAVIOUR = "DOWNLOAD_BEHAVIOUR";
    public static final String DOWNLOAD_SEND = "DOWNLOAD_SEND";


    public static final String FOLDER_LAYOUT_LIST = "LIST";
    public static final String FOLDER_LAYOUT_GRID = "GRID";

    public static final String SEARCH_EVENT = "SEARCH_EVENT";

    private static final String KEY_FILE = MY_PACKAGE + ".extra.FILE";

    protected static final String KEY_CURRENT_SEARCH_TYPE = "CURRENT_SEARCH_TYPE";

    private static final String DIALOG_CREATE_FOLDER = "DIALOG_CREATE_FOLDER";
    private static final String DIALOG_CREATE_DOCUMENT = "DIALOG_CREATE_DOCUMENT";
    private static final String DIALOG_BOTTOM_SHEET = "DIALOG_BOTTOM_SHEET";

    private static final int SINGLE_SELECTION = 1;
    private static final int NOT_ENOUGH_SPACE_FRAG_REQUEST_CODE = 2;

    @Inject AppPreferences preferences;
    @Inject UserAccountManager accountManager;
    @Inject ClientFactory clientFactory;
    @Inject Throttler throttler;
    @Inject ThemeUtils themeUtils;
    @Inject ArbitraryDataProvider arbitraryDataProvider;
    @Inject BackgroundJobManager backgroundJobManager;
    @Inject ViewThemeUtils viewThemeUtils;
    @Inject FastScrollUtils fastScrollUtils;
    @Inject EditorUtils editorUtils;
    @Inject ShortcutUtil shortcutUtil;
    @Inject SyncedFolderProvider syncedFolderProvider;
    @Inject AppScanOptionalFeature appScanOptionalFeature;

    protected FileFragment.ContainerActivity mContainerActivity;

    protected OCFile mFile;
    private OCFileListAdapter mAdapter;
    protected boolean mOnlyFoldersClickable;
    protected boolean mFileSelectable;

    protected boolean mHideFab = true;
    protected ActionMode mActiveActionMode;
    protected boolean mIsActionModeNew;
    protected OCFileListFragment.MultiChoiceModeListener mMultiChoiceModeListener;

    protected SearchType currentSearchType;
    protected boolean searchFragment;
    protected SearchEvent searchEvent;
    protected AsyncTask<Void, Void, Boolean> remoteOperationAsyncTask;
    protected String mLimitToMimeType;
    private FloatingActionButton mFabMain;

    @Inject DeviceInfo deviceInfo;

    protected enum MenuItemAddRemove {
        DO_NOTHING,
        REMOVE_SORT,
        REMOVE_GRID_AND_SORT,
        ADD_GRID_AND_SORT_WITH_SEARCH
    }

    protected MenuItemAddRemove menuItemAddRemoveValue = MenuItemAddRemove.ADD_GRID_AND_SORT_WITH_SEARCH;

    private List<MenuItem> mOriginalMenuItems = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mMultiChoiceModeListener = new MultiChoiceModeListener();

        if (savedInstanceState != null) {
            currentSearchType = BundleExtensionsKt.getParcelableArgument(savedInstanceState, KEY_CURRENT_SEARCH_TYPE, SearchType.class);
            searchEvent = BundleExtensionsKt.getParcelableArgument(savedInstanceState, SEARCH_EVENT, SearchEvent.class);
            mFile = BundleExtensionsKt.getParcelableArgument(savedInstanceState, KEY_FILE, OCFile.class);
        }

        searchFragment = currentSearchType != null && isSearchEventSet(searchEvent);
    }

    @Override
    public void onResume() {
        if (getActivity() == null) {
            return;
        }

        Intent intent = getActivity().getIntent();

        if (IntentExtensionsKt.getParcelableArgument(intent, SEARCH_EVENT, SearchEvent.class) != null) {
            searchEvent = IntentExtensionsKt.getParcelableArgument(intent, SEARCH_EVENT, SearchEvent.class);
        }

        if (isSearchEventSet(searchEvent)) {
            handleSearchEvent(searchEvent);
        }

        super.onResume();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log_OC.i(TAG, "onAttach");
        try {
            mContainerActivity = (FileFragment.ContainerActivity) context;
            setTitle();

        } catch (ClassCastException e) {
            throw new IllegalArgumentException(context.toString() + " must implement " +
                                                   FileFragment.ContainerActivity.class.getSimpleName(), e);
        }
        try {
            setOnRefreshListener((OnEnforceableRefreshListener) context);

        } catch (ClassCastException e) {
            throw new IllegalArgumentException(context.toString() + " must implement " +
                                                   OnEnforceableRefreshListener.class.getSimpleName(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log_OC.i(TAG, "onCreateView() start");
        View v = super.onCreateView(inflater, container, savedInstanceState);


        if (savedInstanceState != null &&
            BundleExtensionsKt.getParcelableArgument(savedInstanceState, KEY_CURRENT_SEARCH_TYPE, SearchType.class) != null &&
            BundleExtensionsKt.getParcelableArgument(savedInstanceState, SEARCH_EVENT, SearchEvent.class) != null) {
            searchFragment = true;
            currentSearchType = BundleExtensionsKt.getParcelableArgument(savedInstanceState, KEY_CURRENT_SEARCH_TYPE, SearchType.class);
            searchEvent = BundleExtensionsKt.getParcelableArgument(savedInstanceState, SEARCH_EVENT, SearchEvent.class);
        } else {
            currentSearchType = NO_SEARCH;
        }

        Bundle args = getArguments();
        boolean allowContextualActions = args != null && args.getBoolean(ARG_ALLOW_CONTEXTUAL_ACTIONS, false);
        if (allowContextualActions) {
            setChoiceModeAsMultipleModal(savedInstanceState);
        }

        mFabMain = requireActivity().findViewById(R.id.fab_main);

        if (mFabMain != null) {
            // is not available in FolderPickerActivity
            viewThemeUtils.material.themeFAB(mFabMain);
        }

        Log_OC.i(TAG, "onCreateView() end");
        return v;
    }

    @Override
    public void onDetach() {
        setOnRefreshListener(null);
        mContainerActivity = null;

        if (remoteOperationAsyncTask != null) {
            remoteOperationAsyncTask.cancel(true);
        }
        super.onDetach();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAdapter != null) {
            mAdapter.cancelAllPendingTasks();
        }

        if (getActivity() != null) {
            getActivity().getIntent().removeExtra(OCFileListFragment.SEARCH_EVENT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log_OC.i(TAG, "onActivityCreated() start");

        if (savedInstanceState != null) {
            mFile = BundleExtensionsKt.getParcelableArgument(savedInstanceState, KEY_FILE, OCFile.class);
        }

        Bundle args = getArguments();
        mOnlyFoldersClickable = args != null && args.getBoolean(ARG_ONLY_FOLDERS_CLICKABLE, false);
        mFileSelectable = args != null && args.getBoolean(ARG_FILE_SELECTABLE, false);
        mLimitToMimeType = args != null ? args.getString(ARG_MIMETYPE, "") : "";

        setAdapter(args);

        mHideFab = args != null && args.getBoolean(ARG_HIDE_FAB, false);

        if (mHideFab) {
            setFabVisible(false);
        } else {
            setFabVisible(true);
            registerFabListener();
        }

        if (!searchFragment) {
            // do not touch search event if previously searched
            if (getArguments() == null) {
                searchEvent = null;
            } else {
                searchEvent = BundleExtensionsKt.getParcelableArgument(getArguments(), SEARCH_EVENT, SearchEvent.class);
            }
        }
        prepareCurrentSearch(searchEvent);
        setEmptyView(searchEvent);

        if (mSortButton != null) {
            mSortButton.setOnClickListener(v -> openSortingOrderDialogFragment(requireFragmentManager(),
                                                                               preferences.getSortOrderByFolder(mFile)));
        }

        if (mSwitchGridViewButton != null) {
            mSwitchGridViewButton.setOnClickListener(v -> {
                if (isGridEnabled()) {
                    setListAsPreferred();
                } else {
                    setGridAsPreferred();
                }
                setGridSwitchButton();
            });
        }

        setTitle();

        FragmentActivity fragmentActivity;
        if ((fragmentActivity = getActivity()) != null && fragmentActivity instanceof FileDisplayActivity) {
            FileDisplayActivity fileDisplayActivity = (FileDisplayActivity) fragmentActivity;
            fileDisplayActivity.updateActionBarTitleAndHomeButton(fileDisplayActivity.getCurrentDir());
        }
        listDirectory(MainApp.isOnlyOnDevice(), false);
    }

    protected void setAdapter(Bundle args) {
        boolean hideItemOptions = args != null && args.getBoolean(ARG_HIDE_ITEM_OPTIONS, false);

        mAdapter = new OCFileListAdapter(
            getActivity(),
            accountManager.getUser(),
            preferences,
            syncedFolderProvider,
            mContainerActivity,
            this,
            hideItemOptions,
            isGridViewPreferred(mFile),
            viewThemeUtils
        );

        setRecyclerViewAdapter(mAdapter);

        fastScrollUtils.applyFastScroll(getRecyclerView());
    }

    protected void prepareCurrentSearch(SearchEvent event) {
        if (isSearchEventSet(event)) {

            switch (event.getSearchType()) {
                case FILE_SEARCH:
                    currentSearchType = FILE_SEARCH;
                    break;

                case FAVORITE_SEARCH:
                    currentSearchType = FAVORITE_SEARCH;
                    break;

                case RECENTLY_MODIFIED_SEARCH:
                    currentSearchType = RECENTLY_MODIFIED_SEARCH;
                    break;

                case SHARED_FILTER:
                    currentSearchType = SHARED_FILTER;
                    break;

                default:
                    // do nothing
                    break;
            }

            prepareActionBarItems(event);
        }
    }

    /**
     * register listener on FAB.
     */
    public void registerFabListener() {
        FileActivity activity = (FileActivity) getActivity();

        if (mFabMain != null) {
            // is not available in FolderPickerActivity
            viewThemeUtils.material.themeFAB(mFabMain);
            mFabMain.setOnClickListener(v -> {
                final OCFileListBottomSheetDialog dialog =
                    new OCFileListBottomSheetDialog(activity,
                                                    this,
                                                    deviceInfo,
                                                    accountManager.getUser(),
                                                    getCurrentFile(),
                                                    themeUtils,
                                                    viewThemeUtils,
                                                    editorUtils,
                                                    appScanOptionalFeature);

                dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
                dialog.getBehavior().setSkipCollapsed(true);
                dialog.show();
            });
        }
    }

    @Override
    public void createFolder() {
        CreateFolderDialogFragment.newInstance(mFile)
            .show(getActivity().getSupportFragmentManager(), DIALOG_CREATE_FOLDER);
    }

    @Override
    public void uploadFromApp() {
        Intent action = new Intent(Intent.ACTION_GET_CONTENT);
        action = action.setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
        action.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        getActivity().startActivityForResult(
            Intent.createChooser(action, getString(R.string.upload_chooser_title)),
            FileDisplayActivity.REQUEST_CODE__SELECT_CONTENT_FROM_APPS
                                            );
    }

    @Override
    public void directCameraUpload() {
        FileDisplayActivity fileDisplayActivity = (FileDisplayActivity) getActivity();

        if (fileDisplayActivity == null) {
            DisplayUtils.showSnackMessage(getView(), getString(R.string.error_starting_direct_camera_upload));
            return;
        }

        if (!PermissionUtil.checkSelfPermission(fileDisplayActivity, Manifest.permission.CAMERA)) {
            PermissionUtil.requestCameraPermission(fileDisplayActivity, PermissionUtil.PERMISSIONS_CAMERA);
            return;
        }

        showDirectCameraUploadAlertDialog(fileDisplayActivity);
    }

    private void showDirectCameraUploadAlertDialog(FileDisplayActivity fileDisplayActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(fileDisplayActivity);
        AlertDialog dialog = builder.setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.upload_direct_camera_promt)
            .setNegativeButton(R.string.upload_direct_camera_video, (iface, id) ->
                fileDisplayActivity.getFileOperationsHelper().uploadFromCamera(fileDisplayActivity,FileDisplayActivity.REQUEST_CODE__UPLOAD_FROM_VIDEO_CAMERA, true))
            .setPositiveButton(R.string.upload_direct_camera_photo, (iface, id) ->
                fileDisplayActivity.getFileOperationsHelper().uploadFromCamera(fileDisplayActivity, FileDisplayActivity.REQUEST_CODE__UPLOAD_FROM_CAMERA, false))
            .create();
        dialog.show();
    }

    @Override
    public void scanDocUpload() {
        FileDisplayActivity fileDisplayActivity = (FileDisplayActivity) getActivity();

        final OCFile currentFile = getCurrentFile();
        if (fileDisplayActivity != null && currentFile != null && currentFile.isFolder()) {

            Intent intent = new Intent(requireContext(), DocumentScanActivity.class);
            intent.putExtra(DocumentScanActivity.EXTRA_FOLDER, currentFile.getRemotePath());
            startActivity(intent);
        } else {
            Log.w(TAG, "scanDocUpload: Failed to start doc scanning, fileDisplayActivity=" + fileDisplayActivity +
                ", currentFile=" + currentFile);
            Toast.makeText(getContext(),
                           getString(R.string.error_starting_doc_scan),
                           Toast.LENGTH_SHORT)
                .show();
        }
    }

    @Override
    public void uploadFiles() {
        UploadFilesActivity.startUploadActivityForResult(
            getActivity(),
            ((FileActivity) getActivity()).getUser().orElseThrow(RuntimeException::new),
            FileDisplayActivity.REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM,
            getCurrentFile().isEncrypted());
    }

    @Override
    public void createRichWorkspace() {
        new Thread(() -> {
            RemoteOperationResult result = new RichWorkspaceDirectEditingRemoteOperation(mFile.getRemotePath())
                .execute(accountManager.getUser(), requireContext());

            if (result.isSuccess()) {
                String url = (String) result.getSingleData();
                mContainerActivity.getFileOperationsHelper().openRichWorkspaceWithTextEditor(mFile,
                                                                                             url,
                                                                                             requireContext());
            } else {
                DisplayUtils.showSnackMessage(getView(), R.string.failed_to_start_editor);
            }
        }).start();
    }

    @Override
    public void onShareIconClick(OCFile file) {
        if (file.isFolder()) {
            mContainerActivity.showDetails(file, 1);
        } else {
            throttler.run("shareIconClick", () -> {
                mContainerActivity.getFileOperationsHelper().sendShareFile(file);
            });
        }
    }

    @Override
    public void showShareDetailView(OCFile file) {
        mContainerActivity.showDetails(file, 1);
    }

    @Override
    public void showActivityDetailView(OCFile file) {
        mContainerActivity.showDetails(file, 0);
    }

    @Override
    public void onOverflowIconClicked(OCFile file, View view) {
        final Set<OCFile> checkedFiles = new HashSet<>();
        checkedFiles.add(file);
        openActionsMenu(1, checkedFiles, true);
    }

    public void openActionsMenu(final int filesCount, final Set<OCFile> checkedFiles, final boolean isOverflow) {
        throttler.run("overflowClick", () -> {
            final FragmentManager childFragmentManager = getChildFragmentManager();
            FileActionsBottomSheet.newInstance(filesCount, checkedFiles, isOverflow)
                .setResultListener(childFragmentManager, this, (id) -> {
                    onFileActionChosen(id, checkedFiles);
                })
                .show(childFragmentManager, "actions");
            ;
        });
    }

    @Override
    public void newDocument() {
        ChooseRichDocumentsTemplateDialogFragment.newInstance(mFile,
                                                              ChooseRichDocumentsTemplateDialogFragment.Type.DOCUMENT)
            .show(requireActivity().getSupportFragmentManager(), DIALOG_CREATE_DOCUMENT);
    }

    @Override
    public void newSpreadsheet() {
        ChooseRichDocumentsTemplateDialogFragment.newInstance(mFile,
                                                              ChooseRichDocumentsTemplateDialogFragment.Type.SPREADSHEET)
            .show(requireActivity().getSupportFragmentManager(), DIALOG_CREATE_DOCUMENT);
    }

    @Override
    public void newPresentation() {
        ChooseRichDocumentsTemplateDialogFragment.newInstance(mFile,
                                                              ChooseRichDocumentsTemplateDialogFragment.Type.PRESENTATION)
            .show(requireActivity().getSupportFragmentManager(), DIALOG_CREATE_DOCUMENT);
    }

    @Override
    public void onHeaderClicked() {
        if (!getAdapter().isMultiSelect() && mContainerActivity instanceof FileDisplayActivity) {
            ((FileDisplayActivity) mContainerActivity).startRichWorkspacePreview(getCurrentFile());
        }
    }

    @Override
    public void showTemplate(@NonNull Creator creator, @NonNull String headline) {
        ChooseTemplateDialogFragment.newInstance(mFile, creator, headline).show(requireActivity().getSupportFragmentManager(),
                                                                                DIALOG_CREATE_DOCUMENT);
    }

    /**
     * Handler for multiple selection mode.
     * <p>
     * Manages input from the user when one or more files or folders are selected in the list.
     * <p>
     * Also listens to changes in navigation drawer to hide and recover multiple selection when it's opened and closed.
     */
    private class MultiChoiceModeListener implements AbsListView.MultiChoiceModeListener, DrawerLayout.DrawerListener {

        private static final String KEY_ACTION_MODE_CLOSED_BY_DRAWER = "KILLED_ACTION_MODE";

        /**
         * True when action mode is finished because the drawer was opened
         */
        private boolean mActionModeClosedByDrawer;

        /**
         * Selected items in list when action mode is closed by drawer
         */
        private Set<OCFile> mSelectionWhenActionModeClosedByDrawer = new HashSet<>();

        @Override
        public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
            // nothing to do
        }

        @Override
        public void onDrawerOpened(@NonNull View drawerView) {
            // nothing to do
        }

        /**
         * When the navigation drawer is closed, action mode is recovered in the same state as was when the drawer was
         * (started to be) opened.
         *
         * @param drawerView Navigation drawer just closed.
         */
        @Override
        public void onDrawerClosed(@NonNull View drawerView) {
            if (mActionModeClosedByDrawer && mSelectionWhenActionModeClosedByDrawer.size() > 0) {
                FragmentActivity actionBarActivity = getActivity();
                actionBarActivity.startActionMode(mMultiChoiceModeListener);

                getAdapter().setCheckedItem(mSelectionWhenActionModeClosedByDrawer);

                mActiveActionMode.invalidate();

                mSelectionWhenActionModeClosedByDrawer.clear();
            }
        }

        /**
         * If the action mode is active when the navigation drawer starts to move, the action mode is closed and the
         * selection stored to be recovered when the drawer is closed.
         *
         * @param newState One of STATE_IDLE, STATE_DRAGGING or STATE_SETTLING.
         */
        @Override
        public void onDrawerStateChanged(int newState) {
            if (DrawerLayout.STATE_DRAGGING == newState && mActiveActionMode != null) {
                mSelectionWhenActionModeClosedByDrawer.addAll(((OCFileListAdapter) getRecyclerView().getAdapter())
                                                                  .getCheckedItems());
                mActiveActionMode.finish();
                mActionModeClosedByDrawer = true;
            }
        }

        /**
         * Update action mode bar when an item is selected / unselected in the list
         */
        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            // nothing to do here
        }

        /**
         * Load menu and customize UI when action mode is started.
         */
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActiveActionMode = mode;
            // Determine if actionMode is "new" or not (already affected by item-selection)
            mIsActionModeNew = true;

            // fake menu to be able to use bottom sheet instead
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.custom_menu_placeholder, menu);
            final MenuItem item = menu.findItem(R.id.custom_menu_placeholder_item);
            item.setIcon(viewThemeUtils.platform.colorDrawable(item.getIcon(), ContextCompat.getColor(requireContext(), R.color.white)));
            mode.invalidate();

            //set actionMode color
            viewThemeUtils.platform.colorStatusBar(
                getActivity(),
                ContextCompat.getColor(getContext(), R.color.action_mode_background));

            // hide FAB in multi selection mode
            setFabVisible(false);

            getCommonAdapter().setMultiSelect(true);
            return true;
        }


        /**
         * Updates available action in menu depending on current selection.
         */
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Set<OCFile> checkedFiles = getCommonAdapter().getCheckedItems();
            final int checkedCount = checkedFiles.size();
            String title = getResources().getQuantityString(R.plurals.items_selected_count, checkedCount, checkedCount);
            mode.setTitle(title);

            // Determine if we need to finish the action mode because there are no items selected
            if (checkedCount == 0 && !mIsActionModeNew) {
                exitSelectionMode();
            }

            return true;
        }

        /**
         * Starts the corresponding action when a menu item is tapped by the user.
         */
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            final Set<OCFile> checkedFiles = getCommonAdapter().getCheckedItems();
            if (item.getItemId() == R.id.custom_menu_placeholder_item) {
                openActionsMenu(getCommonAdapter().getFilesCount(), checkedFiles, false);
            }
            return true;
        }

        /**
         * Restores UI.
         */
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActiveActionMode = null;

            // show FAB on multi selection mode exit
            if (!mHideFab && !searchFragment) {
                setFabVisible(true);
            }

            Activity activity = getActivity();
            if (activity != null) {
                viewThemeUtils.platform.resetStatusBar(activity);
            }

            getCommonAdapter().setMultiSelect(false);
            getCommonAdapter().clearCheckedItems();
        }

        public void storeStateIn(Bundle outState) {
            outState.putBoolean(KEY_ACTION_MODE_CLOSED_BY_DRAWER, mActionModeClosedByDrawer);
        }

        public void loadStateFrom(Bundle savedInstanceState) {
            mActionModeClosedByDrawer = savedInstanceState.getBoolean(KEY_ACTION_MODE_CLOSED_BY_DRAWER,
                                                                      mActionModeClosedByDrawer);
        }
    }

    /**
     * Init listener that will handle interactions in multiple selection mode.
     */
    protected void setChoiceModeAsMultipleModal(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mMultiChoiceModeListener.loadStateFrom(savedInstanceState);
        }
        ((FileActivity) getActivity()).addDrawerListener(mMultiChoiceModeListener);
    }

    /**
     * Saves the current listed folder.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        FileExtensionsKt.logFileSize(mFile, TAG);
        outState.putParcelable(KEY_FILE, mFile);
        if (searchFragment) {
            outState.putParcelable(KEY_CURRENT_SEARCH_TYPE, currentSearchType);
            if (isSearchEventSet(searchEvent)) {
                outState.putParcelable(OCFileListFragment.SEARCH_EVENT, searchEvent);
            }
        }
        mMultiChoiceModeListener.storeStateIn(outState);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        if (mOriginalMenuItems.isEmpty()) {
            mOriginalMenuItems.add(menu.findItem(R.id.action_search));
        }

        if (menuItemAddRemoveValue == MenuItemAddRemove.REMOVE_GRID_AND_SORT) {
            menu.removeItem(R.id.action_search);
        }

        updateSortAndGridMenuItems();
    }

    private void updateSortAndGridMenuItems() {
        switch (menuItemAddRemoveValue) {
            case ADD_GRID_AND_SORT_WITH_SEARCH:
                mSwitchGridViewButton.setVisibility(View.VISIBLE);
                mSortButton.setVisibility(View.VISIBLE);
                break;

            case REMOVE_SORT:
                mSortButton.setVisibility(View.GONE);
                break;

            case REMOVE_GRID_AND_SORT:
                mSortButton.setVisibility(View.GONE);
                mSwitchGridViewButton.setVisibility(View.GONE);
                break;

            case DO_NOTHING:
            default:
                Log_OC.v(TAG, "Kept the options menu default structure");
                break;
        }
    }

    /**
     * Call this, when the user presses the up button.
     * <p>
     * Tries to move up the current folder one level. If the parent folder was removed from the database, it continues
     * browsing up until finding an existing folders.
     * <p>
     * return       Count of folder levels browsed up.
     */
    public int onBrowseUp() {
        OCFile parentDir;
        int moveCount = 0;

        if (mFile != null) {
            FileDataStorageManager storageManager = mContainerActivity.getStorageManager();

            String parentPath = null;
            if (mFile.getParentId() != FileDataStorageManager.ROOT_PARENT_ID) {
                parentPath = new File(mFile.getRemotePath()).getParent();
                parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath :
                    parentPath + OCFile.PATH_SEPARATOR;
                parentDir = storageManager.getFileByPath(parentPath);
                moveCount++;
            } else {
                parentDir = storageManager.getFileByPath(ROOT_PATH);
            }
            while (parentDir == null) {
                parentPath = new File(parentPath).getParent();
                parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath :
                    parentPath + OCFile.PATH_SEPARATOR;
                parentDir = storageManager.getFileByPath(parentPath);
                moveCount++;
            }   // exit is granted because storageManager.getFileByPath("/") never returns null
            mFile = parentDir;

            listDirectory(mFile, MainApp.isOnlyOnDevice(), false);

            onRefresh(false);

            // restore index and top position
            restoreIndexAndTopPosition();

        }   // else - should never happen now

        return moveCount;
    }

    /**
     * Will toggle a file selection status from the action mode
     *
     * @param file The concerned OCFile by the selection/deselection
     */
    private void toggleItemToCheckedList(OCFile file) {
        if (getCommonAdapter().isCheckedFile(file)) {
            getCommonAdapter().removeCheckedFile(file);
        } else {
            getCommonAdapter().addCheckedFile(file);
        }
        updateActionModeFile(file);
    }

    /**
     * Will update (invalidate) the action mode adapter/mode to refresh an item selection change
     *
     * @param file The concerned OCFile to refresh in adapter
     */
    private void updateActionModeFile(OCFile file) {
        mIsActionModeNew = false;
        if (mActiveActionMode != null) {
            mActiveActionMode.invalidate();
            getCommonAdapter().notifyItemChanged(file);
        }
    }

    @Override
    public boolean onLongItemClicked(OCFile file) {
        FragmentActivity actionBarActivity = getActivity();
        if (actionBarActivity != null) {
            // Create only once instance of action mode
            if (mActiveActionMode != null) {
                toggleItemToCheckedList(file);
            } else {
                actionBarActivity.startActionMode(mMultiChoiceModeListener);
                getCommonAdapter().addCheckedFile(file);
            }
            updateActionModeFile(file);
        }

        return true;
    }

    private void folderOnItemClick(OCFile file, int position) {
        if (file.isEncrypted()) {
            User user = ((FileActivity) mContainerActivity).getUser().orElseThrow(RuntimeException::new);

            // check if e2e app is enabled
            OCCapability ocCapability = mContainerActivity.getStorageManager()
                .getCapability(user.getAccountName());

            if (ocCapability.getEndToEndEncryption().isFalse() ||
                ocCapability.getEndToEndEncryption().isUnknown()) {
                Snackbar.make(getRecyclerView(), R.string.end_to_end_encryption_not_enabled,
                              Snackbar.LENGTH_LONG).show();
                return;
            }
            // check if keys are stored
            if (FileOperationsHelper.isEndToEndEncryptionSetup(requireContext(), user)) {
                // update state and view of this fragment
                searchFragment = false;
                mHideFab = false;

                if (mContainerActivity instanceof FolderPickerActivity &&
                    ((FolderPickerActivity) mContainerActivity)
                        .isDoNotEnterEncryptedFolder()) {
                    Snackbar.make(getRecyclerView(),
                                  R.string.copy_move_to_encrypted_folder_not_supported,
                                  Snackbar.LENGTH_LONG).show();
                } else {
                    browseToFolder(file, position);
                }
            } else {
                Log_OC.d(TAG, "no public key for " + user.getAccountName());

                FragmentManager fragmentManager = getParentFragmentManager();
                if (fragmentManager.findFragmentByTag(SETUP_ENCRYPTION_DIALOG_TAG) == null) {
                    SetupEncryptionDialogFragment dialog = SetupEncryptionDialogFragment.newInstance(user, position);
                    dialog.setTargetFragment(this, SETUP_ENCRYPTION_REQUEST_CODE);
                    dialog.show(fragmentManager, SETUP_ENCRYPTION_DIALOG_TAG);
                }
            }
        } else {
            // update state and view of this fragment
            searchFragment = false;
            setEmptyListLoadingMessage();
            browseToFolder(file, position);
        }
    }

    private void fileOnItemClick(OCFile file) {
        if (PreviewImageFragment.canBePreviewed(file)) {
            // preview image - it handles the download, if needed
            if (searchFragment) {
                VirtualFolderType type = switch (currentSearchType) {
                    case FAVORITE_SEARCH -> VirtualFolderType.FAVORITE;
                    case GALLERY_SEARCH -> VirtualFolderType.GALLERY;
                    default -> VirtualFolderType.NONE;
                };
                ((FileDisplayActivity) mContainerActivity).startImagePreview(file, type, !file.isDown());
            } else {
                ((FileDisplayActivity) mContainerActivity).startImagePreview(file, !file.isDown());
            }
        } else if (file.isDown() && MimeTypeUtil.isVCard(file)) {
            ((FileDisplayActivity) mContainerActivity).startContactListFragment(file);
        } else if (file.isDown() && MimeTypeUtil.isPDF(file)) {
            ((FileDisplayActivity) mContainerActivity).startPdfPreview(file);
        } else if (PreviewTextFileFragment.canBePreviewed(file)) {
            setFabVisible(false);
            ((FileDisplayActivity) mContainerActivity).startTextPreview(file, false);
        } else if (file.isDown()) {
            if (PreviewMediaActivity.Companion.canBePreviewed(file)) {
                setFabVisible(false);
                ((FileDisplayActivity) mContainerActivity).startMediaPreview(file, 0, true, true, false, true);
            } else {
                mContainerActivity.getFileOperationsHelper().openFile(file);
            }
        } else {
            User account = accountManager.getUser();
            OCCapability capability = mContainerActivity.getStorageManager().getCapability(account.getAccountName());

            if (PreviewMediaActivity.Companion.canBePreviewed(file) && !file.isEncrypted()) {
                setFabVisible(false);
                ((FileDisplayActivity) mContainerActivity).startMediaPreview(file, 0, true, true, true, true);
            } else if (editorUtils.isEditorAvailable(accountManager.getUser(), file.getMimeType()) && !file.isEncrypted()) {
                mContainerActivity.getFileOperationsHelper().openFileWithTextEditor(file, getContext());
            } else if (capability.getRichDocumentsMimeTypeList().contains(file.getMimeType()) &&
                capability.getRichDocumentsDirectEditing().isTrue() && !file.isEncrypted()) {
                mContainerActivity.getFileOperationsHelper().openFileAsRichDocument(file, getContext());
            } else if (mContainerActivity instanceof FileDisplayActivity fileDisplayActivity) {
                fileDisplayActivity.startDownloadForPreview(file, mFile);
            }
        }
    }

    @Override
    @OptIn(markerClass = UnstableApi.class)
    public void onItemClicked(OCFile file) {
        if (mContainerActivity != null && mContainerActivity instanceof FileActivity fileActivity) {
            fileActivity.checkInternetConnection();
        }

        if (getCommonAdapter().isMultiSelect()) {
            toggleItemToCheckedList(file);
        } else {
            if (file == null) {
                Log_OC.d(TAG, "Null object in ListAdapter!");
                return;
            }

            int position = getCommonAdapter().getItemPosition(file);

            if (file.isFolder()) {
                folderOnItemClick(file, position);
            } else if (mFileSelectable) {
                Intent intent = new Intent();
                intent.putExtra(FolderPickerActivity.EXTRA_FILES, file);
                requireActivity().setResult(Activity.RESULT_OK, intent);
                requireActivity().finish();
            } else if (!mOnlyFoldersClickable) {
                fileOnItemClick(file);
            }
        }
    }

    private void browseToFolder(OCFile file, int position) {
        resetSearchIfBrowsingFromFavorites();
        listDirectory(file, MainApp.isOnlyOnDevice(), false);
        // then, notify parent activity to let it update its state and view
        mContainerActivity.onBrowsedDownTo(file);
        // save index and top position
        saveIndexAndTopPosition(position);
    }

    private void resetSearchIfBrowsingFromFavorites() {
        if (currentSearchType == FAVORITE_SEARCH) {
            resetSearchAttributes();
            resetMenuItems();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SETUP_ENCRYPTION_REQUEST_CODE &&
            resultCode == SetupEncryptionDialogFragment.SETUP_ENCRYPTION_RESULT_CODE &&
            data.getBooleanExtra(SetupEncryptionDialogFragment.SUCCESS, false)) {

            int position = data.getIntExtra(SetupEncryptionDialogFragment.ARG_POSITION, -1);
            OCFile file = mAdapter.getItem(position);

            if (file != null) {
                mContainerActivity.getFileOperationsHelper().toggleEncryption(file, true);
                mAdapter.setEncryptionAttributeForItemID(file.getRemoteId(), true);
            }

            // update state and view of this fragment
            searchFragment = false;
            listDirectory(file, MainApp.isOnlyOnDevice(), false);
            // then, notify parent activity to let it update its state and view
            mContainerActivity.onBrowsedDownTo(file);
            // save index and top position
            saveIndexAndTopPosition(position);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Start the appropriate action(s) on the currently selected files given menu selected by the user.
     *
     * @param checkedFiles List of files selected by the user on which the action should be performed
     * @return 'true' if the menu selection started any action, 'false' otherwise.
     */
    public boolean onFileActionChosen(@IdRes final int itemId, Set<OCFile> checkedFiles) {
        if (checkedFiles.isEmpty()) {
            return false;
        }

        if (checkedFiles.size() == SINGLE_SELECTION) {
            /// action only possible on a single file
            OCFile singleFile = checkedFiles.iterator().next();

            if (itemId == R.id.action_send_share_file) {
                mContainerActivity.getFileOperationsHelper().sendShareFile(singleFile);
                return true;
            } else if (itemId == R.id.action_open_file_with) {
                mContainerActivity.getFileOperationsHelper().openFile(singleFile);
                return true;
            } else if (itemId == R.id.action_stream_media) {
                mContainerActivity.getFileOperationsHelper().streamMediaFile(singleFile);
                return true;
            } else if (itemId == R.id.action_edit) {
                // should not be necessary, as menu item is filtered, but better play safe
                if (editorUtils.isEditorAvailable(accountManager.getUser(),
                                                  singleFile.getMimeType())) {
                    mContainerActivity.getFileOperationsHelper().openFileWithTextEditor(singleFile, getContext());
                } else if (EditImageActivity.Companion.canBePreviewed(singleFile)) {
                    ((FileDisplayActivity) mContainerActivity).startImageEditor(singleFile);
                } else {
                    mContainerActivity.getFileOperationsHelper().openFileAsRichDocument(singleFile, getContext());
                }

                return true;
            } else if (itemId == R.id.action_rename_file) {
                RenameFileDialogFragment dialog = RenameFileDialogFragment.newInstance(singleFile, mFile);
                dialog.show(getFragmentManager(), FileDetailFragment.FTAG_RENAME_FILE);
                return true;
            } else if (itemId == R.id.action_see_details) {
                if (mActiveActionMode != null) {
                    mActiveActionMode.finish();
                }

                mContainerActivity.showDetails(singleFile);
                mContainerActivity.showSortListGroup(false);
                return true;
            } else if (itemId == R.id.action_set_as_wallpaper) {
                mContainerActivity.getFileOperationsHelper().setPictureAs(singleFile, getView());
                return true;
            } else if (itemId == R.id.action_encrypted) {
                mContainerActivity.getFileOperationsHelper().toggleEncryption(singleFile, true);
                return true;
            } else if (itemId == R.id.action_unset_encrypted) {
                mContainerActivity.getFileOperationsHelper().toggleEncryption(singleFile, false);
                return true;
            } else if (itemId == R.id.action_lock_file) {
                mContainerActivity.getFileOperationsHelper().toggleFileLock(singleFile, true);
            } else if (itemId == R.id.action_unlock_file) {
                mContainerActivity.getFileOperationsHelper().toggleFileLock(singleFile, false);
            } else if (itemId == R.id.action_pin_to_homescreen) {
                shortcutUtil.addShortcutToHomescreen(singleFile, viewThemeUtils, accountManager.getUser(), syncedFolderProvider);
                return true;
            }
        }

        /// actions possible on a batch of files
        if (itemId == R.id.action_remove_file) {
            RemoveFilesDialogFragment dialog =
                RemoveFilesDialogFragment.newInstance(new ArrayList<>(checkedFiles), mActiveActionMode);
            dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
            return true;
        } else if (itemId == R.id.action_download_file || itemId == R.id.action_sync_file) {
            syncAndCheckFiles(checkedFiles);
            exitSelectionMode();
            return true;
        } else if (itemId == R.id.action_export_file) {
            mContainerActivity.getFileOperationsHelper().exportFiles(checkedFiles,
                                                                     getContext(),
                                                                     getView(),
                                                                     backgroundJobManager);
            exitSelectionMode();
            return true;
        } else if (itemId == R.id.action_cancel_sync) {
            ((FileDisplayActivity) mContainerActivity).cancelTransference(checkedFiles);
            return true;
        } else if (itemId == R.id.action_favorite) {
            mContainerActivity.getFileOperationsHelper().toggleFavoriteFiles(checkedFiles, true);
            return true;
        } else if (itemId == R.id.action_unset_favorite) {
            mContainerActivity.getFileOperationsHelper().toggleFavoriteFiles(checkedFiles, false);
            return true;
        } else if (itemId == R.id.action_move_or_copy) {
            pickFolderForMoveOrCopy(checkedFiles);
            return true;
        } else if (itemId == R.id.action_select_all_action_menu) {
            selectAllFiles(true);
            return true;
        } else if (itemId == R.id.action_deselect_all_action_menu) {
            selectAllFiles(false);
            return true;
        } else if (itemId == R.id.action_send_file) {
            mContainerActivity.getFileOperationsHelper().sendFiles(checkedFiles);
            return true;
        } else if (itemId == R.id.action_lock_file) {
            // TODO call lock API
        }

        return false;
    }

    private void pickFolderForMoveOrCopy(final Set<OCFile> checkedFiles) {
        int requestCode = FileDisplayActivity.REQUEST_CODE__MOVE_OR_COPY_FILES;
        String extraAction = FolderPickerActivity.MOVE_OR_COPY;

        final Intent action = new Intent(getActivity(), FolderPickerActivity.class);
        final ArrayList<String> paths = new ArrayList<>(checkedFiles.size());
        for (OCFile file : checkedFiles) {
            paths.add(file.getRemotePath());
        }
        action.putStringArrayListExtra(FolderPickerActivity.EXTRA_FILE_PATHS, paths);
        action.putExtra(FolderPickerActivity.EXTRA_ACTION, extraAction);
        getActivity().startActivityForResult(action, requestCode);
    }


    /**
     * Use this to query the {@link OCFile} that is currently being displayed by this fragment
     *
     * @return The currently viewed OCFile
     */
    public OCFile getCurrentFile() {
        return mFile;
    }

    /**
     * Calls {@link OCFileListFragment#listDirectory(OCFile, boolean, boolean)} with a null parameter
     */
    public void listDirectory(boolean onlyOnDevice, boolean fromSearch) {
        listDirectory(null, onlyOnDevice, fromSearch);
    }

    public void refreshDirectory() {
        searchFragment = false;

        setFabVisible(true);
        listDirectory(getCurrentFile(), MainApp.isOnlyOnDevice(), false);
    }

    public void listDirectory(OCFile directory, boolean onlyOnDevice, boolean fromSearch) {
        listDirectory(directory, null, onlyOnDevice, fromSearch);
    }

    private OCFile getDirectoryForListDirectory(OCFile directory, FileDataStorageManager storageManager) {
        if (directory == null) {
            if (mFile != null) {
                directory = mFile;
            } else {
                directory = storageManager.getFileByPath(ROOT_PATH);
            }
        }

        // If that's not a directory -> List its parent
        if (!directory.isFolder()) {
            Log_OC.w(TAG, "You see, that is not a directory -> " + directory);
            directory = storageManager.getFileById(directory.getParentId());
        }

        return directory;
    }

    /**
     * Lists the given directory on the view. When the input parameter is null, it will either refresh the last known
     * directory. list the root if there never was a directory.
     *
     * @param directory File to be listed
     */
    public void listDirectory(OCFile directory, OCFile file, boolean onlyOnDevice, boolean fromSearch) {
        if (!searchFragment) {
            FileDataStorageManager storageManager = mContainerActivity.getStorageManager();
            if (storageManager == null) {
                Log_OC.d(TAG, "fileDataStorageManager is null");
                return;
            }

            directory = getDirectoryForListDirectory(directory, storageManager);
            if (directory == null) {
                Log_OC.d(TAG, "directory is null, no files, wait for sync");
                return;
            }

            if (mLimitToMimeType == null) {
                Log_OC.d(TAG, "mLimitToMimeType is null");
                return;
            }

            if (mAdapter == null) {
                Log_OC.d(TAG, "mAdapter is null");
                return;
            }

            mAdapter.swapDirectory(
                accountManager.getUser(),
                directory,
                storageManager,
                onlyOnDevice,
                mLimitToMimeType);

            OCFile previousDirectory = mFile;
            mFile = directory;

            updateLayout();

            if (file != null) {
                mAdapter.setHighlightedItem(file);
                int position = mAdapter.getItemPosition(file);
                if (position != -1) {
                    getRecyclerView().scrollToPosition(position);
                }
            } else if (previousDirectory == null || !previousDirectory.equals(directory)) {
                getRecyclerView().scrollToPosition(0);
            }
        } else if (isSearchEventSet(searchEvent)) {
            handleSearchEvent(searchEvent);
            mRefreshListLayout.setRefreshing(false);
        }
    }

    public List<OCFile> getAdapterFiles() {
        return mAdapter.getFiles();
    }

    public void updateOCFile(@NonNull OCFile file) {
        List<OCFile> mFiles = mAdapter.getFiles();
        int index = mFiles.indexOf(file);
        if (index == -1) {
            Log_OC.d(TAG, "File cannot be found in adapter's files");
            return;
        }

        mFiles.set(index, file);
        mAdapter.notifyItemChanged(file);
    }

    private void updateLayout() {
        // decide grid vs list view
        if (isGridViewPreferred(mFile)) {
            switchToGridView();
        } else {
            switchToListView();
        }

        if (mSortButton != null) {
            mSortButton.setText(DisplayUtils.getSortOrderStringId(preferences.getSortOrderByFolder(mFile)));
        }
        if (mSwitchGridViewButton != null) {
            setGridSwitchButton();
        }

        if (mHideFab) {
            setFabVisible(false);
        } else {
            setFabVisible(true);
            // registerFabListener();
        }

        // FAB
        setFabEnabled(mFile != null && mFile.canWrite());

        invalidateActionMode();
    }

    private void invalidateActionMode() {
        if (mActiveActionMode != null) {
            mActiveActionMode.invalidate();
        }
    }

    public void sortFiles(FileSortOrder sortOrder) {
        mSortButton.setText(DisplayUtils.getSortOrderStringId(sortOrder));
        mAdapter.setSortOrder(mFile, sortOrder);
    }

    /**
     * Determines if user set folder to grid or list view. If folder is not set itself, it finds a parent that is set
     * (at least root is set).
     *
     * @param folder Folder to check or null for root folder
     * @return 'true' is folder should be shown in grid mode, 'false' if list mode is preferred.
     */
    public boolean isGridViewPreferred(@Nullable OCFile folder) {
        return FOLDER_LAYOUT_GRID.equals(preferences.getFolderLayout(folder));
    }

    public void setListAsPreferred() {
        preferences.setFolderLayout(mFile, FOLDER_LAYOUT_LIST);
        switchToListView();
    }

    public void switchToListView() {
        if (isGridEnabled()) {
            switchLayoutManager(false);
        }
    }

    public void setGridAsPreferred() {
        preferences.setFolderLayout(mFile, FOLDER_LAYOUT_GRID);
        switchToGridView();
    }

    public void switchToGridView() {
        if (!isGridEnabled()) {
            switchLayoutManager(true);
        }
    }

    public void switchLayoutManager(boolean grid) {
        int position = 0;

        if (getRecyclerView().getLayoutManager() != null) {
            position = ((LinearLayoutManager) getRecyclerView().getLayoutManager())
                .findFirstCompletelyVisibleItemPosition();
        }

        RecyclerView.LayoutManager layoutManager;
        if (grid) {
            layoutManager = new GridLayoutManager(getContext(), getColumnsCount());
            ((GridLayoutManager) layoutManager).setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (position == getAdapter().getItemCount() - 1 ||
                        position == 0 && getAdapter().shouldShowHeader()) {
                        return ((GridLayoutManager) layoutManager).getSpanCount();
                    } else {
                        return 1;
                    }
                }
            });

        } else {
            layoutManager = new LinearLayoutManager(getContext());
        }

        getRecyclerView().setLayoutManager(layoutManager);
        getRecyclerView().scrollToPosition(position);
        getAdapter().setGridView(grid);
        getRecyclerView().setAdapter(getAdapter());
        getAdapter().notifyDataSetChanged();
    }

    public CommonOCFileListAdapterInterface getCommonAdapter() {
        return mAdapter;
    }

    public OCFileListAdapter getAdapter() {
        return mAdapter;
    }

    protected void setTitle() {
        // set title

        if (getActivity() instanceof FileDisplayActivity && currentSearchType != null) {
            switch (currentSearchType) {
                case FAVORITE_SEARCH:
                    setTitle(R.string.drawer_item_favorites);
                    break;
                case GALLERY_SEARCH:
                    setTitle(R.string.drawer_item_gallery);
                    break;
                case RECENTLY_MODIFIED_SEARCH:
                    setTitle(R.string.drawer_item_recently_modified);
                    break;
                case SHARED_FILTER:
                    setTitle(R.string.drawer_item_shared);
                    break;
                default:
                    setTitle(themeUtils.getDefaultDisplayNameForRootFolder(getContext()), false);
                    break;
            }
        }

    }

    protected void prepareActionBarItems(SearchEvent event) {
        if (event != null) {
            switch (event.getSearchType()) {
                case FAVORITE_SEARCH:
                case RECENTLY_MODIFIED_SEARCH:
                    menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_SORT;
                    break;

                default:
                    // do nothing
                    break;
            }
        }

        if (SearchType.FILE_SEARCH != currentSearchType && getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    protected void setEmptyView(SearchEvent event) {
        if (event != null) {
            switch (event.getSearchType()) {
                case FILE_SEARCH:
                    setEmptyListMessage(SearchType.FILE_SEARCH);
                    break;

                case FAVORITE_SEARCH:
                    setEmptyListMessage(SearchType.FAVORITE_SEARCH);
                    break;

                case RECENTLY_MODIFIED_SEARCH:
                    setEmptyListMessage(SearchType.RECENTLY_MODIFIED_SEARCH);
                    break;

                case SHARED_FILTER:
                    setEmptyListMessage(SearchType.SHARED_FILTER);
                    break;

                default:
                    setEmptyListMessage(SearchType.NO_SEARCH);
                    break;
            }
        } else {
            setEmptyListMessage(SearchType.NO_SEARCH);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ChangeMenuEvent changeMenuEvent) {
        resetSearchAttributes();

        resetMenuItems();
        Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();

            if (activity instanceof FileDisplayActivity) {
                ((FileDisplayActivity) activity).initSyncBroadcastReceiver();
            }

            setTitle(themeUtils.getDefaultDisplayNameForRootFolder(getContext()), false);
            activity.getIntent().removeExtra(OCFileListFragment.SEARCH_EVENT);
        }

        Bundle arguments = getArguments();
        if (arguments != null) {
            arguments.putParcelable(OCFileListFragment.SEARCH_EVENT, null);
        }

        setFabVisible(true);
    }

    private void resetMenuItems() {
        menuItemAddRemoveValue = MenuItemAddRemove.ADD_GRID_AND_SORT_WITH_SEARCH;
        updateSortAndGridMenuItems();
    }

    private void resetSearchAttributes() {
        searchFragment = false;
        searchEvent = null;
        currentSearchType = SearchType.NO_SEARCH;
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(CommentsEvent event) {
        mAdapter.refreshCommentsCount(event.getRemoteId());
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(FavoriteEvent event) {
        try {
            User user = accountManager.getUser();
            OwnCloudClient client = clientFactory.create(user);

            ToggleFavoriteRemoteOperation toggleFavoriteOperation = new ToggleFavoriteRemoteOperation(
                event.getShouldFavorite(), event.getRemotePath());
            RemoteOperationResult remoteOperationResult = toggleFavoriteOperation.execute(client);

            if (remoteOperationResult.isSuccess()) {
                boolean removeFromList = currentSearchType == SearchType.FAVORITE_SEARCH && !event.getShouldFavorite();
                setEmptyListMessage(SearchType.FAVORITE_SEARCH);
                mAdapter.setFavoriteAttributeForItemID(event.getRemotePath(), event.getShouldFavorite(), removeFromList);
            }

        } catch (ClientFactory.CreationException e) {
            Log_OC.e(TAG, "Error processing event", e);
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null) {
            searchEvent = BundleExtensionsKt.getParcelableArgument(savedInstanceState, SEARCH_EVENT, SearchEvent.class);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(final SearchEvent event) {
        handleSearchEvent(event);
    }

    protected void handleSearchEvent(SearchEvent event) {
        if (SearchRemoteOperation.SearchType.PHOTO_SEARCH == event.getSearchType()) {
            return;
        }

        // avoid calling api multiple times if async task is already executing
        if (remoteOperationAsyncTask != null && remoteOperationAsyncTask.getStatus() != AsyncTask.Status.FINISHED) {
            Log_OC.d(TAG, "OCFileListSearchAsyncTask already running skipping new api call for search event: " + searchEvent.getSearchType());
            return;
        }

        prepareCurrentSearch(event);
        searchFragment = true;
        setEmptyListLoadingMessage();
        mAdapter.setData(new ArrayList<>(),
                         SearchType.NO_SEARCH,
                         mContainerActivity.getStorageManager(),
                         mFile,
                         true);

        setFabVisible(false);

        Runnable switchViewsRunnable = () -> {
            if (isGridViewPreferred(mFile) && !isGridEnabled()) {
                switchToGridView();
            } else if (!isGridViewPreferred(mFile) && isGridEnabled()) {
                switchToListView();
            }
        };

        new Handler(Looper.getMainLooper()).post(switchViewsRunnable);

        final User currentUser = accountManager.getUser();

        final RemoteOperation remoteOperation = getSearchRemoteOperation(currentUser, event);

        remoteOperationAsyncTask = new OCFileListSearchAsyncTask(mContainerActivity, this, remoteOperation, currentUser, event);

        remoteOperationAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    protected RemoteOperation getSearchRemoteOperation(final User currentUser, final SearchEvent event) {
        boolean searchOnlyFolders = false;
        if (getArguments() != null && getArguments().getBoolean(ARG_SEARCH_ONLY_FOLDER, false)) {
            searchOnlyFolders = true;
        }

        OCCapability ocCapability = mContainerActivity.getStorageManager()
            .getCapability(currentUser.getAccountName());

        return new SearchRemoteOperation(event.getSearchQuery(),
                                         event.getSearchType(),
                                         searchOnlyFolders,
                                         ocCapability);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(EncryptionEvent event) {
        final User user = accountManager.getUser();

        // check if keys are stored
        String publicKey = arbitraryDataProvider.getValue(user, EncryptionUtils.PUBLIC_KEY);
        String privateKey = arbitraryDataProvider.getValue(user, EncryptionUtils.PRIVATE_KEY);

        FileDataStorageManager storageManager = mContainerActivity.getStorageManager();
        OCFile file = storageManager.getFileByRemoteId(event.getRemoteId());

        if (publicKey.isEmpty() || privateKey.isEmpty()) {
            Log_OC.d(TAG, "no public key for " + user.getAccountName());

            int position = -1;
            if (file != null) {
                position = mAdapter.getItemPosition(file);
            }
            SetupEncryptionDialogFragment dialog = SetupEncryptionDialogFragment.newInstance(user, position);
            dialog.setTargetFragment(this, SETUP_ENCRYPTION_REQUEST_CODE);
            dialog.show(getParentFragmentManager(), SETUP_ENCRYPTION_DIALOG_TAG);
        } else {
            // TODO E2E: if encryption fails, to not set it as encrypted!
            encryptFolder(file,
                          event.getLocalId(),
                          event.getRemoteId(),
                          event.getRemotePath(),
                          event.getShouldBeEncrypted(),
                          publicKey,
                          privateKey,
                          storageManager);
        }
    }

    private void encryptFolder(OCFile folder,
                               long localId,
                               String remoteId,
                               String remotePath,
                               boolean shouldBeEncrypted,
                               String publicKeyString,
                               String privateKeyString,
                               FileDataStorageManager storageManager) {
        try {
            Log_OC.d(TAG, "encrypt folder " + folder.getRemoteId());
            User user = accountManager.getUser();
            OwnCloudClient client = clientFactory.create(user);
            RemoteOperationResult remoteOperationResult = new ToggleEncryptionRemoteOperation(localId,
                                                                                              remotePath,
                                                                                              shouldBeEncrypted)
                .execute(client);

            if (remoteOperationResult.isSuccess()) {
                // lock folder
                String token = EncryptionUtils.lockFolder(folder, client);

                OCCapability ocCapability = mContainerActivity.getStorageManager().getCapability(user.getAccountName());

                if (ocCapability.getEndToEndEncryptionApiVersion() == E2EVersion.V2_0) {
                    // Update metadata
                    Pair<Boolean, DecryptedFolderMetadataFile> metadataPair = EncryptionUtils.retrieveMetadata(folder,
                                                                                                               client,
                                                                                                               privateKeyString,
                                                                                                               publicKeyString,
                                                                                                               storageManager,
                                                                                                               user,
                                                                                                               requireContext(),
                                                                                                               arbitraryDataProvider);

                    boolean metadataExists = metadataPair.first;
                    DecryptedFolderMetadataFile metadata = metadataPair.second;

                    new EncryptionUtilsV2().serializeAndUploadMetadata(folder,
                                                                       metadata,
                                                                       token,
                                                                       client,
                                                                       metadataExists,
                                                                       requireContext(),
                                                                       user,
                                                                       storageManager);

                    // unlock folder
                    EncryptionUtils.unlockFolder(folder, client, token);

                } else if (ocCapability.getEndToEndEncryptionApiVersion() == E2EVersion.V1_0 ||
                    ocCapability.getEndToEndEncryptionApiVersion() == E2EVersion.V1_1 ||
                    ocCapability.getEndToEndEncryptionApiVersion() == E2EVersion.V1_2
                ) {
                    // unlock folder
                    EncryptionUtils.unlockFolderV1(folder, client, token);
                } else if (ocCapability.getEndToEndEncryptionApiVersion() == E2EVersion.UNKNOWN) {
                    throw new IllegalArgumentException("Unknown E2E version");
                }

                mAdapter.setEncryptionAttributeForItemID(remoteId, shouldBeEncrypted);
            } else if (remoteOperationResult.getHttpCode() == HttpStatus.SC_FORBIDDEN) {
                Snackbar.make(getRecyclerView(),
                              R.string.end_to_end_encryption_folder_not_empty,
                              Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(getRecyclerView(),
                              R.string.common_error_unknown,
                              Snackbar.LENGTH_LONG).show();
            }

        } catch (Throwable e) {
            Log_OC.e(TAG, "Error creating encrypted folder", e);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(FileLockEvent event) {
        final User user = accountManager.getUser();

        try {
            new Handler(Looper.getMainLooper()).post(() -> setLoading(true));
            NextcloudClient client = clientFactory.createNextcloudClient(user);
            ToggleFileLockRemoteOperation operation = new ToggleFileLockRemoteOperation(event.getShouldLock(), event.getFilePath());
            RemoteOperationResult<Void> result = operation.execute(client);

            if (result.isSuccess()) {
                // TODO only refresh the modified file?
                new Handler(Looper.getMainLooper()).post(this::onRefresh);
            } else {
                Snackbar.make(getRecyclerView(),
                              R.string.error_file_lock,
                              Snackbar.LENGTH_LONG).show();
            }

        } catch (ClientFactory.CreationException e) {
            Log_OC.e(TAG, "Cannot create client", e);
            Snackbar.make(getRecyclerView(),
                          R.string.error_file_lock,
                          Snackbar.LENGTH_LONG).show();
        } finally {
            new Handler(Looper.getMainLooper()).post(() -> setLoading(false));
        }
    }

    /**
     * Theme default action bar according to provided parameters. Replaces back arrow with hamburger menu icon.
     *
     * @param title string res id of title to be shown in action bar
     */
    protected void setTitle(@StringRes final int title) {
        setTitle(requireContext().getString(title), true);
    }

    /**
     * Theme default action bar according to provided parameters.
     *
     * @param title          title to be shown in action bar
     * @param showBackAsMenu iff true replace back arrow with hamburger menu icon
     */
    protected void setTitle(final String title, Boolean showBackAsMenu) {
        requireActivity().runOnUiThread(() -> {
            if (getActivity() != null) {
                final ActionBar actionBar = ((FileDisplayActivity) getActivity()).getSupportActionBar();
                final Context context = getContext();

                if (actionBar != null && context != null) {
                    viewThemeUtils.files.themeActionBar(context, actionBar, title, showBackAsMenu);
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }


    @Override
    public void onRefresh() {
        if (searchFragment && isSearchEventSet(searchEvent)) {
            handleSearchEvent(searchEvent);

            mRefreshListLayout.setRefreshing(false);
        } else {
            searchFragment = false;
            super.onRefresh();
        }
    }

    public void setSearchFragment(boolean searchFragment) {
        this.searchFragment = searchFragment;
    }

    public boolean isSearchFragment() {
        return searchFragment;
    }

    /**
     * De-/select all elements in the current list view.
     *
     * @param select <code>true</code> to select all, <code>false</code> to deselect all
     */
    public void selectAllFiles(boolean select) {
        OCFileListAdapter ocFileListAdapter = (OCFileListAdapter) getRecyclerView().getAdapter();

        if (select) {
            ocFileListAdapter.addAllFilesToCheckedFiles();
        } else {
            ocFileListAdapter.clearCheckedItems();
        }

        for (int i = 0; i < mAdapter.getItemCount(); i++) {
            mAdapter.notifyItemChanged(i);
        }

        mActiveActionMode.invalidate();
    }

    /**
     * Exits the multi file selection mode.
     */
    public void exitSelectionMode() {
        if (mActiveActionMode != null) {
            mActiveActionMode.finish();
        }
    }

    private boolean isSearchEventSet(SearchEvent event) {
        if (event == null) {
            return false;
        }
        SearchRemoteOperation.SearchType searchType = event.getSearchType();
        return !TextUtils.isEmpty(event.getSearchQuery()) ||
            searchType == SearchRemoteOperation.SearchType.SHARED_FILTER ||
            searchType == SearchRemoteOperation.SearchType.FAVORITE_SEARCH ||
            searchType == SearchRemoteOperation.SearchType.RECENTLY_MODIFIED_SEARCH;
    }

    private void syncAndCheckFiles(Collection<OCFile> files) {
        for (OCFile file : files) {
            // Get the remaining space on device
            long availableSpaceOnDevice = FileOperationsHelper.getAvailableSpaceOnDevice();

            if (FileStorageUtils.checkIfEnoughSpace(file)) {
                mContainerActivity.getFileOperationsHelper().syncFile(file);
            } else {
                showSpaceErrorDialog(file, availableSpaceOnDevice);
            }
        }
    }

    private void showSpaceErrorDialog(OCFile file, long availableSpaceOnDevice) {
        SyncFileNotEnoughSpaceDialogFragment dialog =
            SyncFileNotEnoughSpaceDialogFragment.newInstance(file, availableSpaceOnDevice);
        dialog.setTargetFragment(this, NOT_ENOUGH_SPACE_FRAG_REQUEST_CODE);

        if (getFragmentManager() != null) {
            dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
        }
    }

    @Override
    public boolean isLoading() {
        return false;
    }

    /**
     * Sets the 'visibility' state of the FAB contained in the fragment.
     * <p>
     * When 'false' is set, FAB visibility is set to View.GONE programmatically.
     *
     * @param visible Desired visibility for the FAB.
     */
    public void setFabVisible(final boolean visible) {
        if (mFabMain == null) {
            // is not available in FolderPickerActivity
            return;
        }

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (visible) {
                    mFabMain.show();
                    viewThemeUtils.material.themeFAB(mFabMain);
                } else {
                    mFabMain.hide();
                }

                showFabWithBehavior(visible);
            });
        }
    }

    /**
     * Remove this, if HideBottomViewOnScrollBehavior is fix by Google
     *
     * @param visible flag if FAB should be shown or hidden
     */
    private void showFabWithBehavior(boolean visible) {
        ViewGroup.LayoutParams layoutParams = mFabMain.getLayoutParams();
        if (layoutParams instanceof CoordinatorLayout.LayoutParams) {
            CoordinatorLayout.Behavior coordinatorLayoutBehavior =
                ((CoordinatorLayout.LayoutParams) layoutParams).getBehavior();
            if (coordinatorLayoutBehavior instanceof HideBottomViewOnScrollBehavior) {
                @SuppressWarnings("unchecked")
                HideBottomViewOnScrollBehavior<FloatingActionButton> behavior =
                    (HideBottomViewOnScrollBehavior<FloatingActionButton>) coordinatorLayoutBehavior;
                if (visible) {
                    behavior.slideUp(mFabMain);
                } else {
                    behavior.slideDown(mFabMain);
                }
            }
        }
    }

    /**
     * Sets the 'visibility' state of the FAB contained in the fragment.
     * <p>
     * When 'false' is set, FAB is greyed out
     *
     * @param enabled Desired visibility for the FAB.
     */
    public void setFabEnabled(final boolean enabled) {
        if (mFabMain == null) {
            // is not available in FolderPickerActivity
            return;
        }

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (enabled) {
                    mFabMain.setEnabled(true);
                    viewThemeUtils.material.themeFAB(mFabMain);
                } else {
                    mFabMain.setEnabled(false);
                    viewThemeUtils.material.themeFAB(mFabMain);
                }
            });
        }
    }

    public boolean isEmpty() {
        return mAdapter == null || mAdapter.isEmpty();
    }
}
